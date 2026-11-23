# Audit payload codec-owned buffer cleanup

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

Audit compression previously retained its temporary UTF-8 payload and current compressed stream allocation. Decompression converted a returned byte array to a string without clearing that owned array.

Compression now completes/closes gzip before returning the copied compressed output, then clears its UTF-8 array and current output backing allocation in finally. Failure paths also run cleanup. Decompression clears its owned decoded array after string conversion. Caller-owned compressed input, returned compressed output and returned strings retain their values. Empty/null legacy behavior, UTF-8 replacement behavior and existing error propagation remain unchanged.

Implementation: [`fdf52396c2`](https://github.com/PardasHealth/dhis2-core/commit/fdf52396c23f065c8ef542c28799415cc54701e1).

| Code or check | Evidence |
| --- | --- |
| Compression/decompression and owned output cleanup | [JdbcAuditRepository.java](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java) |
| Retained UTF-8/output arrays on success, checked/runtime compression failure; Unicode/malformed/empty decoded arrays; read-error and caller ownership controls | [Seven lifecycle tests](../../../dhis-2/dhis-support/dhis-support-audit/src/test/java/org/hisp/dhis/audit/JdbcAuditPayloadBufferTest.java) |
| Real gzip, integrity-tag storage and broker/database recovery compatibility; exact hashes and commands | [Execution record](../evidence/afta-audit-codec-buffers-2026-11-23.json) |

Five of seven pre-fix cleanup checks failed; empty decoded data and read-error controls passed. The baseline includes the new test-only Mockito dependency with unchanged old production helpers. **40 unit cases and 25 PostgreSQL/JMS cases passed (65 total), with no failures, errors or skips.** Lifecycle tests retain the actual arrays handed to gzip/returned by the byte reader and the current output stream; separate real codec and storage tests verify format and recovery compatibility. CI now selects and requires the lifecycle suite. Mockito is a test dependency; hosted CI success is not claimed.

This is best-effort cleanup. Earlier allocations replaced during output growth, codec/library/provider copies, immutable strings, caller-retained records, dumps, JIT/GC effects and physical erasure remain unproven. Full protected-resource scope, storage/replica/backup handling and AFTA acceptance remain open. Row 20 stays Gap.
