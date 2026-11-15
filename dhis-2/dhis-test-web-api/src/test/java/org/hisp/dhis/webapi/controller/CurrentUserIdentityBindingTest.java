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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.user.UserConstants.LOGIN_MAX_FAILED_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CurrentUserIdentityBindingTest extends H2ControllerIntegrationTestBase {
  private static final String SEED = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
  private static final String OTHER_SEED = "KRSXG5DSNFXGOIDBKRSXG5DSNFXGOIDB";
  @Autowired private DhisConfigurationProvider config;
  private String previousTotp;
  private User owner;
  private User namesake;

  @BeforeEach
  void enableTotp() {
    previousTotp = config.getProperty(ConfigurationKey.TOTP_2FA_ENABLED);
    config.getProperties().setProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey(), "on");
  }

  @AfterEach
  void restoreTotp() {
    config.getProperties().setProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey(), previousTotp);
    if (owner != null) userService.registerSuccess2FADisable(owner.getUsername());
    userService.registerSuccess2FADisable("identity-old");
  }

  private void renameAndReuse(TwoFactorType type) {
    owner = switchToNewUser("identity-old");
    var originalContext = SecurityContextHolder.getContext();
    owner.setUsername("identity-renamed");
    owner.setTwoFactorType(type);
    owner.setSecret(type == TwoFactorType.NOT_ENABLED ? null : SEED);
    userService.updateUser(owner, new SystemUser());
    injectSecurityContextUser(getAdminUser());
    namesake = createUserWithAuth("identity-replacement");
    namesake.setUsername("identity-old");
    userService.updateUser(namesake, new SystemUser());
    SecurityContextHolder.setContext(originalContext);
    startNewRequestSession(); // Persist mutations without refreshing the authenticated snapshot.
    assertFalse(CurrentUserUtil.getCurrentUserDetails().isSuper());
    assertEquals("identity-old", CurrentUserUtil.getCurrentUsername());
  }

  @Test
  void profileEntityRemainsBoundToTheAuthenticatedUidAfterNameReuse() {
    renameAndReuse(TwoFactorType.NOT_ENABLED);
    var me = GET("/me?fields=id,username").content(HttpStatus.OK);
    assertEquals(owner.getUid(), me.getString("id").string());
    assertEquals("identity-renamed", me.getString("username").string());
  }

  @Test
  void totpEnrollmentCannotBeRedirectedToTheNewUsernameOwner() {
    renameAndReuse(TwoFactorType.NOT_ENABLED);
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    assertEquals(
        TwoFactorType.ENROLLING_TOTP, userService.getUser(owner.getUid()).getTwoFactorType());
    assertEquals(
        TwoFactorType.NOT_ENABLED, userService.getUser(namesake.getUid()).getTwoFactorType());
    assertNull(userService.getUser(namesake.getUid()).getSecret());
  }

  @Test
  void enableUsesTheAuthenticatedAccountProofAfterRename() {
    renameAndReuse(TwoFactorType.ENROLLING_TOTP);
    assertEquals(
        HttpStatus.OK, POST("/2fa/enable", "{'code':'" + new Totp(SEED).now() + "'}").status());
    assertEquals(
        TwoFactorType.TOTP_ENABLED, userService.getUser(owner.getUid()).getTwoFactorType());
    assertEquals(
        TwoFactorType.NOT_ENABLED, userService.getUser(namesake.getUid()).getTwoFactorType());
  }

  @Test
  void disableUsesTheAuthenticatedAccountAfterRename() {
    renameAndReuse(TwoFactorType.TOTP_ENABLED);
    assertEquals(
        HttpStatus.OK, POST("/2fa/disable", "{'code':'" + new Totp(SEED).now() + "'}").status());
    assertEquals(TwoFactorType.NOT_ENABLED, userService.getUser(owner.getUid()).getTwoFactorType());
    assertNull(userService.getUser(owner.getUid()).getSecret());
    assertNull(userService.getUser(namesake.getUid()).getSecret());
  }

  @Test
  void qrReadCannotExposeTheUsernameNamesakesSecret() {
    renameAndReuse(TwoFactorType.ENROLLING_TOTP);
    var replacement = userService.getUser(namesake.getUid());
    replacement.setTwoFactorType(TwoFactorType.ENROLLING_TOTP);
    replacement.setSecret(OTHER_SEED);
    userService.updateUser(replacement, new SystemUser());
    startNewRequestSession();
    var qr = GET("/2fa/qrCodeJson").content(HttpStatus.OK);
    assertEquals(SEED, qr.getString("base32Secret").string());
  }

  @Test
  void renamedAccountDisableFailuresReachTheSameCounterAsTheLockCheck() {
    renameAndReuse(TwoFactorType.TOTP_ENABLED);
    for (int i = 0; i < LOGIN_MAX_FAILED_ATTEMPTS; i++) {
      assertEquals(HttpStatus.FORBIDDEN, POST("/2fa/disable", "{'code':'wrong'}").status());
    }
    assertEquals(HttpStatus.CONFLICT, POST("/2fa/disable", "{'code':'wrong'}").status());
    assertTrue(userService.is2FADisableEndpointLocked("identity-renamed"));
    assertFalse(userService.is2FADisableEndpointLocked("identity-old"));
  }
}
