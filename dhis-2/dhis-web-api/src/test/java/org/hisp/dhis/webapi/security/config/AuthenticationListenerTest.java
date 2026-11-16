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
package org.hisp.dhis.webapi.security.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.basic.HttpBasicWebAuthenticationDetails;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.security.apikey.ApiTokenAuthenticationFailureEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuthenticationListenerTest {

  private static final String LOGIN_COUNTER_NAME = "dhis2_user_logins_total";

  @Mock private UserService userService;

  @Mock private DhisConfigurationProvider config;

  private SimpleMeterRegistry meterRegistry;

  private AuthenticationListener listener;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    listener = new AuthenticationListener();
    ReflectionTestUtils.setField(listener, "userService", userService);
    ReflectionTestUtils.setField(listener, "config", config);
    ReflectionTestUtils.setField(listener, "meterRegistry", meterRegistry);

    lenient()
        .when(userService.getUserByUsername(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(null);
    lenient().when(config.isReadOnlyMode()).thenReturn(false);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void tokenAuditFailureDoesNotIncrementPasswordFailureCount() {
    var auth = UsernamePasswordAuthenticationToken.unauthenticated("pat-user", null);
    listener.handleAuthenticationFailure(
        new ApiTokenAuthenticationFailureEvent(
            auth, new BadCredentialsException("Token rejected")));
    verifyNoInteractions(userService);
  }

  @Test
  void passwordFailureStillIncrementsFailureCount() {
    var auth = UsernamePasswordAuthenticationToken.unauthenticated("password-user", null);
    listener.handleAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Bad password")));
    verify(userService).registerFailedLogin("password-user");
  }

  @Test
  void formLoginIsCountedUnderFormMethod() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
    assertEquals(0.0, counterValue("basic"));
    assertEquals(0.0, counterValue("oidc"));
  }

  @Test
  void basicAuthIsCountedUnderBasicMethod() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    when(auth.getDetails()).thenReturn(mock(HttpBasicWebAuthenticationDetails.class));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("basic"));
    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void apiTokenIsCountedUnderApitokenMethod() {
    ApiTokenAuthenticationToken auth = mock(ApiTokenAuthenticationToken.class);
    when(auth.getName()).thenReturn("pat-user");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("apitoken"));
    assertEquals(0.0, counterValue("form"));
    assertEquals(0.0, counterValue("basic"));
  }

  @Test
  void oidcLoginIsCountedUnderOidcMethod() {
    OAuth2LoginAuthenticationToken auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("oidcuser");

    DhisOidcUser principal = oidcPrincipal("oidcuser");
    when(auth.getPrincipal()).thenReturn(principal);

    WebAuthenticationDetails details = mock(WebAuthenticationDetails.class);
    when(details.getRemoteAddress()).thenReturn("127.0.0.1");
    when(auth.getDetails()).thenReturn(details);

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("oidc"));
    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void formLoginIsNotDoubleCountedWhenBothEventsFireForTheSameLogin() {
    // AuthenticationController (the /api/auth/login endpoint) publishes both an
    // InteractiveAuthenticationSuccessEvent and an AuthenticationSuccessEvent for the same
    // login - only the latter should be counted.
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    listener.handleAuthenticationSuccess(
        new InteractiveAuthenticationSuccessEvent(auth, getClass()));

    assertEquals(1.0, counterValue("form"));
  }

  @Test
  void interactiveEventAloneIsNotCounted() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(
        new InteractiveAuthenticationSuccessEvent(auth, getClass()));

    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void metricsScrapeRequestIsExcludedFromTheCounter() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("prometheus");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/metrics");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void nonMetricsRequestIsStillCountedWhenRequestContextIsPresent() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/dataElements");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
  }

  @Test
  void missingRequestContextFailsOpenAndStillCounts() {
    // No RequestContextHolder attributes bound (e.g. embeddedJetty profile does not register
    // RequestContextListener) - the exclusion must never suppress a real login.
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
  }

  @Test
  void oidcSuccessWithoutHttpDetailsUpdatesTheCanonicalLocalAccount() {
    var auth = oidcLogin("external-subject", "local-user");
    assertDoesNotThrow(
        () -> listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth)));
    verify(userService).getUser("Oidc1234567");
    verify(userService).registerSuccessfulLogin("local-user");
    verify(userService, never()).registerSuccessfulLogin("external-subject");
  }

  @Test
  void oidcSuccessWithUnexpectedDetailsStillUpdatesTheLocalAccount() {
    var auth = oidcLogin("external-subject", "local-user");
    when(auth.getDetails()).thenReturn("non-http-details");
    assertDoesNotThrow(
        () -> listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth)));
    verify(userService).registerSuccessfulLogin("local-user");
  }

  @Test
  void oidcFailureWithoutHttpDetailsStillCountsAgainstTheKnownLocalAccount() {
    var auth = oidcLogin("external-subject", "local-user");
    assertDoesNotThrow(
        () ->
            listener.handleAuthenticationFailure(
                new AuthenticationFailureBadCredentialsEvent(
                    auth, new BadCredentialsException("Denied"))));
    verify(userService).registerFailedLogin("local-user");
    verify(userService, never()).registerFailedLogin("external-subject");
  }

  @Test
  void completedOauthLoginUsesCanonicalLocalIdentityAndOidcMetric() {
    var auth = mock(OAuth2AuthenticationToken.class);
    when(auth.getName()).thenReturn("external-subject");
    var principal = oidcPrincipal("local-user");
    when(auth.getPrincipal()).thenReturn(principal);
    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    verify(userService).registerSuccessfulLogin("local-user");
    verify(userService, never()).getUserByUsername("external-subject");
    assertEquals(1.0, counterValue("oidc"));
    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void completedOauthWithUnmappedPrincipalCannotMutateALocalNamesake() {
    var auth = mock(OAuth2AuthenticationToken.class);
    when(auth.getName()).thenReturn("local-account-namesake");
    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    verifyNoInteractions(userService);
    assertEquals(1.0, counterValue("oidc"));
  }

  @Test
  void oidcSuccessWithoutLocalPrincipalDoesNotMutateAccountState() {
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("local-account-namesake");
    assertDoesNotThrow(
        () -> listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth)));
    verifyNoInteractions(userService);
  }

  @Test
  void unmappedOidcFailureRetainsAttemptedNameWithoutCastingPrincipal() {
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("attempted-user");
    assertDoesNotThrow(
        () ->
            listener.handleAuthenticationFailure(
                new AuthenticationFailureBadCredentialsEvent(
                    auth, new BadCredentialsException("Denied"))));
    verify(userService).registerFailedLogin("attempted-user");
  }

  @Test
  void userInfoSuccessWithNonJwtPrincipalAndMissingDetailsDoesNotThrow() {
    var auth = mock(OidcUserInfoAuthenticationToken.class);
    when(auth.getName()).thenReturn("userinfo-user");
    var principal =
        UsernamePasswordAuthenticationToken.authenticated(
            "userinfo-user", null, java.util.List.of());
    when(auth.getPrincipal()).thenReturn(principal);
    assertDoesNotThrow(
        () -> listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth)));
    verify(userService).registerSuccessfulLogin("userinfo-user");
    assertEquals(1.0, counterValue("oidc"));
  }

  private OAuth2LoginAuthenticationToken oidcLogin(String externalName, String localName) {
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn(externalName);
    var principal = oidcPrincipal(localName);
    when(auth.getPrincipal()).thenReturn(principal);
    return auth;
  }

  private DhisOidcUser oidcPrincipal(String username) {
    var owner = new org.hisp.dhis.user.User();
    owner.setUid("Oidc1234567");
    owner.setUsername(username);
    lenient().when(userService.getUser("Oidc1234567")).thenReturn(owner);
    return new DhisOidcUser(
        UserDetails.empty().uid("Oidc1234567").username(username).build(),
        Map.of("sub", "external-subject"),
        "sub",
        null);
  }

  private double counterValue(String method) {
    Counter counter = meterRegistry.find(LOGIN_COUNTER_NAME).tag("method", method).counter();
    return counter == null ? 0.0 : counter.count();
  }
}
