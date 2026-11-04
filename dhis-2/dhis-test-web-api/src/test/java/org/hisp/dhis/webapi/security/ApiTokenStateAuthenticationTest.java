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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.http.HttpClientAdapter.ApiTokenHeader;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.test.webapi.ControllerWithApiTokenAuthTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/** Fork regressions for security-state changes after successful API-token authentication. */
@ActiveProfiles("cache-test")
class ApiTokenStateAuthenticationTest extends ControllerWithApiTokenAuthTestBase {
  private static final String URI = "/me?fields=settings,id";

  @Autowired private ApiTokenService apiTokenService;

  @BeforeAll
  static void setUpClass() {
    DhisWebApiWebSecurityConfig.setApiContextPath("");
  }

  @Test
  void testDisabledOwnerCannotReusePreviouslyAuthenticatedToken() {
    User owner = switchToNewUser("token-owner", "ALL");
    String plaintext = new String(createNewToken().getPlaintextToken());
    assertEquals(
        owner.getUid(), GET(URI, ApiTokenHeader(plaintext)).content().as(JsonUser.class).getId());

    injectAdminIntoSecurityContext();
    owner.setDisabled(true);
    userService.updateUser(owner);
    hibernateService.flushSession();

    GET(URI, ApiTokenHeader(plaintext)).error(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void testChangedConstraintsApplyToPreviouslyAuthenticatedToken() {
    ApiKeyTokenGenerator.TokenWrapper wrapper = createNewToken();
    String plaintext = new String(wrapper.getPlaintextToken());
    GET(URI, ApiTokenHeader(plaintext)).content(HttpStatus.OK);

    injectAdminIntoSecurityContext();
    wrapper.getApiToken().addMethodToAllowedList("POST");
    apiTokenService.update(wrapper.getApiToken());
    hibernateService.flushSession();

    assertEquals(
        "Failed to authenticate API token, request http method is not allowed.",
        GET(URI, ApiTokenHeader(plaintext)).error(HttpStatus.UNAUTHORIZED).getMessage());
  }

  private ApiKeyTokenGenerator.TokenWrapper createNewToken() {
    long expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
    ApiKeyTokenGenerator.TokenWrapper wrapper = generatePersonalAccessToken(null, expiry, null);
    apiTokenService.save(wrapper.getApiToken());
    return wrapper;
  }
}
