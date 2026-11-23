/*
 * Copyright (c) 2004-2024, University of Oslo
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

import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.Test;

class EmailChallengeSecretBufferTest {
  @Test
  void returnedChallengeRetainsItsCodeWhileOwnedRandomCharactersAreCleared() {
    char[] owned = "123456".toCharArray();
    long before = System.currentTimeMillis();
    try (var random = mockStatic(CodeGenerator.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomNumber(6)).thenReturn(owned);
      var result = TwoFactorAuthService.generateEmail2FACode();
      long after = System.currentTimeMillis();
      assertEquals("123456", result.code());
      String[] parts = result.encodedCode().split("\\|");
      assertEquals("123456", parts[0]);
      long expiry = Long.parseLong(parts[1]);
      assertTrue(expiry >= before + TwoFactorAuthService.TWOFA_EMAIL_CODE_EXPIRY_MILLIS);
      assertTrue(expiry <= after + TwoFactorAuthService.TWOFA_EMAIL_CODE_EXPIRY_MILLIS);
      assertArrayEquals(new char[6], owned);
    }
  }

  @Test
  void randomGenerationFailurePropagatesWithoutProducingAChallenge() {
    var failure = new IllegalStateException("synthetic random failure");
    try (var random = mockStatic(CodeGenerator.class)) {
      random.when(() -> CodeGenerator.generateSecureRandomNumber(6)).thenThrow(failure);
      assertSame(
          failure,
          assertThrows(IllegalStateException.class, TwoFactorAuthService::generateEmail2FACode));
    }
  }

  @Test
  void realChallengeKeepsTheSixDigitAndExpiryFormat() {
    var result = TwoFactorAuthService.generateEmail2FACode();
    assertTrue(result.code().matches("[0-9]{6}"));
    assertTrue(result.encodedCode().matches("[0-9]{6}\\|[0-9]+"));
    assertTrue(result.encodedCode().startsWith(result.code() + "|"));
  }
}
