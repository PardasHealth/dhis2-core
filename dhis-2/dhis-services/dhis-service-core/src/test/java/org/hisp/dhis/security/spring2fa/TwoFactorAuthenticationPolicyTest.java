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
package org.hisp.dhis.security.spring2fa;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorAuthUtils;
import org.hisp.dhis.security.twofa.TwoFactorReplayService;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsImpl;
import org.hisp.dhis.user.UserService;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

/** Fork regression for AFTA checklist row 36 (FIA_UAU.5.2); not a complete MFA evaluation. */
@ExtendWith(MockitoExtension.class)
class TwoFactorAuthenticationPolicyTest {
  private static final String PASSWORD = "Correct-test-password";
  private static final String TOTP_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  @Mock private UserDetailsService detailsService;
  @Mock private UserService userService;
  @Mock private TwoFactorAuthService twoFactorService;
  @Mock private DhisConfigurationProvider configuration;
  @Mock private TwoFactorReplayService replayService;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private TwoFactorAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    lenient()
        .when(replayService.consume(any(UserDetails.class), anyString()))
        .thenAnswer(
            call -> {
              UserDetails user = call.getArgument(0);
              return TwoFactorAuthUtils.isValid2FACode(
                  user.getTwoFactorType(), call.getArgument(1), user.getSecret());
            });
    provider =
        new TwoFactorAuthenticationProvider(
            detailsService, encoder, userService, twoFactorService, configuration, replayService);
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void disabledEnrolledFactorRejectsPasswordOnlyLogin(TwoFactorType type) {
    givenUser(type, true, TOTP_SECRET, false, Set.of());
    assertThrows(
        TwoFactorAuthenticationException.class, () -> provider.authenticate(login(PASSWORD, null)));
    verifyNoInteractions(twoFactorService);
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void disabledEnrolledFactorRejectsSubmittedCode(TwoFactorType type) {
    givenUser(type, true, TOTP_SECRET, false, Set.of());
    assertThrows(
        TwoFactorAuthenticationException.class,
        () -> provider.authenticate(login(PASSWORD, "123456")));
    verifyNoInteractions(twoFactorService);
  }

  @Test
  void enabledTotpAcceptsCorrectPasswordAndCode() {
    givenUser(TwoFactorType.TOTP_ENABLED, true, TOTP_SECRET, false, Set.of());
    when(configuration.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)).thenReturn(true);
    assertTrue(
        provider.authenticate(login(PASSWORD, new Totp(TOTP_SECRET).now())).isAuthenticated());
  }

  @Test
  void enabledTotpRejectsMissingCode() {
    givenUser(TwoFactorType.TOTP_ENABLED, true, TOTP_SECRET, false, Set.of());
    when(configuration.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)).thenReturn(true);
    assertThrows(
        TwoFactorAuthenticationException.class, () -> provider.authenticate(login(PASSWORD, null)));
  }

  @Test
  void enabledEmailAcceptsCorrectPasswordAndUnexpiredCode() {
    givenUser(
        TwoFactorType.EMAIL_ENABLED,
        true,
        "123456|" + (System.currentTimeMillis() + 60_000),
        false,
        Set.of());
    when(configuration.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)).thenReturn(true);
    assertTrue(provider.authenticate(login(PASSWORD, "123456")).isAuthenticated());
  }

  @Test
  void enabledEmailRejectsWrongCode() {
    givenUser(
        TwoFactorType.EMAIL_ENABLED,
        true,
        "123456|" + (System.currentTimeMillis() + 60_000),
        false,
        Set.of());
    when(configuration.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)).thenReturn(true);
    assertThrows(
        TwoFactorAuthenticationException.class,
        () -> provider.authenticate(login(PASSWORD, "654321")));
  }

  @Test
  void enabledEmailStillSendsChallengeWhenCodeMissing() throws Exception {
    givenUser(TwoFactorType.EMAIL_ENABLED, true, "", false, Set.of());
    when(configuration.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)).thenReturn(true);
    assertThrows(
        TwoFactorCodeSentException.class, () -> provider.authenticate(login(PASSWORD, null)));
    verify(twoFactorService).sendEmail2FACode("alice");
  }

  @Test
  void nonEnrolledUserStillAuthenticatesWithPassword() {
    givenUser(TwoFactorType.NOT_ENABLED, false, null, false, Set.of());
    assertTrue(provider.authenticate(login(PASSWORD, null)).isAuthenticated());
    verifyNoInteractions(configuration, twoFactorService);
  }

  @Test
  void roleRequiredEnrollmentIsStillEnforced() {
    givenUser(
        TwoFactorType.NOT_ENABLED,
        false,
        null,
        false,
        Set.of(TwoFactorAuthService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME));
    assertThrows(
        TwoFactorAuthenticationEnrolmentException.class,
        () -> provider.authenticate(login(PASSWORD, null)));
  }

  @Test
  void nonFormAuthenticationCannotBypassEnrolledFactor() {
    givenUser(TwoFactorType.TOTP_ENABLED, true, TOTP_SECRET, false, Set.of());
    assertThrows(
        PreAuthenticatedCredentialsNotFoundException.class,
        () -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", PASSWORD)));
  }

  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"NOT_ENABLED", "ENROLLING_TOTP", "ENROLLING_EMAIL"})
  void inconsistentEnrolledStateDoesNotBypassFactor(TwoFactorType type) {
    givenUser(type, true, TOTP_SECRET, false, Set.of());
    assertThrows(
        TwoFactorAuthenticationException.class,
        () -> provider.authenticate(login(PASSWORD, "123456")));
  }

  @Test
  void missingTypeOnEnrolledAccountRejectsAuthentication() {
    givenUser(null, true, TOTP_SECRET, false, Set.of());
    assertThrows(
        TwoFactorAuthenticationException.class,
        () -> provider.authenticate(login(PASSWORD, "123456")));
  }

  @Test
  void invalidPasswordIsStillRejected() {
    givenUser(TwoFactorType.NOT_ENABLED, false, null, false, Set.of());
    assertThrows(
        BadCredentialsException.class,
        () -> provider.authenticate(login("incorrect-password", null)));
  }

  @Test
  void externalOnlyAccountStillRejectsPasswordLogin() {
    givenUser(TwoFactorType.NOT_ENABLED, false, null, true, Set.of());
    assertThrows(BadCredentialsException.class, () -> provider.authenticate(login(PASSWORD, null)));
  }

  @Test
  void lockedAccountIsRejectedBeforeAuthentication() {
    when(userService.isLocked("alice")).thenReturn(true);
    assertThrows(LockedException.class, () -> provider.authenticate(login(PASSWORD, null)));
    verifyNoInteractions(detailsService, configuration, twoFactorService);
  }

  @Test
  void alreadyConsumedCodeIsRejectedBeforeAuthenticationReturns() {
    givenUser(
        TwoFactorType.EMAIL_ENABLED,
        true,
        "123456|" + (System.currentTimeMillis() + 60_000),
        false,
        Set.of());
    when(configuration.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)).thenReturn(true);
    when(replayService.consume(any(UserDetails.class), anyString())).thenReturn(false);
    assertThrows(
        TwoFactorAuthenticationException.class,
        () -> provider.authenticate(login(PASSWORD, "123456")));
  }

  @Test
  void unavailableReplayStorageDoesNotReturnAuthentication() {
    givenUser(
        TwoFactorType.EMAIL_ENABLED,
        true,
        "123456|" + (System.currentTimeMillis() + 60_000),
        false,
        Set.of());
    when(configuration.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)).thenReturn(true);
    when(replayService.consume(any(UserDetails.class), anyString()))
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
    assertThrows(
        org.springframework.dao.DataAccessResourceFailureException.class,
        () -> provider.authenticate(login(PASSWORD, "123456")));
  }

  private void givenUser(
      TwoFactorType type,
      boolean enrolled,
      String secret,
      boolean external,
      Set<String> restrictions) {
    var user =
        UserDetailsImpl.builder()
            .username("alice")
            .password(encoder.encode(PASSWORD))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .externalAuth(external)
            .isTwoFactorEnabled(enrolled)
            .twoFactorType(type)
            .secret(secret)
            .authorities(List.of())
            .allAuthorities(Set.of())
            .allRestrictions(restrictions)
            .userGroupIds(Set.of())
            .userOrgUnitIds(Set.of())
            .userDataOrgUnitIds(Set.of())
            .userSearchOrgUnitIds(Set.of())
            .userEffectiveSearchOrgUnitIds(Set.of())
            .userRoleIds(Set.of())
            .managedGroupLongIds(Set.of())
            .userRoleLongIds(Set.of())
            .build();
    when(detailsService.loadUserByUsername("alice")).thenReturn(user);
  }

  private Authentication login(String password, String code) {
    var auth = new UsernamePasswordAuthenticationToken("alice", password);
    auth.setDetails(new TwoFactorWebAuthenticationDetails("127.0.0.1", null, "127.0.0.1", code));
    return auth;
  }
}
