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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.util.Timer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
class AuditRepositoryTest extends PostgresIntegrationTestBase {

  @Autowired private AuditRepository auditRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testSaveAudit() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 1, 1, 0, 0))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .build();
    auditRepository.save(audit);
    assertEquals(1, auditRepository.query(AuditQuery.builder().build()).size());
  }

  @Test
  void testDeleteAudit() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 1, 1, 0, 0))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .build();
    long id = auditRepository.save(audit);
    assertNotEquals(0, id, "audit should have been saved");
    audit.setId(id);
    auditRepository.delete(audit);
    List<Audit> audits = auditRepository.query(AuditQuery.builder().build());
    assertTrue(audits.isEmpty());
  }

  @Test
  void testAuditQueryAll() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits = auditRepository.query(AuditQuery.builder().build());
    assertEquals(100, audits.size());
  }

  @Test
  void testAuditQueryAuditType() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder().auditType(Sets.newHashSet(AuditType.CREATE)).build());
    assertEquals(100, audits.size());
  }

  @Test
  void testAuditQueryAuditTypeNoMatch() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder().auditType(Sets.newHashSet(AuditType.UPDATE)).build());
    assertTrue(audits.isEmpty());
  }

  @Test
  void testAuditQueryCountAuditType() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder().auditType(Sets.newHashSet(AuditType.CREATE)).build());
    assertEquals(100, audits);
  }

  @Test
  void testAuditQueryCountAuditTypeNoMatch() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder().auditType(Sets.newHashSet(AuditType.UPDATE)).build());
    assertEquals(0, audits);
  }

  @Test
  void testAuditQueryAuditScope() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder().auditScope(Sets.newHashSet(AuditScope.AGGREGATE)).build());
    assertEquals(100, audits.size());
  }

  @Test
  void testAuditQueryAuditScopeNoMatch() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder().auditScope(Sets.newHashSet(AuditScope.TRACKER)).build());
    assertTrue(audits.isEmpty());
  }

  @Test
  void testAuditQueryCountAuditScope() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder().auditScope(Sets.newHashSet(AuditScope.AGGREGATE)).build());
    assertEquals(100, audits);
  }

  @Test
  void testAuditQueryKlasses() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder()
                .klass(
                    Sets.newHashSet(DataElement.class.getName(), OrganisationUnit.class.getName()))
                .build());
    assertEquals(100, audits.size());
  }

  @Test
  void testAuditQueryUids() {
    List<String> uids = new ArrayList<>();
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              uids.add(uid);
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(AuditQuery.builder().uid(new HashSet<>(uids.subList(0, 50))).build());
    assertEquals(50, audits.size());
  }

  @Test
  void testAuditQueryCodes() {
    List<String> codes = new ArrayList<>();
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              codes.add(code);
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    List<Audit> audits =
        auditRepository.query(
            AuditQuery.builder().code(new HashSet<>(codes.subList(50, 100))).build());
    assertEquals(50, audits.size());
  }

  @Test
  void testAuditQueryCountKlasses() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder()
                .klass(
                    Sets.newHashSet(DataElement.class.getName(), OrganisationUnit.class.getName()))
                .build());
    assertEquals(100, audits);
  }

  @Test
  void testAuditQueryCountUids() {
    List<String> uids = new ArrayList<>();
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              uids.add(uid);
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder().uid(new HashSet<>(uids.subList(50, 100))).build());
    assertEquals(50, audits);
  }

  @Test
  void testAuditQueryCountCodes() {
    List<String> codes = new ArrayList<>();
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              codes.add(code);
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder().code(new HashSet<>(codes.subList(50, 100))).build());
    assertEquals(50, audits);
  }

  @Test
  void testAuditQueryDateRange() {
    IntStream.rangeClosed(1, 100)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(1999 + n, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              auditRepository.save(audit);
            });
    int audits =
        auditRepository.count(
            AuditQuery.builder()
                .range(AuditQuery.range(LocalDateTime.of(2050, 1, 1, 0, 0, 0)))
                .build());
    assertEquals(50, audits);
    audits =
        auditRepository.count(
            AuditQuery.builder()
                .range(
                    AuditQuery.range(
                        LocalDateTime.of(2050, 1, 1, 0, 0, 0),
                        LocalDateTime.of(2080, 1, 1, 0, 0, 0)))
                .build());
    assertEquals(30, audits);
  }

  @Test
  void testCompressDecompress() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    AuditAttributes attributes = new AuditAttributes();
    attributes.put("path", "a.b.c");
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 1, 1, 0, 0))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .attributes(attributes)
            .data("This is a message")
            .build();
    auditRepository.save(audit);
    List<Audit> query = auditRepository.query(AuditQuery.builder().build());
    assertEquals(1, query.size());
    Audit persistedAudit = query.get(0);
    assertEquals(uid, persistedAudit.getUid());
    assertEquals(code, persistedAudit.getCode());
    assertEquals("This is a message", persistedAudit.getData());
    assertNotNull(persistedAudit.getAttributes());
    assertEquals("a.b.c", persistedAudit.getAttributes().get("path"));
  }


  @Test
  void testCorruptCompressedAuditPayloadFailsExplicitly() {
    String uid = CodeGenerator.generateUid();
    long auditId =
        auditRepository.save(
            Audit.builder()
                .auditType(AuditType.CREATE)
                .auditScope(AuditScope.AGGREGATE)
                .createdAt(LocalDateTime.of(2026, 10, 20, 13, 14))
                .createdBy("test-user")
                .klass(DataElement.class.getName())
                .uid(uid)
                .code(CodeGenerator.generateUid())
                .data("valid compressed audit payload")
                .build());
    jdbcTemplate.update(
        "UPDATE audit SET data = ? WHERE auditid = ?",
        new byte[] {0x01, 0x02, 0x03},
        auditId);
    assertThrows(
        IllegalStateException.class,
        () -> auditRepository.query(AuditQuery.builder().uid(Sets.newHashSet(uid)).build()));
  }

  @Test
  void testAuditTimestampPreservesMicroseconds() {
    LocalDateTime timestamp = LocalDateTime.of(2026, 10, 20, 13, 14, 15, 123456000);
    String uid = CodeGenerator.generateUid();
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(timestamp)
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(CodeGenerator.generateUid())
            .data("{}")
            .build();
    auditRepository.save(audit);
    List<Audit> found = auditRepository.query(AuditQuery.builder().uid(Sets.newHashSet(uid)).build());
    assertEquals(1, found.size());
    assertEquals(timestamp, found.get(0).getCreatedAt());
  }

  @Test
  void testAuditAttributeQueryTreatsSqlMetacharactersAsData() {
    String uid = CodeGenerator.generateUid();
    AuditAttributes attributes = new AuditAttributes();
    attributes.put("key' OR 1=1 --", "value' OR 1=1 --");
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2026, 10, 20, 13, 14))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(CodeGenerator.generateUid())
            .attributes(attributes)
            .data("{}")
            .build();
    auditRepository.save(audit);
    List<Audit> found =
        auditRepository.query(
            AuditQuery.builder().uid(Sets.newHashSet(uid)).auditAttributes(attributes).build());
    assertEquals(1, found.size());
    assertEquals(uid, found.get(0).getUid());
    AuditAttributes mismatch = new AuditAttributes();
    mismatch.put("key' OR 1=1 --", "different' OR 1=1 --");
    assertTrue(
        auditRepository.query(AuditQuery.builder().auditAttributes(mismatch).build()).isEmpty());
  }

  @Test
  void testAuditUidAndCodeFiltersTreatSqlInjectionStringsAsData() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    auditRepository.save(
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2026, 10, 20, 13, 14))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .build());

    String attack = "' OR 1=1 --";
    assertEquals(
        0,
        auditRepository.count(
            AuditQuery.builder().uid(Sets.newHashSet(attack)).build()));
    assertTrue(
        auditRepository.query(
            AuditQuery.builder().code(Sets.newHashSet(attack)).build()).isEmpty());
    assertEquals(
        1,
        auditRepository.count(
            AuditQuery.builder().uid(Sets.newHashSet(uid)).build()));
  }

  @Test
  void testAuditBulkDeleteRejectsUnboundedQueriesAndPreservesRows() {
    String uid = CodeGenerator.generateUid();
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2026, 10, 20, 13, 14))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(CodeGenerator.generateUid())
            .data("{}")
            .build();
    auditRepository.save(audit);
    assertThrows(IllegalArgumentException.class, () -> auditRepository.delete(AuditQuery.builder().build()));
    assertThrows(
        IllegalArgumentException.class,
        () -> auditRepository.delete(AuditQuery.builder().uid(Sets.newHashSet(uid)).build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            auditRepository.delete(
                AuditQuery.builder()
                    .auditType(Sets.newHashSet(AuditType.CREATE))
                    .range(AuditQuery.range(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)))
                    .build()));
    assertThrows(
        IllegalArgumentException.class,
        () -> auditRepository.delete((AuditQuery) null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            auditRepository.delete(
                AuditQuery.builder()
                    .uid(Sets.newHashSet(uid))
                    .range(AuditQuery.range(LocalDateTime.of(2027, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)))
                    .build()));
    assertEquals(
        1, auditRepository.count(AuditQuery.builder().uid(Sets.newHashSet(uid)).build()));
  }

  @Test
  void testAuditBulkDeleteWithBoundedUidDoesNotDeleteOtherRows() {
    String targetUid = CodeGenerator.generateUid();
    String otherUid = CodeGenerator.generateUid();
    for (String uid : List.of(targetUid, otherUid)) {
      auditRepository.save(
          Audit.builder()
              .auditType(AuditType.CREATE)
              .auditScope(AuditScope.AGGREGATE)
              .createdAt(LocalDateTime.of(2026, 10, 20, 13, 14))
              .createdBy("test-user")
              .klass(DataElement.class.getName())
              .uid(uid)
              .code(CodeGenerator.generateUid())
              .data("{}")
              .build());
    }
    auditRepository.delete(
        AuditQuery.builder()
            .uid(Sets.newHashSet(targetUid))
            .range(AuditQuery.range(LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0)))
            .build());
    assertEquals(0, auditRepository.count(AuditQuery.builder().uid(Sets.newHashSet(targetUid)).build()));
    assertEquals(1, auditRepository.count(AuditQuery.builder().uid(Sets.newHashSet(otherUid)).build()));
  }

  @Test
  @Disabled
  void testAuditInsert200k() {
    List<Audit> audits = new ArrayList<>();
    IntStream.rangeClosed(1, 200_000)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(2000, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              audits.add(audit);
            });
    Timer timer = new Timer().start();
    audits.forEach(audit -> auditRepository.save(audit));
    System.err.println("Single Insert: " + timer.getTimeInS() + "s");
    assertEquals(200_000, auditRepository.count(AuditQuery.builder().build()));
  }

  @Test
  @Disabled
  void testAuditInsertBatch200k() {
    List<Audit> audits = new ArrayList<>();
    IntStream.rangeClosed(1, 200_000)
        .forEach(
            n -> {
              String uid = CodeGenerator.generateUid();
              String code = CodeGenerator.generateUid();
              Audit audit =
                  Audit.builder()
                      .auditType(AuditType.CREATE)
                      .auditScope(AuditScope.AGGREGATE)
                      .createdAt(LocalDateTime.of(2000, 1, 1, 0, 0))
                      .createdBy("test-user")
                      .klass(DataElement.class.getName())
                      .uid(uid)
                      .code(code)
                      .data("{}")
                      .build();
              audits.add(audit);
            });
    Timer timer = new Timer().start();
    auditRepository.save(audits);
    System.err.println("Batch Insert: " + timer.getTimeInS() + "s");
    assertEquals(200_000, auditRepository.count(AuditQuery.builder().build()));
  }

  @Test
  void testSaveAuditWithAttributes() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    String categoryComboUid = CodeGenerator.generateUid();
    AuditAttributes auditAttributes = new AuditAttributes();
    auditAttributes.put("categoryCombo", categoryComboUid);
    auditAttributes.put("valueType", "TEXT");
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 1, 1, 0, 0))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .attributes(auditAttributes)
            .build();
    auditRepository.save(audit);
    List<Audit> audits = auditRepository.query(AuditQuery.builder().build());
    assertEquals(1, audits.size());
    assertEquals(2, audits.get(0).getAttributes().size());
    assertEquals(categoryComboUid, audits.get(0).getAttributes().get("categoryCombo"));
    assertEquals("TEXT", audits.get(0).getAttributes().get("valueType"));
  }

  @Test
  void testGetAuditByAttributes() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    String categoryComboUid = CodeGenerator.generateUid();
    AuditAttributes auditAttributes = new AuditAttributes();
    auditAttributes.put("categoryCombo", categoryComboUid);
    auditAttributes.put("valueType", "TEXT");
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 1, 1, 0, 0))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .attributes(auditAttributes)
            .build();
    auditRepository.save(audit);
    List<Audit> audits =
        auditRepository.query(AuditQuery.builder().auditAttributes(auditAttributes).build());
    assertEquals(1, audits.size());
    assertEquals(2, audits.get(0).getAttributes().size());
    assertEquals(categoryComboUid, audits.get(0).getAttributes().get("categoryCombo"));
    assertEquals("TEXT", audits.get(0).getAttributes().get("valueType"));
  }
}
