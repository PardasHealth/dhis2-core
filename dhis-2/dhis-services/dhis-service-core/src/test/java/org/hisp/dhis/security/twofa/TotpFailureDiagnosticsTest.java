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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpFailureDiagnosticsTest {
  private static final String SECRET = "JBSWY3DPEHPK3PXP";
  private static final String CODE = "123456";
  private static final long NOW = 1_800_000_000_000L;
  private final Capture capture = new Capture();
  private Logger logger;
  private LoggerConfig original;

  @BeforeEach
  void captureLogger() {
    logger = (Logger) LogManager.getLogger(TwoFactorAuthUtils.class);
    var configuration = logger.getContext().getConfiguration();
    original = configuration.getLoggers().get(logger.getName());
    var isolated = new LoggerConfig(logger.getName(), Level.WARN, false);
    capture.start();
    isolated.addAppender(capture, null, null);
    configuration.addLogger(logger.getName(), isolated);
    logger.getContext().updateLoggers();
  }

  @AfterEach
  void restoreLogger() {
    var configuration = logger.getContext().getConfiguration();
    configuration.removeLogger(logger.getName());
    if (original != null) configuration.addLogger(logger.getName(), original);
    logger.getContext().updateLoggers();
    capture.stop();
  }

  @Test
  void decoderFailureRejectsProofWithoutLoggingSecretBearingException() throws Exception {
    try (var decoder = mockStatic(Base32.class)) {
      decoder
          .when(() -> Base32.decode(SECRET))
          .thenThrow(new Base32.DecodingException("private-secret=" + SECRET + " proof=" + CODE));
      assertFalse(TwoFactorAuthUtils.verifyTOTP2FACode(CODE, SECRET, NOW));
      assertSafeFailure("DECODE_ERROR");
    }
  }

  @Test
  void verifierFailureRejectsProofWithoutLoggingSecretBearingException() {
    Totp verifier = mock(Totp.class);
    when(verifier.verify(CODE))
        .thenThrow(new IllegalStateException("private-secret=" + SECRET + " proof=" + CODE));
    try (var utils = mockStatic(TwoFactorAuthUtils.class, CALLS_REAL_METHODS)) {
      utils
          .when(() -> TwoFactorAuthUtils.totpAtInterval(SECRET, NOW / 30_000))
          .thenReturn(verifier);
      assertFalse(TwoFactorAuthUtils.verifyTOTP2FACode(CODE, SECRET, NOW));
      assertSafeFailure("VERIFY_ERROR");
    }
  }

  private void assertSafeFailure(String reason) {
    assertEquals(1, capture.events.size());
    LogEvent event = capture.events.get(0);
    assertEquals(Level.WARN, event.getLevel());
    assertNull(event.getThrown());
    assertEquals(
        "TOTP_VALIDATION_FAILURE reason=" + reason + " action=REJECT_PROOF",
        event.getMessage().getFormattedMessage());
    assertFalse(event.getMessage().getFormattedMessage().contains(SECRET));
    assertFalse(event.getMessage().getFormattedMessage().contains(CODE));
  }

  private static final class Capture extends AbstractAppender {
    final List<LogEvent> events = new ArrayList<>();

    Capture() {
      super("afta-totp-failure-capture", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      events.add(event.toImmutable());
    }
  }
}
