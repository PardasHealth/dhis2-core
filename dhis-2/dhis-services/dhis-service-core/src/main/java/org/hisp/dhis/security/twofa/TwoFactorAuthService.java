/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.common.CodeGenerator.generateSecureRandomBytes;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs
 */
@Slf4j
@Service
@AllArgsConstructor
public class TwoFactorAuthService {

  public static final String TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME = "R_ENABLE_2FA";
  public static final long TWOFA_EMAIL_CODE_EXPIRY_MILLIS = 900_000; // 15 minutes

  private final SystemSettingsProvider settingsProvider;
  private final UserService userService;
  private final MessageSender emailMessageSender;
  private final UserSettingsService userSettingsService;
  private final I18nManager i18nManager;
  private final DhisConfigurationProvider configurationProvider;

  /**
   * Enroll user in time-based one-time password (TOTP) 2FA authentication.
   *
   * @param username The user that is being enrolled.
   */
  @Transactional
  public void enrollTOTP2FA(@Nonnull String username) throws ConflictException {
    enrollTOTP2FA(userService.getUserByUsername(username));
  }

  /** Continue enrollment for an already resolved account without another username lookup. */
  @Transactional
  public void enrollTOTP2FA(@Nonnull User user) throws ConflictException {
    if (user == null) {
      throw new ConflictException(ErrorCode.E6201);
    }
    if (user.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }
    if (!configurationProvider.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)) {
      throw new ConflictException(ErrorCode.E3046);
    }
    byte[] rawSeed = generateSecureRandomBytes(20);
    String totpSeed;
    try {
      totpSeed = Base32.encode(rawSeed);
    } finally {
      // Enrollment owns these random bytes; retain only the encoded user secret.
      Arrays.fill(rawSeed, (byte) 0);
    }
    user.setSecret(totpSeed);
    user.setTwoFactorType(TwoFactorType.ENROLLING_TOTP);
    userService.updateUser(user);
  }

  /**
   * Enroll user in email-based 2FA authentication.
   *
   * @param username The user that is being enrolled.
   */
  @Transactional
  public void enrollEmail2FA(@Nonnull String username) throws ConflictException {
    enrollEmail2FA(userService.getUserByUsername(username));
  }

  /** Continue enrollment for an already resolved account without another username lookup. */
  @Transactional
  public void enrollEmail2FA(@Nonnull User user) throws ConflictException {
    if (user == null) {
      throw new ConflictException(ErrorCode.E6201);
    }
    if (user.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }
    if (!configurationProvider.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED)) {
      throw new ConflictException(ErrorCode.E3045);
    }
    if (!user.isEmailVerified()) {
      throw new ConflictException(ErrorCode.E3043);
    }
    Email2FACode email2FACode = generateEmail2FACode();
    user.setSecret(email2FACode.encodedCode());
    user.setTwoFactorType(TwoFactorType.ENROLLING_EMAIL);

    send2FACodeWithEmailSender(user, email2FACode.code());

    userService.updateUser(user);
  }

  /**
   * Enable 2FA authentication for the user if the user is in the 2FA enrolling state. The user must
   * provide the correct 2FA code to enable 2FA. This proves that the user has access to the 2FA
   * secret (TOTP) or has access to the verified email (Email-based 2FA).
   *
   * @param username The user to enable 2FA authentication for.
   * @param code The 2FA code that the user generated with the authenticator app (TOTP), or the
   *     email based 2FA code sent to the user's email address.
   */
  @Transactional
  public void enable2FA(@Nonnull String username, @Nonnull String code, UserDetails currentUser)
      throws ConflictException, ForbiddenException {
    enable2FA(userService.getUserByUsername(username), code, currentUser);
  }

  /** Verify and update the same resolved account, retaining its immutable identity. */
  @Transactional
  public void enable2FA(@Nonnull User user, @Nonnull String code, UserDetails currentUser)
      throws ConflictException, ForbiddenException {
    if (user == null) {
      throw new ConflictException(ErrorCode.E6201);
    }
    if (user.getTwoFactorType().isEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }
    if (!user.getTwoFactorType().isEnrolling()) {
      throw new ConflictException(ErrorCode.E3029);
    }
    if (isInvalid2FACode(user, code)) {
      throw new ForbiddenException(ErrorCode.E3023);
    }

    user.setTwoFactorType(user.getTwoFactorType().getEnabledType());
    userService.updateUser(user, currentUser);
  }

  /**
   * If the user has 2FA authentication enabled, and the code is valid, then disable 2FA
   * authentication.
   *
   * @param username The user to disable 2FA authentication
   * @param code The 2FA code, (If no code i supplied and user has email 2FA, a code will be sent to
   *     the verified email address)
   */
  @Transactional
  public void disable2FA(@Nonnull String username, @Nonnull String code)
      throws ConflictException, ForbiddenException {
    disable2FA(userService.getUserByUsername(username), code);
  }

  /** Disable the resolved target; never redirect a verified proof through a mutable name. */
  @Transactional
  public void disable2FA(@Nonnull User user, @Nonnull String code)
      throws ConflictException, ForbiddenException {
    if (user == null) {
      throw new ConflictException(ErrorCode.E6201);
    }
    String username = user.getUsername();
    if (userService.is2FADisableEndpointLocked(username)) {
      throw new ConflictException(ErrorCode.E3042);
    }
    if (!user.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3031);
    }
    if (TwoFactorType.EMAIL_ENABLED.equals(user.getTwoFactorType())
        && Strings.isNullOrEmpty(code)) {
      sendEmail2FACode(user);
      throw new ConflictException(ErrorCode.E3051);
    }
    if (Strings.isNullOrEmpty(code)) {
      throw new ConflictException(ErrorCode.E3050);
    }

    if (isInvalid2FACode(user, code)) {
      userService.registerFailed2FADisableAttempt(user.getUsername());
      throw new ForbiddenException(ErrorCode.E3023);
    }

    reset2FA(user, CurrentUserUtil.getCurrentUserDetails());
    userService.registerSuccess2FADisable(user.getUsername());
  }

  private void reset2FA(@Nonnull User user, @Nonnull UserDetails actingUser) {
    user.setSecret(null);
    user.setTwoFactorType(null);
    userService.updateUser(user, actingUser);
  }

  /**
   * "If the current user is not the user being modified, and the current user has the authority to
   * modify the user, then disable two-factor authentication for the user."
   *
   * @param currentUser The user who is making the request.
   * @param userUid The user UID of the user to disable 2FA for.
   * @param errors A Consumer<ErrorReport> object that will be called if there is an error.
   */
  @Transactional
  public void privileged2FADisable(
      @Nonnull UserDetails currentUser,
      @Nonnull String userUid,
      @Nonnull Consumer<ErrorReport> errors)
      throws ForbiddenException, NotFoundException {
    User user = userService.getUser(userUid);
    if (user == null) {
      throw new NotFoundException(ErrorCode.E6201);
    }
    if (currentUser.getUid().equals(user.getUid())
        || !userService.canCurrentUserCanModify(currentUser, user, errors)) {
      throw new ForbiddenException(ErrorCode.E3021);
    }
    reset2FA(user, currentUser);
  }

  /**
   * Email the user with a new 2FA code.
   *
   * @param username The user to send the 2FA code.
   */
  @Transactional
  public void sendEmail2FACode(@Nonnull String username) throws ConflictException {
    sendEmail2FACode(userService.getUserByUsername(username));
  }

  /** Issue a login challenge for the immutable identity carried by the authenticated principal. */
  @Transactional
  public void sendEmail2FACodeByUid(String uid) throws ConflictException {
    if (uid == null || uid.isBlank()) {
      throw new ConflictException(ErrorCode.E6201);
    }
    sendEmail2FACode(userService.getUser(uid));
  }

  /** Retain the selected account through challenge validation, delivery and persistence. */
  private void sendEmail2FACode(User user) throws ConflictException {
    if (user == null) {
      throw new ConflictException(ErrorCode.E6201);
    }
    if (!user.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3031);
    }
    if (!user.getTwoFactorType().equals(TwoFactorType.EMAIL_ENABLED)) {
      throw new ConflictException(ErrorCode.E3048);
    }
    if (!user.isEmailVerified()) {
      throw new ConflictException(ErrorCode.E3043);
    }

    Email2FACode email2FACode = generateEmail2FACode();
    user.setSecret(email2FACode.encodedCode());

    send2FACodeWithEmailSender(user, email2FACode.code());

    userService.updateUser(user, new SystemUser());
  }

  public record Email2FACode(String code, String encodedCode) {}

  @Nonnull
  @NonTransactional
  public static Email2FACode generateEmail2FACode() {
    char[] owned = CodeGenerator.generateSecureRandomNumber(6);
    String code;
    try {
      code = new String(owned);
    } finally {
      Arrays.fill(owned, '\0');
    }
    String encodedCode = code + "|" + (System.currentTimeMillis() + TWOFA_EMAIL_CODE_EXPIRY_MILLIS);
    return new Email2FACode(code, encodedCode);
  }

  private void send2FACodeWithEmailSender(@Nonnull User user, @Nonnull String code)
      throws ConflictException {
    I18n i18n =
        i18nManager.getI18n(
            userSettingsService.getUserSettings(user.getUsername(), true).getUserUiLocale());

    String applicationTitle = settingsProvider.getCurrentSettings().getApplicationTitle();

    Map<String, Object> vars = new HashMap<>();
    vars.put("applicationTitle", applicationTitle);
    vars.put("i18n", i18n);
    vars.put("username", user.getUsername());
    vars.put("email", user.getEmail());
    vars.put("fullName", user.getName());
    vars.put("code", code);

    VelocityManager vm = new VelocityManager();
    String messageBody = vm.render(vars, "twofa_email_body_template_v1");
    String messageSubject = i18n.getString("email_2fa_subject") + " " + applicationTitle;

    OutboundMessageResponse status =
        emailMessageSender.sendMessage(messageSubject, messageBody, null, null, Set.of(user), true);

    if (EmailResponse.SENT != status.getResponseObject()) {
      throw new ConflictException(ErrorCode.E3049);
    }
  }

  /**
   * Generates the TOTP enrollment QR code.
   *
   * <p>Takes the {@link User} entity, not {@link UserDetails}: enrollment writes a new secret and
   * {@code ENROLLING_TOTP} type to the database without refreshing the authenticated principal, so
   * a {@code UserDetails} snapshot from login time still reports the pre-enrollment secret and
   * type, and this method would reject with {@link ErrorCode#E3047}.
   */
  @NonTransactional
  public @Nonnull byte[] generateQRCode(@Nonnull User currentUser) throws ConflictException {
    if (!configurationProvider.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED)) {
      throw new ConflictException(ErrorCode.E3046);
    }
    if (!TwoFactorType.ENROLLING_TOTP.equals(currentUser.getTwoFactorType())) {
      throw new ConflictException(ErrorCode.E3047);
    }

    String totpURL =
        TwoFactorAuthUtils.generateTOTP2FAURL(
            settingsProvider.getCurrentSettings().getApplicationTitle(),
            currentUser.getSecret(),
            currentUser.getUsername());

    List<ErrorCode> errorCodes = new ArrayList<>();
    byte[] qrCode = TwoFactorAuthUtils.generateQRCode(totpURL, 200, 200, errorCodes::add);
    // Check for errors in the QR code generation
    if (!errorCodes.isEmpty()) {
      throw new ConflictException(errorCodes.get(0));
    }
    return qrCode;
  }

  /**
   * Verify the 2FA code for the user.
   *
   * @param user The user
   * @param code The 2FA code
   * @return true if the code is invalid, false if the code is valid.
   */
  private boolean isInvalid2FACode(@Nonnull User user, @Nonnull String code)
      throws ConflictException {
    if (Strings.isNullOrEmpty(user.getSecret())) {
      throw new ConflictException(ErrorCode.E3028);
    }
    return !TwoFactorAuthUtils.isValid2FACode(user.getTwoFactorType(), code, user.getSecret());
  }
}
