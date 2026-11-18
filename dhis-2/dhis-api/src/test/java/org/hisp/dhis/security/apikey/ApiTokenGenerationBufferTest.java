/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.security.apikey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiTokenGenerationBufferTest {
  static Stream<Arguments> generationOutcomes() {
    return Arrays.stream(ApiTokenType.values())
        .flatMap(type -> IntStream.range(0, 3).mapToObj(outcome -> Arguments.of(type, outcome)));
  }

  @ParameterizedTest
  @MethodSource("generationOutcomes")
  void clearsOwnedComponentsAfterAssemblyOrFailure(ApiTokenType type, int outcome) {
    char[] code = new char[type.getLength() - (outcome == 2 ? 1 : 0)];
    Arrays.fill(code, 's');
    char[] original = code.clone();
    char[] checksum = "0000000000".toCharArray();
    char[] originalChecksum = checksum.clone();
    var error = new IllegalStateException("Synthetic checksum generation error");
    try (var random = mockStatic(CodeGenerator.class);
        var generator = mockStatic(ApiKeyTokenGenerator.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomCode(type.getLength())).thenReturn(code);
      generator.when(() -> ApiKeyTokenGenerator.generatePatToken(type)).thenCallRealMethod();
      org.mockito.stubbing.Answer<char[]> generate =
          call -> {
            assertSame(code, call.getArgument(0));
            if (outcome == 1) throw error;
            return checksum;
          };
      generator
          .when(() -> ApiKeyTokenGenerator.generateCRC32InDecimal(any(char[].class)))
          .thenAnswer(generate);
      generator
          .when(() -> ApiKeyTokenGenerator.generateCRC32InBase62(any(char[].class)))
          .thenAnswer(generate);

      if (outcome == 0) {
        char[] returned = ApiKeyTokenGenerator.generatePatToken(type);
        assertArrayEquals(
            (type.getPrefix() + "_" + new String(original) + new String(originalChecksum))
                .toCharArray(),
            returned);
        assertNotSame(code, returned);
        assertCleared(checksum);
      } else if (outcome == 1) {
        assertSame(
            error,
            assertThrows(
                IllegalStateException.class, () -> ApiKeyTokenGenerator.generatePatToken(type)));
      } else {
        assertThrows(
            IllegalArgumentException.class, () -> ApiKeyTokenGenerator.generatePatToken(type));
      }
      assertCleared(code);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedWrapperCreationClearsUnreturnedPlaintext(boolean fatal) {
    var type = ApiTokenType.getDefaultPatType();
    char[] plaintext = "d2p_synthetic-plaintext".toCharArray();
    Throwable error =
        fatal
            ? new AssertionError("Synthetic hashing error")
            : new IllegalStateException("Synthetic hashing error");
    try (var generator = mockStatic(ApiKeyTokenGenerator.class)) {
      generator
          .when(() -> ApiKeyTokenGenerator.generatePersonalAccessToken(null, 1234L, "synthetic"))
          .thenCallRealMethod();
      generator.when(() -> ApiKeyTokenGenerator.generatePatToken(type)).thenReturn(plaintext);
      generator
          .when(() -> ApiKeyTokenGenerator.hashToken(plaintext))
          .thenAnswer(
              call -> {
                throw error;
              });

      assertSame(
          error,
          assertThrows(
              Throwable.class,
              () -> ApiKeyTokenGenerator.generatePersonalAccessToken(null, 1234L, "synthetic")));
      assertCleared(plaintext);
    }
  }

  @Test
  void successfulWrapperTransfersPlaintextOwnershipToCaller() {
    var type = ApiTokenType.getDefaultPatType();
    char[] plaintext = "d2p_synthetic-plaintext".toCharArray();
    char[] original = plaintext.clone();
    try (var generator = mockStatic(ApiKeyTokenGenerator.class)) {
      generator
          .when(() -> ApiKeyTokenGenerator.generatePersonalAccessToken(null, 1234L, "synthetic"))
          .thenCallRealMethod();
      generator.when(() -> ApiKeyTokenGenerator.generatePatToken(type)).thenReturn(plaintext);
      generator.when(() -> ApiKeyTokenGenerator.hashToken(plaintext)).thenReturn("synthetic-hash");

      var wrapper = ApiKeyTokenGenerator.generatePersonalAccessToken(null, 1234L, "synthetic");
      assertSame(plaintext, wrapper.getPlaintextToken());
      assertArrayEquals(original, wrapper.getPlaintextToken());
      assertEquals("synthetic-hash", wrapper.getApiToken().getKey());
    }
  }

  @ParameterizedTest
  @EnumSource(ApiTokenType.class)
  void realGeneratedTokensRetainChecksumAndHashCompatibility(ApiTokenType type) throws Exception {
    char[] token = ApiKeyTokenGenerator.generatePatToken(type);
    char[] original = token.clone();
    assertEquals(type, ApiTokenType.fromToken(token));
    assertTrue(ApiKeyTokenGenerator.isValidTokenChecksum(token));
    String expected =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance(type.getHashType())
                    .digest(new String(token).getBytes(StandardCharsets.UTF_8)));
    assertEquals(expected, ApiKeyTokenGenerator.hashToken(token));
    assertArrayEquals(original, token);
  }

  private static void assertCleared(char[] buffer) {
    assertTrue(buffer.length > 0);
    assertTrue(
        IntStream.range(0, buffer.length).allMatch(i -> buffer[i] == '\0'),
        "Generation-owned temporary token characters must be cleared");
  }
}
