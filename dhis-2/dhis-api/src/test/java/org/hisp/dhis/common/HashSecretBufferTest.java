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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HashSecretBufferTest {
  static Stream<Arguments> ownedOutcomes() {
    return IntStream.range(0, 8)
        .mapToObj(n -> Arguments.of((n & 4) != 0, (n & 2) != 0, (n & 1) != 0));
  }

  static Stream<Arguments> callerOutcomes() {
    return IntStream.range(0, 4).mapToObj(n -> Arguments.of((n & 2) != 0, (n & 1) != 0));
  }

  static Stream<Arguments> utf8Vectors() {
    return Stream.of("", "ASCII", "سلام 東京", "A 😀 Z")
        .flatMap(text -> Stream.of(Arguments.of(text, false), Arguments.of(text, true)));
  }

  @ParameterizedTest
  @MethodSource("ownedOutcomes")
  void clearsOwnedUtf8StorageAfterHashOrFailure(boolean sha512, boolean textInput, boolean fails) {
    String text = "synthetic-東京";
    char[] chars = text.toCharArray();
    char[] original = chars.clone();
    var observed = new AtomicReference<byte[]>();
    var function = mock(HashFunction.class);
    var error = new IllegalStateException("Synthetic hash failure");
    var result = HashCode.fromInt(12345);
    when(function.hashBytes(any(byte[].class)))
        .thenAnswer(
            call -> {
              observed.set(call.getArgument(0));
              if (fails) throw error;
              return result;
            });
    when(function.hashBytes(any(ByteBuffer.class)))
        .thenAnswer(
            call -> {
              ByteBuffer buffer = call.getArgument(0);
              observed.set(buffer.array());
              buffer.position(
                  buffer
                      .limit()); // Cleanup must cover the whole allocation even after consumption.
              if (fails) throw error;
              return result;
            });
    try (var hashing = mockStatic(Hashing.class)) {
      hashing.when(Hashing::sha256).thenReturn(function);
      hashing.when(Hashing::sha512).thenReturn(function);
      if (fails) {
        assertSame(
            error,
            assertThrows(IllegalStateException.class, () -> hash(sha512, textInput, text, chars)));
      } else {
        assertEquals(result.toString(), hash(sha512, textInput, text, chars));
      }
      assertArrayEquals(original, chars);
      assertNotNull(observed.get());
      assertTrue(observed.get().length > 0);
      assertTrue(
          IntStream.range(0, observed.get().length).allMatch(i -> observed.get()[i] == 0),
          "Hash-helper owned UTF-8 storage must be cleared");
    }
  }

  @ParameterizedTest
  @MethodSource("callerOutcomes")
  void byteArrayOverloadRetainsCallerOwnership(boolean sha512, boolean fails) {
    byte[] caller = "synthetic-input".getBytes(StandardCharsets.UTF_8);
    byte[] original = caller.clone();
    var function = mock(HashFunction.class);
    when(function.hashBytes(caller))
        .thenAnswer(
            call -> {
              if (fails) throw new IllegalStateException("Synthetic hash failure");
              return HashCode.fromInt(12345);
            });
    try (var hashing = mockStatic(Hashing.class)) {
      hashing.when(Hashing::sha256).thenReturn(function);
      hashing.when(Hashing::sha512).thenReturn(function);
      if (fails) {
        assertThrows(
            IllegalStateException.class,
            () -> {
              if (sha512) HashUtils.hashSHA512(caller);
              else HashUtils.hashSHA256(caller);
            });
      } else if (sha512) {
        HashUtils.hashSHA512(caller);
      } else {
        HashUtils.hashSHA256(caller);
      }
      assertArrayEquals(original, caller);
    }
  }

  @ParameterizedTest
  @MethodSource("utf8Vectors")
  void matchesStandardDigestForEveryInputOverload(String text, boolean sha512) throws Exception {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    byte[] original = bytes.clone();
    char[] chars = text.toCharArray();
    char[] originalChars = chars.clone();
    String expected =
        HexFormat.of()
            .formatHex(MessageDigest.getInstance(sha512 ? "SHA-512" : "SHA-256").digest(bytes));
    assertEquals(expected, hash(sha512, true, text, chars));
    assertEquals(expected, hash(sha512, false, text, chars));
    assertEquals(expected, sha512 ? HashUtils.hashSHA512(bytes) : HashUtils.hashSHA256(bytes));
    assertArrayEquals(original, bytes);
    assertArrayEquals(originalChars, chars);
  }

  private String hash(boolean sha512, boolean textInput, String text, char[] chars) {
    return sha512
        ? (textInput ? HashUtils.hashSHA512(text) : HashUtils.hashSHA512(chars))
        : (textInput ? HashUtils.hashSHA256(text) : HashUtils.hashSHA256(chars));
  }
}
