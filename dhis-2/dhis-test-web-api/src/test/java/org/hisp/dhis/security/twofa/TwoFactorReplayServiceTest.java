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
package org.hisp.dhis.security.twofa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.h2.jdbcx.JdbcDataSource;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integrationH2")
class TwoFactorReplayServiceTest {
  private static final String SEED = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
  private static final long NOW = 1_794_182_415_000L;
  private final AtomicLong clock = new AtomicLong(NOW);
  private JdbcTemplate jdbc;
  private DataSourceTransactionManager transactions;
  private TwoFactorReplayService node;

  @BeforeEach
  void setUp() {
    var data = new JdbcDataSource();
    data.setURL(
        "jdbc:h2:mem:"
            + UUID.randomUUID()
            + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
    jdbc = new JdbcTemplate(data);
    jdbc.execute(
        "create table usedtwofactorcode (codehash varchar(64) primary key, expires bigint not"
            + " null)");
    transactions = new DataSourceTransactionManager(data);
    node = newNode();
  }

  @Test
  void emailChallengeSucceedsOnlyOnce() {
    UserDetails user = email("User1234567", "123456|" + (NOW + 60_000));
    assertTrue(node.consume(user, "123456"));
    assertFalse(node.consume(user, "123456"));
  }

  @Test
  void totpStepSucceedsOnlyOnce() {
    UserDetails user = totp("User1234567");
    String code = codeAt(NOW);
    assertTrue(node.consume(user, code));
    assertFalse(node.consume(user, code));
  }

  @Test
  void concurrentIndependentNodesAcceptEmailChallengeOnce() throws Exception {
    assertSingleWinner(email("User1234567", "123456|" + (NOW + 60_000)), "123456");
  }

  @Test
  void concurrentIndependentNodesAcceptTotpStepOnce() throws Exception {
    assertSingleWinner(totp("User1234567"), codeAt(NOW));
  }

  @Test
  void reservationSurvivesOuterTransactionRollbackAndServiceRestart() {
    UserDetails user = email("User1234567", "123456|" + (NOW + 60_000));
    new TransactionTemplate(transactions)
        .execute(
            status -> {
              assertTrue(node.consume(user, "123456"));
              status.setRollbackOnly();
              return null;
            });
    assertFalse(newNode().consume(user, "123456"));
  }

  @Test
  void failureToStoreReservationDoesNotAcceptCode() {
    jdbc.execute("drop table usedtwofactorcode");
    assertThrows(
        DataAccessException.class,
        () -> node.consume(email("User1234567", "123456|" + (NOW + 60_000)), "123456"));
  }

  @Test
  void wrongEmailCodeDoesNotConsumeTheChallenge() {
    UserDetails user = email("User1234567", "123456|" + (NOW + 60_000));
    assertFalse(node.consume(user, "654321"));
    assertTrue(node.consume(user, "123456"));
  }

  @Test
  void emailCodeExpiresAtItsDeadline() {
    assertFalse(node.consume(email("User1234567", "123456|" + NOW), "123456"));
    assertFalse(node.consume(email("User1234567", "123456|" + (NOW - 1)), "123456"));
  }

  @Test
  void malformedOrOverflowingEmailExpiryIsRejected() {
    assertFalse(node.consume(email("User1234567", "123456|99999999999999999999999"), "123456"));
    assertFalse(node.consume(email("User1234567", "invalid"), "123456"));
  }

  @Test
  void whitespaceDoesNotCreateAnotherUseOfTheSameChallenge() {
    UserDetails user = email("User1234567", "123456|" + (NOW + 60_000));
    assertTrue(node.consume(user, "123 456"));
    assertFalse(node.consume(user, "123456"));
  }

  @Test
  void sameNumericCodeDoesNotConsumeAnotherUsersChallenge() {
    String secret = "123456|" + (NOW + 60_000);
    assertTrue(node.consume(email("User1234567", secret), "123456"));
    assertTrue(node.consume(email("User7654321", secret), "123456"));
  }

  @Test
  void replacementEmailChallengeIsIndependent() {
    assertTrue(node.consume(email("User1234567", "123456|" + (NOW + 60_000)), "123456"));
    assertTrue(node.consume(email("User1234567", "123456|" + (NOW + 60_001)), "123456"));
  }

  @Test
  void currentAndPreviousTotpStepsKeepTheirOwnReservations() {
    UserDetails user = totp("User1234567");
    String previous = codeAt(NOW - 30_000);
    String current = codeAt(NOW);
    assertNotEquals(previous, current);
    assertTrue(node.consume(user, previous));
    assertTrue(node.consume(user, current));
    assertFalse(node.consume(user, previous));
    assertFalse(node.consume(user, current));
  }

  @Test
  void futureAndTooOldTotpValuesAreRejected() {
    UserDetails user = totp("User1234567");
    assertFalse(node.consume(user, codeAt(NOW + 30_000)));
    assertFalse(node.consume(user, codeAt(NOW - 60_000)));
  }

  @Test
  void nextTotpStepWorksAndExpiredRecordsAreCleaned() {
    UserDetails user = totp("User1234567");
    String first = codeAt(NOW);
    assertTrue(node.consume(user, first));
    clock.addAndGet(30_000);
    assertFalse(node.consume(user, first));
    assertTrue(node.consume(user, codeAt(clock.get())));
    clock.addAndGet(30_000);
    assertTrue(node.consume(user, codeAt(clock.get())));
    assertEquals(3, jdbc.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
    clock.addAndGet(TwoFactorReplayService.RETENTION_MILLIS + 60_000);
    assertTrue(node.consume(user, codeAt(clock.get())));
    assertEquals(1, jdbc.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
  }

  @Test
  void incompleteOrNonEnrolledUserStateIsRejected() {
    assertFalse(node.consume(null, "123456"));
    assertFalse(node.consume(email("", "123456|" + (NOW + 60_000)), "123456"));
    assertFalse(node.consume(email("User1234567", null), "123456"));
    assertFalse(
        node.consume(
            user("User1234567", TwoFactorType.ENROLLING_EMAIL, "123456|" + (NOW + 60_000)),
            "123456"));
  }

  @Test
  void cleanupOnFasterNodeCannotReopenCodeStillValidOnSlowerNode() {
    UserDetails user = totp("User1234567");
    String first = codeAt(NOW);
    assertTrue(node.consume(user, first));
    clock.set(NOW + 60_000);
    assertTrue(newNode().consume(user, codeAt(clock.get())));
    clock.set(NOW + 30_000);
    assertFalse(newNode().consume(user, first));
  }

  private void assertSingleWinner(UserDetails user, String code) throws Exception {
    var start = new CountDownLatch(1);
    var workers = Executors.newFixedThreadPool(8);
    try {
      var futures = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 8; i++) {
        TwoFactorReplayService independent = newNode();
        futures.add(
            workers.submit(
                () -> {
                  start.await();
                  return independent.consume(user, code);
                }));
      }
      start.countDown();
      int successes = 0;
      for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    } finally {
      workers.shutdownNow();
    }
  }

  private TwoFactorReplayService newNode() {
    var target = new TwoFactorReplayService(jdbc, clock::get);
    var proxy = new ProxyFactory(target);
    proxy.setProxyTargetClass(true);
    proxy.addAdvice(
        new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
    return (TwoFactorReplayService) proxy.getProxy();
  }

  private String codeAt(long millis) {
    return TwoFactorAuthUtils.totpAtInterval(SEED, millis / 30_000).now();
  }

  private UserDetails email(String uid, String secret) {
    return user(uid, TwoFactorType.EMAIL_ENABLED, secret);
  }

  private UserDetails totp(String uid) {
    return user(uid, TwoFactorType.TOTP_ENABLED, SEED);
  }

  private UserDetails user(String uid, TwoFactorType type, String secret) {
    UserDetails user = mock(UserDetails.class);
    when(user.getUid()).thenReturn(uid);
    when(user.getTwoFactorType()).thenReturn(type);
    when(user.getSecret()).thenReturn(secret);
    return user;
  }
}
