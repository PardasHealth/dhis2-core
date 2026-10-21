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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class AbstractAuditConsumerFailureTest {
  @Test
  void databaseFailureEscapesConsumerMethod() {
    DataAccessResourceFailureException databaseFailure =
        new DataAccessResourceFailureException("simulated audit storage outage");
    AuditService failingService =
        (AuditService)
            Proxy.newProxyInstance(
                AuditService.class.getClassLoader(),
                new Class<?>[] {AuditService.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("addAudit")) {
                    throw databaseFailure;
                  }
                  throw new UnsupportedOperationException(method.getName());
                });
    TextMessage message =
        (TextMessage)
            Proxy.newProxyInstance(
                TextMessage.class.getClassLoader(),
                new Class<?>[] {TextMessage.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("getText")) {
                    return "{\"auditType\":\"CREATE\"}";
                  }
                  throw new UnsupportedOperationException(method.getName());
                });
    TestConsumer consumer = new TestConsumer();
    consumer.auditService = failingService;
    consumer.objectMapper = new ObjectMapper();
    consumer.isAuditDatabaseEnabled = true;

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> consumer._consume(message));
    assertSame(databaseFailure, thrown.getCause());
  }

  private static final class TestConsumer extends AbstractAuditConsumer {}
}
