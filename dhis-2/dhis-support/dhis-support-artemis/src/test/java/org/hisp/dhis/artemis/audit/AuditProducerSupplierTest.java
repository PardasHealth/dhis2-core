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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.audit.AuditScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuditProducerSupplierTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void missingOrBlankDestinationThrowsWithoutSending(String destination) {
    MessageManager messages = mock(MessageManager.class);
    Map<AuditScope, String> destinations = new HashMap<>();
    destinations.put(AuditScope.METADATA, destination);
    Audit event = Audit.builder().auditScope(AuditScope.METADATA).data("private-payload").build();
    var producer = new AuditProducerSupplier(messages, destinations);
    var error = assertThrows(IllegalStateException.class, () -> producer.publish(event));
    assertFalse(error.getMessage().contains("private-payload"));
    verifyNoInteractions(messages);
  }

  @Test
  void unmappedScopeThrowsWithoutSending() {
    MessageManager messages = mock(MessageManager.class);
    var producer = new AuditProducerSupplier(messages, Map.of());
    assertThrows(
        IllegalStateException.class,
        () -> producer.publish(Audit.builder().auditScope(AuditScope.TRACKER).build()));
    verifyNoInteractions(messages);
  }

  @Test
  void mappedEventIsSentOnceAndSendFailurePropagates() {
    MessageManager messages = mock(MessageManager.class);
    var producer = new AuditProducerSupplier(messages, Map.of(AuditScope.API, "audit-api"));
    Audit event = Audit.builder().auditScope(AuditScope.API).build();
    producer.publish(event);
    verify(messages).send("audit-api", event);
    RuntimeException failure = new IllegalStateException("broker unavailable");
    doThrow(failure).when(messages).send("audit-api", event);
    assertSame(failure, assertThrows(IllegalStateException.class, () -> producer.publish(event)));
    verify(messages, times(2)).send("audit-api", event);
  }

  @Test
  @SuppressWarnings("unchecked")
  void schedulerRetainsUnmappedEventAndDeliversAfterMappingRepair() throws Exception {
    MessageManager messages = mock(MessageManager.class);
    Map<AuditScope, String> destinations = new HashMap<>();
    var scheduler = new AuditScheduler(new AuditProducerSupplier(messages, destinations));
    Field field = AuditScheduler.class.getDeclaredField("delayed");
    field.setAccessible(true);
    BlockingQueue<QueuedAudit> queue = (BlockingQueue<QueuedAudit>) field.get(scheduler);
    Audit event = Audit.builder().auditScope(AuditScope.METADATA).uid("retained").build();
    queue.add(new QueuedAudit(event, 0));
    assertThrows(IllegalStateException.class, scheduler::process);
    assertEquals(1, queue.size());
    verifyNoInteractions(messages);
    destinations.put(AuditScope.METADATA, "repaired-destination");
    scheduler.process();
    assertTrue(queue.isEmpty());
    verify(messages).send("repaired-destination", event);
  }
}
