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
package org.hisp.dhis.webapi.security.apikey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiTokenSecretBufferTest {
  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void temporaryRawTokenIsClearedAfterSuccessAndEveryProcessingFailure(int outcome) {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "ApiToken synthetic-token");
    var observed = new AtomicReference<char[]>();
    try (var generator = mockStatic(ApiKeyTokenGenerator.class)) {
      generator
          .when(() -> ApiKeyTokenGenerator.isValidTokenChecksum(any(char[].class)))
          .thenAnswer(
              call -> {
                char[] raw = call.getArgument(0);
                observed.set(raw);
                assertEquals('s', raw[0]);
                if (outcome == 2) throw new IllegalArgumentException("Synthetic checksum error");
                return outcome != 1;
              });
      generator
          .when(() -> ApiKeyTokenGenerator.hashToken(any(char[].class)))
          .thenAnswer(
              call -> {
                assertSame(observed.get(), call.getArgument(0));
                if (outcome == 3) throw new IllegalStateException("Synthetic hashing error");
                return "synthetic-hash";
              });

      var resolver = new ApiTokenResolver();
      if (outcome == 0) {
        assertEquals("synthetic-hash", resolver.resolve(request));
      } else {
        assertThrows(ApiTokenAuthenticationException.class, () -> resolver.resolve(request));
      }

      assertNotNull(observed.get());
      assertTrue(observed.get().length > 0);
      assertTrue(
          IntStream.range(0, observed.get().length).allMatch(i -> observed.get()[i] == '\0'),
          "The resolver-owned temporary buffer must not retain raw token characters");
    }
  }
}
