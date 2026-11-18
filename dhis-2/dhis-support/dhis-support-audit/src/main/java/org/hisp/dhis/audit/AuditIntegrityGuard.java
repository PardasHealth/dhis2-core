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
package org.hisp.dhis.audit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/** Versioned, keyed authentication of the database audit record representation. */
@Slf4j
public final class AuditIntegrityGuard {
  private static final String ALGORITHM = "HmacSHA256";
  private static final Pattern TAG = Pattern.compile("v1:[0-9a-f]{64}");
  private final boolean enabled;
  private final SecretKeySpec key;

  public AuditIntegrityGuard(boolean enabled, String encodedKey) {
    this.enabled = enabled;
    if (!enabled) {
      key = null;
      return;
    }
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey);
    } catch (IllegalArgumentException error) {
      throw invalidKey();
    }
    try {
      if (decoded.length != 32) throw invalidKey();
      key = new SecretKeySpec(decoded, ALGORITHM);
    } finally {
      Arrays.fill(decoded, (byte) 0);
    }
    newMac(); // Fail startup rather than discovering a missing provider on the first read.
  }

  public boolean isEnabled() {
    return enabled;
  }

  String sign(StoredRecord record) {
    return enabled ? "v1:" + HexFormat.of().formatHex(authenticate(record)) : null;
  }

  void verify(StoredRecord record, String tag) {
    if (!enabled) return;
    if (tag == null || !TAG.matcher(tag).matches()) {
      throw rejection(record.id(), "missing_or_malformed_tag");
    }
    boolean valid;
    try {
      valid =
          MessageDigest.isEqual(authenticate(record), HexFormat.of().parseHex(tag.substring(3)));
    } catch (RuntimeException error) {
      throw rejection(record.id(), "verification_unavailable");
    }
    if (!valid) throw rejection(record.id(), "authentication_failed");
  }

  private static IllegalStateException invalidKey() {
    return new IllegalStateException(
        "audit.integrity.key must be a Base64-encoded 32-byte key when audit integrity is enabled");
  }

  private static AuditIntegrityViolationException rejection(long id, String reason) {
    log.error("AUDIT_INTEGRITY_FAILURE recordId={} action=BLOCK_READ reason={}", id, reason);
    return new AuditIntegrityViolationException(id);
  }

  private Mac newMac() {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(key);
      return mac;
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("Audit integrity authentication is unavailable");
    }
  }

  private byte[] authenticate(StoredRecord record) {
    Mac mac = newMac();
    text(mac, "DHIS2 audit integrity v1");
    text(mac, Long.toString(record.id()));
    text(mac, record.auditType());
    text(mac, record.auditScope());
    text(mac, record.createdAt() == null ? null : record.createdAt().toString());
    text(mac, record.createdBy());
    text(mac, record.klass());
    text(mac, record.uid());
    text(mac, record.code());
    // The repository supplies PostgreSQL's stored JSONB text, avoiding lossy numeric parsing.
    text(mac, record.attributes());
    field(mac, record.data());
    return mac.doFinal();
  }

  private static void text(Mac mac, String value) {
    byte[] encoded = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    try {
      field(mac, encoded);
    } finally {
      if (encoded != null) Arrays.fill(encoded, (byte) 0);
    }
  }

  private static void field(Mac mac, byte[] value) {
    mac.update(
        ByteBuffer.allocate(Integer.BYTES).putInt(value == null ? -1 : value.length).array());
    if (value != null) mac.update(value);
  }

  record StoredRecord(
      long id,
      String auditType,
      String auditScope,
      LocalDateTime createdAt,
      String createdBy,
      String klass,
      String uid,
      String code,
      String attributes,
      byte[] data) {
    @Override
    public String toString() {
      return "AuditIntegrityRecord[id=" + id + "]";
    }
  }
}
