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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class ExternalArtemisConnectionFactoryTest {
  static DhisConfigurationProvider configuration(Map<ConfigurationKey, String> overrides) {
    var result = mock(DhisConfigurationProvider.class, CALLS_REAL_METHODS);
    when(result.getProperty(any(ConfigurationKey.class)))
        .thenAnswer(
            invocation -> {
              ConfigurationKey key = invocation.getArgument(0);
              return overrides.getOrDefault(key, key.getDefaultValue());
            });
    return result;
  }

  @Test
  void tlsRequiresHostVerificationAndModernProtocols() {
    var config = configuration(Map.of(ConfigurationKey.ARTEMIS_SSL_ENABLED, "on"));
    try (var factory = ExternalArtemisConnectionFactory.create(new ArtemisConfigData(), config)) {
      var transport = factory.getStaticConnectors()[0].getParams();
      assertEquals(true, transport.get(TransportConstants.SSL_ENABLED_PROP_NAME));
      assertEquals(true, transport.get(TransportConstants.VERIFY_HOST_PROP_NAME));
      assertEquals(false, transport.get(TransportConstants.TRUST_ALL_PROP_NAME));
      assertEquals(true, transport.get(TransportConstants.FORCE_SSL_PARAMETERS));
      assertEquals(
          "TLSv1.3,TLSv1.2", transport.get(TransportConstants.ENABLED_PROTOCOLS_PROP_NAME));
      assertEquals("JDK", transport.get(TransportConstants.SSL_PROVIDER));
    }
  }

  @Test
  void plaintextDefaultRemainsExplicitlyNonTls() {
    try (var factory =
        ExternalArtemisConnectionFactory.create(new ArtemisConfigData(), configuration(Map.of()))) {
      assertEquals(
          false,
          factory
              .getStaticConnectors()[0]
              .getParams()
              .get(TransportConstants.SSL_ENABLED_PROP_NAME));
    }
  }

  @Test
  void explicitTruststoreIsPassedWithoutUrlInterpolation() {
    String path = "/private/broker trust#store.p12";
    String password = "fixture-only &?=#";
    var config =
        configuration(
            Map.of(
                ConfigurationKey.ARTEMIS_SSL_ENABLED,
                "on",
                ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PATH,
                path,
                ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PASSWORD,
                password));
    try (var factory = ExternalArtemisConnectionFactory.create(new ArtemisConfigData(), config)) {
      var transport = factory.getStaticConnectors()[0].getParams();
      assertEquals(path, transport.get(TransportConstants.TRUSTSTORE_PATH_PROP_NAME));
      assertEquals(password, transport.get(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME));
      assertEquals("PKCS12", transport.get(TransportConstants.TRUSTSTORE_TYPE_PROP_NAME));
    }
  }

  @Test
  void configuredTruststoreCannotSilentlyUsePlaintext() {
    var config =
        configuration(Map.of(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PATH, "/fixture.p12"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExternalArtemisConnectionFactory.create(new ArtemisConfigData(), config));
  }

  @Test
  void passwordWithoutStoreIsRejectedWithoutDisclosingIt() {
    var config =
        configuration(
            Map.of(
                ConfigurationKey.ARTEMIS_SSL_ENABLED,
                "on",
                ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PASSWORD,
                "private-fixture"));
    var error =
        assertThrows(
            IllegalArgumentException.class,
            () -> ExternalArtemisConnectionFactory.create(new ArtemisConfigData(), config));
    assertFalse(error.getMessage().contains("private-fixture"));
  }

  @Test
  void configuredCredentialsRemainOnExternalFactory() throws Exception {
    var broker = new ArtemisConfigData();
    broker.setMode(ArtemisMode.NATIVE);
    broker.setUsername("fixture-user");
    broker.setPassword("fixture-password");
    var config =
        new ArtemisConfig(
            configuration(Map.of(ConfigurationKey.ARTEMIS_SSL_ENABLED, "on")),
            mock(LocationManager.class),
            mock(Environment.class));
    try (var factory = (ActiveMQJMSConnectionFactory) config.jmsConnectionFactory(broker)) {
      assertEquals("fixture-user", factory.getUser());
      assertEquals("fixture-password", factory.getPassword());
      assertEquals(
          true,
          factory
              .getStaticConnectors()[0]
              .getParams()
              .get(TransportConstants.SSL_ENABLED_PROP_NAME));
    }
  }

  @Test
  void embeddedModeDoesNotUseExternalTlsConfiguration() throws Exception {
    var provider =
        configuration(Map.of(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PATH, "/unused.p12"));
    var config = new ArtemisConfig(provider, mock(LocationManager.class), mock(Environment.class));
    try (var factory =
        (ActiveMQJMSConnectionFactory) config.jmsConnectionFactory(new ArtemisConfigData())) {
      assertTrue(factory.getStaticConnectors()[0].getFactoryClassName().contains("InVM"));
    }
  }

  @Test
  void truststorePasswordIsConfidential() {
    assertTrue(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PASSWORD.isConfidential());
  }
}
