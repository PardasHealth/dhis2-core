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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

class OidcEventIdentityTest {
  private final UserService users = mock(UserService.class);
  private final AuthenticationListener listener = new AuthenticationListener();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(listener, "userService", users);
    ReflectionTestUtils.setField(listener, "config", mock(DhisConfigurationProvider.class));
    ReflectionTestUtils.setField(listener, "meterRegistry", new SimpleMeterRegistry());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void successRetainsUidOwnerWhenSnapshotNameHasBeenReused(boolean completed) {
    var owner = account("Owner123456", "renamed-owner");
    var namesake = account("Other123456", "snapshot-name");
    when(users.getUser("Owner123456")).thenReturn(owner);
    when(users.getUserByUsername("snapshot-name")).thenReturn(namesake);

    listener.handleAuthenticationSuccess(
        new AuthenticationSuccessEvent(token(completed, "Owner123456")));

    assertNotNull(owner.getLastLogin());
    assertNull(namesake.getLastLogin());
    verify(users).updateUser(eq(owner), any());
    verify(users, never()).getUserByUsername(anyString());
    verify(users).registerSuccessfulLogin("renamed-owner");
    verify(users, never()).registerSuccessfulLogin("snapshot-name");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedEventCountsAgainstTheCurrentUidOwnersName(boolean completed) {
    when(users.getUser("Owner123456")).thenReturn(account("Owner123456", "renamed-owner"));

    listener.handleAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(
            token(completed, "Owner123456"), new BadCredentialsException("Denied")));

    verify(users).registerFailedLogin("renamed-owner");
    verify(users, never()).registerFailedLogin("snapshot-name");
    verify(users, never()).registerFailedLogin("external-subject");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void deletedOwnerHasNoNamesakeFallbackForEitherOutcome(boolean completed) {
    var namesake = account("Other123456", "snapshot-name");
    when(users.getUserByUsername("snapshot-name")).thenReturn(namesake);
    Authentication auth = token(completed, "Deleted1234");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    listener.handleAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Denied")));

    assertNull(namesake.getLastLogin());
    verify(users, never()).getUserByUsername(anyString());
    verify(users, never()).updateUser(any(), any());
    verify(users, never()).registerSuccessfulLogin(any());
    verify(users, never()).registerFailedLogin(any());
  }

  @Test
  void missingMappedUidCannotFallBackToSnapshotOrSubjectName() {
    Authentication auth = token(false, null);
    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    listener.handleAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Denied")));
    verifyNoInteractions(users);
  }

  private Authentication token(boolean completed, String uid) {
    var principal =
        new DhisOidcUser(
            UserDetails.empty().uid(uid).username("snapshot-name").build(),
            Map.of("sub", "external-subject"),
            "sub",
            null);
    if (completed) {
      var auth = mock(OAuth2AuthenticationToken.class);
      when(auth.getName()).thenReturn("external-subject");
      when(auth.getPrincipal()).thenReturn(principal);
      return auth;
    }
    var auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("external-subject");
    when(auth.getPrincipal()).thenReturn(principal);
    return auth;
  }

  private User account(String uid, String username) {
    var user = new User();
    user.setUid(uid);
    user.setUsername(username);
    return user;
  }
}
