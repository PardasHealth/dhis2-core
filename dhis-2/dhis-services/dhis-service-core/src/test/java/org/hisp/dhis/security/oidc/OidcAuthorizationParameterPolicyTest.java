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
package org.hisp.dhis.security.oidc;

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_PKCE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTRA_REQUEST_PARAMETERS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

class OidcAuthorizationParameterPolicyTest {
  private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

  @ParameterizedTest
  @MethodSource("reservedOverrides")
  void rejectsSecurityParameterOverridesBeforeReturningARedirect(
      String key, boolean pkce, boolean openid, boolean explicitRegistration) {
    var extras = Map.<String, Object>of(key, "synthetic-sensitive-override");
    var resolver = resolver(pkce, extras, openid);
    var error =
        assertThrows(IllegalArgumentException.class, () -> resolve(resolver, explicitRegistration));
    assertEquals("Authorization security parameters cannot be overridden", error.getMessage());
    assertNull(error.getCause());
    assertEquals(Map.of(key, "synthetic-sensitive-override"), extras);
  }

  @ParameterizedTest
  @MethodSource("modes")
  void preservesGeneratedSecurityFieldsAndProviderHints(
      boolean pkce, boolean openid, boolean explicitRegistration) {
    var extras =
        Map.<String, Object>of(
            "prompt", "login", "login_hint", "test-user", "hd", "example.invalid");
    var request = resolve(resolver(pkce, extras, openid), explicitRegistration);
    assertNotNull(request);
    var query =
        new LinkedMultiValueMap<>(
            UriComponentsBuilder.fromUriString(request.getAuthorizationRequestUri())
                .build()
                .getQueryParams());
    query.replaceAll(
        (key, values) ->
            values.stream().map(value -> UriUtils.decode(value, StandardCharsets.UTF_8)).toList());
    assertEquals(List.of(request.getState()), query.get("state"));
    assertEquals(List.of(request.getClientId()), query.get("client_id"));
    assertEquals(List.of(request.getRedirectUri()), query.get("redirect_uri"));
    assertEquals(List.of("code"), query.get("response_type"));
    assertEquals(List.of(openid ? "openid" : "read"), query.get("scope"));
    for (var entry : extras.entrySet()) {
      assertEquals(List.of(entry.getValue()), query.get(entry.getKey()));
    }
    if (openid) {
      assertNotNull(request.getAttribute("nonce"));
      assertEquals(List.of(request.getAdditionalParameters().get("nonce")), query.get("nonce"));
    } else {
      assertNull(query.get("nonce"));
    }
  }

  private static Stream<Arguments> reservedOverrides() {
    return Stream.of("state", "nonce", "client_id", "redirect_uri", "response_type", "scope")
        .flatMap(key -> modeValues().map(mode -> Arguments.of(key, mode[0], mode[1], mode[2])));
  }

  private static Stream<Arguments> modes() {
    return modeValues().map(mode -> Arguments.of(mode[0], mode[1], mode[2]));
  }

  private static Stream<Boolean[]> modeValues() {
    return Stream.of(false, true)
        .flatMap(
            pkce ->
                Stream.of(false, true)
                    .flatMap(
                        openid ->
                            Stream.of(false, true)
                                .map(explicit -> new Boolean[] {pkce, openid, explicit})));
  }

  private static OAuth2AuthorizationRequest resolve(
      DhisCustomAuthorizationRequestResolver resolver, boolean explicitRegistration) {
    return explicitRegistration ? resolver.resolve(request(), "test") : resolver.resolve(request());
  }

  private static DhisCustomAuthorizationRequestResolver resolver(
      boolean enabled, Map<String, Object> extras, boolean openid) {
    var registration =
        ClientRegistration.withRegistrationId("test")
            .clientId("test-client")
            .clientSecret("synthetic-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope(openid ? "openid" : "read")
            .jwkSetUri("https://idp.example.invalid/jwks")
            .authorizationUri("https://idp.example.invalid/authorize")
            .tokenUri("https://idp.example.invalid/token")
            .providerConfigurationMetadata(
                Map.of(ENABLE_PKCE, enabled ? "on" : "off", EXTRA_REQUEST_PARAMETERS, extras))
            .build();
    var repository = mock(DhisOidcProviderRepository.class);
    when(repository.findByRegistrationId("test")).thenReturn(registration);
    var resolver = new DhisCustomAuthorizationRequestResolver();
    ReflectionTestUtils.setField(resolver, "clientRegistrationRepository", repository);
    ReflectionTestUtils.setField(
        resolver, "secureKeyGenerator", (StringKeyGenerator) () -> VERIFIER);
    resolver.init();
    return resolver;
  }

  private static MockHttpServletRequest request() {
    return new MockHttpServletRequest("GET", "/oauth2/authorization/test");
  }
}
