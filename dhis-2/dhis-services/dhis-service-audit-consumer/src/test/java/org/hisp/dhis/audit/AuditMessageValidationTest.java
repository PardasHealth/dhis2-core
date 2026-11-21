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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuditMessageValidationTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"auditType\":\"CREATE\",\"createdBy\":\"first\",\"createdBy\":\"synthetic-private-actor\"}",
        "{\"auditType\":\"CREATE\",\"auditScope\":\"API\",\"auditScope\":\"METADATA\"}",
        "{\"auditType\":\"CREATE\",\"attributes\":{\"uid\":\"first\",\"uid\":\"second\"}}",
        "{\"auditType\":\"CREATE\"} {\"createdBy\":\"synthetic-private-actor\"}",
        "{\"auditType\":\"CREATE\"} []"
      })
  void ambiguousOrTrailingJsonNeverReachesStorage(String json) throws Exception {
    var consumer = new TestConsumer();
    var error = assertThrows(IllegalArgumentException.class, () -> consumer.accept(json));
    assertEquals("Invalid audit message payload", error.getMessage());
    assertNull(error.getCause());
    assertTrue(consumer.stored.isEmpty());
  }

  @Test
  void validStructuredPayloadRetainsActorAndSerializesData() throws Exception {
    var consumer = new TestConsumer();
    consumer.accept(
        "{\"auditType\":\"CREATE\",\"auditScope\":\"METADATA\",\"createdBy\":\"actor\",\"data\":{\"message\":\"سلام\"}}   ");
    assertEquals(1, consumer.stored.size());
    assertEquals("actor", consumer.stored.get(0).getCreatedBy());
    assertEquals(
        "سلام",
        consumer.objectMapper.readTree(consumer.stored.get(0).getData()).get("message").asText());
  }

  @Test
  void serializedStringPayloadIsRetainedAsOpaqueData() throws Exception {
    var consumer = new TestConsumer();
    consumer.accept("{\"auditType\":\"CREATE\",\"data\":\"opaque payload\"}");
    assertEquals(1, consumer.stored.size());
    assertEquals("opaque payload", consumer.stored.get(0).getData());
  }

  @Test
  void strictConsumerDoesNotMutateSharedMapperPolicy() throws Exception {
    var consumer = new TestConsumer();
    consumer.accept("{\"auditType\":\"CREATE\"}");
    assertFalse(consumer.objectMapper.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
    assertFalse(consumer.objectMapper.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
    assertEquals(
        2, consumer.objectMapper.readTree("{\"value\":1,\"value\":2}").get("value").asInt());
  }

  private static final class TestConsumer extends AbstractAuditConsumer {
    final List<Audit> stored = new ArrayList<>();

    TestConsumer() {
      objectMapper = new ObjectMapper();
      auditService =
          (AuditService)
              Proxy.newProxyInstance(
                  AuditService.class.getClassLoader(),
                  new Class<?>[] {AuditService.class},
                  (proxy, method, args) -> {
                    if (method.getName().equals("addAudit")) {
                      stored.add((Audit) args[0]);
                      return 1L;
                    }
                    throw new UnsupportedOperationException(method.getName());
                  });
      isAuditDatabaseEnabled = true;
    }

    void accept(String json) throws Exception {
      TextMessage message =
          (TextMessage)
              Proxy.newProxyInstance(
                  TextMessage.class.getClassLoader(),
                  new Class<?>[] {TextMessage.class},
                  (proxy, method, args) -> {
                    if (method.getName().equals("getText")) return json;
                    throw new UnsupportedOperationException(method.getName());
                  });
      _consume(message);
    }
  }
}
