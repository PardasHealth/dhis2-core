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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.LongSupplier;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Atomically reserves a login challenge/step across nodes, before authentication can succeed. */
@Service
public class TwoFactorReplayService {
  private static final long STEP_MILLIS = 30_000;
  // Keep reservations beyond the OTP window so a faster node cannot immediately
  // delete state for a code still considered current by a slower node.
  static final long RETENTION_MILLIS = 86_400_000;
  private final JdbcTemplate jdbcTemplate;
  private final LongSupplier clock;

  @Autowired
  public TwoFactorReplayService(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, System::currentTimeMillis);
  }

  TwoFactorReplayService(JdbcTemplate jdbcTemplate, LongSupplier clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
  }

  // Commit the reservation independently: a later session/login failure must not
  // restore the ability to replay an already accepted code.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean consume(UserDetails user, String suppliedCode) {
    if (user == null
        || StringUtils.isBlank(user.getUid())
        || StringUtils.isBlank(user.getSecret())
        || StringUtils.isBlank(user.getPassword())
        || StringUtils.isBlank(suppliedCode)) {
      return false;
    }
    String code = StringUtils.deleteWhitespace(suppliedCode);
    String secret = user.getSecret();
    TwoFactorType type = user.getTwoFactorType();
    if (type != TwoFactorType.EMAIL_ENABLED && type != TwoFactorType.TOTP_ENABLED) return false;
    // A principal may predate a replacement email challenge or factor reset. Read
    // the primary row directly, bypassing ORM caches, and serialize consumption
    // with updates to that row until this independent transaction commits.
    var authoritative =
        jdbcTemplate.query(
            "select secret, twofactortype, password, disabled, externalauth, accountexpiry"
                + " from userinfo where uid = ? for update",
            (rs, row) ->
                new CurrentLoginState(
                    secret.equals(rs.getString(1))
                        && type.name().equals(rs.getString(2))
                        && user.getPassword().equals(rs.getString(3))
                        && !rs.getBoolean(4)
                        && !rs.getBoolean(5),
                    rs.getTimestamp(6)),
            user.getUid());
    if (authoritative.size() != 1 || !authoritative.get(0).matches()) return false;
    // Capture time after waiting for the row lock, so waiting cannot prolong validity.
    long now = clock.getAsLong();
    var accountExpiry = authoritative.get(0).accountExpiry();
    if (accountExpiry != null && accountExpiry.getTime() <= now) return false;
    long expires;
    String challenge;
    if (type == TwoFactorType.EMAIL_ENABLED) {
      if (!TwoFactorAuthUtils.verifyEmail2FACode(code, secret, now)) return false;
      expires = Long.parseLong(secret.substring(secret.indexOf('|') + 1));
      if (expires <= now) return false;
      challenge = secret; // Includes expiry, so a replacement challenge has distinct state.
    } else if (type == TwoFactorType.TOTP_ENABLED) {
      if (!TwoFactorAuthUtils.verifyTOTP2FACode(code, secret, now)) return false;
      long current = now / STEP_MILLIS;
      long matched = -1;
      // AeroGear accepts the current or immediately preceding 30-second step.
      // Prefer the current step in the rare case that both yield the same value.
      for (long step = current; step >= Math.max(0, current - 1); step--) {
        if (code.equals(TwoFactorAuthUtils.totpAtInterval(secret, step).now())) {
          matched = step;
          break;
        }
      }
      if (matched < 0) return false;
      expires = Math.multiplyExact(matched + 2, STEP_MILLIS);
      challenge = secret + ":" + matched;
    } else {
      return false;
    }
    String key = fingerprint("login:v1:" + user.getUid() + ":" + type + ":" + challenge);
    jdbcTemplate.update("delete from usedtwofactorcode where expires <= ?", now);
    return jdbcTemplate.update(
            "insert into usedtwofactorcode (codehash, expires) values (?, ?) on conflict do"
                + " nothing",
            key,
            Math.addExact(expires, RETENTION_MILLIS))
        == 1;
  }

  private record CurrentLoginState(boolean matches, java.sql.Timestamp accountExpiry) {}

  private static String fingerprint(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
