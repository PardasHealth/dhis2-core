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
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisAtomicCacheUpdateTest {
  private static GenericContainer<?> redis;
  private static LettuceConnectionFactory connections;
  private RedisTemplate<String, Integer> template;
  private String region;

  @BeforeAll
  static void startRedis() {
    redis =
        new GenericContainer<>(
                DockerImageName.parse(
                    "redis:7.4-alpine@sha256:7f7d30b7fdd09467bac58198a1f166dd69789d348d4fe859cccb20e0cf287fbe"))
            .withExposedPorts(6379);
    redis.start();
    connections = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
    connections.afterPropertiesSet();
    connections.start();
  }

  @AfterAll
  static void stopRedis() {
    if (connections != null) connections.destroy();
    if (redis != null) redis.stop();
  }

  @BeforeEach
  void setUp() {
    template = new RedisTemplate<>();
    template.setConnectionFactory(connections);
    template.setKeySerializer(new StringRedisSerializer());
    template
        .afterPropertiesSet(); // Same default JDK value serialization as production RedisConfig.
    region = "atomic-" + UUID.randomUUID();
  }

  private RedisCache<Integer> cache(boolean expiring) {
    var builder =
        new ExtendedCacheBuilder<Integer>(template, mock(DhisConfigurationProvider.class));
    builder.forRegion(region).withDefaultValue(0);
    if (expiring) builder.expireAfterWrite(60, TimeUnit.SECONDS);
    return new RedisCache<>(builder);
  }

  @Test
  void independentCacheInstancesRetryADeterministicWriteConflict() throws Exception {
    var first = cache(true);
    var second = cache(true);
    var barrier = new CyclicBarrier(2);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var tasks = new ArrayList<java.util.concurrent.Future<?>>();
      for (var instance : java.util.List.of(first, second)) {
        tasks.add(
            pool.submit(
                () -> {
                  var initial = new AtomicBoolean(true);
                  return instance.update(
                      "account",
                      value -> {
                        if (initial.getAndSet(false)) {
                          try {
                            barrier.await(10, TimeUnit.SECONDS);
                          } catch (Exception e) {
                            throw new IllegalStateException(e);
                          }
                        }
                        return value + 1;
                      });
                }));
      }
      for (var task : tasks) task.get(15, TimeUnit.SECONDS);
      assertEquals(2, first.get("account").orElseThrow());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void concurrentRedisUpdatesDoNotLoseIncrements() throws Exception {
    var pool = Executors.newFixedThreadPool(8);
    try {
      var tasks = new ArrayList<java.util.concurrent.Future<?>>();
      for (int worker = 0; worker < 8; worker++) {
        var instance = cache(true);
        tasks.add(
            pool.submit(
                () -> {
                  for (int i = 0; i < 25; i++) instance.update("account", value -> value + 1);
                }));
      }
      for (var task : tasks) task.get(30, TimeUnit.SECONDS);
      assertEquals(200, cache(true).get("account").orElseThrow());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void updateRetainsJdkSerializationAndRefreshesConfiguredExpiry() {
    var cache = cache(true);
    cache.put("account", 41, 1);
    assertEquals(42, cache.update("account", value -> value + 1));
    assertEquals(42, template.opsForValue().get(region + ":account"));
    long ttl = template.getExpire(region + ":account");
    assertTrue(ttl > 1 && ttl <= 60);
  }

  @Test
  void eternalUpdateDoesNotAddAnExpiry() {
    var cache = cache(false);
    assertEquals(1, cache.update("account", value -> value + 1));
    assertEquals(-1L, template.getExpire(region + ":account"));
  }

  @Test
  void failedMapperDoesNotChangeStoredValue() {
    var cache = cache(true);
    cache.put("account", 3);
    assertThrows(
        IllegalStateException.class,
        () ->
            cache.update(
                "account",
                value -> {
                  throw new IllegalStateException("failure");
                }));
    assertEquals(3, cache.get("account").orElseThrow());
    assertEquals(4, cache.update("account", value -> value + 1));
  }

  @Test
  void concurrentResetCannotResurrectThePreResetFailureCount() throws Exception {
    var cache = cache(true);
    cache.put("account", 5);
    var read = new java.util.concurrent.CountDownLatch(1);
    var reset = new java.util.concurrent.CountDownLatch(1);
    var pool = Executors.newSingleThreadExecutor();
    try {
      var result =
          pool.submit(
              () -> {
                var first = new AtomicBoolean(true);
                return cache.update(
                    "account",
                    value -> {
                      if (first.getAndSet(false)) {
                        read.countDown();
                        try {
                          if (!reset.await(10, TimeUnit.SECONDS))
                            throw new IllegalStateException("reset timeout");
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(e);
                        }
                      }
                      return value + 1;
                    });
              });
      assertTrue(read.await(10, TimeUnit.SECONDS));
      cache(true).invalidate("account");
      reset.countDown();
      assertEquals(1, result.get(15, TimeUnit.SECONDS));
      assertEquals(1, cache.get("account").orElseThrow());
    } finally {
      reset.countDown();
      pool.shutdownNow();
    }
  }
}
