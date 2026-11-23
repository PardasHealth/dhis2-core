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
package org.hisp.dhis.security.twofa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TotpQrOwnedBufferTest {
  private static final String URI = "otpauth://totp/Test:alice?secret=JBSWY3DPEHPK3PXP&issuer=Test";

  @Test
  void successfulPngCopyRemainsAvailableWhileOwnedStreamBytesAreCleared() throws Exception {
    var observed = new AtomicReference<ByteArrayOutputStream>();
    List<ErrorCode> errors = new ArrayList<>();
    try (var png = mockStatic(MatrixToImageWriter.class, CALLS_REAL_METHODS)) {
      png.when(
              () ->
                  MatrixToImageWriter.writeToStream(
                      any(BitMatrix.class), eq("PNG"), any(OutputStream.class)))
          .thenAnswer(
              call -> {
                observed.set(call.getArgument(2));
                call.callRealMethod();
                return null;
              });
      byte[] result = TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add);
      assertTrue(result.length > 8);
      assertArrayEquals(
          new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, java.util.Arrays.copyOf(result, 8));
      assertTrue(errors.isEmpty());
      assertCleared(observed.get());
    }
  }

  @Test
  void checkedWriterFailureClearsPartialOutputAndPreservesErrorResponse() throws Exception {
    var observed = new AtomicReference<ByteArrayOutputStream>();
    byte[] partial = "synthetic partial image".getBytes(StandardCharsets.UTF_8);
    byte[] original = partial.clone();
    List<ErrorCode> errors = new ArrayList<>();
    try (var png = mockStatic(MatrixToImageWriter.class)) {
      png.when(
              () ->
                  MatrixToImageWriter.writeToStream(
                      any(BitMatrix.class), eq("PNG"), any(OutputStream.class)))
          .thenAnswer(
              call -> {
                ByteArrayOutputStream output = call.getArgument(2);
                observed.set(output);
                output.write(partial);
                throw new IOException("synthetic image failure");
              });
      assertEquals(0, TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add).length);
      assertEquals(List.of(ErrorCode.E3026), errors);
      assertCleared(observed.get());
      assertArrayEquals(original, partial);
    }
  }

  @Test
  void uncheckedWriterFailureClearsPartialOutputAndPropagatesTheSameException() throws Exception {
    var observed = new AtomicReference<ByteArrayOutputStream>();
    var failure = new IllegalStateException("synthetic provider failure");
    List<ErrorCode> errors = new ArrayList<>();
    try (var png = mockStatic(MatrixToImageWriter.class)) {
      png.when(
              () ->
                  MatrixToImageWriter.writeToStream(
                      any(BitMatrix.class), eq("PNG"), any(OutputStream.class)))
          .thenAnswer(
              call -> {
                ByteArrayOutputStream output = call.getArgument(2);
                observed.set(output);
                output.write(new byte[] {1, 2, 3});
                throw failure;
              });
      assertSame(
          failure,
          assertThrows(
              IllegalStateException.class,
              () -> TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add)));
      assertTrue(errors.isEmpty());
      assertCleared(observed.get());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void clearsTheOwnedQrMatrixAfterCopyOrCheckedFailure(boolean fails) throws Exception {
    var observed = new AtomicReference<BitMatrix>();
    List<ErrorCode> errors = new ArrayList<>();
    try (var png = mockStatic(MatrixToImageWriter.class)) {
      png.when(
              () ->
                  MatrixToImageWriter.writeToStream(
                      any(BitMatrix.class), eq("PNG"), any(OutputStream.class)))
          .thenAnswer(
              call -> {
                BitMatrix matrix = call.getArgument(0);
                assertNotNull(matrix.getEnclosingRectangle());
                observed.set(matrix);
                if (fails) throw new IOException("synthetic image failure");
                ((OutputStream) call.getArgument(2)).write(1);
                return null;
              });
      byte[] result = TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add);
      assertEquals(fails ? 0 : 1, result.length);
      assertEquals(fails ? List.of(ErrorCode.E3026) : List.of(), errors);
      assertNotNull(observed.get());
      assertNull(observed.get().getEnclosingRectangle());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"سلام 🌍", "broken-\uD800-end"})
  void preservesExistingUtf8NormalizationBeforeQrEncoding(String input) throws Exception {
    String normalized = new String(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    try (var writers =
        mockConstruction(
            MultiFormatWriter.class,
            (writer, context) ->
                when(writer.encode(anyString(), eq(BarcodeFormat.QR_CODE), eq(200), eq(200)))
                    .thenReturn(new BitMatrix(200, 200)))) {
      List<ErrorCode> errors = new ArrayList<>();
      assertTrue(TwoFactorAuthUtils.generateQRCode(input, 200, 200, errors::add).length > 0);
      assertTrue(errors.isEmpty());
      verify(writers.constructed().get(0)).encode(normalized, BarcodeFormat.QR_CODE, 200, 200);
    }
  }

  private static void assertCleared(ByteArrayOutputStream stream) {
    assertNotNull(stream);
    byte[] retained = stream.toByteArray();
    assertTrue(retained.length > 0);
    assertArrayEquals(new byte[retained.length], retained);
  }
}
