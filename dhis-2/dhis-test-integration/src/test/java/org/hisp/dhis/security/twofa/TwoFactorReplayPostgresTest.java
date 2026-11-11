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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.hisp.dhis.user.UserDetails;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Real PostgreSQL migration/uniqueness/privilege tests, isolated from deployment data. */
@Tag("integration")
class TwoFactorReplayPostgresTest {
  private static final PostgreSQLContainer<?> DATABASE =
      new PostgreSQLContainer<>(
              DockerImageName.parse(
                      System.getProperty(
                          "cc.replay.postgres.image", "imresamu/postgis:16-3.5-alpine"))
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("replay_test")
          .withUsername("replay_admin")
          .withPassword("isolated-test-only")
          .withTmpFs(Map.of("/var/lib/postgresql/data", "rw,size=512m"));
  private static JdbcTemplate admin;
  private static DriverManagerDataSource writer;

  @BeforeAll
  static void startDatabaseAndMigrate() throws Exception {
    DATABASE.start();
    var data =
        new DriverManagerDataSource(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    var flyway =
        Flyway.configure()
            .dataSource(data)
            .locations("classpath:org/hisp/dhis/db/migration/pardas")
            .table("replay_test_schema_history")
            .baselineOnMigrate(true)
            .load();
    assertEquals(1, flyway.migrate().migrationsExecuted);
    assertEquals(0, flyway.migrate().migrationsExecuted);
    var mapping = new org.hibernate.cfg.Configuration().addAnnotatedClass(UsedTwoFactorCode.class);
    mapping.getProperties().put("hibernate.connection.datasource", data);
    mapping.setProperty("hibernate.hbm2ddl.auto", "validate");
    try (var factory = mapping.buildSessionFactory()) {
      assertTrue(factory.isOpen());
    }
    admin = new JdbcTemplate(data);
    admin.execute(
        "create role replay_writer login password 'isolated-writer-test-only' nosuperuser");
    admin.execute(
        "create table userinfo (uid varchar(11) primary key, secret text, twofactortype"
            + " varchar(50))");
    admin.execute("grant select, update on userinfo to replay_writer");
    admin.execute("grant usage on schema public to replay_writer");
    admin.execute("grant select, insert, delete on usedtwofactorcode to replay_writer");
    writer =
        new DriverManagerDataSource(
            DATABASE.getJdbcUrl(), "replay_writer", "isolated-writer-test-only");
  }

  @AfterAll
  static void stopDatabase() {
    DATABASE.stop();
  }

  @BeforeEach
  void clearReplayState() {
    admin.execute("grant insert on usedtwofactorcode to replay_writer");
    admin.execute("delete from usedtwofactorcode");
    admin.execute("delete from userinfo");
  }

  @Test
  void concurrentEmailLoginsHaveExactlyOneWinner() throws Exception {
    assertSingleWinner(
        user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000)),
        "123456");
  }

  @Test
  void concurrentTotpLoginsHaveExactlyOneWinner() throws Exception {
    String seed = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    assertSingleWinner(user(TwoFactorType.TOTP_ENABLED, seed), new Totp(seed).now());
  }

  @Test
  void outerRollbackAndServiceReplacementDoNotMakeCodeReusable() {
    var user =
        user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000));
    new TransactionTemplate(new DataSourceTransactionManager(writer))
        .execute(
            status -> {
              assertTrue(newNode().consume(user, "123456"));
              status.setRollbackOnly();
              return null;
            });
    assertFalse(newNode().consume(user, "123456"));
  }

  @Test
  void deniedDatabaseInsertFailsClosed() {
    admin.execute("revoke insert on usedtwofactorcode from replay_writer");
    try {
      var user =
          user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000));
      assertThrows(DataAccessException.class, () -> newNode().consume(user, "123456"));
      assertEquals(
          0, admin.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
    } finally {
      admin.execute("grant insert on usedtwofactorcode to replay_writer");
    }
  }

  private TwoFactorReplayService newNode() {
    var proxy = new ProxyFactory(new TwoFactorReplayService(new JdbcTemplate(writer)));
    proxy.setProxyTargetClass(true);
    proxy.addAdvice(
        new TransactionInterceptor(
            new DataSourceTransactionManager(writer), new AnnotationTransactionAttributeSource()));
    return (TwoFactorReplayService) proxy.getProxy();
  }

  private void assertSingleWinner(UserDetails user, String code) throws Exception {
    var workers = Executors.newFixedThreadPool(8);
    var start = new CountDownLatch(1);
    try {
      var attempts = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 8; i++) {
        TwoFactorReplayService independent = newNode();
        attempts.add(
            workers.submit(
                () -> {
                  start.await();
                  return independent.consume(user, code);
                }));
      }
      start.countDown();
      int accepted = 0;
      for (var result : attempts) if (result.get(15, TimeUnit.SECONDS)) accepted++;
      assertEquals(1, accepted);
      assertEquals(
          1, admin.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
    } finally {
      workers.shutdownNow();
    }
  }

  @Test
  void committedReplacementRejectsStaleEmailSnapshot() {
    var old = user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000));
    admin.update(
        "update userinfo set secret = ?", "654321|" + (System.currentTimeMillis() + 120_000));
    assertFalse(newNode().consume(old, "123456"));
    assertEquals(0, admin.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
  }

  @Test
  void committedFactorResetRejectsStaleTotpSnapshot() {
    String seed = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    var old = user(TwoFactorType.TOTP_ENABLED, seed);
    admin.update("update userinfo set secret = null, twofactortype = null");
    assertFalse(newNode().consume(old, new Totp(seed).now()));
  }

  @Test
  void concurrentReplacementIsObservedAfterWaitingForUserRow() throws Exception {
    var old = user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000));
    var worker = Executors.newSingleThreadExecutor();
    try (var updating = DATABASE.createConnection("")) {
      updating.setAutoCommit(false);
      try (var statement =
          updating.prepareStatement("update userinfo set secret = ? where uid = ?")) {
        statement.setString(1, "654321|" + (System.currentTimeMillis() + 120_000));
        statement.setString(2, old.getUid());
        statement.executeUpdate();
      }
      var attempt = worker.submit(() -> newNode().consume(old, "123456"));
      boolean waiting = false;
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      while (System.nanoTime() < deadline) {
        waiting =
            Boolean.TRUE.equals(
                admin.queryForObject(
                    "select exists (select 1 from pg_stat_activity where usename = 'replay_writer'"
                        + " and wait_event_type = 'Lock')",
                    Boolean.class));
        if (waiting || attempt.isDone()) break;
        Thread.sleep(20);
      }
      assertTrue(waiting, "consumption must wait for the concurrent primary-row update");
      updating.commit();
      assertFalse(attempt.get(10, TimeUnit.SECONDS));
      assertEquals(
          0, admin.queryForObject("select count(*) from usedtwofactorcode", Integer.class));
    } finally {
      worker.shutdownNow();
    }
  }

  @Test
  void unavailableAuthoritativeStateFailsClosed() {
    var old = user(TwoFactorType.EMAIL_ENABLED, "123456|" + (System.currentTimeMillis() + 120_000));
    admin.execute("revoke select on userinfo from replay_writer");
    try {
      assertThrows(DataAccessException.class, () -> newNode().consume(old, "123456"));
    } finally {
      admin.execute("grant select on userinfo to replay_writer");
    }
  }

  private UserDetails user(TwoFactorType type, String secret) {
    admin.update(
        "insert into userinfo (uid, secret, twofactortype) values (?, ?, ?)",
        "User1234567",
        secret,
        type.name());
    UserDetails user = mock(UserDetails.class);
    when(user.getUid()).thenReturn("User1234567");
    when(user.getTwoFactorType()).thenReturn(type);
    when(user.getSecret()).thenReturn(secret);
    return user;
  }
}
