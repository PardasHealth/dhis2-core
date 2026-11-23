# TOTP enrollment-owned raw seed cleanup

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

TOTP enrollment previously passed a newly generated 20-byte random seed directly to Base32 encoding and retained no reference with which to clear it. Those raw bytes remained populated after successful enrollment and after encoder/persistence failures.

Enrollment now owns the raw seed in a local variable and clears it in a `finally` block around encoding. Cleanup happens before changing the user or calling persistence. The encoded secret remains available to the user/enrollment flow as required. Encoding failure still propagates without changing the user; persistence failure still propagates after the existing user mutation. This does not add rollback or a new enrollment policy. The random generation call remains 20 bytes, and disabled enrollment still rejects before allocation.

Implementation: [`46af98846e`](https://github.com/PardasHealth/dhis2-core/commit/46af98846e7b30daf77de51e85baaa8f4f3bbae1).

| Code or check | Evidence |
| --- | --- |
| Enrollment encoding and finally cleanup | [TwoFactorAuthService.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java) |
| Success preserves Base32 output; encoder failure leaves user untouched; persistence failure propagates; disabled mode does not allocate | [Four lifecycle tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TotpEnrollmentSecretBufferTest.java) |
| Exact tested sources, reports and command; existing MFA compatibility suites | [Execution record](../evidence/afta-totp-enrollment-buffer-2026-11-23.json) |

The tests retain the exact raw array through scoped random-generator interception and check overwrite after method completion/failure. The pre-fix run recorded three failures in four enrollment cases; disabled-mode control passed. **48 final cases passed with no failures, errors or skips**, including TOTP validation cleanup, policy, provisioning URI, identity mutation and email challenge regressions. CI now selects and requires the enrollment suite; hosted CI success is not claimed.

This is best-effort cleanup of one owned array. The intended immutable Base32 secret, random/encoder/provider/JIT/GC copies, dumps, persisted/replicated/backed-up values and physical JVM erasure remain unproven. Complete protected-resource scope and platform/evaluator acceptance are still required. Row 20 stays Gap.
