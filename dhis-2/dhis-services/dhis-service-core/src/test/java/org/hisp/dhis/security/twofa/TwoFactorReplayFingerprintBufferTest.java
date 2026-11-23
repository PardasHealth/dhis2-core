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

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TwoFactorReplayFingerprintBufferTest {
  private static final String INPUT = "login:v1:Owner123456:TOTP_ENABLED:synthetic-secret:60000000";

  @Test
  void successfulFingerprintClearsOwnedPlaintextBytesAndRetainsDigestOutput() throws Throwable {
    var observed = new AtomicReference<byte[]>();
    var digest = mock(MessageDigest.class);
    byte[] output = new byte[32];
    Arrays.fill(output, (byte) 7);
    byte[] expectedOutput = output.clone();
    when(digest.digest(any(byte[].class)))
        .thenAnswer(
            call -> {
              byte[] encoded = call.getArgument(0);
              assertArrayEquals(INPUT.getBytes(StandardCharsets.UTF_8), encoded);
              observed.set(encoded);
              return output;
            });
    try (var provider = mockStatic(MessageDigest.class)) {
      provider.when(() -> MessageDigest.getInstance("SHA-256")).thenReturn(digest);
      assertEquals(HexFormat.of().formatHex(expectedOutput), fingerprint(INPUT));
      assertNotNull(observed.get());
      assertArrayEquals(new byte[observed.get().length], observed.get());
      assertArrayEquals(expectedOutput, output);
    }
  }

  @Test
  void digestFailureClearsOwnedPlaintextBytesAndPropagatesTheSameError() throws Throwable {
    var observed = new AtomicReference<byte[]>();
    var digest = mock(MessageDigest.class);
    var failure = new IllegalStateException("synthetic digest failure");
    when(digest.digest(any(byte[].class)))
        .thenAnswer(
            call -> {
              observed.set(call.getArgument(0));
              throw failure;
            });
    try (var provider = mockStatic(MessageDigest.class)) {
      provider.when(() -> MessageDigest.getInstance("SHA-256")).thenReturn(digest);
      assertSame(failure, assertThrows(IllegalStateException.class, () -> fingerprint(INPUT)));
      assertNotNull(observed.get());
      assertArrayEquals(new byte[observed.get().length], observed.get());
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "login:v1:Owner123456:EMAIL_ENABLED:123456|1800000000000",
        "login:v1:کاربر:TOTP_ENABLED:secret:60000000"
      })
  void fingerprintsRemainStandardSha256Utf8(String value) throws Throwable {
    String expected =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
    assertEquals(expected, fingerprint(value));
  }

  @Test
  void unavailableAlgorithmRetainsTheExistingFailureBoundary() {
    var failure = new NoSuchAlgorithmException("synthetic missing provider");
    try (var provider = mockStatic(MessageDigest.class)) {
      provider.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(failure);
      var thrown = assertThrows(IllegalStateException.class, () -> fingerprint(INPUT));
      assertEquals("SHA-256 is unavailable", thrown.getMessage());
      assertSame(failure, thrown.getCause());
    }
  }

  private static String fingerprint(String value) throws Throwable {
    var method = TwoFactorReplayService.class.getDeclaredMethod("fingerprint", String.class);
    method.setAccessible(true);
    try {
      return (String) method.invoke(null, value);
    } catch (InvocationTargetException error) {
      throw error.getCause();
    }
  }
}
