/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AuditIntegrityConfigTest {
  @Test
  void enabledModeWithoutAValidKeyFailsSpringStartup() {
    var configuration = mock(DhisConfigurationProvider.class);
    when(configuration.isEnabled(ConfigurationKey.AUDIT_INTEGRITY_ENABLED)).thenReturn(true);
    when(configuration.getProperty(ConfigurationKey.AUDIT_INTEGRITY_KEY))
        .thenReturn("synthetic-invalid-secret");
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(DhisConfigurationProvider.class, () -> configuration);
      context.register(AuditIntegrityConfig.class);
      var error = assertThrows(BeanCreationException.class, context::refresh);
      assertFalse(error.getMessage().contains("synthetic-invalid-secret"));
    }
  }

  @Test
  void disabledModeAndConfidentialKeyHaveSafeDefaults() {
    var configuration = mock(DhisConfigurationProvider.class);
    assertFalse(new AuditIntegrityConfig().auditIntegrityGuard(configuration).isEnabled());
    assertTrue(ConfigurationKey.AUDIT_INTEGRITY_KEY.isConfidential());
    assertEquals("off", ConfigurationKey.AUDIT_INTEGRITY_ENABLED.getDefaultValue());
  }

  @Test
  void configuredIndependentKeyEnablesEnforcement() {
    var configuration = mock(DhisConfigurationProvider.class);
    when(configuration.isEnabled(ConfigurationKey.AUDIT_INTEGRITY_ENABLED)).thenReturn(true);
    when(configuration.getProperty(ConfigurationKey.AUDIT_INTEGRITY_KEY))
        .thenReturn(Base64.getEncoder().encodeToString(new byte[32]));
    assertTrue(new AuditIntegrityConfig().auditIntegrityGuard(configuration).isEnabled());
  }
}
