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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AtomicCacheUpdateTest {
  @Test
  void concurrentLocalUpdatesDoNotLoseIncrements() throws Exception {
    Cache<Integer> cache = new SimpleCacheBuilder<Integer>().withDefaultValue(0).build();
    var pool = Executors.newFixedThreadPool(8);
    try {
      var tasks = new ArrayList<java.util.concurrent.Future<?>>();
      for (int worker = 0; worker < 8; worker++) {
        tasks.add(
            pool.submit(
                () -> {
                  for (int i = 0; i < 100; i++) cache.update("account", value -> value + 1);
                }));
      }
      for (var task : tasks) task.get(10, TimeUnit.SECONDS);
      assertEquals(800, cache.get("account").orElseThrow());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void failedLocalMappingPreservesThePreviousValue() {
    Cache<Integer> cache = new SimpleCacheBuilder<Integer>().withDefaultValue(0).build();
    cache.put("account", 4);
    assertThrows(
        RuntimeException.class,
        () ->
            cache.update(
                "account",
                value -> {
                  throw new IllegalStateException("failure");
                }));
    assertEquals(4, cache.get("account").orElseThrow());
    assertEquals(5, cache.update("account", value -> value + 1));
  }

  @Test
  void localUpdateHonorsAbsentDefaultAndRejectsNullResults() {
    Cache<Integer> cache = new SimpleCacheBuilder<Integer>().withDefaultValue(9).build();
    assertEquals(10, cache.update("account", value -> value + 1));
    assertThrows(RuntimeException.class, () -> cache.update("account", value -> null));
    assertEquals(10, cache.get("account").orElseThrow());
  }

  @Test
  void disabledCacheEvaluatesTheUpdateWithoutStoringState() {
    Cache<Integer> cache = new NoOpCache<>(0);
    assertEquals(1, cache.update("account", value -> value + 1));
    assertEquals(1, cache.update("account", value -> value + 1));
    assertTrue(cache.getIfPresent("account").isEmpty());
  }
}
