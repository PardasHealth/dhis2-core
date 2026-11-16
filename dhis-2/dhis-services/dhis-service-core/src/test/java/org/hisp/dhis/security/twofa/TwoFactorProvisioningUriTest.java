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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TwoFactorProvisioningUriTest {
  private static final String SECRET = "JBSWY3DPEHPK3PXP";

  private static void assertProvisioning(String app, String username, String issuer) {
    URI uri = URI.create(TwoFactorAuthUtils.generateTOTP2FAURL(app, SECRET, username));
    assertEquals("otpauth", uri.getScheme());
    assertEquals("totp", uri.getHost());
    assertNull(uri.getRawFragment());
    assertEquals("/" + issuer + ":" + username, uri.getPath());
    assertEquals(1, uri.getRawPath().chars().filter(c -> c == ':').count());
    Map<String, String> query =
        Arrays.stream(uri.getRawQuery().split("&"))
            .map(part -> part.split("=", 2))
            .collect(
                Collectors.toMap(
                    part -> part[0], part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
    assertEquals(Map.of("secret", SECRET, "issuer", issuer), query);
  }

  @Test
  void preservesOrdinaryProvisioning() {
    assertProvisioning("Health Service", "alice@example.com", "DHIS2_Health Service");
  }

  @Test
  void containsUsernameDelimitersWithinAccountLabel() {
    assertProvisioning("Health", "alice?secret=other&issuer=other#fragment:%2F+ /", "DHIS2_Health");
  }

  @Test
  void containsIssuerDelimitersWithinIssuerValue() {
    assertProvisioning(
        "Health?x=y&secret=other#fragment:%20+ /",
        "alice", "DHIS2_Health?x=y&secret=other#fragment:%20+ /");
  }

  @Test
  void encodesUnicodeAccountNameAndRetainsIssuerNormalization() {
    assertProvisioning("  Santé  ", "علی + 東京", "DHIS2_SantX");
  }
}
