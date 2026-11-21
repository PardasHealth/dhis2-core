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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.audit.consumers.*;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class AuditDestinationScopeTest {
  static Stream<Arguments> mismatches() {
    return Stream.of(AuditScope.values())
        .flatMap(
            destination ->
                Stream.of(AuditScope.values())
                    .filter(claimed -> claimed != destination)
                    .map(claimed -> Arguments.of(destination, claimed)));
  }

  @ParameterizedTest
  @MethodSource("mismatches")
  void mismatchedScopeNeverReachesStorage(AuditScope destination, AuditScope claimed)
      throws Exception {
    var harness = new Harness(destination);
    assertThrows(IllegalArgumentException.class, () -> harness.consume(payload(claimed)));
    assertTrue(harness.stored.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(AuditScope.class)
  void absentScopeNeverReachesStorage(AuditScope destination) throws Exception {
    var harness = new Harness(destination);
    assertThrows(
        IllegalArgumentException.class, () -> harness.consume("{\"auditType\":\"CREATE\"}"));
    assertTrue(harness.stored.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(AuditScope.class)
  void nullRecordIsRejectedAsInvalidPayload(AuditScope destination) throws Exception {
    var harness = new Harness(destination);
    assertThrows(IllegalArgumentException.class, () -> harness.consume("null"));
    assertTrue(harness.stored.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(AuditScope.class)
  void matchingScopePersistsExactlyOnce(AuditScope destination) throws Exception {
    var harness = new Harness(destination);
    harness.consume(payload(destination));
    assertEquals(1, harness.stored.size());
    assertEquals(destination, harness.stored.get(0).getAuditScope());
    assertEquals("actor", harness.stored.get(0).getCreatedBy());
    assertEquals("payload", harness.stored.get(0).getData());
  }

  private static String payload(AuditScope scope) {
    return "{\"auditType\":\"CREATE\",\"auditScope\":\""
        + scope.name()
        + "\",\"createdBy\":\"actor\",\"data\":\"payload\"}";
  }

  private static final class Harness {
    final List<Audit> stored = new ArrayList<>();
    final java.util.function.Consumer<TextMessage> consumer;

    Harness(AuditScope destination) {
      AuditService service =
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
      DhisConfigurationProvider config =
          (DhisConfigurationProvider)
              Proxy.newProxyInstance(
                  DhisConfigurationProvider.class.getClassLoader(),
                  new Class<?>[] {DhisConfigurationProvider.class},
                  (proxy, method, args) -> {
                    if (method.getName().equals("isEnabled"))
                      return args[0] == ConfigurationKey.AUDIT_DATABASE;
                    if (method.getName().equals("getPropertyOrDefault")) return "off";
                    throw new UnsupportedOperationException(method.getName());
                  });
      ObjectMapper mapper = new ObjectMapper();
      consumer =
          switch (destination) {
            case METADATA -> new MetadataAuditConsumer(service, mapper, config)::consume;
            case AGGREGATE -> new AggregateAuditConsumer(service, mapper, config)::consume;
            case TRACKER -> new TrackerAuditConsumer(service, mapper, config)::consume;
            case API -> new ApiAuditConsumer(service, mapper, config)::consume;
          };
    }

    void consume(String json) throws Exception {
      TextMessage message =
          (TextMessage)
              Proxy.newProxyInstance(
                  TextMessage.class.getClassLoader(),
                  new Class<?>[] {TextMessage.class},
                  (proxy, method, args) -> {
                    if (method.getName().equals("getText")) return json;
                    throw new UnsupportedOperationException(method.getName());
                  });
      consumer.accept(message);
    }
  }
}
