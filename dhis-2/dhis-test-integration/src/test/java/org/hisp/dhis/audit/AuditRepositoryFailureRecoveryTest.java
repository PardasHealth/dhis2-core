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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Savepoint;
import java.time.LocalDateTime;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Fork regression for rejected PostgreSQL audit writes and recovery. */
@Transactional
class AuditRepositoryFailureRecoveryTest extends PostgresIntegrationTestBase {
  @Autowired private AuditRepository auditRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testRejectedWritePreservesEarlierRecordsAndRecovers() {
    Audit earlier =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.METADATA)
            .createdAt(LocalDateTime.now())
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(CodeGenerator.generateUid())
            .data("{}")
            .build();
    long earlierId = auditRepository.save(earlier);
    Audit rejected =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.METADATA)
            .createdAt(LocalDateTime.now())
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid("ccReject001")
            .data("{}")
            .build();
    jdbcTemplate.execute(
        "ALTER TABLE audit ADD CONSTRAINT cc_reject_audit_write CHECK (uid <> 'ccReject001')");

    jdbcTemplate.execute(
        (ConnectionCallback<Void>)
            connection -> {
              Savepoint savepoint = connection.setSavepoint();
              try {
                assertThrows(DataAccessException.class, () -> auditRepository.save(rejected));
              } finally {
                // PostgreSQL aborts a transaction after a rejected insert. Restore
                // this test's transaction while retaining the earlier record.
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
              }
              return null;
            });
    List<Audit> retained = auditRepository.query(AuditQuery.builder().build());
    assertEquals(1, retained.size());
    assertEquals(earlierId, retained.get(0).getId());

    jdbcTemplate.execute("ALTER TABLE audit DROP CONSTRAINT cc_reject_audit_write");
    long recoveredId = auditRepository.save(rejected);
    assertNotEquals(earlierId, recoveredId);
    assertEquals(2, auditRepository.count(AuditQuery.builder().build()));
  }
}
