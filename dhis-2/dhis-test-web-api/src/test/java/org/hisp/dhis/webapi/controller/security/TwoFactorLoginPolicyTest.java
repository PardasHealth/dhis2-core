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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.external.conf.ConfigurationKey.EMAIL_2FA_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.TOTP_2FA_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.message.DefaultFakeMessageSender;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonLoginResponse;
import org.hisp.dhis.user.User;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Checks login response semantics; HTTP 200 alone does not mean authentication succeeded. */
@ContextConfiguration(classes = TwoFactorLoginPolicyTest.EmailFixture.class)
// Use committed fixtures: the login guard reads primary state in REQUIRES_NEW.
// PER_CLASS makes the project extension bind a session and empty the DB after the class.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TwoFactorLoginPolicyTest extends AuthenticationApiTestBase {
  static class EmailFixture {
    @Bean("emailMessageSender")
    public MessageSender emailMessageSender() {
      return new DefaultFakeMessageSender();
    }
  }

  @Autowired private DhisConfigurationProvider config;
  @Autowired private MessageSender emailMessageSender;
  private String originalEmail;
  private String originalTotp;

  @BeforeEach
  void rememberFlags() {
    originalEmail = config.getProperties().getProperty(EMAIL_2FA_ENABLED.getKey());
    originalTotp = config.getProperties().getProperty(TOTP_2FA_ENABLED.getKey());
  }

  @AfterEach
  void restoreFlagsAndSessions() {
    restore(EMAIL_2FA_ENABLED.getKey(), originalEmail);
    restore(TOTP_2FA_ENABLED.getKey(), originalTotp);
    emailMessageSender.clearMessages();
    userService.invalidateAllSessions();
    clearSecurityContext();
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void disabledEnrolledFactorRejectsMissingCode(TwoFactorType type) {
    prepare(type, false);
    JsonLoginResponse response = login(null);
    assertDenied(type, response);
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void disabledEnrolledFactorRejectsEvenValidCode(TwoFactorType type) {
    String code = prepare(type, false);
    assertDenied(type, login(code));
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void enabledFactorAllowsValidCode(TwoFactorType type) {
    String code = prepare(type, true);
    JsonLoginResponse response = login(code);
    assertEquals("SUCCESS", response.getLoginStatus());
    assertNotNull(response.getRedirectUrl());
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void secondLoginCannotReuseAcceptedCode(TwoFactorType type) {
    String code = prepare(type, true);
    assertEquals("SUCCESS", login(code).getLoginStatus());
    clearSecurityContext();
    assertDenied(type, login(code));
  }

  @Test
  void newEmailChallengeAllowsAnotherLoginAfterThePreviousOneWasConsumed() {
    String first = prepare(TwoFactorType.EMAIL_ENABLED, true);
    assertEquals("SUCCESS", login(first).getLoginStatus());
    clearSecurityContext();
    assertDenied(TwoFactorType.EMAIL_ENABLED, login(first));
    clearSecurityContext();
    assertEquals("EMAIL_TWO_FACTOR_CODE_SENT", login(null).getLoginStatus());
    String replacement = userService.getUserByUsername("admin").getSecret().split("\\|")[0];
    clearSecurityContext();
    assertEquals("SUCCESS", login(replacement).getLoginStatus());
  }

  private String prepare(TwoFactorType type, boolean enabled) {
    String key =
        type == TwoFactorType.EMAIL_ENABLED
            ? EMAIL_2FA_ENABLED.getKey()
            : TOTP_2FA_ENABLED.getKey();
    config.getProperties().setProperty(key, enabled ? "on" : "off");
    User user = userService.getUserByUsername("admin");
    user.setTwoFactorType(type);
    String code;
    if (type == TwoFactorType.EMAIL_ENABLED) {
      code = "123456";
      user.setEmail("test@example.invalid");
      user.setVerifiedEmail("test@example.invalid");
      user.setSecret(code + "|" + (System.currentTimeMillis() + 60_000));
    } else {
      String seed =
          org.jboss.aerogear.security.otp.api.Base32.encode(
              org.hisp.dhis.common.CodeGenerator.generateSecureRandomBytes(20));
      user.setSecret(seed);
      code = new Totp(seed).now();
    }
    userService.updateUser(user);
    manager.flush();
    manager.clear();
    clearSecurityContext();
    return code;
  }

  private JsonLoginResponse login(String code) {
    String body =
        "{'username':'admin','password':'district'"
            + (code == null ? "" : ",'twoFactorCode':'" + code + "'")
            + "}";
    return POST("/auth/login", body).content(HttpStatus.OK).as(JsonLoginResponse.class);
  }

  private void assertDenied(TwoFactorType type, JsonLoginResponse response) {
    assertEquals(
        type == TwoFactorType.EMAIL_ENABLED
            ? "INCORRECT_TWO_FACTOR_CODE_EMAIL"
            : "INCORRECT_TWO_FACTOR_CODE_TOTP",
        response.getLoginStatus());
    assertNull(response.getRedirectUrl());
  }

  private void restore(String key, String value) {
    if (value == null) config.getProperties().remove(key);
    else config.getProperties().setProperty(key, value);
  }
}
