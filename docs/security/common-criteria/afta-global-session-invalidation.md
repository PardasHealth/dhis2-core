# AFTA remediation: enforce global session invalidation

Date: **15 November 2026**. Selected checklist row 39, `FIA_USB.1.3`.
Scope: the existing global revocation mechanism, not complete subject-attribute
freshness or a newly selected session policy.

## Defect and correction

`DefaultUserService.invalidateAllSessions` removed session registry entries.
That is not HTTP-session invalidation. With the in-memory registry, the existing
HTTP session and security context could remain usable because
`ConcurrentSessionFilter` no longer found an expired session marker.

The service now calls `SessionInformation.expireNow()` and retains the marker.
The configured session filter can then perform logout/invalidation when that
session makes its next request. Per-user invalidation already uses this pattern
and remains unchanged. The admin `DELETE /api/sessions` endpoint delegates to
the shared service instead of maintaining a separate expiration loop. Its
existing `ALL` authority requirement is retained.

This preserves next-request revocation, not interruption of requests already in
progress. Sessions created after enumeration are not covered by an atomic global
barrier. Registry cleanup still happens through the normal session lifecycle.
The registry's nonexpired session list excludes revoked sessions, so expired
markers do not count as active sessions for that lookup.

## Tests and evidence

Four new `UserSessionInvalidationTest` cases cover multiple users, actual rejection
by Spring's `ConcurrentSessionFilter`, repeated/empty invalidation, and the
username-based fallback used when principal enumeration is unsupported. The real
filter fixture uses a test expiration handler returning HTTP 401 and checks
HTTP-session invalidation, cleared authentication and an unreached protected
chain. Production retains its existing configured expiration redirect; no new
response policy is introduced. The fallback fixture uses a mocked registry; it is
not an executed Redis deployment test.

With the preceding production service, the seven-case suite has **four assertion
failures**, including an existing session reaching the chain with HTTP 200 after
global invalidation. The three existing per-user cases pass. The patched executions pass **31 checks** (ten core units and 21 H2 API cases),
with no final failures/errors/skips. Source identities are recorded in the
[dated evidence inventory](../evidence/afta-global-session-invalidation-2026-11-15.json).

`SessionControllerTest` adds all-user expiration and ordinary-user denial checks
for the unchanged authority boundary. Existing authentication and role-revocation
regressions are retained. The fork CI now explicitly runs
`UserSessionInvalidationTest` and requires its successful report, alongside the
already selected session-controller tests. A workflow definition is not a hosted
CI result.

The first combined H2 invocation passed the four session-controller checks and
one role-revocation check, but the existing `testSessionGetsCreated` assertion
counted every registry principal, including intentionally retained expired
markers. It reported three principals instead of one. That test now counts only
principals with nonexpired sessions and still requires exactly one active admin
principal. This retains its login/session assertion rather than requiring unsafe
registry deletion as test cleanup. The final rerun is recorded separately.

## Remaining limits

Complete selected revocation timing, browser/SSO/provider paths, background jobs,
Redis persistence/failover, cross-node behavior and deployed acceptance remain
open. The known fallback obtains usernames from the user store; it does not
establish discovery of every orphaned external registry entry. No new cookie,
password, timeout, concurrency-limit, schema or dependency policy is introduced.
Row 39 remains partial and checklist percentages are unchanged.
