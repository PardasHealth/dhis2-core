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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.AuditIntegrityGuard.StoredRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository
public class JdbcAuditRepository implements AuditRepository {
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  private final SimpleJdbcInsert auditInsert;
  private final AuditIntegrityGuard integrityGuard;

  private ObjectMapper jsonMapper;

  public JdbcAuditRepository(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
    this(jdbcTemplate, jsonMapper, new AuditIntegrityGuard(false, null));
  }

  @Autowired
  public JdbcAuditRepository(
      JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper, AuditIntegrityGuard integrityGuard) {
    this.integrityGuard = integrityGuard;
    this.jdbcTemplate = jdbcTemplate;
    this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    this.jsonMapper = jsonMapper;

    String[] columns =
        integrityGuard.isEnabled()
            ? new String[] {
              "auditId",
              "auditType",
              "auditScope",
              "createdAt",
              "createdBy",
              "klass",
              "uid",
              "code",
              "data",
              "attributes",
              "integrityTag"
            }
            : new String[] {
              "auditType",
              "auditScope",
              "createdAt",
              "createdBy",
              "klass",
              "uid",
              "code",
              "data",
              "attributes"
            };
    this.auditInsert =
        new SimpleJdbcInsert(jdbcTemplate).withTableName("audit").usingColumns(columns);
    if (integrityGuard.isEnabled()) {
      if (!"UTF8".equals(jdbcTemplate.queryForObject("SHOW server_encoding", String.class))) {
        throw new IllegalStateException("Audit integrity requires PostgreSQL UTF8 storage");
      }
    } else {
      auditInsert.usingGeneratedKeyColumns("auditid");
    }
  }

  @Override
  public long save(Audit audit) {
    MapSqlParameterSource parameterSource = buildParameterSource(audit);
    if (integrityGuard.isEnabled()) {
      auditInsert.execute(parameterSource);
      return ((Number) parameterSource.getValue("auditId")).longValue();
    }
    return auditInsert.executeAndReturnKey(parameterSource).longValue();
  }

  @Override
  public void save(List<Audit> audits) {
    List<MapSqlParameterSource> parameterSources = new ArrayList<>();
    audits.forEach(audit -> parameterSources.add(buildParameterSource(audit)));
    auditInsert.executeBatch(parameterSources.toArray(new MapSqlParameterSource[0]));
  }

  @Override
  public void delete(Audit audit) {
    jdbcTemplate.update("DELETE FROM audit WHERE auditId=?", audit.getId());
  }

  @Override
  public void delete(AuditQuery query) {
    if (query == null
        || query.getRange() == null
        || query.getRange().getFrom() == null
        || query.getRange().getTo() == null
        || !query.getRange().getFrom().isBefore(query.getRange().getTo())
        || (query.getUid().isEmpty() && query.getCode().isEmpty())) {
      throw new IllegalArgumentException(
          "Audit deletion requires a bounded time range and a UID or code filter");
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    namedJdbcTemplate.update("DELETE FROM audit" + buildQuery(query, params), params);
  }

  @Override
  public int count(AuditQuery query) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    String where = buildQuery(query, params);
    if (integrityGuard.isEnabled()) {
      // A count must not silently bypass integrity checks on its matching records.
      int[] count = {0};
      namedJdbcTemplate.query(
          "SELECT * FROM audit" + where,
          params,
          (RowCallbackHandler)
              rs -> {
                verifyStoredRecord(rs);
                count[0] = Math.incrementExact(count[0]);
              });
      return count[0];
    }
    return namedJdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM audit" + where, params, Integer.class);
  }

  @Override
  public List<Audit> query(AuditQuery query) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    return namedJdbcTemplate.query(
        "SELECT * FROM audit" + buildQuery(query, params), params, this::mapAuditRow);
  }

  private String buildQuery(AuditQuery query, MapSqlParameterSource params) {
    if (query == null) {
      throw new IllegalArgumentException("Audit query cannot be null");
    }
    List<String> conditions = new ArrayList<>();
    if (!query.getAuditType().isEmpty()) {
      conditions.add("auditType IN (:auditTypes)");
      params.addValue("auditTypes", query.getAuditType().stream().map(Enum::name).toList());
    }
    if (!query.getAuditScope().isEmpty()) {
      conditions.add("auditScope IN (:auditScopes)");
      params.addValue("auditScopes", query.getAuditScope().stream().map(Enum::name).toList());
    }
    if (!query.getKlass().isEmpty()) {
      conditions.add("klass IN (:klasses)");
      params.addValue("klasses", query.getKlass());
    }
    List<String> identityConditions = new ArrayList<>();
    if (!query.getUid().isEmpty()) {
      identityConditions.add("uid IN (:uids)");
      params.addValue("uids", query.getUid());
    }
    if (!query.getCode().isEmpty()) {
      identityConditions.add("code IN (:codes)");
      params.addValue("codes", query.getCode());
    }
    if (!identityConditions.isEmpty()) {
      conditions.add("(" + String.join(" OR ", identityConditions) + ")");
    }
    int index = 0;
    for (var entry : query.getAuditAttributes().entrySet()) {
      String keyParam = "attrKey" + index;
      String valueParam = "attrValue" + index;
      conditions.add("attributes ->> CAST(:" + keyParam + " AS text) = :" + valueParam);
      params.addValue(keyParam, entry.getKey());
      params.addValue(valueParam, String.valueOf(entry.getValue()));
      index++;
    }
    if (query.getRange() != null) {
      AuditQuery.Range range = query.getRange();
      if (range.getFrom() != null) {
        conditions.add("createdAt >= :fromDate");
        params.addValue("fromDate", range.getFrom());
      }
      if (range.getTo() != null) {
        conditions.add("createdAt < :toDate");
        params.addValue("toDate", range.getTo());
      }
    }
    return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
  }

  private MapSqlParameterSource buildParameterSource(Audit audit) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    LocalDateTime createdAt = audit.getCreatedAt();
    if (integrityGuard.isEnabled() && createdAt != null) {
      // PostgreSQL timestamp storage has microsecond precision. Sign exactly what is stored.
      createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
    }
    parameters.addValue(
        "auditType", audit.getAuditType() == null ? null : audit.getAuditType().name());
    parameters.addValue(
        "auditScope", audit.getAuditScope() == null ? null : audit.getAuditScope().name());
    parameters.addValue("createdAt", createdAt);
    parameters.addValue("createdBy", audit.getCreatedBy());
    parameters.addValue("klass", audit.getKlass());
    parameters.addValue("uid", audit.getUid());
    parameters.addValue("code", audit.getCode());
    parameters.addValue("data", compress(audit.getData()));

    try {
      String attributes = jsonMapper.writeValueAsString(audit.getAttributes());
      if (integrityGuard.isEnabled()) {
        // JSONB normalizes whitespace/key ordering and numeric notation. Authenticate its
        // actual stored representation without parsing numbers through floating point.
        attributes =
            jdbcTemplate.queryForObject("SELECT CAST(? AS jsonb)::text", String.class, attributes);
      }
      parameters.addValue("attributes", attributes);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to serialize audit attributes", e);
    }

    if (integrityGuard.isEnabled()) {
      Long id =
          jdbcTemplate.queryForObject(
              "SELECT nextval(pg_get_serial_sequence('audit', 'auditid'))", Long.class);
      if (id == null)
        throw new IllegalStateException("Audit integrity requires an audit ID sequence");
      parameters.addValue("auditId", id);
      // Build from the parameter snapshot, never re-read mutable fields from the caller.
      StoredRecord stored =
          new StoredRecord(
              id,
              (String) parameters.getValue("auditType"),
              (String) parameters.getValue("auditScope"),
              (LocalDateTime) parameters.getValue("createdAt"),
              (String) parameters.getValue("createdBy"),
              (String) parameters.getValue("klass"),
              (String) parameters.getValue("uid"),
              (String) parameters.getValue("code"),
              (String) parameters.getValue("attributes"),
              (byte[]) parameters.getValue("data"));
      parameters.addValue("integrityTag", integrityGuard.sign(stored));
    }
    return parameters;
  }

  private StoredRecord verifyStoredRecord(ResultSet rs) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("createdAt");
    StoredRecord stored =
        new StoredRecord(
            rs.getLong("auditId"),
            rs.getString("auditType"),
            rs.getString("auditScope"),
            createdAt == null ? null : createdAt.toLocalDateTime(),
            rs.getString("createdBy"),
            rs.getString("klass"),
            rs.getString("uid"),
            rs.getString("code"),
            rs.getString("attributes"),
            rs.getBytes("data"));
    if (integrityGuard.isEnabled()) integrityGuard.verify(stored, rs.getString("integrityTag"));
    return stored;
  }

  private Audit mapAuditRow(ResultSet rs, int rowNum) throws SQLException {
    // Verify the stored bytes before enum conversion, decompression or payload deserialization.
    StoredRecord stored = verifyStoredRecord(rs);
    Audit.AuditBuilder auditBuilder =
        Audit.builder()
            .id(stored.id())
            .auditType(AuditType.valueOf(stored.auditType()))
            .auditScope(AuditScope.valueOf(stored.auditScope()))
            .createdAt(stored.createdAt())
            .createdBy(stored.createdBy())
            .klass(stored.klass())
            .uid(stored.uid())
            .code(stored.code())
            .data(decompress(stored.data()));
    try {
      auditBuilder.attributes(jsonMapper.readValue(stored.attributes(), AuditAttributes.class));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to deserialize audit attributes", e);
    }
    return auditBuilder.build();
  }

  private static byte[] compress(String data) {
    if (StringUtils.isEmpty(data)) {
      return new byte[] {};
    }

    byte[] encoded = data.getBytes(StandardCharsets.UTF_8);
    ClearingAuditOutputStream output = null;
    try {
      output = new ClearingAuditOutputStream(data.length());
      try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
        gzip.write(encoded);
        gzip.finish();
      }
      return output.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to compress audit data", e);
    } finally {
      Arrays.fill(encoded, (byte) 0);
      if (output != null) output.clearOwnedBuffer();
    }
  }

  private static final class ClearingAuditOutputStream extends ByteArrayOutputStream {
    private ClearingAuditOutputStream(int size) {
      super(size);
    }

    private void clearOwnedBuffer() {
      Arrays.fill(buf, (byte) 0);
    }
  }

  private static String decompress(byte[] data) {
    if (data == null || data.length == 0) {
      return "{}";
    }

    try (ByteArrayInputStream input = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(input)) {
      byte[] decoded = IOUtils.toByteArray(gzip);
      try {
        return new String(decoded, StandardCharsets.UTF_8);
      } finally {
        Arrays.fill(decoded, (byte) 0);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to decompress audit data", e);
    }
  }
}
