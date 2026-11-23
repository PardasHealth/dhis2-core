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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CodeGeneratorSecretBufferTest {
  @Test
  void secureStringWrapperClearsTheCharactersTransferredFromItsGenerator() {
    char[] owned = new char[CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE];
    Arrays.fill(owned, 'A');
    String expected = new String(owned);
    try (var codes = mockStatic(CodeGenerator.class, CALLS_REAL_METHODS)) {
      codes
          .when(
              () ->
                  CodeGenerator.generateSecureRandomCode(
                      CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE))
          .thenReturn(owned);
      assertEquals(expected, CodeGenerator.getRandomSecureToken());
      codes.verify(
          () -> CodeGenerator.generateSecureRandomCode(CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE));
      assertArrayEquals(new char[owned.length], owned);
    }
  }

  @Test
  void secureStringWrapperPropagatesGeneratorFailure() {
    var failure = new IllegalStateException("synthetic random failure");
    try (var codes = mockStatic(CodeGenerator.class, CALLS_REAL_METHODS)) {
      codes
          .when(
              () ->
                  CodeGenerator.generateSecureRandomCode(
                      CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE))
          .thenThrow(failure);
      assertSame(
          failure, assertThrows(IllegalStateException.class, CodeGenerator::getRandomSecureToken));
    }
  }

  @Test
  void characterGeneratorStillReturnsUsableCallerOwnedCharacters() {
    char[] result =
        CodeGenerator.generateSecureRandomCode(CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE);
    assertEquals(CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE, result.length);
    assertTrue(new String(result).matches("[A-Za-z][A-Za-z0-9]*"));
    assertTrue(CodeGenerator.getRandomSecureToken().matches("[A-Za-z][A-Za-z0-9]*"));
  }
}
