# AFTA requirements-to-code traceability: 15 November remediation

Updated: **15 November 2026**. Branch: `work`. Reviewed code snapshot:
`a0958140b2`. Subsequent documentation-only commits do not change that code.

This guide traces the four recent implementation fixes to selected checklist
rows, methods, regression tests and recorded execution evidence. For the complete
82-row source list, use the [row-by-row register](afta-requirement-traceability.md)
and [CSV](afta-requirement-traceability.csv). The authoritative assessment is
[the selected-checklist JSON](../evidence/afta-selected-checklist-2026-11-07.json).
Source hashes in dated evidence identify the exact versions tested; links here
show the methods in the reviewed code snapshot.

The primary list remains **78 entries: 43 partial, 10 gaps and 25 unresolved
requirements/edition definitions; none fully closed**. Three explicitly optional
entries and the original duplicated identifier retain their separate treatment.
These code fixes do not select missing AFTA policies or establish certification.

## Implementation summary

| Selected source rows | Behavior corrected | Implementation commit | Detailed trace and execution evidence |
| --- | --- | --- | --- |
| 1–3: `FAU_GEN.1.1`, `FAU_GEN.1.2`, `FAU_GEN.2.1` | Emit token-failure events, retain available local identity/connection context, clear stale authentication, isolate password counters | `760ade1f11` | [Token failure audit trace](afta-token-failure-audit.md), [62-case record](../evidence/afta-token-failure-audit-2026-11-15.json) |
| 2: `FAU_GEN.1.2` | Use the servlet-resolved audit address instead of client-supplied forwarding headers | `5d426cda14` | [Address-source trace](afta-authentication-address-source.md), [88-case record](../evidence/afta-authentication-address-source-2026-11-15.json) |
| 39: `FIA_USB.1.3` | Preserve session-expiration markers for next-request revocation; share the corrected service with the admin endpoint | `7af7d5a659` | [Global revocation trace](afta-global-session-invalidation.md), [31-case record](../evidence/afta-global-session-invalidation-2026-11-15.json) |
| 29 and 37: `FIA_AFL.1.1`, `FIA_USB.1.1` | Handle optional OIDC event details and associate bookkeeping with the resolved local account | `efea229e58` | [OIDC account-association trace](afta-oidc-event-account-association.md), [81-case record](../evidence/afta-oidc-event-account-association-2026-11-15.json) |

The counts above belong to separate, overlapping executions. **Do not add them**
to obtain a unique pass total. The latest batch has 52 unit checks plus 29 H2 API
checks. Earlier records retain their original source hashes and outcomes; the
[generated register](afta-requirement-traceability.md) identifies sources changed
since each historical record. A previous test result is not silently upgraded to
proof for a later source version.

## 1. API-token failure event path

| Code | Owning symbols and invariant |
| --- | --- |
| [Dhis2ApiTokenFilter](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L193) | `doFilterInternal`, `authenticationFailed`: resolver and manager/constraint failures clear the security context, publish a sanitized failure authentication and stop before the protected chain. Only an already resolved owner name is retained; credentials are null. |
| [AuthenticationProviderConfig](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationProviderConfig.java#L122) | `authenticationEventPublisher`: explicitly maps invalid, expired and constraint-rejected token exceptions to a real application event. |
| [ApiTokenAuthenticationFailureEvent](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthenticationFailureEvent.java#L37) | Dedicated failure event distinguishes token rejection from a wrong password/MFA proof. |
| [AuthenticationListener](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationListener.java#L91) | `handleAuthenticationFailure`: token audit failures do not increment password failure counters. |

[ApiTokenIpConstraintTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenIpConstraintTest.java)
uses the actual production-configured event publisher and checks emitted events,
not only mocked publish calls. Named cases include
`constraintFailurePublishesResolvedOwnerWithoutToken`,
`malformedTokenPublishesFailureAndClearsPreviousContext` and
`expiredTokenAlsoEmitsAnActualFailureEvent`.
[AuthenticationListenerTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java)
retains `tokenAuditFailureDoesNotIncrementPasswordFailureCount` and
`passwordFailureStillIncrementsFailureCount`.

The preceding production filter/configuration/listener produced six failures in
24 regression cases. Complete event coverage, trusted time, protected collection,
retention and full-storage behavior remain open. The token itself, its hash and
the full account object are not attached to the new failure authentication.

## 2. Authentication audit address construction

[ForwardedIpAwareWebAuthenticationDetails](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/ForwardedIpAwareWebAuthenticationDetails.java#L41),
its HTTP-request constructor, captures the servlet address already held by
`WebAuthenticationDetails`. The password/MFA and HTTP Basic subclasses inherit
this behavior. Explicit-value constructors and session serialization retain
their existing interface.

[AuthenticationAuditRecordTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/AuthenticationAuditRecordTest.java)
captures actual audit messages. Relevant cases include
`passwordFailureCannotForgeAuditAddressWithForwardedHeader`,
`basicSuccessCannotForgeAuditAddressWithForwardedChain` and
`emptyForwardedHeaderCannotEraseAvailableAuditAddress`. Four of the 20 audit
cases fail against the preceding constructor; the patched 88-case batch also
retains provider, listener and token regressions.

Trusted proxy/container client-address resolution and restricted ingress still
need deployment verification. Without that configuration, the servlet address
may identify the proxy. This code does not establish an IP-based lockout policy.

## 3. Global session revocation

| Code | Owning symbols and invariant |
| --- | --- |
| [DefaultUserService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java#L1038) | `invalidateAllSessions`: call `SessionInformation.expireNow()` and retain the registry marker so an existing HTTP session cannot escape revocation merely because its registry entry disappeared. |
| [SessionController](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/SessionController.java#L103) | `invalidateAllSessions`: the existing `DELETE /api/sessions` endpoint delegates to the corrected service, retaining its `ALL` authority requirement. The endpoint previously had its own expiration loop; the defect was in the service path. |

[UserSessionInvalidationTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/user/UserSessionInvalidationTest.java)
contains `invalidatedSessionIsRejectedByTheRealSessionFilter`, which exercises
Spring's real `ConcurrentSessionFilter`, HTTP-session invalidation, cleared
authentication and an unreached chain. Its test expiration handler returns 401;
production retains its existing expiration redirect. Other new cases cover all
users, repeated/empty invalidation and unsupported principal enumeration.

[SessionControllerTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/SessionControllerTest.java)
checks admin all-user expiration and ordinary-user denial using the real H2/API
security configuration. The existing login fixture now counts only principals
with nonexpired sessions, preserving exactly one active admin login while
allowing revoked markers to remain until lifecycle cleanup.

Four of seven core cases fail against the preceding service, including a request
that reaches the chain with status 200 after global invalidation. The final batch
passes ten core and 21 H2 checks. This establishes next-request enforcement in the
fixtures, not interruption of in-flight requests, atomic revocation of concurrent
new logins, real Redis persistence or cross-node/browser acceptance.

## 4. OIDC event bookkeeping

[AuthenticationListener](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationListener.java#L158)
uses `isOidcLogin`, `localOidcUsername` and `logOidcAddress` from its success/failure
handlers. Login-stage and completed OAuth tokens obtain their local name from
`DhisOidcUser`; optional HTTP details only affect debug address output. Unmapped
success events do not look up or reset a local namesake. Unmapped failures retain
the attempted name. User-info address handling does not assume a JWT principal.

[AuthenticationListenerTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java)
uses a real local OIDC-principal wrapper and mocked account services. Cases such
as `oidcFailureWithoutHttpDetailsStillCountsAgainstTheKnownLocalAccount` and
`completedOauthLoginUsesCanonicalLocalIdentityAndOidcMetric` verify local-account
method routing. Eight new assertions fail against the preceding listener; all
19 listener cases pass after the patch. The 81-case batch also includes existing
audit/token units and real H2 form/token API checks.

These are not live IdP protocol or browser tests. Provider acceptance/claims
validation is unchanged. Complete attribute freshness, username/UID races,
selected failure/reset rules and atomic/distributed counters remain open.

## CI and maintaining the trace

The [fork regression workflow](../../../.github/workflows/cc-audit-regression.yml)
now explicitly selects `AuthenticationListenerTest` and
`UserSessionInvalidationTest` and requires their successful, nonempty reports.
Existing session/authentication API cases remain selected. Workflow syntax and
report definitions are checked locally; a committed workflow is not proof of a
successful hosted run.

From the repository root, validate the authoritative assessment and generated
row-by-row artifacts with:

```bash
python3 docs/security/tools/render_afta_selected_review.py --check
python3 docs/security/tools/render_afta_traceability.py --check
python3 -m unittest discover -s docs/security/tools -p test_afta_traceability.py
```

For Java reproduction, use each dated evidence file's commands, profiles,
toolchain and dependency-provenance conditions. When changing code, update the
current assessment's source anchors/hashes after reviewing the diff, add a new
execution record, and regenerate the register. Preserve historical hashes and
failed/skipped results. A code link or passing subset never closes a complete
AFTA row without its remaining acceptance work.
