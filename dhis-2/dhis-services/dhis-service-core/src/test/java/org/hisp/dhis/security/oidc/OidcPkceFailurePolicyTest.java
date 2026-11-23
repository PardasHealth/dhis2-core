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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

class OidcPkceFailurePolicyTest {
  private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
  private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

  @Test
  void enabledPkceKeepsRfc7636S256AndProviderHints() {
    var resolver = resolver(true, Map.of("prompt", "login"));
    var request = resolver.resolve(request(), "test");
    assertNotNull(request);
    assertEquals(VERIFIER, request.getAttribute(PkceParameterNames.CODE_VERIFIER));
    assertEquals(
        CHALLENGE, request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE));
    assertEquals(
        "S256", request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
    assertEquals("login", request.getAdditionalParameters().get("prompt"));
    assertFalse(request.getAuthorizationRequestUri().contains(VERIFIER));
    var query =
        UriComponentsBuilder.fromUriString(request.getAuthorizationRequestUri())
            .build()
            .getQueryParams();
    assertEquals(List.of(CHALLENGE), query.get(PkceParameterNames.CODE_CHALLENGE));
    assertEquals(List.of("S256"), query.get(PkceParameterNames.CODE_CHALLENGE_METHOD));
  }

  @Test
  void missingSha256NeverReturnsAPlainChallengeRequest() {
    var resolver = resolver(true, Map.of());
    try (var hashes = mockStatic(MessageDigest.class, CALLS_REAL_METHODS)) {
      hashes
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("synthetic provider details " + VERIFIER));
      var error =
          assertThrows(IllegalStateException.class, () -> resolver.resolve(request(), "test"));
      assertEquals("PKCE S256 is unavailable", error.getMessage());
      assertNull(error.getCause());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"code_challenge", "code_challenge_method", "code_verifier"})
  void enabledPkceRejectsReservedExtraOverrides(String key) {
    var resolver = resolver(true, Map.of(key, "synthetic-override"));
    var error =
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request(), "test"));
    assertEquals("PKCE parameters cannot be overridden", error.getMessage());
  }

  @Test
  void failedHashLeavesPrivatePkceMapsUntouched() {
    var resolver = resolver(true, Map.of());
    Map<String, Object> attributes = new HashMap<>(Map.of("existing", "attribute"));
    Map<String, Object> parameters = new HashMap<>(Map.of("existing", "parameter"));
    try (var hashes = mockStatic(MessageDigest.class, CALLS_REAL_METHODS)) {
      hashes
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("synthetic unavailable digest"));
      assertThrows(
          IllegalStateException.class,
          () ->
              ReflectionTestUtils.invokeMethod(
                  resolver, "addPkceParameters", attributes, parameters));
      assertEquals(Map.of("existing", "attribute"), attributes);
      assertEquals(Map.of("existing", "parameter"), parameters);
    }
  }

  @Test
  void disabledCustomPkcePreservesSpringS256AndUnrelatedHints() {
    var resolver = resolver(false, Map.of("prompt", "login"));
    ReflectionTestUtils.setField(
        resolver,
        "secureKeyGenerator",
        (StringKeyGenerator)
            () -> {
              throw new AssertionError("Custom generator must not run when disabled");
            });
    var request = resolver.resolve(request(), "test");
    assertEquals("login", request.getAdditionalParameters().get("prompt"));
    assertEquals(
        "S256", request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
    assertNotNull(request.getAttribute(PkceParameterNames.CODE_VERIFIER));
  }

  @Test
  void springGeneratedPkceAlsoFailsClosedOnMissingSha256() {
    var resolver = resolver(false, Map.of());
    try (var hashes = mockStatic(MessageDigest.class, CALLS_REAL_METHODS)) {
      hashes
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("synthetic unavailable digest"));
      var error =
          assertThrows(IllegalStateException.class, () -> resolver.resolve(request(), "test"));
      assertEquals("PKCE S256 is required", error.getMessage());
      assertNull(error.getCause());
    }
  }

  @Test
  void springGeneratedPkceFieldsAlsoRejectExtraOverrides() {
    var resolver = resolver(false, Map.of("code_challenge", "synthetic-override"));
    var error =
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request(), "test"));
    assertEquals("PKCE parameters cannot be overridden", error.getMessage());
  }

  @Test
  void openIdRequestKeepsNonceAndS256() {
    var request = resolver(true, Map.of(), true).resolve(request(), "test");
    assertNotNull(request.getAdditionalParameters().get("nonce"));
    assertNotNull(request.getAttribute("nonce"));
    assertEquals(
        CHALLENGE, request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE));
    assertEquals(
        "S256", request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void customHashClearsItsOwnedVerifierBytes(boolean fails) throws Exception {
    var observed = new AtomicReference<byte[]>();
    var digest = mock(MessageDigest.class);
    var failure = new IllegalStateException("synthetic provider failure");
    byte[] output = new byte[32];
    when(digest.digest(any(byte[].class)))
        .thenAnswer(
            call -> {
              byte[] input = call.getArgument(0);
              assertArrayEquals(VERIFIER.getBytes(StandardCharsets.US_ASCII), input);
              observed.set(input);
              if (fails) throw failure;
              return output;
            });
    try (var hashes = mockStatic(MessageDigest.class)) {
      hashes.when(() -> MessageDigest.getInstance("SHA-256")).thenReturn(digest);
      if (fails) {
        assertSame(
            failure,
            assertThrows(
                IllegalStateException.class,
                () ->
                    ReflectionTestUtils.invokeMethod(
                        DhisCustomAuthorizationRequestResolver.class, "createHash", VERIFIER)));
      } else {
        assertEquals(
            Base64.getUrlEncoder().withoutPadding().encodeToString(output),
            ReflectionTestUtils.invokeMethod(
                DhisCustomAuthorizationRequestResolver.class, "createHash", VERIFIER));
      }
      assertNotNull(observed.get());
      assertArrayEquals(new byte[observed.get().length], observed.get());
    }
  }

  private static DhisCustomAuthorizationRequestResolver resolver(
      boolean enabled, Map<String, Object> extras) {
    return resolver(enabled, extras, false);
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
