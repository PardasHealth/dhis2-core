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
package org.hisp.dhis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class HibernateEncryptionPolicyTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void strictModeRejectsMissingAndBlankPasswords(String password) throws Exception {
    var config = bean(provider(true, password));
    assertThrows(IllegalStateException.class, config::init);
  }

  @Test
  void strictModeRejectsTheBuiltInFallbackWithoutDisclosingIt() throws Exception {
    String legacy = password(initialized(false, null));
    var config = bean(provider(true, legacy));
    var error = assertThrows(IllegalStateException.class, config::init);
    assertFalse(error.getMessage().contains(legacy));
  }

  @Test
  void explicitPasswordIsNotTrimmedOrReplaced() throws Exception {
    String selected = " fixture-secret-with-intentional-spaces ";
    assertEquals(selected, password(initialized(true, selected)));
  }

  @Test
  void compatibilityModeRetainsTheOriginalFallback() throws Exception {
    assertEquals(password(initialized(false, null)), password(initialized(false, "")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"aes", "legacy"})
  void strictModeReadsCiphertextUsingTheSameExplicitKey(String algorithm) throws Exception {
    String key = "retained-fixture-key-do-not-rotate";
    var old = encryptor(initialized(false, key), algorithm);
    var strict = encryptor(initialized(true, key), algorithm);
    String ciphertext = old.encrypt("previously-stored-setting");
    assertEquals("previously-stored-setting", strict.decrypt(ciphertext));
    assertEquals("new-setting", old.decrypt(strict.encrypt("new-setting")));
  }

  @Test
  void legacyFallbackCiphertextRemainsReadableInCompatibilityMode() throws Exception {
    var old = encryptor(initialized(false, null), "legacy");
    var compatible = encryptor(initialized(false, ""), "legacy");
    assertEquals("old-value", compatible.decrypt(old.encrypt("old-value")));
  }

  @Test
  void strictFailureAbortsSpringContextStartup() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(DhisConfigurationProvider.class, () -> provider(true, null));
      context.register(HibernateEncryptionConfig.class);
      assertThrows(BeansException.class, context::refresh);
      assertFalse(context.isActive());
    }
  }

  private static DhisConfigurationProvider provider(boolean strict, String password) {
    var provider = mock(DhisConfigurationProvider.class, CALLS_REAL_METHODS);
    when(provider.getProperty(any(ConfigurationKey.class)))
        .thenAnswer(
            invocation -> {
              ConfigurationKey key = invocation.getArgument(0);
              if (key == ConfigurationKey.ENCRYPTION_REQUIRE_EXPLICIT_PASSWORD)
                return strict ? "on" : "off";
              if (key == ConfigurationKey.ENCRYPTION_PASSWORD) return password;
              return key.getDefaultValue();
            });
    when(provider.getPropertyOrDefault(any(ConfigurationKey.class), any(String.class)))
        .thenAnswer(
            invocation -> {
              String value = provider.getProperty(invocation.getArgument(0));
              return value == null ? invocation.getArgument(1) : value;
            });
    return provider;
  }

  private static HibernateEncryptionConfig bean(DhisConfigurationProvider provider)
      throws Exception {
    var result = new HibernateEncryptionConfig();
    var field = HibernateEncryptionConfig.class.getDeclaredField("config");
    field.setAccessible(true);
    field.set(result, provider);
    return result;
  }

  private static HibernateEncryptionConfig initialized(boolean strict, String password)
      throws Exception {
    var result = bean(provider(strict, password));
    result.init();
    return result;
  }

  private static String password(HibernateEncryptionConfig config) throws Exception {
    var field = HibernateEncryptionConfig.class.getDeclaredField("password");
    field.setAccessible(true);
    return (String) field.get(config);
  }

  private static PooledPBEStringEncryptor encryptor(
      HibernateEncryptionConfig config, String algorithm) {
    var result =
        algorithm.equals("aes")
            ? config.aes128StringEncryptor()
            : config.tripleDesStringEncryptor();
    if (algorithm.equals("aes")) result.setProvider(new BouncyCastleProvider());
    return result;
  }
}
