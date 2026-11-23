# Replay fingerprint-owned challenge buffer cleanup

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

The MFA replay fingerprint input contains the login context and secret-bearing challenge. Its UTF-8 byte array previously remained populated after SHA-256 hashing or a provider error.

The helper now retains and clears its owned UTF-8 array in `finally`. Provider selection still precedes encoding. SHA-256, UTF-8 encoding, lowercase hexadecimal output, challenge construction and reservation keys remain unchanged. Digest failures propagate as before, including the existing missing-algorithm exception boundary. Returned digest output is retained; only the helper-owned plaintext array is cleared.

Implementation: [`bfbb87408c`](https://github.com/PardasHealth/dhis2-core/commit/bfbb87408c6c92835cd90654815f511a9d7e8615).

| Code or check | Evidence |
| --- | --- |
| Fingerprint and finally cleanup | [TwoFactorReplayService.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java) |
| Retained input bytes on digest success/failure, preserved digest output, three standard UTF-8/SHA-256 vectors and missing-provider behavior | [Six fingerprint tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayFingerprintBufferTest.java) |
| Real PostgreSQL reservation/replay compatibility, exact sources/reports and commands | [Execution record](../evidence/afta-replay-fingerprint-buffer-2026-11-23.json) |

Two of six pre-fix fingerprint checks failed; vector/provider controls passed. **59 unit cases and 13 PostgreSQL replay cases passed (72 total), with no failures, errors or skips.** The tests retain the exact helper-owned array through digest interception, assert it originally contains the expected plaintext and is cleared afterward, and check real digest vectors. Existing MFA/QR/identity regressions and PostgreSQL replay controls retain compatibility coverage. CI now selects and requires the fingerprint suite alongside existing replay tests; hosted CI success is not claimed.

The isolated PostgreSQL fixture now loads the real upstream audit-table definition before both fork repeatable migrations. Its previous setup omitted that prerequisite for the audit-integrity migration. It requires two fork migrations on the first run and zero on repetition.

This is best-effort cleanup of one array. Immutable concatenated challenge/secret strings, provider/runtime copies, dumps, physical memory erasure and complete storage/replica/backup scope remain unproven. Full AFTA protected-resource and evaluator acceptance remain open. Row 20 stays Gap.
