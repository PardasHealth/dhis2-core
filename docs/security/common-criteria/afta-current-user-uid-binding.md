# AFTA remediation: current-user and MFA target UID binding

Date: **15 November 2026**. Selected checklist rows 29 and 37–39:
`FIA_AFL.1.1`, `FIA_USB.1.1`, `FIA_USB.1.2`, `FIA_USB.1.3`.
Scope: persisted current-user argument identity and MFA target/counter association.
No complete selected row or session/provider policy is closed.

## Defect reproduced

The `@CurrentUser User` resolver used the authenticated snapshot's username to
load a database account. A username is mutable and reusable. If an ordinary
logged-in user's name was changed and that old name assigned to another account,
the existing session could resolve to the other account instead of its own UID.

The six-case H2 baseline uses a non-superuser session, an administrative rename
and name reuse, and the actual HTTP/security pipeline. Against the preceding
code it reports six assertion failures: `/me` identifies the new name owner,
the QR JSON endpoint returns that other account's pending enrollment seed, and
MFA enrollment/enable/disable and failed-disable association act on the wrong
name or reject the intended user's proof. These are controlled fixture results,
not a claim of an incident in a deployed system.

## Implementation trace

| Owning code | Change and invariant |
| --- | --- |
| `CurrentUserHandlerMethodArgumentResolver.resolveArgument` | Persisted `User` arguments are loaded by the authenticated DHIS2 principal's UID. A missing UID/account never falls back to username. Required entity arguments raise `AccessDeniedException`; optional entity arguments return null. |
| `TwoFactorController` | Enrollment, enable/disable and QR paths require a persisted UID-resolved `User`. Self-MFA mutations pass that entity directly to the service. Enable retains the existing authenticated `UserDetails` separately for acting-user metadata. |
| `TwoFactorAuthService` entity overloads | Verify and mutate the same resolved entity, without reloading a target through a mutable name. Existing username overloads remain as convenience wrappers for existing service callers. |
| `TwoFactorAuthService.reset2FA` | Clear the already resolved/verified target object. Both self-disable and privileged UID-based disable avoid a second username lookup after target selection/authorization. |
| `TwoFactorAuthService.disable2FA` | Register a rejected proof under the target's current username, the same value used by that invocation's disable lock check. Do not use a stale session username for that counter. |

The HTTP endpoints and request bodies, credential formats, factor verification,
thresholds and existing privileged authorization checks are retained. No schema,
credential migration, dependency version or POM change is introduced. The service
wrappers resolve the requested username before entering the common entity-based
operation; self-MFA HTTP paths use the UID-resolved entity overloads instead.

The resolver's `String` and `UserDetails` argument forms keep their existing
snapshot contracts. Third-party principals used for persisted current-user
arguments must expose a mapped local UID; a display/subject username alone is
not treated as immutable identity. Required missing-account entity arguments are
denied rather than causing a namesake lookup.

## Tests and baseline comparison

[CurrentUserIdentityResolverTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/mvc/CurrentUserIdentityResolverTest.java)
has seven cases: rename with/without name reuse, deleted optional/required owner,
missing UID, and unchanged details/string contracts. Five assertions fail against
the preceding resolver; the two unchanged argument-contract cases pass.

[TwoFactorIdentityMutationTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorIdentityMutationTest.java)
has three regressions for rejected-proof counter identity and self/privileged
reset using the selected entity. All three fail against the preceding service.
The name-lookup fixture supplies a different account for a subsequent name lookup,
modeling reassignment between target selection and reset. It verifies the same
object is mutated without depending on nondeterministic thread timing.

[CurrentUserIdentityBindingTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/CurrentUserIdentityBindingTest.java)
uses an ordinary session whose snapshot retains the retired username and original
UID. The database is flushed/cleared between setup and requests without refreshing
that session. Six cases cover profile identity, TOTP enrollment/enable/disable,
QR secret selection and failed-disable lockout association. All six fail against
the preceding three production classes. Synthetic test seeds are used; no live
account or authenticator secret is involved.

Final execution passes **145 checks** (42 unit and 103 H2 API cases), with no
failures/errors/skips. Source identities and commands are in the
[dated evidence inventory](../evidence/afta-current-user-uid-binding-2026-11-15.json).
Existing profile, MFA/QR/audit, login and token cases are retained. CI now requires
the new resolver/service/API reports and the existing profile suite; a workflow
edit is not a claim that a hosted run succeeded.

## Remaining acceptance

This does not refresh every session authority or every `String`/`UserDetails`
consumer, audit snapshot name, indirect username lookup or identity-provider
mapping. It does not make all account/factor changes on the same UID atomic with
proof verification. Cross-purpose OTP single use, full session freshness,
concurrent same-account changes, deployed browser/Redis/multi-node behavior and
the exact AFTA operation definitions still require separate work and acceptance.
The username-based counter key scheme and legacy service entry points retain
their existing broader rename/reset-policy questions. No full application-wide
identity-binding or certification claim follows from these cases.
