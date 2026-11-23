# Generated secret character ownership

Updated: **23 November 2026**. Branch: `work`.

**Selected source row: 20 (`FDP_RIP.2.1`). Status remains Gap.**

Email challenge generation and the shared secure-token string wrapper previously constructed immutable strings from new random character arrays without clearing the arrays they owned.

Each wrapper now clears its array in finally immediately after string construction. Email generation retains the six-digit code, encoded `code|expiry` value and existing expiry interval. The secure-token string wrapper reuses the existing public secure character generator at the same configured length; that generator still uses the same SecureRandom holder and alphanumeric construction. The public character-array API retains caller ownership and usable output. Generation errors still propagate before any returned buffer is available.

Implementation: [`e98b283c8b`](https://github.com/PardasHealth/dhis2-core/commit/e98b283c8be740d62e39bad3b87152b3532da930).

| Code or check | Evidence |
| --- | --- |
| Shared secure string wrapper and unchanged character generator | [CodeGenerator.java](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CodeGenerator.java) |
| Email code construction and cleanup | [TwoFactorAuthService.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java) |
| Secure-wrapper handoff, retained string, failure and usable public character output | [Three API lifecycle tests](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/common/CodeGeneratorSecretBufferTest.java) |
| Email retained string/TTL, cleared owned array, RNG error and real format | [Three email lifecycle tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/EmailChallengeSecretBufferTest.java) |
| Exact sources/reports, existing generator and MFA compatibility | [Execution record](../evidence/afta-generated-secret-characters-2026-11-23.json) |

The email-only baseline recorded one failure in three cases: its owned digits stayed populated. The format/TTL and generator-failure controls passed. Secure-token tests exercise the new internal handoff to the existing public generator; no pre-fix secure-token runtime observation is claimed. **76 final cases passed (7 API and 69 core), with no failures, errors or skips.** This includes existing token-format, QR, buffer, failure-diagnostic, proof policy, enrollment, identity and email-delivery checks. CI selects and requires both new suites; hosted CI success is not claimed.

This is best-effort cleanup of wrapper-owned arrays. Returned immutable codes/tokens, encoded challenge strings, caller-owned character arrays, RNG/provider/JIT/GC copies, dumps and physical JVM erasure remain outside this cleanup proof. Complete protected-resource and memory/media/replica/backup acceptance remain open. Row 20 stays Gap.
