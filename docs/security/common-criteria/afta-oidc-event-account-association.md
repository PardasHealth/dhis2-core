# AFTA remediation: OIDC event account association

Date: **15 November 2026**. Selected checklist rows 29 (`FIA_AFL.1.1`)
and 37 (`FIA_USB.1.1`). Scope is authentication-event bookkeeping, not a new
provider acceptance policy or complete lockout/subject-binding implementation.

## Defects and changes

The lifecycle `AuthenticationListener` cast OIDC principals and HTTP details
without checking their type or presence. Missing/non-HTTP details could throw
before failed-login counting or successful-login bookkeeping. This listener is
separate from the authentication audit logger hardened in earlier changes.

Completed `OAuth2AuthenticationToken` login events were also treated as generic
form events. Their external subject could be used as a local username for
last-login/invitation updates and successful-login counter reset, even when the
principal already contained a different resolved DHIS2 username.

The listener now resolves local account names from `DhisOidcUser` for both the
login-stage and completed OAuth token types. Optional HTTP details only affect
debug address output and no longer gate bookkeeping. A known local account is
used for failure counting and success updates. An unmapped failure retains its
attempted name; that is not a verified account identity. An unmapped successful
OIDC event does not mutate a coincidentally named local account. Its event metric
is still counted as OIDC, independently of local bookkeeping.

User-info debug handling accepts an authentication principal without requiring
its implementation to be a JWT token, and tolerates absent HTTP details. It
retains the existing user-info account-name behavior. Provider authentication,
claims validation, token issuance, MFA, session acceptance and the INFO audit
message format are unchanged. External IdP claims are not guessed into local
identities when no DHIS2 mapping is available.

## Regression verification

Eight new `AuthenticationListenerTest` cases cover known local identity with
missing/unexpected HTTP details, failure counting without details, completed
OAuth identity and metric classification, unmapped success events, attempted
unmapped failure names and user-info events with a non-JWT principal. Known
local fixtures use a real `DhisOidcUser` wrapper with local `UserDetails` and
an external subject attribute; no live IdP or credential is used.

The preceding listener has **eight assertion failures in 19 cases**, with no
errors/skips. The original eleven cases continue to pass. The patched execution passes **81 checks** (52 unit and 29 H2 API cases), with no
failures/errors/skips. Commands and source identities are recorded in the
[dated evidence inventory](../evidence/afta-oidc-event-account-association-2026-11-15.json).
The fork workflow now explicitly runs this lifecycle-listener suite and rejects
missing, failed or entirely skipped reports. This is not a hosted CI result.

## Remaining acceptance

The exact AFTA lockout thresholds, counting/reset policy across all providers,
atomic/distributed counters and recovery remain open. This fix does not make the
cache's read/modify/write failure counters atomic. A complete local-subject
attribute inventory, principal freshness and racing username/UID changes,
SSO/browser/IdP end-to-end behavior, deployment configuration
and full requirement acceptance are also still open. No full row is closed and
checklist percentages remain unchanged. Other OAuth integrations need an explicit
local account mapping to receive local success-bookkeeping updates; an external
subject alone is insufficient.

## Subsequent atomic-counter follow-up

The [atomic failure-counter change](afta-atomic-failure-counters.md) separately
adds local/Redis key-update atomicity. The OIDC test inventory above remains
historical and is not rewritten; broader provider/reset rules, deployed failover
and full AFTA acceptance remain open.
