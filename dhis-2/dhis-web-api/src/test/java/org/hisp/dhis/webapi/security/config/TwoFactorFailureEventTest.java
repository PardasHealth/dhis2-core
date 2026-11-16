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
package org.hisp.dhis.webapi.security.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;
import org.hisp.dhis.security.spring2fa.TwoFactorCodeSentException;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.test.util.ReflectionTestUtils;

class TwoFactorFailureEventTest {
  @ParameterizedTest
  @EnumSource(
      value = TwoFactorType.class,
      names = {"TOTP_ENABLED", "EMAIL_ENABLED"})
  void rejectedProofPublishesFailureAndReachesAccountCounter(TwoFactorType type) {
    var users = mock(UserService.class);
    var listener = new AuthenticationListener();
    ReflectionTestUtils.setField(listener, "userService", users);
    var events = new ArrayList<Object>();
    var publisher = new AuthenticationProviderConfig().authenticationEventPublisher();
    publisher.setApplicationEventPublisher(
        event -> {
          events.add(event);
          if (event instanceof AbstractAuthenticationFailureEvent failure) {
            listener.handleAuthenticationFailure(failure);
          }
        });
    var authentication = UsernamePasswordAuthenticationToken.unauthenticated("owner", null);
    var failure = new TwoFactorAuthenticationException("Rejected proof", type);

    publisher.publishAuthenticationFailure(failure, authentication);

    assertEquals(1, events.size());
    var event = assertInstanceOf(AuthenticationFailureBadCredentialsEvent.class, events.get(0));
    assertSame(authentication, event.getAuthentication());
    assertSame(failure, event.getException());
    verify(users).registerFailedLogin("owner");
  }

  @Test
  void successfullyIssuedEmailChallengeDoesNotPublishCredentialFailure() {
    List<Object> events = new ArrayList<>();
    var publisher = new AuthenticationProviderConfig().authenticationEventPublisher();
    publisher.setApplicationEventPublisher(events::add);
    publisher.publishAuthenticationFailure(
        new TwoFactorCodeSentException("Challenge sent", TwoFactorType.EMAIL_ENABLED),
        UsernamePasswordAuthenticationToken.unauthenticated("owner", null));
    assertTrue(events.isEmpty());
  }
}
