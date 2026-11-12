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
package org.hisp.dhis.webapi.security.apikey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ApiTokenAuthManagerTest {
  private static final String TOKEN_KEY = "test-token-hash";
  private static final String OWNER_UID = "Owner123456";

  @Mock private ApiTokenService apiTokenService;
  @Mock private UserService userService;

  private ApiTokenAuthManager manager;
  private ApiToken token;
  private UserDetails user;

  @BeforeEach
  void setUp() {
    manager = new ApiTokenAuthManager(apiTokenService, userService);
    User owner = new User();
    owner.setUid(OWNER_UID);
    owner.setUsername("alice");
    token = new ApiToken();
    token.setCreatedBy(owner);
    token.setExpire(System.currentTimeMillis() + 60_000);
    user = mock(UserDetails.class);
    lenient().when(user.getUid()).thenReturn(OWNER_UID);
    lenient().when(user.isEnabled()).thenReturn(true);
    lenient().when(user.isAccountNonLocked()).thenReturn(true);
    lenient().when(user.isAccountNonExpired()).thenReturn(true);
    lenient().when(user.isCredentialsNonExpired()).thenReturn(true);
    when(apiTokenService.getByKey(TOKEN_KEY)).thenReturn(token);
    lenient().when(userService.createUserDetailsByUsername("alice")).thenReturn(user);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"disabled", "locked", "expiredAccount", "expiredCredentials", "twoFactor"})
  void rejectsAccountChangesAfterSuccessfulAuthentication(String change) {
    authenticate();
    switch (change) {
      case "disabled" -> when(user.isEnabled()).thenReturn(false);
      case "locked" -> when(user.isAccountNonLocked()).thenReturn(false);
      case "expiredAccount" -> when(user.isAccountNonExpired()).thenReturn(false);
      case "expiredCredentials" -> when(user.isCredentialsNonExpired()).thenReturn(false);
      case "twoFactor" -> when(user.isTwoFactorEnabled()).thenReturn(true);
      default -> throw new IllegalArgumentException(change);
    }
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void refreshesPermissionsAndDoesNotShareRequestAuthentication() {
    doReturn(List.of(new SimpleGrantedAuthority("ALL"))).when(user).getAuthorities();
    ApiTokenAuthenticationToken first = authenticate();
    first.setDetails("first-request");
    first.setAuthenticated(true);
    UserDetails changedUser = mock(UserDetails.class);
    lenient().when(changedUser.getUid()).thenReturn(OWNER_UID);
    doReturn(List.of(new SimpleGrantedAuthority("F_VIEW_EVENT")))
        .when(changedUser)
        .getAuthorities();
    when(changedUser.isEnabled()).thenReturn(true);
    when(changedUser.isAccountNonLocked()).thenReturn(true);
    when(changedUser.isAccountNonExpired()).thenReturn(true);
    when(changedUser.isCredentialsNonExpired()).thenReturn(true);
    when(userService.createUserDetailsByUsername("alice")).thenReturn(changedUser);

    ApiTokenAuthenticationToken second = authenticate();

    assertNotSame(first, second);
    assertSame(changedUser, second.getPrincipal());
    assertEquals(
        List.of(new SimpleGrantedAuthority("F_VIEW_EVENT")), List.copyOf(second.getAuthorities()));
    assertFalse(second.getAuthorities().contains(new SimpleGrantedAuthority("ALL")));
    assertNull(second.getDetails());
    assertFalse(second.isAuthenticated());
    assertEquals("first-request", first.getDetails());
  }

  @Test
  void rejectsDeletedOwnerAfterSuccessfulAuthentication() {
    authenticate();
    when(userService.createUserDetailsByUsername("alice")).thenReturn(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void rejectsDifferentAccountReturnedForOwnersUsername() {
    authenticate();
    // A stale owner name or a concurrent rename/reuse must never bind the token
    // to the new holder of that name, even when that account is fully enabled.
    when(user.getUid()).thenReturn("Other123456");
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void rejectsOwnerWithoutStableIdentity() {
    token.getCreatedBy().setUid(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void rejectsReloadedAccountWithoutStableIdentity() {
    when(user.getUid()).thenReturn(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void renamedOwnerWithSameIdentityRetainsAccess() {
    token.getCreatedBy().setUsername("renamed-alice");
    when(userService.createUserDetailsByUsername("renamed-alice")).thenReturn(user);
    assertSame(user, authenticate().getPrincipal());
  }

  @Test
  void rejectsDeletedTokenAfterSuccessfulAuthentication() {
    authenticate();
    when(apiTokenService.getByKey(TOKEN_KEY)).thenReturn(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void usesUpdatedTokenConstraintsAfterSuccessfulAuthentication() {
    authenticate();
    ApiToken replacement = new ApiToken();
    replacement.setCreatedBy(token.getCreatedBy());
    replacement.setExpire(token.getExpire());
    replacement.addMethodToAllowedList("GET");
    when(apiTokenService.getByKey(TOKEN_KEY)).thenReturn(replacement);
    assertSame(replacement, authenticate().getToken());
  }

  @Test
  void rejectsExpiredTokenAfterSuccessfulAuthentication() {
    authenticate();
    token.setExpire(System.currentTimeMillis() - 1);
    assertThrows(ApiTokenExpiredException.class, this::authenticate);
  }

  @Test
  void rejectsTokenWithoutExpiry() {
    token.setExpire(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void rejectsTokenWithoutOwner() {
    token.setCreatedBy(null);
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  private ApiTokenAuthenticationToken authenticate() {
    return (ApiTokenAuthenticationToken)
        manager.authenticate(new ApiTokenAuthenticationToken(TOKEN_KEY));
  }
}
