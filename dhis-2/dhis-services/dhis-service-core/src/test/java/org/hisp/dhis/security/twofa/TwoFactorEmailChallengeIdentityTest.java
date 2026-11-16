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

import java.util.Set;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TwoFactorEmailChallengeIdentityTest {
  private final UserService users = mock(UserService.class);
  private final MessageSender sender = mock(MessageSender.class);
  private TwoFactorAuthService factors;

  @BeforeEach
  void setUp() {
    var settings = mock(SystemSettingsProvider.class, RETURNS_DEEP_STUBS);
    when(settings.getCurrentSettings().getApplicationTitle()).thenReturn("Health");
    var userSettings = mock(UserSettingsService.class, RETURNS_DEEP_STUBS);
    var i18nManager = mock(I18nManager.class);
    when(i18nManager.getI18n(nullable(org.hisp.dhis.common.Locale.class)))
        .thenReturn(mock(I18n.class));
    var response = new OutboundMessageResponse();
    response.setResponseObject(EmailResponse.SENT);
    when(sender.sendMessage(any(), any(), isNull(), isNull(), anySet(), eq(true)))
        .thenReturn(response);
    factors =
        new TwoFactorAuthService(
            settings,
            users,
            sender,
            userSettings,
            i18nManager,
            mock(DhisConfigurationProvider.class));
  }

  @Test
  void disableChallengeStaysWithTheInitiallyResolvedAccount() {
    var selected = account("Owner123456", "owner@example.invalid");
    var namesake = account("Other123456", "other@example.invalid");
    when(users.getUserByUsername("reused-name")).thenReturn(selected, namesake);

    var result = assertThrows(ConflictException.class, () -> factors.disable2FA("reused-name", ""));

    assertEquals(ErrorCode.E3051, result.getCode());
    assertNotEquals("old-challenge", selected.getSecret());
    assertTrue(selected.getSecret().matches("[0-9]{6}\\|[0-9]+"));
    assertEquals("old-challenge", namesake.getSecret());
    verify(users, times(1)).getUserByUsername("reused-name");
    verify(sender).sendMessage(any(), any(), isNull(), isNull(), eq(Set.of(selected)), eq(true));
    verify(users).updateUser(eq(selected), any(SystemUser.class));
    verify(users, never()).updateUser(eq(namesake), any());
  }

  @Test
  void challengeChecksTheSelectedAccountsVerifiedEmail() {
    var selected = account("Owner123456", "owner@example.invalid");
    selected.setVerifiedEmail(null);
    var namesake = account("Other123456", "other@example.invalid");
    when(users.getUserByUsername("reused-name")).thenReturn(selected, namesake);

    var result = assertThrows(ConflictException.class, () -> factors.disable2FA("reused-name", ""));

    assertEquals(ErrorCode.E3043, result.getCode());
    assertEquals("old-challenge", selected.getSecret());
    assertEquals("old-challenge", namesake.getSecret());
    verifyNoInteractions(sender);
  }

  @Test
  void loginChallengeUsesUidDespiteAReusedUsername() throws Exception {
    var selected = account("Owner123456", "owner@example.invalid");
    selected.setUsername("renamed-owner");
    var namesake = account("Other123456", "other@example.invalid");
    when(users.getUser("Owner123456")).thenReturn(selected);
    lenient().when(users.getUserByUsername(anyString())).thenReturn(namesake);

    factors.sendEmail2FACodeByUid("Owner123456");

    assertNotEquals("old-challenge", selected.getSecret());
    assertEquals("old-challenge", namesake.getSecret());
    verify(users, never()).getUserByUsername(anyString());
    verify(sender).sendMessage(any(), any(), isNull(), isNull(), eq(Set.of(selected)), eq(true));
    verify(users).updateUser(eq(selected), any(SystemUser.class));
  }

  @Test
  void deletedLoginOwnerHasNoNameFallback() {
    var namesake = account("Other123456", "other@example.invalid");
    lenient().when(users.getUserByUsername(anyString())).thenReturn(namesake);

    var result =
        assertThrows(ConflictException.class, () -> factors.sendEmail2FACodeByUid("Deleted1234"));

    assertEquals(ErrorCode.E6201, result.getCode());
    assertEquals("old-challenge", namesake.getSecret());
    verify(users, never()).getUserByUsername(anyString());
    verifyNoInteractions(sender);
  }

  @Test
  void missingLoginUidCannotIssueAChallenge() {
    for (String uid : new String[] {null, "", " "}) {
      var result = assertThrows(ConflictException.class, () -> factors.sendEmail2FACodeByUid(uid));
      assertEquals(ErrorCode.E6201, result.getCode());
    }
    verifyNoInteractions(users, sender);
  }

  private User account(String uid, String email) {
    var user = new User();
    user.setUid(uid);
    user.setUsername("reused-name");
    user.setFirstName("Test");
    user.setSurname("Account");
    user.setEmail(email);
    user.setVerifiedEmail(email);
    user.setTwoFactorType(TwoFactorType.EMAIL_ENABLED);
    user.setSecret("old-challenge");
    return user;
  }
}
