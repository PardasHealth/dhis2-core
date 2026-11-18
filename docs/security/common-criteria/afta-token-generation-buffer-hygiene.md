# Token-generation buffer ownership and failure cleanup

Date applied: **18 November 2026**. Branch: `work`.
Implementation: [2493199a48](https://github.com/PardasHealth/dhis2-core/commit/2493199a48ff5e85a5688177eccc33c8643edbd9).

## Selected source rows

| Selected source row | Requirement ID | Bounded change | Overall status |
| --- | --- | --- | --- |
| 20 | `FDP_RIP.2.1` | Clear generation-owned random-code/checksum components and plaintext not transferred to a successful token wrapper. | **Gap / evidence not established** for complete residual-information protection. |

## Implementation and compatibility

Earlier row 20 follow-ups cover [resolver buffers](afta-token-secret-buffer.md),
[validator copies](afta-token-checksum-buffers.md) and
[hash/CRC encoding storage](afta-hash-crc-buffer-hygiene.md). Token generation
also owns temporary component arrays. The preceding implementation retained those
arrays after assembly or failure, and retained the assembled plaintext if wrapper
creation failed during hashing/building.

`ApiKeyTokenGenerator.generatePatToken` now clears its random-code and checksum
arrays in `finally` after copying them into the independent output, or after a
length/checksum-generation failure. `generatePersonalAccessToken` clears its
assembled plaintext if ownership has not transferred to a completed wrapper.
Successful wrappers deliberately retain the returned plaintext for the authorized
caller. Formats, hash/checksum choices, expiry metadata and public signatures stay
the same. This does not introduce cleanup of caller-owned returned outputs.

- [ApiKeyTokenGenerator](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java): `generatePatToken`, `generatePersonalAccessToken`, component cleanup and wrapper ownership transfer.
- [ApiTokenGenerationBufferTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiTokenGenerationBufferTest.java): six component-lifetime cases for both token versions; two failed-wrapper cases for a runtime exception and an error; one successful-wrapper ownership control; two real generation/checksum/digest compatibility controls. Static random/generator mocks expose owned arrays in lifecycle cases. Real controls use synthetic generated credentials without real accounts.
- [ApiTokenControllerTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/ApiTokenControllerTest.java): existing H2 create/read/update/delete cases verify the returned-token API behavior, alongside token authentication/state regressions.
- [Dated execution evidence](../evidence/afta-token-generation-buffer-hygiene-2026-11-18.json): implementation commit, selected source row, exact source/report hashes, baseline and reproduction commands.
- [Fork workflow](../../../.github/workflows/cc-audit-regression.yml): explicitly selects the new lifecycle suite and the existing token-controller suite, and requires successful nonempty reports.

The baseline runs eleven new cases against the preceding generator: eight
cleanup cases fail; the three successful-ownership/real-compatibility controls
pass. The returned-token controls guard against accidentally erasing usable
output while cleaning temporary components.

## Remaining gap

These are observable Java-array lifecycle checks and best-effort cleanup, not
physical erasure guarantees. Immutable strings, random/provider/runtime copies,
caller-owned returned token outputs, JVM/JIT behavior, heap dumps, swap and the
full file/media/replica/backup resource boundary remain outside this proof.
Process interruption and deployed residual-data acceptance are not established.
Row 20 stays Gap pending the complete resource inventory and allocation/
deallocation acceptance rules. This adds a commit to an already linked entry,
without increasing the distinct primary-entry count or declaring full closure.

Final verification: **121 Java cases passed** (94 unit and 27 H2 API), with
zero failures, errors or skips. Eight baseline cleanup failures and three passing
controls remain separately recorded. Progress shows 28 linked implementation
commits across 21/78 primary entries; row 20 stays Gap. Counts overlap earlier records.
