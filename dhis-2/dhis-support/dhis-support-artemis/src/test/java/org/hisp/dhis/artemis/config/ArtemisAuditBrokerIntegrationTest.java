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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.nio.file.Path;
import java.nio.file.Path;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.hisp.dhis.artemis.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDir;

class ArtemisAuditBrokerIntegrationTest {
  @Test
  void failedAuditDeliveryIsRetriedAndThenDeadLettered() throws Exception {
    ConfigurationImpl config = new ConfigurationImpl();
    config.setSecurityEnabled(false);
    config.setPersistenceEnabled(false);
    config.addAcceptorConfiguration("in-vm", "vm://0");
    ArtemisConfig.configureAuditRedelivery(config);
    config.addAddressConfiguration(
        new CoreAddressConfiguration()
            .setName("DLQ")
            .addRoutingType(RoutingType.ANYCAST)
            .addQueueConfiguration(
                new CoreQueueConfiguration().setName("DLQ").setRoutingType(RoutingType.ANYCAST)));

    EmbeddedActiveMQ broker = new EmbeddedActiveMQ();
    broker.setConfiguration(config);
    broker.start();
    try (ActiveMQJMSConnectionFactory factory = new ActiveMQJMSConnectionFactory("vm://0");
        Connection connection = factory.createConnection()) {
      connection.start();
      Topic auditTopic;
      try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
        auditTopic = session.createTopic(Topics.API_TOPIC_NAME);
        try (MessageConsumer consumer =
                session.createSharedDurableConsumer(auditTopic, "cc-audit");
            MessageProducer producer = session.createProducer(auditTopic)) {
          producer.send(session.createTextMessage("audit-persistence-failure"));
          session.commit();

          for (int attempt = 1; attempt <= 5; attempt++) {
            Message received = consumer.receive(5000);
            assertNotNull(received, "Audit delivery attempt " + attempt);
            assertEquals("audit-persistence-failure", ((TextMessage) received).getText());
            session.rollback();
          }
          assertNull(consumer.receive(1500), "Poison audit event must leave the subscription");
        }
      }

      try (Session inspection = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
          MessageConsumer deadLetters = inspection.createConsumer(inspection.createQueue("DLQ"))) {
        Message deadLetter = deadLetters.receive(5000);
        assertNotNull(deadLetter, "Failed audit message must reach the dead-letter queue");
        assertEquals("audit-persistence-failure", ((TextMessage) deadLetter).getText());
      }
    } finally {
      broker.stop();
    }
  }

  @Test
  void durableAuditMessageSurvivesBrokerRestart(@TempDir Path storage) throws Exception {
    ConfigurationImpl config = new ConfigurationImpl();
    config.setSecurityEnabled(false);
    config.setPersistenceEnabled(true);
    config.setJournalDirectory(storage.resolve("journal").toString());
    config.setBindingsDirectory(storage.resolve("bindings").toString());
    config.setPagingDirectory(storage.resolve("paging").toString());
    config.setLargeMessagesDirectory(storage.resolve("large-messages").toString());
    config.addAcceptorConfiguration("in-vm", "vm://0");
    ArtemisConfig.configureAuditRedelivery(config);

    EmbeddedActiveMQ broker = new EmbeddedActiveMQ();
    broker.setConfiguration(config);
    broker.start();
    try {
      try (ActiveMQJMSConnectionFactory factory = new ActiveMQJMSConnectionFactory("vm://0");
          Connection connection = factory.createConnection()) {
        connection.start();
        try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
          Topic topic = session.createTopic(Topics.API_TOPIC_NAME);
          try (MessageConsumer subscription =
                  session.createSharedDurableConsumer(topic, "cc-restart");
              MessageProducer producer = session.createProducer(topic)) {
            producer.setDeliveryMode(jakarta.jms.DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage("persistent-audit-event"));
            session.commit();
          }
        }
      }
      broker.stop();

      EmbeddedActiveMQ restarted = new EmbeddedActiveMQ();
      restarted.setConfiguration(config);
      restarted.start();
      try (ActiveMQJMSConnectionFactory factory = new ActiveMQJMSConnectionFactory("vm://0");
          Connection connection = factory.createConnection()) {
        connection.start();
        try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer subscription =
                session.createSharedDurableConsumer(
                    session.createTopic(Topics.API_TOPIC_NAME), "cc-restart")) {
          Message restored = subscription.receive(5000);
          assertNotNull(restored, "Persistent audit event must survive a broker restart");
          assertEquals("persistent-audit-event", ((TextMessage) restored).getText());
          session.commit();
        }
      } finally {
        restarted.stop();
      }
    } finally {
      broker.stop();
    }
  }
}
