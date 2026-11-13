# AFTA remediation implementation progress

Updated: **13 November 2026**. Scope: the [user-selected AFTA checklist](afta-selected-requirements-review.md).
This records implemented changes separately from the remaining requirements.
It is not AFTA/Common Criteria approval or a statement that all listed gaps are closed.

## Latest follow-up: token IP constraints and raw forwarding headers

Rows 16, 17 and 19 have a concrete access-control fix: token IP rules now use
the servlet remote address rather than trusting raw `X-Forwarded-For`. Two
baseline assertions reproduce the header-trust defect; all 37 targeted
filter/token/audit-review cases pass with the fix. Proxy deployments need trusted
server-side client-address resolution before accepting IP-constrained tokens.
See the [implementation, compatibility notes and execution evidence](afta-token-ip-constraints.md).
The selected rows remain partial.

## Earlier 13 November follow-up: audit-review paging and optional filters

Selected rows 4, 5 and 7 have two concrete controller fixes: requested paging
now reaches the audit query and response metadata; omitting the optional entity
filter no longer causes a null-pointer exception. Seven new cases reproduce six
failed assertions and one error on the preceding controller. With the fixes,
all 30 targeted audit-review and token cases pass. See the
[code/test trace, dated evidence and remaining acceptance](afta-audit-review-paging.md).
The rows remain partial; complete AFTA fit is not asserted.

## Earlier 12 November follow-up: stable PAT owner binding

Rows 37–39 now have a concrete identity-binding fix: a token's reloaded account
must have the same stable UID as its owner. Missing or mismatched identities
reject; a legitimate rename retains access. Three baseline denial assertions
failed before the fix; all 23 targeted manager/resolver cases pass after it.
See the [code, tests, execution evidence and remaining boundaries](afta-token-owner-binding.md).
The selected rows remain partial; full checklist percentages do not change.

## Latest follow-up: stable pending TOTP QR reads

The deprecated QR image route now reuses an existing nonempty pending TOTP seed,
so a refresh does not replace the seed already scanned by the user. The bundled
profile app, initial legacy flow and explicit POST rotation are retained. See the
[QR stability trace and remaining GET/CSRF limits](afta-pending-qr-stability.md).

## Earlier 11 November follow-up: explicit platform encryption key

The platform now enables a core startup guard requiring a nonblank, non-default
encryption password. Its existing secret is retained; no key rotation or
ciphertext migration occurs. See the [key guard and migration limits](afta-explicit-encryption-key.md).
The unknown AFTA crypto iterations remain unresolved; percentages do not change.

## Earlier 11 November follow-up: current account state during MFA login

The proof guard now rechecks the current stored password hash, disabled and
external-only flags, and account expiry under its primary-row lock. See the
[account-state trace, ordering and remaining limits](afta-account-state-freshness.md).
This prevents these stale snapshots from accepting an MFA proof; it does not
revoke existing sessions or cover every other authentication provider. This phase
records **114 passing authentication/database checks** plus successful
WAR packaging and PDF verification; counts overlap the historical inventories.

## Earlier 11 November follow-up: external broker TLS

The optional NATIVE audit-broker connection now supports server-authenticated TLS
with certificate and hostname verification. See the [implementation, configuration
and limits](afta-external-broker-tls.md). One bounded transfer path moves row 47
from gap to partial evidence; full internal transport protection remains open.

## Earlier 11 November follow-up: current factor state at consumption

The login guard now locks and rechecks the current primary user row before
accepting a proof. Replaced email challenges, changed secret/type snapshots and
deleted users are rejected. Database-read failure prevents acceptance. See the
[factor-state freshness trace and limits](afta-factor-state-freshness.md).
This does not revoke existing sessions or close broader account/provider freshness.
This phase records **102 passing targeted cases**; WAR packaging and
the PDF check pass. It overlaps the older inventories below.

## 9 November implementation: login replay protection

The next implementation adds durable single-use reservations for email challenges
and accepted authenticator time steps. Reuse and failed storage cannot return
successful MFA authentication. The new internal table is created through a
fork-specific, additive repeatable migration; no existing user secret is cleared.
See [login replay protection, upgrade notes and evidence](afta-login-replay-protection.md).

The first-phase counts below belong to commit `d62f105528`; the source files now
also contain the replay follow-up. The separate dated replay record carries the
9 November **94-case** passing inventory (including overlaps with the first phase). Neither phase closes the entire AFTA item.

## Implemented: prevent password-only downgrade of enrolled two-factor accounts

**Checklist:** source row 36, `FIA_UAU.5.2`; related broader gap G-15.
Implementation commit: `d62f10552816e93a19f8e4bb37e6f87a12bca44d`.

**Before:** after a correct password, the provider could accept an enrolled user
without validating the second factor if that factor type was disabled globally.
An inconsistent enrolled state could also skip the factor; a missing type raised
a null-pointer exception.

**After:** the same provider rejects login when an enrolled type is globally
disabled, unsupported or missing. Enabled factors still validate their codes.
Password-only users without an enrollment requirement retain normal login;
role-required enrollment, lockout, external-only account restrictions and
non-form restrictions remain in place. The existing generic failure response is
used; no parallel authentication provider, database migration or dependency change
is introduced. This controls new password-based authentication; it does not
automatically revoke existing authenticated sessions when global factor settings change.

**Operational impact:** before disabling a factor used by enrolled users, make
sure that authorized recovery is available. Re-enable the factor or use the
existing authorized account-recovery/enrollment-management path; disabling its
global flag no longer makes an enrolled account password-only. Do not reset user
secrets directly in the database as an upgrade workaround. Development CSRF-off
mode is unrelated and is not evidence for production browser acceptance.

### Code and regression traces

| Artifact | What it establishes |
| --- | --- |
| [TwoFactorAuthenticationProvider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java), `checkTwoFactorAuthentication` and `isTwoFactorTypeEnabled` | Reject disabled/unsupported/missing enrolled factor types before accepting login; validate enabled factors. |
| [TwoFactorAuthenticationPolicyTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java) | 19 cases exercise real provider/code-validation behavior with mocked external services/configuration: disabled email/TOTP with missing/submitted code, enabled valid codes, wrong/missing codes, required enrollment, ordinary password login, lockout, external-only and non-form restrictions, malformed enrolled states. |
| [TwoFactorLoginPolicyTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorLoginPolicyTest.java) | Six H2/Spring-security API cases assert disabled email/TOTP failure status and absent redirect, including submitted valid codes, plus successful enabled-factor logins. HTTP 200 alone is not treated as authentication success. |
| [AuthenticationControllerTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java), `testLoginEmail2FA` | Its success fixture now explicitly enables the factor and restores the prior configuration. The original fixture relied on globally disabled email MFA being accepted. The expected successful login is retained. |
| [Fork regression workflow](../../../.github/workflows/cc-audit-regression.yml) | Runs the new classes with existing authentication tests and requires their reports to be present and successful. CI definition is not a claim that a hosted run has completed. |

### Verification and limits

- On the original provider, the new 19-case policy suite produced **eight failures**: seven expected-denial checks accepted login and one missing-type check raised the wrong exception. The other 11 cases passed. An earlier fixture setup error was corrected before recording this baseline comparison.
- On the patched provider, **19 policy cases pass, with no failures, errors or skips**.
- H2 web regression: **49 pass, with no failures, errors or skips**, across `AuthenticationControllerTest` (16), `TwoFactorLoginPolicyTest` (6), `TwoFactorControllerTest` (18) and `UserTwoFactorAuditControllerTest` (9).
- These are **68 targeted cases**, not 68 newly closed requirements. They are reported separately from the earlier 262-pass inventory because some classes overlap that inventory.
- Tests use checksum-verified Temurin 17.0.17+10 and Maven 3.9.11 with the existing local dependency cache. The H2 run required local broker sockets outside the command sandbox; the initial sandbox attempt could not allocate a port. The first unrestricted web run identified the email-success fixture issue, which was corrected before the passing run.
- The local cached `sms-compression` artifact retains the previously documented source-build provenance caveat. No new dependency or upstream app selection is introduced.
- Embedded WAR packaging succeeded with existing bundled browser assets (`-Dskip.bundle.apps=true`), without downloading new app versions. The packaged provider matches the compiled patched class, and the PDF class check passes with the verified OpenPDF artifact. This is not a cold release build or live deployment acceptance.
- A new real browser/MFA recovery session, cross-node deployment, code replay/race test and independent evaluation are not established by these checks. See the dated [machine-readable follow-up](../evidence/afta-remediation-2026-11-09.json) for source hashes, build outcome and commands.

Example commands from `dhis-2` (use the project's normal configured repositories):

```bash
mvn -B -ntp -pl dhis-services/dhis-service-core -am -Punit-test \
  -Dtest=TwoFactorAuthenticationPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -ntp -pl dhis-test-web-api -am -Pintegration-h2-test \
  -Dtest=TwoFactorLoginPolicyTest,AuthenticationControllerTest,TwoFactorControllerTest,UserTwoFactorAuditControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Still open

| Work | Why it is not closed by this patch |
| --- | --- |
| Full `FIA_UAU.5.2` fit | The complete selected AFTA mechanism-combination rules, provider/SSO paths, recovery, browser and multi-node evidence are not finalized. The row remains **Partial evidence**. |
| Wider single-use policy, G-14 | Login reservation is now implemented. Enrollment/disable/other purposes, broader account/session freshness, trusted clocks, restore and full deployed acceptance remain open. |
| Production CSRF-aware login | Local development can use the explicitly selected localhost-only CSRF-off mode. The production client integration gap remains open. |
| Audit protection, capacity and failure policy | Retention/grants, threshold/full-storage actions, persistence and recovery need the selected AFTA definitions and deployed evidence. |
| Integrity/residual protection, reliable time, fault tolerance and access history | The selected guarantees still need implementation/environment designs and acceptance evidence as described per checklist row. |
| Extensions, iterations and edition mismatches | Exact AFTA document/version and clause definitions remain missing. No behavior or algorithm is assigned to an unknown extension/crypto iteration by guesswork. |

The current checklist has **43/78 primary entries with partial evidence; 10 gaps;
25 needing definitions; none fully closed**. The external broker transport tests
add partial evidence for one previously untraced item. They do not close the whole
item. Earlier MFA fixes corrected subgaps within an already partial requirement.
