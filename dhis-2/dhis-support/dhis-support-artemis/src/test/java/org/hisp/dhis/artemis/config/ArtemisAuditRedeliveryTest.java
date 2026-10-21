package org.hisp.dhis.artemis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.hisp.dhis.artemis.Topics;
import org.junit.jupiter.api.Test;

class ArtemisAuditRedeliveryTest {
  @Test
  void retriesAreBoundedOnlyForAuditTopics() {
    ConfigurationImpl configuration = new ConfigurationImpl();
    ArtemisConfig.configureAuditRedelivery(configuration);

    for (String topic :
        new String[] {
          Topics.METADATA_TOPIC_NAME,
          Topics.AGGREGATE_TOPIC_NAME,
          Topics.TRACKER_TOPIC_NAME,
          Topics.API_TOPIC_NAME
        }) {
      AddressSettings settings = configuration.getAddressesSettings().get(topic);
      assertEquals(5, settings.getMaxDeliveryAttempts());
      assertEquals(1000L, settings.getRedeliveryDelay());
      assertEquals("DLQ", settings.getDeadLetterAddress().toString());
      assertEquals("ExpiryQueue", settings.getExpiryAddress().toString());
    }

    AddressSettings fallback = configuration.getAddressesSettings().get("#");
    assertEquals("DLQ", fallback.getDeadLetterAddress().toString());
    assertEquals("ExpiryQueue", fallback.getExpiryAddress().toString());
    assertNull(configuration.getAddressesSettings().get(Topics.TRACKER_IMPORT_JOB_TOPIC_NAME));
  }
}
