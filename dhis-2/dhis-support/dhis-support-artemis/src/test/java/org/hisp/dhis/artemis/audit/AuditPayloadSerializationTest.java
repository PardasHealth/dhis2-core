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
package org.hisp.dhis.artemis.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.hisp.dhis.artemis.AuditProducerConfiguration;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.DefaultAuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuditPayloadSerializationTest {
  @ParameterizedTest
  @EnumSource(AuditScope.class)
  void serializationFailurePropagatesForEveryScopeWithoutPayloadDetails(AuditScope scope) {
    var factory = new DefaultAuditObjectFactory(new ObjectMapper());
    var error =
        assertThrows(
            IllegalStateException.class,
            () -> factory.create(scope, AuditType.CREATE, new FailingPayload(), "actor"));
    assertEquals("Unable to serialize audit payload", error.getMessage());
    assertNull(error.getCause());
  }

  @ParameterizedTest
  @EnumSource(AuditScope.class)
  void validPayloadRemainsJsonForEveryScope(AuditScope scope) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    var factory = new DefaultAuditObjectFactory(mapper);
    Map<String, String> payload = Map.of("message", "سلام 🌍");
    Object serialized = factory.create(scope, AuditType.CREATE, payload, "actor");
    assertInstanceOf(String.class, serialized);
    assertEquals(mapper.valueToTree(payload), mapper.readTree((String) serialized));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void managerDoesNotPublishOrBufferAnEventWhoseSerializationFails(boolean buffered) {
    var producer = mock(AuditProducerSupplier.class);
    var scheduler = mock(AuditScheduler.class);
    var matrix = mock(AuditMatrix.class);
    var username = mock(UsernameSupplier.class);
    var manager =
        new AuditManager(
            producer,
            scheduler,
            AuditProducerConfiguration.builder().useQueue(buffered).build(),
            matrix,
            new DefaultAuditObjectFactory(new ObjectMapper()),
            username);
    Audit event =
        Audit.builder()
            .auditScope(AuditScope.METADATA)
            .auditType(AuditType.CREATE)
            .createdBy("actor")
            .auditableEntity(new AuditableEntity(FailingPayload.class, new FailingPayload()))
            .build();
    when(matrix.isEnabled(event)).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> manager.send(event));
    assertNull(event.getData());
    verifyNoInteractions(producer, scheduler);
  }

  public static class FailingPayload {
    public String getValue() {
      throw new IllegalStateException("synthetic-private-payload");
    }
  }
}
