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
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hisp.dhis.common.CRC32Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;

class ApiTokenChecksumBufferTest {
  static Stream<Arguments> processingOutcomes() {
    return Arrays.stream(ApiTokenType.values())
        .flatMap(type -> IntStream.range(0, 3).mapToObj(outcome -> Arguments.of(type, outcome)));
  }

  @ParameterizedTest
  @MethodSource("processingOutcomes")
  void clearsValidatorOwnedCopiesWithoutChangingCallerToken(ApiTokenType type, int outcome) {
    char[] token =
        (type.getPrefix() + "_" + "s".repeat(type.getLength()) + "0000000000").toCharArray();
    char[] original = token.clone();
    var code = new AtomicReference<char[]>();
    var checksum = new AtomicReference<char[]>();
    Answer<Boolean> validation =
        call -> {
          code.set(call.getArgument(0));
          checksum.set(call.getArgument(1));
          assertEquals(type.getLength(), code.get().length);
          assertEquals('s', code.get()[0]);
          if (outcome == 2) throw new IllegalStateException("Synthetic checksum failure");
          return outcome == 0;
        };
    try (var crc = mockStatic(CRC32Utils.class)) {
      crc.when(() -> CRC32Utils.isMatchingCRC32Checksum(any(char[].class), any(char[].class)))
          .thenAnswer(validation);
      crc.when(() -> CRC32Utils.isMatchingCRC32Base62Checksum(any(char[].class), any(char[].class)))
          .thenAnswer(validation);

      if (outcome == 2) {
        assertThrows(
            IllegalStateException.class, () -> ApiKeyTokenGenerator.isValidTokenChecksum(token));
      } else {
        assertEquals(outcome == 0, ApiKeyTokenGenerator.isValidTokenChecksum(token));
      }

      assertArrayEquals(original, token, "Checksum validation must not erase caller-owned input");
      assertCleared(code.get());
      assertCleared(checksum.get());
    }
  }

  private static void assertCleared(char[] buffer) {
    assertNotNull(buffer);
    assertTrue(buffer.length > 0);
    assertTrue(
        IntStream.range(0, buffer.length).allMatch(i -> buffer[i] == '\0'),
        "Validator-owned temporary characters must be cleared");
  }
}
