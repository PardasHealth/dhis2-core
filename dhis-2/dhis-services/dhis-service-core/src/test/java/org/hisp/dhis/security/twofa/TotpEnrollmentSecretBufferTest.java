/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.security.twofa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpEnrollmentSecretBufferTest {
  private UserService users;
  private DhisConfigurationProvider config;
  private TwoFactorAuthService service;
  private User user;
  private byte[] seed;

  @BeforeEach
  void setUp() {
    users = mock(UserService.class);
    config = mock(DhisConfigurationProvider.class);
    service =
        new TwoFactorAuthService(
            mock(SystemSettingsProvider.class),
            users,
            mock(MessageSender.class),
            mock(UserSettingsService.class),
            mock(I18nManager.class),
            config);
    user = new User();
    user.setTwoFactorType(TwoFactorType.NOT_ENABLED);
    seed = new byte[20];
    Arrays.fill(seed, (byte) 7);
    when(config.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)).thenReturn(true);
  }

  @Test
  void successfulEnrollmentClearsRawSeedAndPersistsTheSameBase32Secret() throws Exception {
    String expected = Base32.encode(seed.clone());
    try (var random = mockStatic(CodeGenerator.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomBytes(20)).thenReturn(seed);
      service.enrollTOTP2FA(user);
      assertArrayEquals(new byte[20], seed);
      assertEquals(expected, user.getSecret());
      assertEquals(TwoFactorType.ENROLLING_TOTP, user.getTwoFactorType());
      verify(users).updateUser(user);
    }
  }

  @Test
  void encodingFailureClearsRawSeedWithoutMutatingOrPersistingTheUser() {
    var failure = new IllegalStateException("synthetic encoding failure");
    try (var random = mockStatic(CodeGenerator.class);
        var encoder = mockStatic(Base32.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomBytes(20)).thenReturn(seed);
      encoder.when(() -> Base32.encode(seed)).thenThrow(failure);
      assertSame(
          failure, assertThrows(IllegalStateException.class, () -> service.enrollTOTP2FA(user)));
      assertArrayEquals(new byte[20], seed);
      assertNull(user.getSecret());
      assertEquals(TwoFactorType.NOT_ENABLED, user.getTwoFactorType());
      verifyNoInteractions(users);
    }
  }

  @Test
  void persistenceFailureStillFindsRawSeedClearedAndPropagates() {
    var failure = new IllegalStateException("synthetic storage failure");
    doThrow(failure).when(users).updateUser(user);
    try (var random = mockStatic(CodeGenerator.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomBytes(20)).thenReturn(seed);
      assertSame(
          failure, assertThrows(IllegalStateException.class, () -> service.enrollTOTP2FA(user)));
      assertArrayEquals(new byte[20], seed);
      verify(users).updateUser(user);
    }
  }

  @Test
  void disabledEnrollmentDoesNotAllocateASeed() {
    when(config.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)).thenReturn(false);
    try (var random = mockStatic(CodeGenerator.class)) {
      assertThrows(ConflictException.class, () -> service.enrollTOTP2FA(user));
      random.verifyNoInteractions();
      verifyNoInteractions(users);
    }
  }
}
