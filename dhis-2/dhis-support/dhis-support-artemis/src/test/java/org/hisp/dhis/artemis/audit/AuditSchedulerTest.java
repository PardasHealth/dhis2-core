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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

  @Test
  void concurrentAdmissionRespectsCapacityAndPublishesEveryOverflowEvent() throws Exception {
    List<Audit> events = new ArrayList<>();
    for (int i = 0; i < 400; i++) events.add(Audit.builder().uid("event-" + i).build());
    admitConcurrently(events);
    assertEquals(200, queue.size());
    for (Audit event : events) {
      if (queue.stream().anyMatch(item -> item.getAuditItem() == event)) {
        verify(producer, never()).publish(event);
      } else {
        verify(producer).publish(event);
      }
    }
    verify(producer, times(200)).publish(any());
  }

  @Test
  void concurrentWritersCompeteForTheLastBufferSlot() throws Exception {
    for (int i = 0; i < 199; i++) {
      scheduler.addAuditItem(Audit.builder().uid("existing-" + i).build());
    }
    List<Audit> events = new ArrayList<>();
    for (int i = 0; i < 32; i++) events.add(Audit.builder().uid("contender-" + i).build());
    admitConcurrently(events);
    assertEquals(200, queue.size());
    verify(producer, times(31)).publish(any());
  }

  @Test
  void concurrentDuplicateAdmissionRetainsOnePendingEvent() throws Exception {
    Audit event = Audit.builder().uid("same-event").build();
    admitConcurrently(java.util.Collections.nCopies(64, event));
    assertEquals(1, queue.size());
    verifyNoInteractions(producer);
  }

  @Test
  void overflowPublishFailurePropagatesWithoutEvictingBufferedEvents() {
    for (int i = 0; i < 200; i++) {
      scheduler.addAuditItem(Audit.builder().uid("buffered-" + i).build());
    }
    Audit overflow = Audit.builder().uid("overflow").build();
    RuntimeException failure = new IllegalStateException("Broker unavailable");
    doThrow(failure).when(producer).publish(overflow);
    assertSame(
        failure, assertThrows(RuntimeException.class, () -> scheduler.addAuditItem(overflow)));
    assertEquals(200, queue.size());
    assertFalse(queue.stream().anyMatch(item -> item.getAuditItem() == overflow));
    verify(producer).publish(overflow);
  }

  private void admitConcurrently(List<Audit> events) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch ready = new CountDownLatch(Math.min(32, events.size()));
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (Audit event : events) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(10, TimeUnit.SECONDS));
                  scheduler.addAuditItem(event);
                  return null;
                }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }
}
