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
    assertEquals(false, configuration.getAddressesSettings().containsKey(Topics.TRACKER_IMPORT_JOB_TOPIC_NAME));
  }
}
