# TOTP validation-owned secret buffer cleanup

Updated: **21 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

The TOTP helper decodes the Base32 secret to validate its length before the OTP library independently decodes it for verification. The helper-owned precheck array previously remained populated after the check, including rejected lengths.

The helper now overwrites that array in a `finally` block. Cleanup occurs before subsequent proof verification, so accepted and rejected proofs follow the same precheck lifecycle. Invalid decoded lengths also return through cleanup. If decoding never returns an array, there is no returned helper-owned buffer to clear. The secret string, caller-owned data and OTP library copy are not treated as this helper's property.

Implementation: [`8c36aaa0c5`](https://github.com/PardasHealth/dhis2-core/commit/8c36aaa0c58db153945a8d513567e169d9c5ab8f).

| Code or check | Evidence |
| --- | --- |
| Decoding, length check and finally cleanup | [TwoFactorAuthUtils.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java) |
| Accepted/rejected real proofs with 10/20-byte secrets; rejected decoded lengths 0/9/19/21 | [Eight lifecycle tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TotpValidationSecretBufferTest.java) |
| Exact tested sources, reports and command; existing MFA compatibility suites | [Execution record](../evidence/afta-totp-validation-buffer-2026-11-21.json) |

The tests retain the exact precheck array through scoped static decoder interception, while supplying an independent byte array to real OTP verification. They check overwrite and verification results, and require the independent library copy to remain unchanged. The pre-fix run recorded seven failures in eight lifecycle cases; the empty-array control passed. **44 final cases passed with no failures, errors or skips**, including policy, provisioning URI, UID mutation and email-challenge regressions. CI now selects and requires the lifecycle suite alongside these existing security checks; hosted CI success is not claimed.

This is best-effort cleanup, not proof of physical erasure. Immutable Base32 strings, OTP-library/provider/JIT/GC copies, dumps, caller state and stored/replicated/backed-up secrets remain outside this array's ownership. Complete protected-resource scope and platform/evaluator acceptance are still missing. Row 20 remains Gap.
