# TOTP failure diagnostics without secret-bearing exceptions

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 46 (`FPT_FLS.1.1`). Status remains Partial evidence.**

TOTP validation previously attached the full decoder or verifier exception to WARN logs. Those exception objects can carry sensitive diagnostic text. The tests deliberately inject synthetic secret/proof values to demonstrate this boundary; no live production leak is claimed.

Both failure paths still return false. They now emit only fixed WARN messages:

```text
TOTP_VALIDATION_FAILURE reason=DECODE_ERROR action=REJECT_PROOF
TOTP_VALIDATION_FAILURE reason=VERIFY_ERROR action=REJECT_PROOF
```

No exception object or its message/cause is passed to those log calls. Other outcomes, including the existing invalid-length warning, remain unchanged. The separate authentication failure/event paths are not replaced by these diagnostics.

Implementation: [`635df509a8`](https://github.com/PardasHealth/dhis2-core/commit/635df509a80c9615d341828dd3c13a5f0bfbd949).

| Code or check | Evidence |
| --- | --- |
| Decode/verification failure handling | [TwoFactorAuthUtils.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java) |
| Captured WARN events contain only fixed reason/action, no attached exception and no synthetic secret/proof; method rejects in both cases | [Two failure-diagnostic tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TotpFailureDiagnosticsTest.java) |
| Exact tested sources/reports, commands and MFA compatibility | [Execution record](../evidence/afta-totp-failure-diagnostics-2026-11-23.json) |

Both pre-fix logging checks failed because their exceptions were retained; proof rejection was already correct. **50 final cases passed with no failures, errors or skips**, including normal proof, buffer cleanup, policy, enrollment, provisioning, identity and email-challenge regressions. Logger capture is isolated and restored after each test. CI now selects and requires the diagnostic suite; hosted CI success is not claimed.

This is a bounded example of preserving rejection and limiting diagnostic disclosure on failure. It does not prove every component's logs are secret-free or define the complete selected fault classes/secure states. Deployment log protection, transport/retention, external alert delivery and full evaluator acceptance remain open. Row 46 is not fully closed.
