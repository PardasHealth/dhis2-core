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
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditSchedulerTest {
  private AuditProducerSupplier producer;
  private AuditScheduler scheduler;
  private BlockingQueue<QueuedAudit> queue;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws ReflectiveOperationException {
    producer = mock(AuditProducerSupplier.class);
    scheduler = new AuditScheduler(producer);
    Field field = AuditScheduler.class.getDeclaredField("delayed");
    field.setAccessible(true);
    queue = (BlockingQueue<QueuedAudit>) field.get(scheduler);
  }

  @Test
  void publishFailureRetainsFailedAndUnprocessedEventsForRecovery() {
    Audit first = Audit.builder().uid("first").build();
    Audit second = Audit.builder().uid("second").build();
    queue.add(new QueuedAudit(first, 0));
    queue.add(new QueuedAudit(second, 0));
    RuntimeException failure = new IllegalStateException("Broker unavailable");
    doThrow(failure).when(producer).publish(first);

    assertSame(failure, assertThrows(RuntimeException.class, scheduler::process));
    assertEquals(2, queue.size());
    verify(producer, never()).publish(second);

    doNothing().when(producer).publish(first);
    scheduler.process();

    assertTrue(queue.isEmpty());
    verify(producer, times(2)).publish(first);
    verify(producer).publish(second);
  }

  @Test
  void recoveryDoesNotReplayAlreadyPublishedEvents() {
    Audit first = Audit.builder().uid("first").build();
    Audit second = Audit.builder().uid("second").build();
    queue.add(new QueuedAudit(first, 0));
    queue.add(new QueuedAudit(second, 0));
    doThrow(new IllegalStateException("Broker unavailable")).when(producer).publish(second);

    assertThrows(IllegalStateException.class, scheduler::process);
    assertEquals(1, queue.size());

    doNothing().when(producer).publish(second);
    scheduler.process();

    verify(producer).publish(first);
    verify(producer, times(2)).publish(second);
    assertTrue(queue.isEmpty());
  }

  @Test
  void doesNotPublishEventsBeforeTheirDelayExpires() {
    Audit pending = Audit.builder().uid("pending").build();
    queue.add(new QueuedAudit(pending, 60_000));
    scheduler.process();
    assertEquals(1, queue.size());
    verifyNoInteractions(producer);
  }

  @Test
  void successfulProcessingEmptiesQueue() {
    Audit event = Audit.builder().uid("event").build();
    queue.add(new QueuedAudit(event, 0));
    scheduler.process();
    verify(producer).publish(event);
    assertTrue(queue.isEmpty());
  }
}
