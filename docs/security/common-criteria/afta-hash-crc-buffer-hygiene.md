# Helper-owned UTF-8 hashing and CRC buffer cleanup

Date applied: **16 November 2026**. Branch: `work`.
Implementation: [469ad5a9d0](https://github.com/PardasHealth/dhis2-core/commit/469ad5a9d024e9a6f7cd6002b51101274e7cb30f).

## Selected source rows

| Selected source row | Requirement ID | Bounded change | Overall status |
| --- | --- | --- | --- |
| 20 | `FDP_RIP.2.1` | Clear helper-owned UTF-8 storage after SHA-256/SHA-512 and CRC processing, while retaining caller-owned inputs. | **Gap / evidence not established** for complete residual-information protection. |

## Implementation and compatibility

The resolver and private token validators already clear their own character
buffers; [resolver cleanup](afta-token-secret-buffer.md) and
[validator cleanup](afta-token-checksum-buffers.md) record those earlier changes.
Their hashing/CRC helpers still left encoded plaintext byte storage intact.

`HashUtils` now clears its string-input encoding array after digest processing.
Character-input hashing passes the encoder's remaining `ByteBuffer` range directly
to the digest, avoids a second plaintext byte array, and clears the encoder's
heap backing array even after the buffer position has advanced. The public
byte-array overloads retain caller ownership and do not clear the supplied array.

`CRC32Utils.generateCRC32Checksum` clears its encoding array after the CRC result
or an exception. Its existing input byte range and checksum behavior are retained;
this cleanup does not change the token wire format or correct/change its CRC
algorithm. Caller-owned character arrays remain unchanged.

- [HashUtils](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/HashUtils.java): SHA-256/SHA-512 string/character overloads, `hashOwnedBytes`, `hashChars`.
- [CRC32Utils](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CRC32Utils.java): encoded-array lifetime in `generateCRC32Checksum`.
- [HashSecretBufferTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/common/HashSecretBufferTest.java): eight owned-buffer success/failure cases; four caller-byte-array ownership cases; eight real UTF-8 digest vectors covering empty, ASCII, multilingual and supplementary-character input across both algorithms and all three input overloads. The vectors compare with JDK `MessageDigest` results.
- [CrcSecretBufferTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/common/CrcSecretBufferTest.java): constructed CRC mocks expose the helper-owned array on success/error and require unchanged caller input. Existing real CRC/generator tests retain compatibility checks.
- [Dated execution evidence](../evidence/afta-hash-crc-buffer-hygiene-2026-11-16.json): implementation commit, selected source row, baseline, commands and source/report hashes.
- [Fork workflow](../../../.github/workflows/cc-audit-regression.yml): explicitly selects both new suites and requires successful nonempty reports.

The baseline runs 22 new cases against the preceding helpers: all ten owned-buffer
lifetime cases fail; the twelve ownership and digest-vector cases pass. New tests
use synthetic input and controlled hash/CRC mocks to observe array contents, with
separate real algorithm compatibility checks.

## Remaining gap

This is best-effort heap-buffer hygiene, not secure physical erasure. Immutable
source strings, caller-owned token outputs, digest/provider/runtime copies,
JIT behavior, heap dumps, swap and the complete file/media/replica/backup boundary
remain outside this proof. The full protected-resource inventory and deployed
allocation/deallocation acceptance are still undefined. Row 20 stays Gap.

The cleanup does not assign SHA algorithms to unspecified AFTA crypto iterations
or establish provider/algorithm approval. It adds one implementation commit to an
already linked row; the distinct primary-entry count remains unchanged.

Final verification: **96 Java cases passed** (83 unit and 13 H2 API), with
zero failures, errors or skips. Ten baseline lifetime failures and twelve passing
controls remain separately recorded. Progress now shows 27 linked implementation
commits across 21/78 primary entries; row 20 stays Gap. Counts overlap earlier records.
