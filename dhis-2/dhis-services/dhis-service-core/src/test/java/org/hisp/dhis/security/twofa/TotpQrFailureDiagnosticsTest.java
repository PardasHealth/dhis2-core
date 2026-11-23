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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpQrFailureDiagnosticsTest {
  private static final String URI = "otpauth://totp/Test:alice?secret=JBSWY3DPEHPK3PXP&issuer=Test";
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
  void encoderFailureReturnsExistingErrorWithoutLoggingProvisioningUri() {
    try (var writers =
        mockConstruction(
            MultiFormatWriter.class,
            (writer, context) ->
                when(writer.encode(anyString(), eq(BarcodeFormat.QR_CODE), eq(200), eq(200)))
                    .thenThrow(new WriterException("private URI=" + URI)))) {
      assertSafeFailure();
    }
  }

  @Test
  void pngWriteFailureReturnsExistingErrorWithoutLoggingProvisioningUri() {
    try (var png = mockStatic(MatrixToImageWriter.class)) {
      png.when(
              () ->
                  MatrixToImageWriter.writeToStream(
                      any(BitMatrix.class), eq("PNG"), any(OutputStream.class)))
          .thenThrow(new IOException("private URI=" + URI));
      assertSafeFailure();
    }
  }

  @Test
  void validPngStillDecodesToTheOriginalProvisioningUri() throws Exception {
    List<ErrorCode> errors = new ArrayList<>();
    byte[] png = TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add);
    assertTrue(png.length > 0);
    var bitmap =
        new BinaryBitmap(
            new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))));
    assertEquals(URI, new MultiFormatReader().decode(bitmap).getText());
    assertTrue(errors.isEmpty());
    assertTrue(capture.events.isEmpty());
  }

  private void assertSafeFailure() {
    List<ErrorCode> errors = new ArrayList<>();
    assertEquals(0, TwoFactorAuthUtils.generateQRCode(URI, 200, 200, errors::add).length);
    assertEquals(List.of(ErrorCode.E3026), errors);
    assertEquals(1, capture.events.size());
    LogEvent event = capture.events.get(0);
    assertEquals(Level.WARN, event.getLevel());
    assertNull(event.getThrown());
    assertEquals(
        "TOTP_QR_FAILURE errorCode=E3026 action=RETURN_ERROR",
        event.getMessage().getFormattedMessage());
    assertFalse(event.getMessage().getFormattedMessage().contains(URI));
    assertFalse(event.getMessage().getFormattedMessage().contains("JBSWY3DPEHPK3PXP"));
  }

  private static final class Capture extends AbstractAppender {
    final List<LogEvent> events = new ArrayList<>();

    Capture() {
      super("afta-totp-qr-failure-capture", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      events.add(event.toImmutable());
    }
  }
}
