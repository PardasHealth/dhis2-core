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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractAuditConsumer implements AuditConsumer {
  protected AuditService auditService;

  protected ObjectMapper objectMapper;

  protected boolean isAuditLogEnabled;

  protected boolean isAuditDatabaseEnabled;

  protected void _consume(TextMessage message, AuditScope expectedScope) {
    try {
      org.hisp.dhis.artemis.audit.Audit auditMessage =
          objectMapper
              .readerFor(org.hisp.dhis.artemis.audit.Audit.class)
              .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
              .readValue(message.getText());

      // The listener destination, not an untrusted message claim, selects the scope.
      if (auditMessage == null
          || expectedScope == null
          || auditMessage.getAuditScope() != expectedScope
          || auditMessage.getAuditType() == null) {
        throw new IOException("Audit type is missing or scope does not match destination");
      }

      if (auditMessage.getData() != null && !(auditMessage.getData() instanceof String)) {
        auditMessage.setData(objectMapper.writeValueAsString(auditMessage.getData()));
      }

      org.hisp.dhis.audit.Audit audit = auditMessage.toAudit();

      if (isAuditLogEnabled) {
        log.info(objectMapper.writeValueAsString(audit));
      }

      if (isAuditDatabaseEnabled) {
        auditService.addAudit(audit);
      }
    } catch (IOException e) {
      // Parser diagnostics may contain audited values. Keep them out of logs and
      // the exception handed to JMS retry/dead-letter handling.
      log.error("AUDIT_MESSAGE_REJECTED reason=INVALID_PAYLOAD action=FAIL_DELIVERY");
      throw new IllegalArgumentException("Invalid audit message payload");
    } catch (Exception e) {
      log.error("An error occurred persisting an audit message", e);
      throw new IllegalStateException("Audit message processing failed", e);
    }
  }
}
