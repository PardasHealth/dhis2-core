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
package org.hisp.dhis.security.twofa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TwoFactorIdentityMutationTest {
  private UserService users;
  private TwoFactorAuthService factors;
  private UserDetails actor;

  @BeforeEach
  void setUp() {
    users = mock(UserService.class);
    factors =
        new TwoFactorAuthService(
            mock(SystemSettingsProvider.class),
            users,
            mock(MessageSender.class),
            mock(UserSettingsService.class),
            mock(I18nManager.class),
            mock(DhisConfigurationProvider.class));
    actor = UserDetails.empty().uid("Owner123456").username("retired-name").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(actor, null, List.of()));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectedDisableProofCountsAgainstTheResolvedAccount() {
    var user = target("Owner123456", "renamed-user");
    when(users.getUserByUsername("renamed-user")).thenReturn(user);
    assertThrows(ForbiddenException.class, () -> factors.disable2FA("renamed-user", "wrong"));
    verify(users).registerFailed2FADisableAttempt("renamed-user");
    verify(users, never()).registerFailed2FADisableAttempt("retired-name");
  }

  @Test
  void privilegedResetMutatesTheUidResolvedTargetWithoutASecondNameLookup() throws Exception {
    var user = target("Target12345", "shared-name");
    var namesake = target("Other123456", "shared-name");
    when(users.getUser("Target12345")).thenReturn(user);
    when(users.canCurrentUserCanModify(eq(actor), eq(user), any())).thenReturn(true);
    lenient().when(users.getUserByUsername("shared-name")).thenReturn(namesake);
    factors.privileged2FADisable(
        actor, "Target12345", error -> fail("Unexpected authorization error"));
    assertNull(user.getSecret());
    assertEquals(TwoFactorType.NOT_ENABLED, user.getTwoFactorType());
    assertNotNull(namesake.getSecret());
    verify(users).updateUser(user, actor);
    verify(users, never()).getUserByUsername(anyString());
  }

  @Test
  void successfulSelfDisableDoesNotRedirectThroughASecondNameLookup() throws Exception {
    var user = target("Owner123456", "renamed-user");
    user.setTwoFactorType(TwoFactorType.EMAIL_ENABLED);
    user.setSecret("123456|" + (System.currentTimeMillis() + 600000));
    var namesake = target("Other123456", "renamed-user");
    when(users.getUserByUsername("renamed-user")).thenReturn(user, namesake);
    factors.disable2FA("renamed-user", "123456");
    assertNull(user.getSecret());
    assertNotNull(namesake.getSecret());
    verify(users, times(1)).getUserByUsername("renamed-user");
    verify(users).updateUser(user, actor);
  }

  private User target(String uid, String name) {
    var user = new User();
    user.setUid(uid);
    user.setUsername(name);
    user.setTwoFactorType(TwoFactorType.TOTP_ENABLED);
    user.setSecret("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP");
    return user;
  }
}
