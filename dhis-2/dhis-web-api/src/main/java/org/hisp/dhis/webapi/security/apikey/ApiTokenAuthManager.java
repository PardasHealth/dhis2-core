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

import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Processes API token authentication requests, looks for the 'Authorization' header containing an
 * API token. If the token is found, the request will be authenticated only if the token is not
 * expired and the request constraint rules are matching.
 */
@Service
public class ApiTokenAuthManager implements AuthenticationManager {
  private final ApiTokenService apiTokenService;
  private final UserService userService;

  public ApiTokenAuthManager(ApiTokenService apiTokenService, @Lazy UserService userService) {
    this.userService = userService;
    this.apiTokenService = apiTokenService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    final String tokenKey = ((ApiTokenAuthenticationToken) authentication).getTokenKey();

    // Account status, permissions and token constraints can change between requests,
    // including on another node. Do not reuse a process-local authentication snapshot.
    ApiToken apiToken = apiTokenService.getByKey(tokenKey);
    if (apiToken == null) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token does not exists"));
    }

    validateTokenExpiry(apiToken.getExpire());
    UserDetails currentUserDetails = validateAndCreateUserDetails(apiToken.getCreatedBy());

    // The filter attaches request details and authentication state to this object.
    // Each request must have its own instance.
    return new ApiTokenAuthenticationToken(apiToken, currentUserDetails);
  }

  private UserDetails validateAndCreateUserDetails(User createdBy) {
    if (createdBy == null || createdBy.getUid() == null || createdBy.getUid().isBlank()) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token does not have any owner."));
    }

    // User lookup and UserDetails creation must happen within a single transaction, so that lazy
    // collections (e.g. User.userRoles) are accessible. This code can run on endpoints excluded
    // from open-session-in-view, where no Hibernate session is otherwise available.
    UserDetails userDetails = userService.createUserDetailsByUsername(createdBy.getUsername());
    // A username is mutable and can be reused by a different account. A current
    // name lookup must never transfer the token to a different stable identity.
    if (userDetails == null || !createdBy.getUid().equals(userDetails.getUid())) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token owner does not exists."));
    }

    boolean isTwoFactorDisabled = !userDetails.isTwoFactorEnabled();
    boolean enabled = userDetails.isEnabled();
    boolean credentialsNonExpired = userDetails.isCredentialsNonExpired();
    boolean accountNonLocked = userDetails.isAccountNonLocked();
    boolean accountNonExpired = userDetails.isAccountNonExpired();

    if (ObjectUtils.anyIsFalse(
        enabled, isTwoFactorDisabled, credentialsNonExpired, accountNonLocked, accountNonExpired)) {

      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token is disabled, locked or 2FA is enabled."));
    }

    return userDetails;
  }

  private static void validateTokenExpiry(Long expiry) {
    if (expiry == null) {
      throw new ApiTokenAuthenticationException(
          ApiTokenErrors.invalidToken("The API token does not have an expiry."));
    }
    if (expiry <= System.currentTimeMillis()) {
      throw new ApiTokenExpiredException("Failed to authenticate API token, token has expired.");
    }
  }
}
