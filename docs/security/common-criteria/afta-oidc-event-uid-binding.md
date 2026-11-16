# OIDC event bookkeeping retains the principal UID owner

Date applied: **16 November 2026**. Branch: `work`.
Implementation: [90c44ffa76](https://github.com/PardasHealth/dhis2-core/commit/90c44ffa766e6b413f0e08a94a08fa5d961abcfd).

## Selected source rows

These are the original checklist row numbers, not test or document row numbers.
Each remains **Partial evidence**; this fix addresses event bookkeeping only.

| Selected source row | Requirement ID | Behavior changed by this fix |
| --- | --- | --- |
| 29 | `FIA_AFL.1.1` | Mapped OIDC failure counting follows the UID owner's current name. Missing/deleted owners do not increment a namesake's counter. |
| 37 | `FIA_USB.1.1` | Resolve the mapped local principal by UID rather than its reusable name snapshot. |
| 38 | `FIA_USB.1.2` | Retain the resolved local entity for last-login/invitation updates and success-counter association. |
| 39 | `FIA_USB.1.3` | After name changes or deletion, bookkeeping cannot substitute an account that now owns the old name. |

The implementation commit and exact tested source hashes are recorded in the
[dated execution evidence](../evidence/afta-oidc-event-uid-binding-2026-11-16.json)
and [code traceability summary](afta-code-traceability.md).

## Defect and implementation

The preceding listener used `DhisOidcUser.getUsername()` from its principal
snapshot. Success processing then looked up the account by that name; failure
processing incremented that name's counter. After a rename and name reuse, these
operations could affect the namesake instead of the authenticated UID owner.

`AuthenticationListener.localOidcUser` now resolves a mapped principal's UID.
Null/blank UIDs and missing owners return no account, without a name fallback.
Success handling passes the resolved entity directly into bookkeeping, avoiding
a second lookup; failure handling uses its current name. The read-only mutation
check and metric method tags remain. Unmapped success still makes no account
mutation; unmapped failure retains its existing attempted-name contract.

Code and regression trace:

- [AuthenticationListener](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationListener.java): `handleAuthenticationFailure`, `handleAuthenticationSuccess`, `localOidcUser`, entity-based `registerSuccessfulLogin`.
- [OidcEventIdentityTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/OidcEventIdentityTest.java): seven deterministic cases using actual `DhisOidcUser` wrappers and mocked account services. Both login-stage and completed OAuth tokens cover renamed owners, failure attribution and deleted owners; a missing-UID case requires no account interaction. All seven fail against the preceding listener.
- [AuthenticationListenerTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java): existing mapped OIDC fixtures now have a UID and persisted entity, retaining prior missing-details, unmapped-principal and metric checks.
- [Fork workflow](../../../.github/workflows/cc-audit-regression.yml): explicitly selects the new suite and requires a successful nonempty report.

These controlled service fixtures model stale principal names and name reuse;
they do not establish a concurrent external IdP exploit. Name-keyed counters
still have same-account rename races after UID resolution. This is not a full
session-authority refresh, a new OIDC authorization rule, atomic account-state
validation, or acceptance of the complete selected failure/identity policy.

Final verification: **105 Java cases passed** (73 unit and 32 H2 API), with
zero failures, errors or skips. Seven baseline failures remain separately
recorded. These executions overlap earlier inventories and are not additive.
