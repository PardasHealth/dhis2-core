/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.audit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.artemis.config.ArtemisConfig;
import org.hisp.dhis.artemis.config.ArtemisConfigData;
import org.hisp.dhis.artemis.config.NameDestinationResolver;
import org.hisp.dhis.audit.consumers.MetadataAuditConsumer;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Real broker/consumer/PostgreSQL checks isolated from the application test database. */
@Tag("integration")
class AuditStorageDeliveryRecoveryTest {
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("imresamu/postgis:16-3.5-alpine")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("cc_audit")
          .withUsername("cc_admin")
          .withPassword("cc-isolated-test-only")
          .withTmpFs(Map.of("/var/lib/postgresql/data", "rw,size=512m"));
  private static final ObjectMapper MAPPER =
      JacksonObjectMapperConfig.configureMapper(new ObjectMapper());

  private JdbcTemplate admin;
  private JdbcTemplate inspection;
  private DriverManagerDataSource writerDataSource;
  private DefaultAuditService writerService;
  private String schema;
  private String writerRole;

  @BeforeAll
  static void startDatabase() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopDatabase() {
    POSTGRES.stop();
  }

  @BeforeEach
  void createRestrictedWriterAndAuditTable() throws Exception {
    admin = new JdbcTemplate(dataSource(POSTGRES.getUsername(), POSTGRES.getPassword(), "public"));
    String suffix = UUID.randomUUID().toString().replace("-", "");
    schema = "cc_audit_" + suffix;
    writerRole = "cc_writer_" + suffix;
    String password = UUID.randomUUID().toString();
    admin.execute("CREATE SCHEMA " + schema);
    admin.execute("CREATE ROLE " + writerRole + " LOGIN PASSWORD '" + password + "'");
    inspection =
        new JdbcTemplate(dataSource(POSTGRES.getUsername(), POSTGRES.getPassword(), schema));
    // Exercise the repository's real table definition, not a mock persistence schema.
    ClassPathResource migration =
        new ClassPathResource("org/hisp/dhis/db/migration/2.34/V2_34_8__Create_Audit_Table.sql");
    try (var sql = migration.getInputStream()) {
      inspection.execute(new String(sql.readAllBytes(), StandardCharsets.UTF_8));
    }
    admin.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + writerRole);
    admin.execute("GRANT SELECT, INSERT ON " + schema + ".audit TO " + writerRole);
    admin.execute("GRANT USAGE ON ALL SEQUENCES IN SCHEMA " + schema + " TO " + writerRole);
    writerDataSource = dataSource(writerRole, password, schema);
    writerService = new DefaultAuditService(repository(writerDataSource));
  }

  @AfterEach
  void removeRestrictedWriterAndSchema() {
    try {
      if (schema != null) admin.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
    } finally {
      if (writerRole != null) admin.execute("DROP ROLE IF EXISTS " + writerRole);
    }
  }

  @Test
  void transientInsertFailureRollsBackReceiveAndPersistsAfterRecovery() throws Exception {
    writerService.addAudit(audit("earlier")); // A separate, autocommitted connection.
    try (BrokerHarness broker = new BrokerHarness()) {
      broker.start();
      revokeInsert();
      broker.publish(payload("pending"));
      await().atMost(10, SECONDS).until(() -> !broker.failures.isEmpty());
      assertInstanceOf(DataAccessException.class, broker.failures.get(0).getCause());
      assertEquals(1, storedCount());
      assertEquals(0, storedCount("pending"));

      grantInsert();
      await().atMost(10, SECONDS).untilAsserted(() -> assertEquals(2, storedCount()));

      assertEquals(1, storedCount("earlier"));
      assertEquals(1, storedCount("pending"));
      assertEquals(
          "{}",
          writerService
              .getAudits(AuditQuery.builder().uid(Set.of("pending")).build())
              .get(0)
              .getData());
      assertTrue(broker.deliveryCounts.size() >= 2, "The failed receive must be redelivered");
      assertTrue(broker.deliveryCounts.contains(2));
      assertNull(broker.receiveDeadLetter(300));
    }
  }

  @Test
  void sustainedInsertFailureReachesDeadLetterQueueAndCanBeReplayed() throws Exception {
    writerService.addAudit(audit("earlier"));
    try (BrokerHarness broker = new BrokerHarness()) {
      broker.start();
      revokeInsert();
      String payload = payload("pending");
      broker.publish(payload);

      TextMessage deadLetter = broker.receiveDeadLetter(15_000);
      assertNotNull(deadLetter, "Repeated database failures must not acknowledge the audit away");
      assertEquals(payload, deadLetter.getText());
      assertEquals(List.of(1, 2, 3, 4, 5), broker.deliveryCounts);
      await().atMost(5, SECONDS).until(() -> broker.failures.size() == 5);
      assertEquals(5, broker.failures.size());
      broker.failures.forEach(
          failure -> assertInstanceOf(DataAccessException.class, failure.getCause()));
      assertEquals(1, storedCount());
      assertEquals(0, storedCount("pending"));

      grantInsert();
      broker.publish(deadLetter.getText());
      await().atMost(10, SECONDS).untilAsserted(() -> assertEquals(2, storedCount()));

      assertEquals(1, storedCount("earlier"));
      assertEquals(1, storedCount("pending"));
      assertNull(broker.receiveDeadLetter(300), "Replayed event must leave the DLQ");
    }
  }

  @Test
  void terminatedDatabaseConnectionFailsReadsAndWritesAndFreshConnectionRecovers()
      throws Exception {
    try (java.sql.Connection connection = writerDataSource.getConnection()) {
      SingleConnectionDataSource fixedConnection = new SingleConnectionDataSource(connection, true);
      JdbcTemplate writer = new JdbcTemplate(fixedConnection);
      DefaultAuditService interrupted = new DefaultAuditService(repository(fixedConnection));
      interrupted.addAudit(audit("earlier"));
      Integer backend = writer.queryForObject("SELECT pg_backend_pid()", Integer.class);
      assertEquals(
          Boolean.TRUE,
          admin.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, backend));

      assertThrows(DataAccessException.class, () -> interrupted.addAudit(audit("pending")));
      assertThrows(
          DataAccessException.class, () -> interrupted.countAudits(AuditQuery.builder().build()));
      assertThrows(
          DataAccessException.class, () -> interrupted.getAudits(AuditQuery.builder().build()));
    }

    // DriverManagerDataSource acquires a fresh authenticated connection per operation.
    assertEquals(1, writerService.countAudits(AuditQuery.builder().build()));
    assertEquals(1, storedCount("earlier"));
    writerService.addAudit(audit("pending"));
    assertEquals(2, writerService.countAudits(AuditQuery.builder().build()));
    assertEquals(1, storedCount("pending"));
  }

  @Test
  void appendOnlyDatabaseRoleCannotModifyOrDeleteAndDeniedReadsAreVisible() {
    Audit earlier = audit("earlier");
    earlier.setId(writerService.addAudit(earlier));
    JdbcAuditRepository writerRepository = repository(writerDataSource);
    JdbcTemplate writer = new JdbcTemplate(writerDataSource);

    assertThrows(DataAccessException.class, () -> writerRepository.delete(earlier));
    assertThrows(
        DataAccessException.class,
        () -> writer.update("UPDATE audit SET createdBy = ? WHERE uid = ?", "changed", "earlier"));
    assertEquals(1, storedCount("earlier"));
    assertEquals(
        "cc-test-user",
        writerService.getAudits(AuditQuery.builder().build()).get(0).getCreatedBy());

    admin.execute("REVOKE SELECT ON " + schema + ".audit FROM " + writerRole);
    assertThrows(
        DataAccessException.class, () -> writerService.countAudits(AuditQuery.builder().build()));
    assertThrows(
        DataAccessException.class, () -> writerService.getAudits(AuditQuery.builder().build()));
    admin.execute("GRANT SELECT ON " + schema + ".audit TO " + writerRole);
    assertEquals(1, writerService.countAudits(AuditQuery.builder().build()));
  }

  private void revokeInsert() {
    admin.execute("REVOKE INSERT ON " + schema + ".audit FROM " + writerRole);
  }

  private void grantInsert() {
    admin.execute("GRANT INSERT ON " + schema + ".audit TO " + writerRole);
  }

  private int storedCount() {
    return inspection.queryForObject("SELECT COUNT(*) FROM audit", Integer.class);
  }

  private int storedCount(String uid) {
    return inspection.queryForObject(
        "SELECT COUNT(*) FROM audit WHERE uid = ?", Integer.class, uid);
  }

  private static JdbcAuditRepository repository(javax.sql.DataSource dataSource) {
    return new JdbcAuditRepository(new JdbcTemplate(dataSource), MAPPER);
  }

  private static DriverManagerDataSource dataSource(
      String username, String password, String schema) {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(POSTGRES.getJdbcUrl(), username, password);
    Properties properties = new Properties();
    properties.setProperty("currentSchema", schema);
    properties.setProperty("connectTimeout", "3");
    properties.setProperty("socketTimeout", "3");
    dataSource.setConnectionProperties(properties);
    return dataSource;
  }

  private static Audit audit(String uid) {
    return Audit.builder()
        .auditType(AuditType.CREATE)
        .auditScope(AuditScope.METADATA)
        .createdAt(LocalDateTime.of(2026, 11, 3, 12, 0))
        .createdBy("cc-test-user")
        .klass("cc.fixture.AuditedObject")
        .uid(uid)
        .data("{}")
        .build();
  }

  private static String payload(String uid) throws Exception {
    Audit audit = audit(uid);
    return MAPPER.writeValueAsString(
        org.hisp.dhis.artemis.audit.Audit.builder()
            .auditType(audit.getAuditType())
            .auditScope(audit.getAuditScope())
            .createdAt(audit.getCreatedAt())
            .createdBy(audit.getCreatedBy())
            .klass(audit.getKlass())
            .uid(audit.getUid())
            .data(audit.getData())
            .build());
  }

  private class BrokerHarness implements AutoCloseable {
    private EmbeddedActiveMQ broker;
    private ActiveMQJMSConnectionFactory connectionFactory;
    private DefaultMessageListenerContainer listener;
    private JmsTemplate producer;
    private final List<Integer> deliveryCounts = new CopyOnWriteArrayList<>();
    private final List<Throwable> failures = new CopyOnWriteArrayList<>();

    void start() throws Exception {
      DhisConfigurationProvider configuration = mock(DhisConfigurationProvider.class);
      when(configuration.isEnabled(ConfigurationKey.AUDIT_DATABASE)).thenReturn(true);
      ArtemisConfig artemis =
          new ArtemisConfig(configuration, mock(LocationManager.class), new StandardEnvironment());
      ArtemisConfigData settings = new ArtemisConfigData();
      settings.getEmbedded().setPersistence(false).setSecurity(false);
      broker = artemis.createEmbeddedServer(settings);
      int serverId = ThreadLocalRandom.current().nextInt(10_000, 1_000_000);
      broker.getConfiguration().getAcceptorConfigurations().clear();
      broker.getConfiguration().addAcceptorConfiguration("cc-in-vm", "vm://" + serverId);
      broker.start();
      connectionFactory = new ActiveMQJMSConnectionFactory("vm://" + serverId);
      NameDestinationResolver resolver = new NameDestinationResolver();
      producer = artemis.jmsTopicTemplate(connectionFactory, resolver);
      MetadataAuditConsumer consumer =
          new MetadataAuditConsumer(writerService, MAPPER, configuration);
      SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
      endpoint.setId("cc-storage-recovery");
      endpoint.setDestination(Topics.METADATA_TOPIC_NAME);
      endpoint.setSubscription("cc-storage-recovery");
      endpoint.setMessageListener(
          message -> {
            try {
              deliveryCounts.add(message.getIntProperty("JMSXDeliveryCount"));
            } catch (jakarta.jms.JMSException ex) {
              throw new IllegalStateException(ex);
            }
            consumer.consume((TextMessage) message);
          });
      listener =
          artemis
              .jmsListenerContainerFactory(connectionFactory, resolver)
              .createListenerContainer(endpoint);
      listener.setReceiveTimeout(200);
      listener.setErrorHandler(failures::add);
      listener.afterPropertiesSet();
      listener.start();
      await().atMost(10, SECONDS).until(() -> listener.getActiveConsumerCount() == 1);
      await()
          .atMost(10, SECONDS)
          .until(
              () ->
                  broker
                          .getActiveMQServer()
                          .bindingQuery(SimpleString.toSimpleString(Topics.METADATA_TOPIC_NAME))
                          .getQueueNames()
                          .size()
                      == 1);
    }

    void publish(String payload) {
      producer.send(Topics.METADATA_TOPIC_NAME, session -> session.createTextMessage(payload));
    }

    TextMessage receiveDeadLetter(long timeout) throws Exception {
      try (Connection connection = connectionFactory.createConnection()) {
        connection.start();
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createQueue("DLQ"))) {
          return (TextMessage) consumer.receive(timeout);
        }
      }
    }

    @Override
    public void close() throws Exception {
      try {
        if (listener != null) listener.shutdown();
      } finally {
        try {
          if (connectionFactory != null) connectionFactory.close();
        } finally {
          if (broker != null) broker.stop();
        }
      }
    }
  }
}
