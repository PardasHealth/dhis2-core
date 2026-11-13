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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiTokenIpConstraintTest {
  private static final String ALLOWED = "198.51.100.10";
  private static final String OTHER = "203.0.113.20";
  private static final String VALID_TOKEN =
      "d2p_lyt5j9WqXaVzJsUhvGJ1w0Ea5GL4jkszazPSWo5HWLFC1QnPCi";
  private ApiToken token;
  private ApiTokenAuthenticationToken authentication;
  private Dhis2ApiTokenFilter filter;
  private FilterChain chain;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    token = new ApiToken();
    UserDetails user = mock(UserDetails.class);
    doReturn(List.of()).when(user).getAuthorities();
    authentication = new ApiTokenAuthenticationToken(token, user);
    ApiTokenAuthManager manager = mock(ApiTokenAuthManager.class);
    when(manager.authenticate(any())).thenReturn(authentication);
    filter =
        new Dhis2ApiTokenFilter(
            manager,
            (req, res, failure) -> res.setStatus(401),
            mock(DefaultAuthenticationEventPublisher.class));
    chain = mock(FilterChain.class);
    request = new MockHttpServletRequest("GET", "/api/me");
    request.addHeader("Authorization", "ApiToken " + VALID_TOKEN);
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void spoofedForwardedAddressCannotSatisfyAnIpConstraint() throws Exception {
    token.addIpToAllowedList(ALLOWED);
    request.setRemoteAddr(OTHER);
    request.addHeader("X-Forwarded-For", ALLOWED);
    assertDenied();
  }

  @Test
  void allowedRemoteAddressAuthenticatesWithoutForwardedHeaders() throws Exception {
    token.addIpToAllowedList(ALLOWED);
    request.setRemoteAddr(ALLOWED);
    assertAccepted();
  }

  @Test
  void disallowedRemoteAddressIsDeniedWithoutForwardedHeaders() throws Exception {
    token.addIpToAllowedList(ALLOWED);
    request.setRemoteAddr(OTHER);
    assertDenied();
  }

  @Test
  void rawForwardedHeaderCannotOverrideAllowedServletAddress() throws Exception {
    token.addIpToAllowedList(ALLOWED);
    request.setRemoteAddr(ALLOWED);
    request.addHeader("X-Forwarded-For", OTHER + ", 192.0.2.1");
    assertAccepted();
  }

  @Test
  void servletResolvedIpv6AddressCanMatchTheConstraint() throws Exception {
    token.addIpToAllowedList("2001:db8::10");
    request.setRemoteAddr("2001:db8::10");
    assertAccepted();
  }

  @Test
  void tokensWithoutIpConstraintsRetainTheirExistingBehavior() throws Exception {
    request.setRemoteAddr(OTHER);
    request.addHeader("X-Forwarded-For", ALLOWED);
    assertAccepted();
  }

  @Test
  void matchingIpDoesNotBypassTheMethodConstraint() throws Exception {
    token.addIpToAllowedList(ALLOWED);
    token.addMethodToAllowedList("POST");
    request.setRemoteAddr(ALLOWED);
    assertDenied();
  }

  private void assertAccepted() throws Exception {
    filter.doFilter(request, response, chain);
    assertEquals(200, response.getStatus());
    verify(chain).doFilter(request, response);
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    assertTrue(authentication.isAuthenticated());
  }

  private void assertDenied() throws Exception {
    filter.doFilter(request, response, chain);
    assertEquals(401, response.getStatus());
    verifyNoInteractions(chain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
