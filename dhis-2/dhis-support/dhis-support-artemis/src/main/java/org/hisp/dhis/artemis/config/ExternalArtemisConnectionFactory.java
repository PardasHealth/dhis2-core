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

import java.util.HashMap;
import java.util.Map;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;

/** Server-authenticated TLS for the optional external broker; embedded transport is unchanged. */
final class ExternalArtemisConnectionFactory {
  private ExternalArtemisConnectionFactory() {}

  static ActiveMQJMSConnectionFactory create(
      ArtemisConfigData broker, DhisConfigurationProvider configuration) {
    boolean tls = configuration.isEnabled(ConfigurationKey.ARTEMIS_SSL_ENABLED);
    String trustStore = configuration.getProperty(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PATH);
    String password = configuration.getProperty(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_PASSWORD);
    if (!tls && (StringUtils.isNotBlank(trustStore) || StringUtils.isNotBlank(password))) {
      throw new IllegalArgumentException(
          "Artemis truststore configuration requires artemis.ssl.enabled=on");
    }
    if (StringUtils.isBlank(trustStore) && StringUtils.isNotBlank(password)) {
      throw new IllegalArgumentException("Artemis truststore password requires a truststore path");
    }
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TransportConstants.HOST_PROP_NAME, broker.getHost());
    parameters.put(TransportConstants.PORT_PROP_NAME, broker.getPort());
    parameters.put(TransportConstants.SSL_ENABLED_PROP_NAME, tls);
    if (tls) {
      // No trust-all, hostname bypass, plaintext fallback or arbitrary URL query options.
      parameters.put(TransportConstants.VERIFY_HOST_PROP_NAME, true);
      parameters.put(TransportConstants.TRUST_ALL_PROP_NAME, false);
      parameters.put(TransportConstants.FORCE_SSL_PARAMETERS, true);
      parameters.put(TransportConstants.SSL_PROVIDER, "JDK");
      parameters.put(TransportConstants.ENABLED_PROTOCOLS_PROP_NAME, "TLSv1.3,TLSv1.2");
      if (StringUtils.isNotBlank(trustStore)) {
        parameters.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, trustStore);
        parameters.put(
            TransportConstants.TRUSTSTORE_TYPE_PROP_NAME,
            configuration.getProperty(ConfigurationKey.ARTEMIS_SSL_TRUSTSTORE_TYPE));
        if (StringUtils.isNotEmpty(password)) {
          parameters.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, password);
        }
      }
    }
    return new ActiveMQJMSConnectionFactory(
        false, new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters));
  }
}
