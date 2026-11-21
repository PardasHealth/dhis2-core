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

import java.util.Arrays;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TotpValidationSecretBufferTest {
  private static final long NOW = 1_800_000_000_000L;

  @ParameterizedTest
  @ValueSource(ints = {10, 20})
  void successfulProofClearsOnlyThePrecheckCopyAndStillVerifies(int length) throws Exception {
    verifyProofAndCleanup(length, true);
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 20})
  void rejectedProofAlsoClearsThePrecheckCopy(int length) throws Exception {
    verifyProofAndCleanup(length, false);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 9, 19, 21})
  void rejectedDecodedLengthIsClearedBeforeReturning(int length) {
    byte[] decoded = new byte[length];
    Arrays.fill(decoded, (byte) 9);
    try (var decoder = mockStatic(Base32.class)) {
      decoder.when(() -> Base32.decode("synthetic-secret")).thenReturn(decoded);
      assertFalse(TwoFactorAuthUtils.verifyTOTP2FACode("123456", "synthetic-secret", NOW));
      assertArrayEquals(new byte[length], decoded);
    }
  }

  private void verifyProofAndCleanup(int length, boolean accepted) throws Exception {
    byte[] secretBytes = new byte[length];
    Arrays.fill(secretBytes, (byte) 7);
    String secret = Base32.encode(secretBytes);
    String valid = TwoFactorAuthUtils.totpAtInterval(secret, NOW / 30_000).now();
    String code =
        accepted ? valid : String.format("%06d", (Integer.parseInt(valid) + 1) % 1_000_000);
    byte[] precheckCopy = Base32.decode(secret);
    byte[] libraryCopy = Base32.decode(secret);
    try (var decoder = mockStatic(Base32.class)) {
      // The factory's own validation decode and the OTP library's verification
      // decode have distinct ownership. Preserve real OTP verification behavior.
      decoder.when(() -> Base32.decode(secret)).thenReturn(precheckCopy, libraryCopy);
      assertEquals(accepted, TwoFactorAuthUtils.verifyTOTP2FACode(code, secret, NOW));
      assertArrayEquals(new byte[length], precheckCopy);
      assertArrayEquals(secretBytes, libraryCopy);
    }
  }
}
