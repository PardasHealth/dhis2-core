/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hisp.dhis.audit.AuditIntegrityGuard.StoredRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuditIntegrityGuardTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
  private final AuditIntegrityGuard guard = new AuditIntegrityGuard(true, KEY);

  static Stream<String> invalidKeys() {
    return Stream.of(
        null,
        "",
        " ",
        "not-base64-key",
        "YQ==",
        Base64.getEncoder().encodeToString(new byte[31]),
        Base64.getEncoder().encodeToString(new byte[33]));
  }

  @ParameterizedTest
  @MethodSource("invalidKeys")
  void enforcementRejectsMissingMalformedOrWrongLengthKey(String key) {
    var error = assertThrows(IllegalStateException.class, () -> new AuditIntegrityGuard(true, key));
    if (key != null && !key.isBlank()) assertFalse(error.getMessage().contains(key));
  }

  @Test
  void disabledModeRetainsLegacyContractWithoutParsingAKey() {
    var disabled = new AuditIntegrityGuard(false, "not-a-key");
    assertFalse(disabled.isEnabled());
    assertNull(disabled.sign(record("unchanged")));
    assertDoesNotThrow(() -> disabled.verify(null, null));
  }

  @Test
  void unchangedStoredRepresentationVerifies() {
    String tag = guard.sign(record("unchanged"));
    assertTrue(tag.matches("v1:[0-9a-f]{64}"));
    assertDoesNotThrow(() -> guard.verify(record("unchanged"), tag));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "id",
        "type",
        "scope",
        "time",
        "actor",
        "klass",
        "uid",
        "code",
        "attributes",
        "data"
      })
  void everyProtectedFieldIsAuthenticated(String field) {
    var error =
        assertThrows(
            AuditIntegrityViolationException.class,
            () -> guard.verify(record(field), guard.sign(record("unchanged"))));
    assertFalse(error.getMessage().contains("protected-payload"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "bad",
        "v2:0000",
        "v1:ABCDEF",
        "v1:0000000000000000000000000000000000000000000000000000000000000000"
      })
  void malformedOrMismatchedTagsBlockReads(String tag) {
    assertThrows(
        AuditIntegrityViolationException.class, () -> guard.verify(record("unchanged"), tag));
  }

  @Test
  void unsignedLegacyRowBlocksWhenEnforcementIsEnabled() {
    assertThrows(
        AuditIntegrityViolationException.class, () -> guard.verify(record("unchanged"), null));
  }

  @Test
  void anotherKeyCannotAuthenticateTheRecord() {
    byte[] other = new byte[32];
    other[0] = 1;
    var writer = new AuditIntegrityGuard(true, Base64.getEncoder().encodeToString(other));
    assertThrows(
        AuditIntegrityViolationException.class,
        () -> guard.verify(record("unchanged"), writer.sign(record("unchanged"))));
  }

  @Test
  void nullAndEmptyOptionalFieldsAreDistinct() {
    var base = record("unchanged");
    var nullCode =
        new StoredRecord(
            base.id(),
            base.auditType(),
            base.auditScope(),
            base.createdAt(),
            base.createdBy(),
            base.klass(),
            base.uid(),
            null,
            base.attributes(),
            base.data());
    var emptyCode =
        new StoredRecord(
            base.id(),
            base.auditType(),
            base.auditScope(),
            base.createdAt(),
            base.createdBy(),
            base.klass(),
            base.uid(),
            "",
            base.attributes(),
            base.data());
    assertNotEquals(guard.sign(nullCode), guard.sign(emptyCode));
  }

  @Test
  void fieldBoundariesCannotBeMovedWithoutChangingTheTag() {
    var base = record("unchanged");
    var first =
        new StoredRecord(
            base.id(),
            base.auditType(),
            base.auditScope(),
            base.createdAt(),
            "a",
            "bc",
            base.uid(),
            base.code(),
            base.attributes(),
            base.data());
    var second =
        new StoredRecord(
            base.id(),
            base.auditType(),
            base.auditScope(),
            base.createdAt(),
            "ab",
            "c",
            base.uid(),
            base.code(),
            base.attributes(),
            base.data());
    assertNotEquals(guard.sign(first), guard.sign(second));
  }

  @Test
  void concurrentCallsDoNotShareMutableMacState() {
    Set<String> tags = ConcurrentHashMap.newKeySet();
    IntStream.range(1, 101)
        .parallel()
        .forEach(
            id -> {
              var base = record("unchanged");
              var record =
                  new StoredRecord(
                      id,
                      base.auditType(),
                      base.auditScope(),
                      base.createdAt(),
                      base.createdBy(),
                      base.klass(),
                      base.uid(),
                      base.code(),
                      base.attributes(),
                      base.data());
              String tag = guard.sign(record);
              guard.verify(record, tag);
              tags.add(tag);
            });
    assertEquals(100, tags.size());
  }

  private static StoredRecord record(String changed) {
    return new StoredRecord(
        changed.equals("id") ? 2 : 1,
        changed.equals("type") ? "UPDATE" : "CREATE",
        changed.equals("scope") ? "TRACKER" : "METADATA",
        LocalDateTime.of(2026, 11, 18, 12, 34, 56, changed.equals("time") ? 1000 : 0),
        changed.equals("actor") ? "other" : "actor-سلام",
        changed.equals("klass") ? "Other" : "DataElement",
        changed.equals("uid") ? "Other123456" : "Owner123456",
        changed.equals("code") ? "other-code" : "code",
        changed.equals("attributes") ? "{\"x\":2}" : "{\"x\":1}",
        (changed.equals("data") ? "changed-payload" : "protected-payload")
            .getBytes(StandardCharsets.UTF_8));
  }
}
