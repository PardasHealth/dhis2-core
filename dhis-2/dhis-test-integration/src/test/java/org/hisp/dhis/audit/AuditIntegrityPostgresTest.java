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

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
class AuditIntegrityPostgresTest {
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse(
                      "postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("audit_integrity_test")
          .withUsername("isolated_test")
          .withPassword("isolated-test-only")
          .withTmpFs(Map.of("/var/lib/postgresql/data", "rw,size=512m"));
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
  private static final String ORIGINAL =
      "org/hisp/dhis/db/migration/2.34/V2_34_8__Create_Audit_Table.sql";
  private static final String INTEGRITY =
      "org/hisp/dhis/db/migration/pardas/R__PardasHealth_audit_integrity.sql";
  private JdbcTemplate jdbc;
  private JdbcAuditRepository repository;
  private Logger logger;
  private org.apache.logging.log4j.core.config.LoggerConfig originalConfig;
  private Capture capture;

  @BeforeAll
  static void startDatabase() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopDatabase() {
    POSTGRES.stop();
  }

  @BeforeEach
  void prepareSchemaAndAlertCapture() throws Exception {
    var source =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(source);
    jdbc.execute("drop table if exists audit cascade");
    migration(ORIGINAL);
    migration(INTEGRITY);
    migration(INTEGRITY); // Repeatable migration must be idempotent.
    repository =
        new JdbcAuditRepository(jdbc, new ObjectMapper(), new AuditIntegrityGuard(true, KEY));
    logger = (Logger) LogManager.getLogger(AuditIntegrityGuard.class);
    var configuration = logger.getContext().getConfiguration();
    originalConfig = configuration.getLoggers().get(logger.getName());
    var isolated =
        new org.apache.logging.log4j.core.config.LoggerConfig(logger.getName(), Level.ERROR, false);
    capture = new Capture();
    capture.start();
    isolated.addAppender(capture, null, null);
    configuration.addLogger(logger.getName(), isolated);
    logger.getContext().updateLoggers();
  }

  @AfterEach
  void restoreLogger() {
    if (logger == null) return;
    var configuration = logger.getContext().getConfiguration();
    configuration.removeLogger(logger.getName());
    if (originalConfig != null) configuration.addLogger(logger.getName(), originalConfig);
    logger.getContext().updateLoggers();
    capture.stop();
  }

  @Test
  void authenticatedRecordsRoundTripWithMicrosecondStoragePrecision() {
    Audit input = audit("Protected01");
    long id = repository.save(input);
    String tag =
        jdbc.queryForObject("select integritytag from audit where auditid = ?", String.class, id);
    assertNotNull(tag);
    assertTrue(tag.matches("v1:[0-9a-f]{64}"));
    var stored = repository.query(all()).get(0);
    assertEquals(id, stored.getId());
    assertEquals(input.getCreatedAt().truncatedTo(ChronoUnit.MICROS), stored.getCreatedAt());
    assertEquals(input.getData(), stored.getData());
    assertEquals(1, repository.count(all()));
    assertTrue(capture.messages.isEmpty());
  }

  @Test
  void batchWritesAuthenticateEveryGeneratedId() {
    repository.save(List.of(audit("Protected01"), audit("Protected02")));
    assertEquals(
        2, jdbc.queryForObject("select count(distinct integritytag) from audit", Integer.class));
    assertEquals(2, repository.query(all()).size());
    assertEquals(2, repository.count(all()));
    assertTrue(capture.messages.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "auditid = auditid + 100",
        "audittype = 'UPDATE'",
        "auditscope = 'TRACKER'",
        "createdat = createdat + interval '1 second'",
        "createdby = 'changed'",
        "klass = 'Other'",
        "uid = 'Changed0001'",
        "code = 'changed'",
        "attributes = '{\"deep\":{\"n\":1.00000000000000000002,\"b\":\"value\"}}'::jsonb",
        "data = decode('010203', 'hex')",
        "integritytag = null",
        "integritytag = 'bad'"
      })
  void alteredStoredFieldsAlertAndBlockBothReadAndCount(String assignment) {
    repository.save(audit("Protected01"));
    jdbc.update("update audit set " + assignment);
    assertBlocked(repository);
  }

  @Test
  void recompressingAReplacementPayloadCannotForgeIntegrity() throws Exception {
    repository.save(audit("Protected01"));
    var bytes = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(bytes)) {
      gzip.write("{\"protected\":\"forged\"}".getBytes(StandardCharsets.UTF_8));
    }
    jdbc.update("update audit set data = ?", bytes.toByteArray());
    assertBlocked(repository);
  }

  @Test
  void movingAValidTagToAnotherRecordCannotAuthenticateIt() {
    long first = repository.save(audit("Protected01"));
    long second = repository.save(audit("Protected02"));
    jdbc.update(
        "update audit set integritytag = (select integritytag from audit where auditid = ?) where auditid = ?",
        second,
        first);
    assertBlocked(repository);
  }

  @Test
  void postgresJsonbNormalizationDoesNotProduceFalseAlerts() {
    Audit input = audit("Protected01");
    var attributes = new AuditAttributes();
    attributes.put("b", "value");
    attributes.put("a", new BigDecimal("1E+3"));
    input.setAttributes(attributes);
    repository.save(input);
    jdbc.update("update audit set attributes = '{\"a\":1000,\"b\":\"value\"}'::jsonb");
    assertEquals(1, repository.query(all()).size());
    assertEquals(1, repository.count(all()));
    assertTrue(capture.messages.isEmpty());
  }

  @Test
  void changedNumericScaleUsedByAttributeFiltersIsNotSilentlyAccepted() {
    Audit input = audit("Protected01");
    var attributes = new AuditAttributes();
    attributes.put("n", 1);
    input.setAttributes(attributes);
    repository.save(input);
    jdbc.update("update audit set attributes = '{\"n\":1.0}'::jsonb");
    assertBlocked(repository);
  }

  @Test
  void legacyHistoryIsNotSilentlySignedByMigration() throws Exception {
    jdbc.execute("alter table audit drop column integritytag");
    var legacy = new JdbcAuditRepository(jdbc, new ObjectMapper());
    legacy.save(audit("Legacy00001"));
    migration(INTEGRITY);
    migration(INTEGRITY);
    assertNull(jdbc.queryForObject("select integritytag from audit", String.class));
    assertEquals(1, legacy.query(all()).size());
    assertEquals(1, legacy.count(all()));
    assertBlocked(repository);
  }

  @Test
  void defaultLegacyModeStillReadsAndWritesWithoutEnforcement() {
    var legacy = new JdbcAuditRepository(jdbc, new ObjectMapper());
    legacy.save(audit("Legacy00001"));
    assertNull(jdbc.queryForObject("select integritytag from audit", String.class));
    assertEquals(1, legacy.query(all()).size());
    assertEquals(1, legacy.count(all()));
    assertTrue(capture.messages.isEmpty());
  }

  @Test
  void anotherValidKeyCannotReadExistingSignedHistory() {
    repository.save(audit("Protected01"));
    byte[] key = new byte[32];
    key[0] = 1;
    var other =
        new JdbcAuditRepository(
            jdbc,
            new ObjectMapper(),
            new AuditIntegrityGuard(true, Base64.getEncoder().encodeToString(key)));
    assertBlocked(other);
  }

  private void assertBlocked(JdbcAuditRepository reader) {
    assertThrows(AuditIntegrityViolationException.class, () -> reader.query(all()));
    assertThrows(AuditIntegrityViolationException.class, () -> reader.count(all()));
    assertEquals(2, capture.messages.size());
    for (String message : capture.messages) {
      assertTrue(message.contains("AUDIT_INTEGRITY_FAILURE"));
      assertTrue(message.contains("action=BLOCK_READ"));
      assertTrue(message.contains("recordId="));
      assertFalse(message.contains(KEY));
      assertFalse(message.contains("patient-sentinel"));
      assertFalse(message.contains("protected-user"));
      assertFalse(message.contains("v1:"));
    }
  }

  private void migration(String path) throws Exception {
    try (var stream = new ClassPathResource(path).getInputStream()) {
      jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  private Audit audit(String uid) {
    var attributes = new AuditAttributes();
    attributes.put("deep", Map.of("n", new BigDecimal("1.00000000000000000001"), "b", "value"));
    return Audit.builder()
        .auditType(AuditType.CREATE)
        .auditScope(AuditScope.METADATA)
        .createdAt(LocalDateTime.of(2026, 11, 18, 12, 34, 56, 123456789))
        .createdBy("protected-user")
        .klass("DataElement")
        .uid(uid)
        .code("audit-code")
        .attributes(attributes)
        .data("{\"protected\":\"patient-sentinel\"}")
        .build();
  }

  private static AuditQuery all() {
    return AuditQuery.builder().build();
  }

  private static class Capture extends AbstractAppender {
    private final List<String> messages = new ArrayList<>();

    Capture() {
      super("afta-integrity-capture", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      assertEquals(Level.ERROR, event.getLevel());
      messages.add(event.getMessage().getFormattedMessage());
    }
  }
}
