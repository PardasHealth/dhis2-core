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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TwoFactorQrStabilityTest extends H2ControllerIntegrationTestBase {
  @Autowired private DhisConfigurationProvider configuration;
  private String originalTotp;

  @BeforeEach
  void enableTotp() {
    originalTotp =
        configuration.getProperties().getProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey());
    configuration.getProperties().setProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey(), "on");
  }

  @AfterEach
  void restoreTotp() {
    if (originalTotp == null)
      configuration.getProperties().remove(ConfigurationKey.TOTP_2FA_ENABLED.getKey());
    else
      configuration
          .getProperties()
          .setProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey(), originalTotp);
  }

  @Test
  void firstLegacyReadRetainsBundledAppEnrollmentFlow() throws Exception {
    var image = legacyQr();
    assertEquals(202, image.getStatus());
    var user = getCurrentUser();
    assertEquals(TwoFactorType.ENROLLING_TOTP, user.getTwoFactorType());
    assertNotNull(user.getSecret());
    assertQrContains(image.getContentAsByteArray(), user.getSecret());
  }

  @Test
  void repeatedLegacyReadPreservesScannedSeedAndCanCompleteEnrollment() throws Exception {
    assertEquals(202, legacyQr().getStatus());
    String scanned = getCurrentUser().getSecret();
    var repeated =
        mvc.perform(
                get("/api/44/2fa/qrCode").accept("application/octet-stream", "application/json"))
            .andReturn()
            .getResponse();
    assertEquals(202, repeated.getStatus());
    assertEquals(scanned, getCurrentUser().getSecret());
    assertQrContains(repeated.getContentAsByteArray(), scanned);
    assertEquals(
        HttpStatus.OK, POST("/2fa/enable", "{'code':'" + new Totp(scanned).now() + "'}").status());
    assertEquals(TwoFactorType.TOTP_ENABLED, getCurrentUser().getTwoFactorType());
  }

  @Test
  void headReadCannotRotatePendingSeed() throws Exception {
    assertEquals(202, legacyQr().getStatus());
    String scanned = getCurrentUser().getSecret();
    var response =
        mvc.perform(head("/api/2fa/qrCode").accept("application/octet-stream", "application/json"))
            .andReturn()
            .getResponse();
    assertEquals(202, response.getStatus());
    assertEquals(scanned, getCurrentUser().getSecret());
  }

  @Test
  void explicitPostStillRotatesAnUnconfirmedSeed() {
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    String first = getCurrentUser().getSecret();
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    assertNotEquals(first, getCurrentUser().getSecret());
    assertEquals(TwoFactorType.ENROLLING_TOTP, getCurrentUser().getTwoFactorType());
  }

  @Test
  void readOfEnabledFactorDoesNotChangeIt() throws Exception {
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    String seed = getCurrentUser().getSecret();
    assertEquals(
        HttpStatus.OK, POST("/2fa/enable", "{'code':'" + new Totp(seed).now() + "'}").status());
    assertEquals(409, legacyQr().getStatus());
    assertEquals(seed, getCurrentUser().getSecret());
    assertEquals(TwoFactorType.TOTP_ENABLED, getCurrentUser().getTwoFactorType());
  }

  @Test
  void disabledTotpStillRejectsPendingReadWithoutChangingIt() throws Exception {
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    String seed = getCurrentUser().getSecret();
    configuration.getProperties().setProperty(ConfigurationKey.TOTP_2FA_ENABLED.getKey(), "off");
    assertEquals(409, legacyQr().getStatus());
    assertEquals(seed, getCurrentUser().getSecret());
  }

  @Test
  void supportedReadEndpointsPreservePostEnrollment() throws Exception {
    assertEquals(HttpStatus.OK, POST("/2fa/enrollTOTP2FA").status());
    String seed = getCurrentUser().getSecret();
    var image =
        mvc.perform(
                get("/api/2fa/qrCodePng").accept("application/octet-stream", "application/json"))
            .andReturn()
            .getResponse();
    assertEquals(202, image.getStatus());
    assertQrContains(image.getContentAsByteArray(), seed);
    assertEquals(HttpStatus.OK, GET("/2fa/qrCodeJson").status());
    assertEquals(seed, getCurrentUser().getSecret());
  }

  @Test
  void missingPendingSeedKeepsExistingLegacyRepairBehavior() throws Exception {
    var user = getCurrentUser();
    user.setTwoFactorType(TwoFactorType.ENROLLING_TOTP);
    user.setSecret(null);
    userService.updateUser(user);
    var image = legacyQr();
    assertEquals(202, image.getStatus());
    assertNotNull(getCurrentUser().getSecret());
    assertQrContains(image.getContentAsByteArray(), getCurrentUser().getSecret());
  }

  private org.springframework.mock.web.MockHttpServletResponse legacyQr() throws Exception {
    return mvc.perform(
            get("/api/2fa/qrCode").accept("application/octet-stream", "application/json"))
        .andReturn()
        .getResponse();
  }

  private static void assertQrContains(byte[] png, String secret) throws Exception {
    var image = ImageIO.read(new ByteArrayInputStream(png));
    assertNotNull(image);
    var data = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
    String uri = new QRCodeReader().decode(data).getText();
    assertTrue(uri.contains("secret=" + secret + "&"));
  }
}
