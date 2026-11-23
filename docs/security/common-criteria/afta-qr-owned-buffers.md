# TOTP QR-owned buffer cleanup

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

QR generation handles a provisioning URI containing the TOTP secret. Its temporary UTF-8 input, current PNG stream buffer and bit matrix previously remained populated after processing or failure.

The helper now clears the temporary UTF-8 bytes immediately after constructing the normalized string. It preserves the existing UTF-8 round-trip normalization. The current PNG backing array is cleared after copying the returned image or when writing fails, and the owned bit matrix is cleared in an outer finally block. The stream's close behavior is unchanged; cleanup is explicit at the helper's ownership boundary, after the caller-returned copy exists. Checked writer/I/O errors retain the empty image/E3026 response and fixed diagnostics; unchecked writer errors still propagate unchanged after cleanup.

Implementation: [`b5313faa57`](https://github.com/PardasHealth/dhis2-core/commit/b5313faa57d714e6721404f3f59d926a5928ca88).

| Code or check | Evidence |
| --- | --- |
| UTF-8 normalization, current PNG allocation and matrix cleanup | [TwoFactorAuthUtils.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java) |
| Three retained-stream paths, two retained-matrix paths and two normalization controls | [Seven lifecycle tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TotpQrOwnedBufferTest.java) |
| Exact source/report hashes, command and existing QR/MFA compatibility suites | [Execution record](../evidence/afta-qr-owned-buffers-2026-11-23.json) |

Five of seven pre-fix checks failed: three output-buffer cases and two matrix-retention cases. Unicode and malformed-UTF-16 normalization controls passed. **66 final cases passed, with no failures, errors or skips**, including real PNG generation/decoding, existing E3026 diagnostics, proof policy, enrollment, replay fingerprint, identity and email-challenge regressions. The lifecycle tests retain the stream/matrix references and inspect them after completion/failure; UTF-8 array cleanup is source-inspected, with its normalization contract covered by tests. CI now selects and requires the lifecycle suite; hosted CI success is not claimed.

The returned PNG intentionally remains available to its caller. The current stream-buffer cleanup does not prove erasure of earlier allocations replaced during growth. Immutable URI/normalized strings, library/ImageIO/provider copies, JIT/GC effects, dumps, physical JVM erasure and complete storage/replica/backup scope remain unproven. Full protected-resource and evaluator acceptance are still required. Row 20 stays Gap.
