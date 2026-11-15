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
package org.hisp.dhis.user;

import static org.hisp.dhis.user.UserConstants.LOGIN_MAX_FAILED_ATTEMPTS;
import static org.hisp.dhis.user.UserConstants.RECOVER_MAX_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.client.RestTemplate;

/** Concurrent failures must not overwrite each other in the configured counters. */
@ExtendWith(MockitoExtension.class)
class UserFailureCounterConcurrencyTest {

  private DefaultUserService userService;

  private final SessionRegistryImpl sessionRegistry = new SessionRegistryImpl();

  @Mock private UserSettingsService userSettingsService;
  @Mock private RestTemplate restTemplate;
  @Mock private MessageSender emailMessageSender;
  @Mock private I18nManager i18nManager;
  @Mock private ObjectMapper jsonMapper;
  @Mock private UserStore userStore;
  @Mock private UserGroupService userGroupService;
  @Mock private UserRoleStore userRoleStore;
  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private SystemSettings settings;
  @Mock private CacheProvider cacheProvider;
  @Mock private PasswordManager passwordManager;
  @Mock private AclService aclService;
  @Mock private OrganisationUnitService organisationUnitService;
  @Mock private AttributeService attributeService;
  @Mock private Cache<String> userDisplayNameCache;
  private Cache<Integer> userFailedLoginAttemptCache =
      spy(new SimpleCacheBuilder<Integer>().withDefaultValue(0).build());
  private Cache<Integer> userAccountRecoverAttemptCache =
      spy(new SimpleCacheBuilder<Integer>().withDefaultValue(0).build());
  private Cache<Integer> twoFaDisableFailedAttemptCache =
      spy(new SimpleCacheBuilder<Integer>().withDefaultValue(0).build());

  @BeforeEach
  void setUp() {
    when(cacheProvider.<String>createUserDisplayNameCache()).thenReturn(userDisplayNameCache);
    when(cacheProvider.<Integer>createUserFailedLoginAttemptCache(0))
        .thenReturn(userFailedLoginAttemptCache);
    when(cacheProvider.<Integer>createUserAccountRecoverAttemptCache(0))
        .thenReturn(userAccountRecoverAttemptCache);
    when(cacheProvider.<Integer>createDisable2FAFailedAttemptCache(0))
        .thenReturn(twoFaDisableFailedAttemptCache);

    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    lenient().when(settings.getLockMultipleFailedLogins()).thenReturn(true);

    userService =
        new DefaultUserService(
            userSettingsService,
            restTemplate,
            emailMessageSender,
            i18nManager,
            jsonMapper,
            userStore,
            userGroupService,
            userRoleStore,
            settingsProvider,
            cacheProvider,
            passwordManager,
            aclService,
            organisationUnitService,
            sessionRegistry,
            attributeService);
  }

  @Test
  void concurrentLoginFailuresBothCount() throws Exception {
    alignLegacyReads(userFailedLoginAttemptCache);
    runTogether(() -> userService.registerFailedLogin("account"));
    assertEquals(2, userFailedLoginAttemptCache.getIfPresent("account").orElseThrow());
  }

  @Test
  void concurrentRecoveryFailuresBothCount() throws Exception {
    alignLegacyReads(userAccountRecoverAttemptCache);
    runTogether(() -> userService.registerRecoveryAttempt("account"));
    assertEquals(2, userAccountRecoverAttemptCache.getIfPresent("account").orElseThrow());
  }

  @Test
  void concurrentMfaDisableFailuresBothCount() throws Exception {
    alignLegacyReads(twoFaDisableFailedAttemptCache);
    runTogether(() -> userService.registerFailed2FADisableAttempt("account"));
    assertEquals(2, twoFaDisableFailedAttemptCache.getIfPresent("account").orElseThrow());
  }

  @Test
  void existingThresholdsAndSuccessResetsRemainInPlace() {
    for (int i = 0; i < LOGIN_MAX_FAILED_ATTEMPTS; i++) {
      userService.registerFailedLogin("account");
      userService.registerFailed2FADisableAttempt("account");
    }
    assertTrue(userService.isLocked("account"));
    assertTrue(userService.is2FADisableEndpointLocked("account"));
    userService.registerSuccessfulLogin("account");
    userService.registerSuccess2FADisable("account");
    assertFalse(userService.isLocked("account"));
    assertFalse(userService.is2FADisableEndpointLocked("account"));
    for (int i = 0; i < RECOVER_MAX_ATTEMPTS; i++) userService.registerRecoveryAttempt("account");
    assertFalse(userService.isRecoveryLocked("account"));
    userService.registerRecoveryAttempt("account");
    assertTrue(userService.isRecoveryLocked("account"));
  }

  @Test
  void disabledLoginBlockingRetainsItsExistingBehavior() {
    when(settings.getLockMultipleFailedLogins()).thenReturn(false);
    userService.registerFailedLogin("account");
    userService.registerRecoveryAttempt("account");
    assertTrue(userFailedLoginAttemptCache.getIfPresent("account").isEmpty());
    assertTrue(userAccountRecoverAttemptCache.getIfPresent("account").isEmpty());
    assertFalse(userService.isLocked("account"));
    assertFalse(userService.isRecoveryLocked("account"));
  }

  private void alignLegacyReads(Cache<Integer> cache) {
    var barrier = new CyclicBarrier(2);
    lenient()
        .doAnswer(
            invocation -> {
              Object old = invocation.callRealMethod();
              barrier.await(10, TimeUnit.SECONDS);
              return old;
            })
        .when(cache)
        .get("account");
  }

  private void runTogether(Runnable attempt) throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var first =
          pool.submit(
              () -> {
                start.await();
                attempt.run();
                return null;
              });
      var second =
          pool.submit(
              () -> {
                start.await();
                attempt.run();
                return null;
              });
      start.countDown();
      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }
  }
}
