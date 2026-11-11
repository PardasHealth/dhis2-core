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
package org.hisp.dhis.artemis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.handler.ssl.SslHandler;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptor;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManagerImpl;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

/** Local Netty broker/certificate fixtures; no deployment broker or data is used. */
@Timeout(30)
class ArtemisExternalTlsIntegrationTest {
  private static final String PASSWORD = "isolated-fixture-only";
  @TempDir static Path storage;

  @BeforeAll
  static void certificates() throws Exception {
    keytool(
        "-genkeypair",
        "-alias",
        "root",
        "-keystore",
        file("ca.p12"),
        "-dname",
        "CN=Test CA",
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-validity",
        "365",
        "-ext",
        "bc=ca:true");
    keytool(
        "-exportcert",
        "-rfc",
        "-alias",
        "root",
        "-keystore",
        file("ca.p12"),
        "-file",
        file("ca.pem"));
    keytool(
        "-importcert",
        "-noprompt",
        "-alias",
        "root",
        "-keystore",
        file("trust.p12"),
        "-file",
        file("ca.pem"));
    serverCertificate("valid", false);
    serverCertificate("expired", true);
    keytool(
        "-genkeypair",
        "-alias",
        "other",
        "-keystore",
        file("other-trust.p12"),
        "-dname",
        "CN=Unrelated CA",
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-validity",
        "365",
        "-ext",
        "bc=ca:true");
  }

  private static void serverCertificate(String name, boolean expired) throws Exception {
    keytool(
        "-genkeypair",
        "-alias",
        "server",
        "-keystore",
        file(name + ".p12"),
        "-dname",
        "CN=localhost",
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-validity",
        "365");
    keytool(
        "-certreq",
        "-alias",
        "server",
        "-keystore",
        file(name + ".p12"),
        "-file",
        file(name + ".csr"));
    var args =
        new ArrayList<>(
            Arrays.asList(
                "-gencert",
                "-rfc",
                "-alias",
                "root",
                "-keystore",
                file("ca.p12"),
                "-infile",
                file(name + ".csr"),
                "-outfile",
                file(name + ".pem"),
                "-ext",
                "SAN=dns:localhost",
                "-ext",
                "EKU=serverAuth",
                "-ext",
                "KU=digitalSignature,keyEncipherment",
                "-validity",
                expired ? "1" : "30"));
    if (expired) args.addAll(Arrays.asList("-startdate", "-2d"));
    keytool(args.toArray(String[]::new));
    keytool(
        "-importcert",
        "-noprompt",
        "-alias",
        "root",
        "-keystore",
        file(name + ".p12"),
        "-file",
        file("ca.pem"));
    keytool(
        "-importcert",
        "-noprompt",
        "-alias",
        "server",
        "-keystore",
        file(name + ".p12"),
        "-file",
        file(name + ".pem"));
  }

  @Test
  void trustedHostExchangesMessagesOverNegotiatedTls() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "localhost", "trust.p12", true);
        var connection = factory.createConnection()) {
      connection.start();
      try (var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
        var queue = session.createQueue("cc-tls-fixture");
        try (var consumer = session.createConsumer(queue);
            var producer = session.createProducer(queue)) {
          producer.send(session.createTextMessage("tls-audit-fixture"));
          var message = consumer.receive(5000);
          assertNotNull(message);
          assertEquals("tls-audit-fixture", ((TextMessage) message).getText());
        }
      }
      var acceptor =
          (NettyAcceptor)
              broker.server.getActiveMQServer().getRemotingService().getAcceptor("fixture");
      var transport = acceptor.getConnections().values().iterator().next();
      var tls = transport.getChannel().pipeline().get(SslHandler.class).engine().getSession();
      assertTrue(Set.of("TLSv1.2", "TLSv1.3").contains(tls.getProtocol()));
      assertFalse(tls.getCipherSuite().contains("NULL"));
    }
  }

  @Test
  void wrongHostnameRejectsTrustedCertificate() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "127.0.0.1", "trust.p12", true)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void untrustedCertificateRejectsConnection() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "localhost", "other-trust.p12", true)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void expiredLeafRejectsConnectionEvenWithTrustedCa() throws Exception {
    try (var broker = broker("expired", true);
        var factory = client(broker, "localhost", "trust.p12", true)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void invalidBrokerCredentialsRejectAfterTls() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "localhost", "trust.p12", true)) {
      factory.setPassword("wrong-fixture-password");
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void tlsClientDoesNotFallBackToPlaintextBroker() throws Exception {
    try (var broker = broker("valid", false);
        var factory = client(broker, "localhost", "trust.p12", true)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void plaintextClientCannotUseTlsOnlyBroker() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "localhost", "trust.p12", false)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  @Test
  void missingTruststoreRejectsConnection() throws Exception {
    try (var broker = broker("valid", true);
        var factory = client(broker, "localhost", "missing.p12", true)) {
      assertThrows(JMSException.class, factory::createConnection);
    }
  }

  private ActiveMQJMSConnectionFactory client(Broker broker, String host, String trust, boolean tls)
      throws Exception {
    Map<ConfigurationKey, String> properties = new HashMap<>();
    properties.put(ConfigurationKey.ARTEMIS_SSL_ENABLED, tls ? "on" : "off");
    if (tls) {
      properties.put(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PATH, file(trust));
      properties.put(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PASSWORD, PASSWORD);
    }
    var data = new ArtemisConfigData();
    data.setMode(ArtemisMode.NATIVE);
    data.setHost(host);
    data.setPort(broker.port);
    data.setUsername("audit-fixture");
    data.setPassword(PASSWORD);
    var config =
        new ArtemisConfig(
            ExternalArtemisConnectionFactoryTest.configuration(properties),
            org.mockito.Mockito.mock(LocationManager.class),
            org.mockito.Mockito.mock(Environment.class));
    var factory = (ActiveMQJMSConnectionFactory) config.jmsConnectionFactory(data);
    factory.setInitialConnectAttempts(0);
    factory.setReconnectAttempts(0);
    factory.setCallTimeout(2000);
    factory
        .getStaticConnectors()[0]
        .getParams()
        .put(TransportConstants.SSL_HANDSHAKE_TIMEOUT, 2000);
    return factory;
  }

  private Broker broker(String certificate, boolean tls) throws Exception {
    var config = new ConfigurationImpl();
    config.setPersistenceEnabled(false);
    config.setSecurityEnabled(true);
    config.setName("isolated-tls-fixture");
    config.setJournalDirectory(file("journal"));
    config.setBindingsDirectory(file("bindings"));
    config.setPagingDirectory(file("paging"));
    config.setLargeMessagesDirectory(file("large"));
    Map<String, Object> transport = new HashMap<>();
    transport.put(TransportConstants.HOST_PROP_NAME, "127.0.0.1");
    transport.put(TransportConstants.PORT_PROP_NAME, 0);
    transport.put(TransportConstants.SSL_ENABLED_PROP_NAME, tls);
    transport.put(TransportConstants.SSL_HANDSHAKE_TIMEOUT, 2000);
    if (tls) {
      transport.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, file(certificate + ".p12"));
      transport.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, PASSWORD);
      transport.put(TransportConstants.KEYSTORE_TYPE_PROP_NAME, "PKCS12");
    }
    config.addAcceptorConfiguration(
        new TransportConfiguration(NettyAcceptorFactory.class.getName(), transport, "fixture"));
    var security = new SecurityConfiguration();
    security.addUser("audit-fixture", PASSWORD);
    security.addRole("audit-fixture", "audit");
    var role = new Role();
    role.setName("audit");
    role.setSend(true);
    role.setConsume(true);
    role.setCreateAddress(true);
    role.setCreateNonDurableQueue(true);
    role.setCreateDurableQueue(true);
    role.setDeleteNonDurableQueue(true);
    config.putSecurityRoles("#", Set.of(role));
    var server =
        new EmbeddedActiveMQ()
            .setConfiguration(config)
            .setSecurityManager(new ActiveMQSecurityManagerImpl(security));
    try {
      server.start();
      int port =
          ((NettyAcceptor) server.getActiveMQServer().getRemotingService().getAcceptor("fixture"))
              .getActualPort();
      return new Broker(server, port);
    } catch (Exception e) {
      server.stop();
      throw e;
    }
  }

  private record Broker(EmbeddedActiveMQ server, int port) implements AutoCloseable {
    @Override
    public void close() throws Exception {
      server.stop();
    }
  }

  private static String file(String name) {
    return storage.resolve(name).toString();
  }

  private static void keytool(String... args) throws Exception {
    var command = new ArrayList<String>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "keytool").toString());
    command.addAll(Arrays.asList(args));
    command.addAll(Arrays.asList("-storetype", "PKCS12", "-storepass", PASSWORD));
    var process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output =
        new String(
            process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(process.waitFor(30, TimeUnit.SECONDS), "keytool timeout");
    assertEquals(0, process.exitValue(), output);
  }
}
