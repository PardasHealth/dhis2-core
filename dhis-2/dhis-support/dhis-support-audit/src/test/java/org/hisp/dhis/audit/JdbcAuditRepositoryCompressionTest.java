/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class JdbcAuditRepositoryCompressionTest {
  @Test
  void compressedAuditDataRoundTrips() throws Exception {
    byte[] compressed = (byte[]) invoke("compress", String.class, "audit payload");
    assertEquals("audit payload", invoke("decompress", byte[].class, compressed));
  }

  @Test
  void emptyAndNullCompressedAuditDataUseLegacyEmptyObject() throws Exception {
    assertEquals("{}", invoke("decompress", byte[].class, new byte[0]));
    assertEquals("{}", invoke("decompress", byte[].class, null));
    assertEquals(0, ((byte[]) invoke("compress", String.class, "")).length);
  }

  @Test
  void unicodeAuditDataRoundTrips() throws Exception {
    String payload = "{\"message\":\"سلام 🌍\"}";
    byte[] compressed = (byte[]) invoke("compress", String.class, payload);
    assertEquals(payload, invoke("decompress", byte[].class, compressed));
  }

  @Test
  void corruptCompressedAuditDataThrowsInsteadOfReturningNull() throws Exception {
    InvocationTargetException error =
        assertThrows(
            InvocationTargetException.class,
            () -> invoke("decompress", byte[].class, new byte[] {1, 2, 3}));
    assertInstanceOf(IllegalStateException.class, error.getCause());
  }

  private static Object invoke(String methodName, Class<?> argumentType, Object argument)
      throws Exception {
    Method method = JdbcAuditRepository.class.getDeclaredMethod(methodName, argumentType);
    method.setAccessible(true);
    return method.invoke(null, argument);
  }
}
