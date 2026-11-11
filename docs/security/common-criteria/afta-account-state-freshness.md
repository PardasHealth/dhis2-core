# AFTA remediation: current account state during MFA login

Date: **11 November 2026**. Selected checklist row 36, `FIA_UAU.5.2`;
related account/session-policy boundary: row 59, `FTA_TSE.1.1`.
**A scoped login improvement, not full AFTA approval.**

## For management

A password is checked before the second-factor proof is consumed. An account
can change during that interval: its password may be reset, it may be disabled
or moved to external-only authentication, or its expiry time may pass. The login
request can still hold an older copy of these settings.

The MFA proof guard now rechecks the current database account record. It rejects
an old password hash, a disabled or external-only account, or an expired account
before reserving the second-factor proof. The existing rules remain in force;
no password-length, lockout or session-timeout value is introduced or changed.
Normal MFA login still works for an unchanged, permitted account.

This improves an already partial checklist item. The percentages remain
**43/78 partial (55.1%), 10 gaps (12.8%) and 25 needing definitions (32.1%)**.
No complete requirement is closed. Existing sessions and other authentication
providers still need their own revocation/freshness evidence.

## Code trace

[TwoFactorReplayService.consume](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java)
retains its primary-database `FOR UPDATE` query and independent reservation
transaction. The query now also reads `password`, `disabled`, `externalauth` and
`accountexpiry` from the existing `userinfo` row. It requires:

- The current secret and factor type match the login snapshot, as before.
- The current stored password hash matches the already password-authenticated
  principal; a missing principal hash is rejected. This does not replace the
  provider's existing password-encoder check or accept a submitted hash as a password.
- The current account is enabled and permits local password authentication.
- Account expiry is absent or strictly later than the time checked after acquiring
  the row lock. The existing [User.isAccountNonExpired](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/user/User.java)
  also uses a strict expiry boundary; the existing provider rejects external-only accounts.

The existing [TwoFactorAuthenticationProvider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
continues to require a successful guard result before returning MFA authentication.
Denial uses the existing failure handling. No hash/password is added to logging,
SQL parameters, the replay fingerprint or the replay ledger. The query reads the
existing credential column; no new credential copy is persisted.

Rejection occurs before reserving the proof. A newly authenticated snapshot with
the new password hash can use a still-valid, unconsumed proof if the account
otherwise remains permitted. Password verification itself is the provider's job.

## Tests and evidence

**114 targeted authentication/database cases pass, with no failures, errors or
skips:** 21 provider policy, 28 H2 guard, 52 authentication/MFA API and 13 isolated
PostgreSQL cases. The new H2 suite reproduced **six expected-denial failures**
against the preceding guard (28 cases total), then passed all 28 with this fix.
This compares guard behavior, not a deployed exploit. WAR packaging succeeds
with existing browser assets; packaged guard/result-record/provider classes match
the compiled classes, and the PDF dependency check passes. The inventory overlaps
earlier runs and is not added to their totals.

The deterministic [H2 guard tests](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayServiceTest.java)
add account disablement, external-only changes, password replacement and fresh
snapshot acceptance, missing password hash, expired/equal/future expiry boundaries.
Guard-only fixtures use synthetic hashes to test equality, not the hash algorithm.
The existing authentication API cases retain real password verification.

The [PostgreSQL tests](../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayPostgresTest.java)
exercise real row-lock ordering for concurrent secret change, password reset,
disablement, external-only conversion and account-expiry change. They observe the
consuming writer waiting on the lock, commit the account change, and require
rejection without a replay record. A fresh-password snapshot remains usable after
an old-password snapshot was rejected.

Exact regression comparison, final counts, source hashes and build checks are
recorded in the [dated evidence inventory](../evidence/afta-account-state-freshness-2026-11-11.json).
These are targeted tests, not an independent or deployed evaluation.

## Deployment and remaining limits

Upgrade every login node to obtain the check. No new migration, column,
dependency, configuration flag or browser asset change is needed. The existing
application writer already reads/updates the user row. Database-read/lock failure
continues to prevent a successful guard result.

The row lock orders consumption against concurrent updates. A change committed
before consumption is checked; an update taking the lock later waits until
consumption completes. A subsequent reset/disable cannot undo proof acceptance
that already occurred. The change does not revoke existing sessions or guarantee
that an already accepted request cannot finish after a later update.

This guard covers password-based MFA login. Password-only users, SSO/OIDC/LDAP,
PATs, role/username changes, global-setting races, temporary lockout state,
credential-age settings, enrollment/disable/recovery and identical reset-and-restore
values are not all covered by this check. Account/session policy across those
paths remains open. Row 59 remains a gap pending the selected complete
session-establishment conditions and their coverage. Trusted time, real deployed
multi-node/browser recovery, AFTA title/version and full clause definitions also
remain open.

Implementation commit: `6dcdde2c3eeb3cb5c9949e7c81cee187b9116319`.
