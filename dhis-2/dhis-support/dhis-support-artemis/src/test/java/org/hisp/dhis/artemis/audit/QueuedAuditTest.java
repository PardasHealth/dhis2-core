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

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class QueuedAuditTest {
  @Test
  void expiredOlderEventIsNotBlockedByNewerShorterDelay() {
    AtomicLong ticker = new AtomicLong();
    QueuedAudit older = new QueuedAudit(Audit.builder().uid("older").build(), 5000, ticker::get);
    ticker.set(TimeUnit.SECONDS.toNanos(6));
    QueuedAudit newer = new QueuedAudit(Audit.builder().uid("newer").build(), 1000, ticker::get);
    DelayQueue<QueuedAudit> queue = new DelayQueue<>();
    queue.add(newer);
    queue.add(older);
    assertSame(older, queue.poll());
    assertNull(queue.poll());
    ticker.set(TimeUnit.SECONDS.toNanos(7));
    assertSame(newer, queue.poll());
  }

  @Test
  void equalConfiguredDelaysAreOrderedByActualDeadline() {
    AtomicLong ticker = new AtomicLong();
    QueuedAudit first = new QueuedAudit(Audit.builder().build(), 5000, ticker::get);
    ticker.set(TimeUnit.SECONDS.toNanos(1));
    QueuedAudit second = new QueuedAudit(Audit.builder().build(), 5000, ticker::get);
    assertTrue(first.compareTo(second) < 0);
    assertTrue(second.compareTo(first) > 0);
    assertEquals(0, first.compareTo(first));
  }

  @Test
  void elapsedTimeUsesNanosecondPrecisionAndExpiresAtDeadline() {
    AtomicLong ticker = new AtomicLong(123);
    QueuedAudit item = new QueuedAudit(Audit.builder().build(), 1, ticker::get);
    ticker.addAndGet(999999);
    assertEquals(1, item.getDelay(TimeUnit.NANOSECONDS));
    ticker.incrementAndGet();
    assertEquals(0, item.getDelay(TimeUnit.NANOSECONDS));
    ticker.incrementAndGet();
    assertEquals(-1, item.getDelay(TimeUnit.NANOSECONDS));
  }

  @Test
  void monotonicCounterWraparoundPreservesExpiryAndOrdering() {
    AtomicLong ticker = new AtomicLong(Long.MAX_VALUE - 500000);
    QueuedAudit first = new QueuedAudit(Audit.builder().build(), 1, ticker::get);
    ticker.addAndGet(100000);
    QueuedAudit second = new QueuedAudit(Audit.builder().build(), 1, ticker::get);
    assertTrue(first.compareTo(second) < 0);
    ticker.addAndGet(900000);
    assertEquals(0, first.getDelay(TimeUnit.NANOSECONDS));
    assertEquals(100000, second.getDelay(TimeUnit.NANOSECONDS));
  }
}
