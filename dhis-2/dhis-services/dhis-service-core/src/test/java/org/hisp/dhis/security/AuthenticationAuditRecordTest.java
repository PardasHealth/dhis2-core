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
package org.hisp.dhis.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;

class AuthenticationAuditRecordTest {
  private final AuthenticationLoggerListener listener = new AuthenticationLoggerListener();
  private final Capture capture = new Capture();
  private Logger logger;
  private org.apache.logging.log4j.core.config.LoggerConfig originalConfig;

  @BeforeEach
  void captureLogger() {
    logger = (Logger) LogManager.getLogger(AuthenticationLoggerListener.class);
    var configuration = logger.getContext().getConfiguration();
    originalConfig = configuration.getLoggers().get(logger.getName());
    var isolated =
        new org.apache.logging.log4j.core.config.LoggerConfig(logger.getName(), Level.INFO, false);
    capture.start();
    isolated.addAppender(capture, null, null);
    configuration.addLogger(logger.getName(), isolated);
    logger.getContext().updateLoggers();
  }

  @AfterEach
  void restoreLogger() {
    var configuration = logger.getContext().getConfiguration();
    configuration.removeLogger(logger.getName());
    if (originalConfig != null) configuration.addLogger(logger.getName(), originalConfig);
    logger.getContext().updateLoggers();
    capture.stop();
  }

  @Test
  void failureRetainsAvailableAddressAlongsideReason() {
    var auth = UsernamePasswordAuthenticationToken.unauthenticated("alice", "not-logged-password");
    auth.setDetails(new ForwardedIpAwareWebAuthenticationDetails("127.0.0.1", null, "192.0.2.7"));
    listener.onApplicationEvent(
        new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Bad credentials")));
    String line = onlyMessage();
    assertTrue(line.contains("username: alice; ip: 192.0.2.7; exception: Bad credentials"));
    assertFalse(line.contains("not-logged-password"));
  }

  @Test
  void genericHttpDetailsRetainRemoteAddress() {
    var auth = UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of());
    var details = mock(WebAuthenticationDetails.class);
    when(details.getRemoteAddress()).thenReturn("192.0.2.8");
    auth.setDetails(details);
    listener.onApplicationEvent(new AuthenticationSuccessEvent(auth));
    assertTrue(onlyMessage().contains("ip: 192.0.2.8"));
  }

  @Test
  void oidcWithoutDetailsStillRecordsCanonicalLocalIdentity() {
    var auth = oidc();
    assertDoesNotThrow(() -> listener.onApplicationEvent(new AuthenticationSuccessEvent(auth)));
    assertTrue(onlyMessage().contains("username: local-user"));
  }

  @Test
  void oidcWithNonHttpDetailsDoesNotDropRecord() {
    var auth = oidc();
    when(auth.getDetails()).thenReturn("not-a-web-details-object");
    assertDoesNotThrow(() -> listener.onApplicationEvent(new AuthenticationSuccessEvent(auth)));
    assertTrue(onlyMessage().contains("username: local-user"));
  }

  @Test
  void oidcIdentityAndForwardedAddressAreResolvedIndependently() {
    var auth = oidc();
    when(auth.getDetails())
        .thenReturn(new ForwardedIpAwareWebAuthenticationDetails("127.0.0.1", null, "2001:db8::1"));
    listener.onApplicationEvent(new AuthenticationSuccessEvent(auth));
    assertTrue(onlyMessage().contains("username: local-user; ip: 2001:db8::1"));
  }

  @Test
  void completedOauthLoginRetainsLocalIdentityAndHttpAddress() {
    var auth =
        mock(
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
                .class);
    var principal = mock(DhisOidcUser.class);
    when(principal.getUsername()).thenReturn("local-user");
    when(auth.getName()).thenReturn("external-subject");
    when(auth.getPrincipal()).thenReturn(principal);
    var details = mock(WebAuthenticationDetails.class);
    when(details.getRemoteAddress()).thenReturn("192.0.2.9");
    when(auth.getDetails()).thenReturn(details);
    listener.onApplicationEvent(new AuthenticationSuccessEvent(auth));
    assertTrue(onlyMessage().contains("username: local-user; ip: 192.0.2.9"));
  }

  @Test
  void unknownLocalOidcIdentityRetainsAttemptedName() {
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("external-attempted-user");
    listener.onApplicationEvent(
        new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Denied")));
    assertTrue(onlyMessage().contains("username: external-attempted-user"));
  }

  @Test
  void personalTokenWithoutDetailsDoesNotDropRecord() {
    var user = mock(UserDetails.class);
    when(user.getUsername()).thenReturn("pat-user");
    when(user.getAuthorities()).thenReturn(List.of());
    var auth = new ApiTokenAuthenticationToken(null, user);
    assertDoesNotThrow(() -> listener.onApplicationEvent(new AuthenticationSuccessEvent(auth)));
    assertTrue(onlyMessage().contains("username: pat-user"));
  }

  @Test
  void unrecognizedTokenDetailsDoNotLeakTokenKey() {
    var auth = new ApiTokenAuthenticationToken("fixture-private-token");
    auth.setDetails("non-http-details");
    assertDoesNotThrow(() -> listener.onApplicationEvent(new AuthenticationSuccessEvent(auth)));
    assertFalse(onlyMessage().contains("fixture-private-token"));
  }

  @Test
  void unicodeAttemptedIdentityIsPreserved() {
    var auth = UsernamePasswordAuthenticationToken.unauthenticated("کاربر", "unused");
    listener.onApplicationEvent(
        new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Denied")));
    assertTrue(onlyMessage().contains("username: کاربر"));
  }

  @Test
  void valuesCannotInjectLineOrFieldBoundaries() {
    var auth =
        UsernamePasswordAuthenticationToken.unauthenticated("a\r\n; username: admin", "unused");
    auth.setDetails(
        new ForwardedIpAwareWebAuthenticationDetails(
            "127.0.0.1", null, "192.0.2.1\u2028ip: forged"));
    listener.onApplicationEvent(
        new AuthenticationFailureBadCredentialsEvent(
            auth, new BadCredentialsException("Denied\n; outcome: success")));
    String message = onlyMessage();
    assertFalse(message.contains("\n"));
    assertFalse(message.contains("\r"));
    assertFalse(message.contains("\u2028"));
    assertTrue(message.contains("\\u003b username: admin"));
    assertTrue(message.contains("Denied\\u000a\\u003b outcome: success"));
  }

  @Test
  void switchUserTargetUsesTheSameSingleLineEscaping() {
    var target = mock(org.springframework.security.core.userdetails.UserDetails.class);
    when(target.getUsername()).thenReturn("target\n; forged");
    var actor = UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of());
    listener.onApplicationEvent(new AuthenticationSwitchUserEvent(actor, target));
    assertEquals(2, capture.messages.size());
    assertTrue(capture.messages.get(0).contains("targetUser: target\\u000a\\u003b forged"));
    assertTrue(capture.messages.stream().noneMatch(m -> m.contains("\n")));
  }

  @Test
  void escapeIsUnambiguousForBackslashAndUnicodeFormatCharacters() {
    var auth = UsernamePasswordAuthenticationToken.unauthenticated("x\\u000a\u202e<", "unused");
    listener.onApplicationEvent(
        new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Denied")));
    assertTrue(onlyMessage().contains("username: x\\u005cu000a\\u202e\\u003c"));
  }

  @Test
  void interactiveDuplicateRemainsExcluded() {
    var auth = UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of());
    listener.onApplicationEvent(new InteractiveAuthenticationSuccessEvent(auth, getClass()));
    assertTrue(capture.messages.isEmpty());
  }

  @Test
  void disabledInfoLoggingDoesNotInspectAuthenticationFields() {
    org.apache.logging.log4j.core.config.Configurator.setLevel(logger.getName(), Level.WARN);
    var auth = mock(org.springframework.security.core.Authentication.class);
    listener.onApplicationEvent(new AuthenticationSuccessEvent(auth));
    verify(auth, never()).getName();
    assertTrue(capture.messages.isEmpty());
  }

  private OAuth2LoginAuthenticationToken oidc() {
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    var principal = mock(DhisOidcUser.class);
    when(principal.getUsername()).thenReturn("local-user");
    when(auth.getName()).thenReturn("external-subject");
    when(auth.getPrincipal()).thenReturn(principal);
    return auth;
  }

  private String onlyMessage() {
    assertEquals(1, capture.messages.size());
    return capture.messages.get(0);
  }

  private static class Capture extends AbstractAppender {
    private final List<String> messages = new ArrayList<>();

    Capture() {
      super("afta-auth-capture", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      messages.add(event.getMessage().getFormattedMessage());
    }
  }
}
