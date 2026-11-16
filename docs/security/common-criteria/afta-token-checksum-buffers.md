# Validator-owned API-token checksum buffer cleanup

Date applied: **16 November 2026**. Branch: `work`.
Implementation: [f9ab1c6c86](https://github.com/PardasHealth/dhis2-core/commit/f9ab1c6c86eb6cb22f866a479e42bb37b347e1bf).

## Selected source rows

| Selected source row | Requirement ID | Bounded change | Overall status |
| --- | --- | --- | --- |
| 20 | `FDP_RIP.2.1` | Clear extracted token-code and checksum arrays owned by private checksum validation, for both supported token versions. | **Gap / evidence not established** for complete residual-information protection. |

## Implementation and evidence

The [preceding resolver cleanup](afta-token-secret-buffer.md) clears the resolver's
raw-token array. `ApiKeyTokenGenerator` still creates separate code/checksum arrays
when validating that token. Its private decimal CRC32 and base62 CRC32 validators
now clear those successfully extracted arrays in `finally` blocks, after valid,
rejected or exceptional validation outcomes.

The caller's input array is not cleared or modified. Public
`extractCodeAndChecksum` retains its caller-owned result contract; token generation,
hashing, checksum formats and exception propagation are unchanged.

- [ApiKeyTokenGenerator](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java): `validateCrc32Checksum`, `validateCrc32B62Checksum`, private `clearChecksumBuffers`.
- [ApiTokenChecksumBufferTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiTokenChecksumBufferTest.java): six cases cover both token versions with valid, rejected and throwing CRC outcomes. The real generator extracts its arrays; static CRC mocks capture those arrays. Assertions require cleared extracted contents and unchanged caller input. Only synthetic input is used.
- [ApiKeyTokenGeneratorTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java) and [CRC32UtilsTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/common/CRC32UtilsTest.java): existing generation/checksum compatibility checks remain selected, along with resolver/filter and token API regressions.
- [Dated execution evidence](../evidence/afta-token-checksum-buffers-2026-11-16.json): implementation commit, selected source row, baseline, commands and source/report hashes.
- [Fork workflow](../../../.github/workflows/cc-audit-regression.yml): the new suite is explicitly selected and required to produce a passing nonempty report.

All six new cases fail against the preceding implementation because the extracted
code arrays retain their contents. These tests observe Java array contents at the
method boundary, not physical JVM erasure.

## Remaining gap

This is best-effort cleanup of two validator-owned buffers, not complete residual
protection. Generator-internal byte arrays, immutable strings, caller-owned token
outputs, runtime copies and the wider memory/storage/replica/backup resource
boundary remain outside this proof. JVM/JIT physical-erasure guarantees are not
established. The protected-resource inventory and deployed allocation/deallocation
acceptance still need agreement. Row 20 therefore remains Gap.

This follow-up adds another implementation commit to an already linked source
row. The commit count rises; the number of primary entries with linked work does
not rise again for the same row. No closure or compliance percentage is claimed.

Final verification: **68 Java cases passed** (55 unit and 13 H2 API), with
zero failures, errors or skips. Six baseline failures remain separately recorded.
The charts now record 26 linked implementation commits across 21/78 primary
entries; row 20 remains Gap. Execution counts overlap earlier records.
