# TOTP provisioning QR failure diagnostics

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 46 (`FPT_FLS.1.1`). Status remains Partial evidence.**

The QR generator processes a provisioning URI containing the TOTP secret. Its checked encoder/I/O failure handler previously attached the complete exception to WARN logs. The tests inject synthetic URI-bearing exceptions to demonstrate the boundary; no live production leak is claimed.

The handler now emits only:

```text
TOTP_QR_FAILURE errorCode=E3026 action=RETURN_ERROR
```

It does not forward the exception object, message or cause to that log call. The existing error callback still receives `E3026`, and the caller still receives an empty byte array. Successful generation is unchanged. No new enrollment, retry or recovery policy is introduced.

Implementation: [`b96f3a2d52`](https://github.com/PardasHealth/dhis2-core/commit/b96f3a2d52c4f720f06796ae380fce2ab3485181).

| Code or check | Evidence |
| --- | --- |
| Checked QR encoding/I/O error handler | [TwoFactorAuthUtils.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java) |
| WriterException and IOException retain E3026/empty result; WARN has fixed content, no attached exception/URI/secret; real generated PNG decodes to original URI | [Three QR tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TotpQrFailureDiagnosticsTest.java) |
| Exact source/report hashes, command and MFA compatibility suites | [Execution record](../evidence/afta-totp-qr-failure-2026-11-23.json) |

Two pre-fix diagnostic checks failed; the real successful QR control passed. **53 final cases passed with no failures, errors or skips**, including the prior validation-diagnostic, buffer-cleanup, policy, enrollment, provisioning, identity and email-challenge checks. Logger configuration is isolated and restored per test. CI now selects and requires the QR suite; hosted CI success is not claimed.

This only covers the existing checked writer/I/O handler. It does not establish handling for every unchecked input/library fault, prove every application log is secret-free, or define the complete selected secure-state/fault model. Deployed log protection/transport/retention, external alert delivery and full AFTA acceptance remain open. Row 46 stays Partial evidence.
