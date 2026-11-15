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
package org.hisp.dhis.webapi.mvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserIdentityResolverTest {
  private UserService users;
  private CurrentUserHandlerMethodArgumentResolver resolver;
  private User owner;
  private User namesake;
  private UserDetails principal;

  @BeforeEach
  void setUp() {
    users = mock(UserService.class);
    resolver = new CurrentUserHandlerMethodArgumentResolver(users);
    owner = new User();
    owner.setUid("Owner123456");
    owner.setUsername("renamed-user");
    namesake = new User();
    namesake.setUid("Other123456");
    namesake.setUsername("retired-name");
    lenient().when(users.getUser("Owner123456")).thenReturn(owner);
    lenient().when(users.getUserByUsername("retired-name")).thenReturn(namesake);
    principal = UserDetails.empty().uid("Owner123456").username("retired-name").build();
    authenticate(principal);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void renamedPrincipalCannotResolveToANewOwnerOfItsOldName() throws Exception {
    assertSame(owner, resolve("required", User.class));
    verify(users).getUser("Owner123456");
    verify(users, never()).getUserByUsername(anyString());
  }

  @Test
  void renamedPrincipalStillResolvesWhenTheOldNameIsUnused() throws Exception {
    when(users.getUserByUsername("retired-name")).thenReturn(null);
    assertSame(owner, resolve("required", User.class));
  }

  @Test
  void deletedOptionalOwnerDoesNotFallBackToAUsernameNamesake() throws Exception {
    when(users.getUser("Owner123456")).thenReturn(null);
    assertNull(resolve("optional", User.class));
  }

  @Test
  void deletedRequiredOwnerIsDenied() {
    when(users.getUser("Owner123456")).thenReturn(null);
    assertThrows(AccessDeniedException.class, () -> resolve("required", User.class));
  }

  @Test
  void missingStableIdentityIsDeniedWithoutLookingUpAName() {
    authenticate(UserDetails.empty().username("retired-name").build());
    assertThrows(AccessDeniedException.class, () -> resolve("required", User.class));
    verifyNoInteractions(users);
  }

  @Test
  void detailsArgumentRetainsItsExistingSnapshotContract() throws Exception {
    assertSame(principal, resolve("details", UserDetails.class));
    verifyNoInteractions(users);
  }

  @Test
  void stringArgumentRetainsItsExistingUsernameContract() throws Exception {
    assertEquals("retired-name", resolve("name", String.class));
    verifyNoInteractions(users);
  }

  private Object resolve(String method, Class<?> type) throws Exception {
    var parameter = new MethodParameter(Parameters.class.getDeclaredMethod(method, type), 0);
    assertTrue(resolver.supportsParameter(parameter));
    return resolver.resolveArgument(parameter, null, null, null);
  }

  private void authenticate(UserDetails user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
  }

  private static class Parameters {
    void required(@CurrentUser(required = true) User user) {}

    void optional(@CurrentUser User user) {}

    void details(@CurrentUser UserDetails user) {}

    void name(@CurrentUser String username) {}
  }
}
