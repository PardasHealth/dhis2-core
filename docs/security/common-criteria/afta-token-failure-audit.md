# AFTA remediation: API-token failure audit events

Date: **15 November 2026**. Selected checklist rows 1–3 (`FAU_GEN.1.1`,
`FAU_GEN.1.2`, `FAU_GEN.2.1`). Scope is the API-token servlet filter and its
application event publisher; no full requirement closure is asserted.

## Problem and behavior

The token filter's failure handler published an authentication object without
identity or HTTP details. Resolver errors bypassed that handler entirely. In
addition, the configured Spring publisher did not map any of the three custom
token exception classes, so invoking it did not produce an application event.

The publisher now explicitly maps invalid, expired and constraint-rejected token
exceptions to `ApiTokenAuthenticationFailureEvent`. Resolver failures use the
same failure path. The filter clears the security context before publishing and
returns through the existing authentication entry point without continuing the
protected chain. Constraint failures retain the owner name already resolved by
the authentication manager; earlier failures have an empty name rather than an
invented identity. All receive servlet connection details. Raw forwarding headers
are not used as the failure event's address source.

Failure authentication contains only a name, null credentials and ordinary HTTP
details. It does not retain the submitted token, token hash, token entity or
full account principal. The existing audit listener escapes field values.
The new event is deliberately excluded from password/MFA failure counters:
expiry or a request constraint is not an incorrect password proof. Existing
password failure counting remains in place. This does not define a token-specific
rate-limit or lockout policy.

## Verification boundary

`ApiTokenIpConstraintTest` exercises the real filter/resolver and the actual
publisher created by `AuthenticationProviderConfig`. It inspects emitted
application events, not only calls to a mocked publisher. Cases cover malformed
and bad-checksum tokens, unknown/expired tokens, Unicode owner attribution,
servlet-address provenance, null credentials, security-context clearing and an
unreached protected chain. Existing IP/method denials and success events remain
covered. `AuthenticationListenerTest` checks token rejections do not increment
password counters and password failures still do. The existing audit-message,
token-manager and resolver suites are retained.

The patched implementation passes **62 checks in five suites**, with zero
failures/errors/skips. The preceding production filter, publisher configuration
and failure listener produce **six failures among 24 cases** in the filter and
listener suites. This includes a new-event compatibility check for avoiding
password-counter side effects; it is not a claim that the old code emitted that
new event. Five failing filter cases expose absent events or stale context.
The final patched rerun is recorded separately from that baseline comparison.

See the [dated evidence inventory](../evidence/afta-token-failure-audit-2026-11-15.json)
for source hashes, toolchain, commands, counts and dependency provenance. These tests mock token/account lookup and the HTTP
entry point; they are not deployed browser, database or collector acceptance.

## Remaining acceptance

The exact required AFTA event inventory, timestamps/trusted time, protected log
storage, retention, full-storage response, all authentication-provider paths and
production acceptance remain open. Invalid/expired tokens are not resolved again
just to infer an owner. INFO logging still needs to be enabled at the intended
protected destination. The 78-entry checklist dispositions remain unchanged.
