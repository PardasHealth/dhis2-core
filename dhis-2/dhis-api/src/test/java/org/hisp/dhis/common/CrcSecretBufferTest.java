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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.zip.CRC32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrcSecretBufferTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void clearsOwnedEncodedBytesWithoutChangingCallerChars(boolean fails) {
    char[] chars = "synthetic-東京".toCharArray();
    char[] original = chars.clone();
    var observed = new AtomicReference<byte[]>();
    var error = new IllegalStateException("Synthetic CRC failure");
    try (var construction =
        mockConstruction(
            CRC32.class,
            (crc, context) -> {
              doAnswer(
                      call -> {
                        observed.set(call.getArgument(0));
                        if (fails) throw error;
                        return null;
                      })
                  .when(crc)
                  .update(any(byte[].class), anyInt(), anyInt());
              when(crc.getValue()).thenReturn(42L);
            })) {
      if (fails) {
        assertSame(
            error,
            assertThrows(
                IllegalStateException.class, () -> CRC32Utils.generateCRC32Checksum(chars)));
      } else {
        assertEquals(42L, CRC32Utils.generateCRC32Checksum(chars));
      }
      assertArrayEquals(original, chars);
      assertNotNull(observed.get());
      assertTrue(observed.get().length > 0);
      assertTrue(
          IntStream.range(0, observed.get().length).allMatch(i -> observed.get()[i] == 0),
          "CRC-helper owned encoded storage must be cleared");
    }
  }
}
