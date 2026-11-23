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
package org.hisp.dhis.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JdbcAuditPayloadBufferTest {
  private static final String PAYLOAD = "synthetic audit payload سلام 🌍";

  @Test
  void compressionSuccessClearsOwnedUtf8() throws Throwable {
    checkCompression(null);
  }

  @Test
  void compressionIoFailureClearsOwnedUtf8() throws Throwable {
    checkCompression(new IOException("synthetic failure"));
  }

  @Test
  void compressionRuntimeFailureClearsOwnedUtf8() throws Throwable {
    checkCompression(new IllegalStateException("synthetic failure"));
  }

  private void checkCompression(Throwable failure) throws Throwable {
    var observed = new AtomicReference<byte[]>();
    var output = new AtomicReference<ByteArrayOutputStream>();
    byte[] compressedCopy = new byte[] {7, 8, 9};
    try (var gzip =
        mockConstruction(
            GZIPOutputStream.class,
            (stream, context) -> {
              output.set((ByteArrayOutputStream) context.arguments().get(0));
              doAnswer(
                      call -> {
                        byte[] encoded = call.getArgument(0);
                        assertArrayEquals(PAYLOAD.getBytes(StandardCharsets.UTF_8), encoded);
                        observed.set(encoded);
                        output.get().write(compressedCopy);
                        if (failure != null) throw failure;
                        return null;
                      })
                  .when(stream)
                  .write(any(byte[].class));
            })) {
      if (failure == null) {
        assertArrayEquals(compressedCopy, (byte[]) invoke("compress", String.class, PAYLOAD));
      } else if (failure instanceof IOException) {
        var error =
            assertThrows(
                IllegalStateException.class, () -> invoke("compress", String.class, PAYLOAD));
        assertEquals("Unable to compress audit data", error.getMessage());
        assertSame(failure, error.getCause());
      } else {
        assertSame(
            failure,
            assertThrows(
                IllegalStateException.class, () -> invoke("compress", String.class, PAYLOAD)));
      }
      assertNotNull(observed.get());
      assertArrayEquals(new byte[observed.get().length], observed.get());
      assertNotNull(output.get());
      assertEquals(compressedCopy.length, output.get().size());
      assertArrayEquals(new byte[compressedCopy.length], output.get().toByteArray());
      assertArrayEquals(new byte[] {7, 8, 9}, compressedCopy);
    }
  }

  static Stream<Arguments> decodedVectors() {
    return Stream.of(
        Arguments.of((Object) PAYLOAD.getBytes(StandardCharsets.UTF_8)),
        Arguments.of((Object) new byte[] {(byte) 0xc3, 0x28}),
        Arguments.of((Object) new byte[0]));
  }

  @ParameterizedTest
  @MethodSource("decodedVectors")
  void decompressionClearsOwnedBytesAndRetainsStringAndCallerInput(byte[] decoded)
      throws Throwable {
    byte[] compressed = gzipFixture();
    byte[] original = compressed.clone();
    String expected = new String(decoded, StandardCharsets.UTF_8);
    try (var io = mockStatic(IOUtils.class)) {
      io.when(() -> IOUtils.toByteArray(any(InputStream.class))).thenReturn(decoded);
      assertEquals(expected, invoke("decompress", byte[].class, compressed));
      assertArrayEquals(new byte[decoded.length], decoded);
      assertArrayEquals(original, compressed);
    }
  }

  @Test
  void decompressionReadFailureRetainsExistingErrorAndCallerInput() throws Throwable {
    byte[] compressed = gzipFixture();
    byte[] original = compressed.clone();
    var failure = new IOException("synthetic read failure");
    try (var io = mockStatic(IOUtils.class)) {
      io.when(() -> IOUtils.toByteArray(any(InputStream.class))).thenThrow(failure);
      var error =
          assertThrows(
              IllegalStateException.class, () -> invoke("decompress", byte[].class, compressed));
      assertEquals("Unable to decompress audit data", error.getMessage());
      assertSame(failure, error.getCause());
      assertArrayEquals(original, compressed);
    }
  }

  private static byte[] gzipFixture() throws IOException {
    var output = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(output)) {
      gzip.write(PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }
    return output.toByteArray();
  }

  private static Object invoke(String name, Class<?> type, Object value) throws Throwable {
    var method = JdbcAuditRepository.class.getDeclaredMethod(name, type);
    method.setAccessible(true);
    try {
      return method.invoke(null, value);
    } catch (InvocationTargetException error) {
      throw error.getCause();
    }
  }
}
