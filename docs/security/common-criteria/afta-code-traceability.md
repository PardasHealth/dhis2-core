# AFTA requirements-to-code traceability: recent remediation

Updated: **23 November 2026**. Branch: `work`. Reviewed code snapshot:
`bfbb87408c`. Subsequent documentation-only commits do not change that code.

This guide traces the thirty recent implementation fixes to selected checklist
rows, methods, regression tests and recorded execution evidence. For the complete
82-row source list, use the [row-by-row register](afta-requirement-traceability.md)
and [CSV](afta-requirement-traceability.csv). The authoritative assessment is
[the selected-checklist JSON](../evidence/afta-selected-checklist-2026-11-07.json).
Source hashes in dated evidence identify the exact versions tested; links here
show the methods in the reviewed code snapshot.

The primary list remains **78 entries: 45 partial, 8 gaps and 25 unresolved
requirements/edition definitions; none fully closed**. Three explicitly optional
entries and the original duplicated identifier retain their separate treatment.
These code fixes do not select missing AFTA policies or establish certification.

## How to read the row references

**29 (`FIA_AFL.1.1`)** means **original checklist entry 29**, whose requirement ID
is **`FIA_AFL.1.1`**. The number 29 is not a standards chapter, severity, test
count or completion percentage. It locates the entry in the original numbered
[checklist review](afta-selected-requirements-review.csv).

| Part | Meaning |
| --- | --- |
| `29` | Original source-list entry number |
| `FIA` | Identification and authentication class |
| `AFL` | Authentication failures family |
| First `.1` | Component 1: authentication failure handling |
| Final `.1` | Element 1 within that component |

Read an implementation row as: “This commit fixes the described behavior relevant
to these source entries.” A listed entry remains partial until its complete
selected definition and acceptance work are satisfied.

## Implementation summary

**Selected source rows** identify the original checklist entries affected by each
applied fix. Use the implementation commit for the code diff and the dated
execution record for the exact tested source, tests and outcomes. A linked fix
does not close every acceptance condition in those rows.

| **Selected source rows** | Behavior corrected | Implementation commit | Detailed trace and execution evidence |
| --- | --- | --- | --- |
| 1–3: `FAU_GEN.1.1`, `FAU_GEN.1.2`, `FAU_GEN.2.1` | Emit token-failure events, retain available local identity/connection context, clear stale authentication, isolate password counters | [760ade1f11](https://github.com/PardasHealth/dhis2-core/commit/760ade1f11) | [Token failure audit trace](afta-token-failure-audit.md), [62-case record](../evidence/afta-token-failure-audit-2026-11-15.json) |
| 2: `FAU_GEN.1.2` | Use the servlet-resolved audit address instead of client-supplied forwarding headers | [5d426cda14](https://github.com/PardasHealth/dhis2-core/commit/5d426cda14) | [Address-source trace](afta-authentication-address-source.md), [88-case record](../evidence/afta-authentication-address-source-2026-11-15.json) |
| 39: `FIA_USB.1.3` | Preserve session-expiration markers for next-request revocation; share the corrected service with the admin endpoint | [7af7d5a659](https://github.com/PardasHealth/dhis2-core/commit/7af7d5a659) | [Global revocation trace](afta-global-session-invalidation.md), [31-case record](../evidence/afta-global-session-invalidation-2026-11-15.json) |
| 29 and 37: `FIA_AFL.1.1`, `FIA_USB.1.1` | Handle optional OIDC event details and associate bookkeeping with the resolved local account | [efea229e58](https://github.com/PardasHealth/dhis2-core/commit/efea229e58) | [OIDC account-association trace](afta-oidc-event-account-association.md), [81-case record](../evidence/afta-oidc-event-account-association-2026-11-15.json) |
| 29–30: `FIA_AFL.1.1`, `FIA_AFL.1.2` | Commit concurrent failure increments atomically in local and Redis caches, retaining existing thresholds/expiry | [bc6b67654b](https://github.com/PardasHealth/dhis2-core/commit/bc6b67654b) | [Atomic-counter trace](afta-atomic-failure-counters.md), [132-case record](../evidence/afta-atomic-failure-counters-2026-11-15.json) |
| 29 and 37–39: `FIA_AFL.1.1`, `FIA_USB.1.1/.2/.3` | Bind persisted current-user/MFA targets to authenticated UID, not reusable names | [11240d03e0](https://github.com/PardasHealth/dhis2-core/commit/11240d03e0) | [Current-user/MFA UID trace](afta-current-user-uid-binding.md), [145-case record](../evidence/afta-current-user-uid-binding-2026-11-15.json) |
| 35–36: `FIA_UAU.5.1/.2` | Encode TOTP provisioning label/query values to contain URI delimiters | [410acbc03e](https://github.com/PardasHealth/dhis2-core/commit/410acbc03e) | [Provisioning URI trace](afta-totp-provisioning-uri.md), [execution record](../evidence/afta-totp-provisioning-uri-2026-11-16.json) |
| 37–39: `FIA_USB.1.1/.2/.3` | Retain selected account during email disable challenge validation, delivery and persistence | [a9152350ac](https://github.com/PardasHealth/dhis2-core/commit/a9152350ac) | [Email challenge trace](afta-email-challenge-identity.md), [execution record](../evidence/afta-email-challenge-identity-2026-11-16.json) |
| 36–39: `FIA_UAU.5.2`, `FIA_USB.1.1/.2/.3` | Issue login email challenges by principal UID, rejecting missing/deleted owners without name fallback | [a107b60400](https://github.com/PardasHealth/dhis2-core/commit/a107b60400) | [Login challenge UID trace](afta-login-email-challenge-uid.md), [execution record](../evidence/afta-login-email-challenge-uid-2026-11-16.json) |
| 1–3, 29–30 and 36: audit, `FIA_AFL.1.1/.2`, `FIA_UAU.5.2` | Map rejected MFA proofs to failure events and existing configured lockout; exclude successful challenge issuance | [51eb123a81](https://github.com/PardasHealth/dhis2-core/commit/51eb123a81) | [MFA failure-event trace](afta-mfa-failure-events.md), [execution record](../evidence/afta-mfa-failure-events-2026-11-16.json) |
| 2: `FAU_GEN.1.2` | Retain success request context with copied factor details and cleared submitted OTP | [1114c5c925](https://github.com/PardasHealth/dhis2-core/commit/1114c5c925) | [Success context trace](afta-login-success-context.md), [execution record](../evidence/afta-login-success-context-2026-11-16.json) |
| 29: `FIA_AFL.1.1`; 37: `FIA_USB.1.1`; 38: `FIA_USB.1.2`; 39: `FIA_USB.1.3` | Applied 16 November: mapped OIDC bookkeeping follows principal UID and retains its entity through success updates | [90c44ffa76](https://github.com/PardasHealth/dhis2-core/commit/90c44ffa76) | [Per-row UID trace](afta-oidc-event-uid-binding.md), [execution record](../evidence/afta-oidc-event-uid-binding-2026-11-16.json) |
| 1: `FAU_GEN.1.1`; 29: `FIA_AFL.1.1` | Applied 16 November: token rejection catch excludes downstream failures, retaining their own handling/event attribution | [38ffff2be6](https://github.com/PardasHealth/dhis2-core/commit/38ffff2be6e2768a8d6730c68a5b5d0a5f88ab26) | [Per-row boundary trace](afta-token-failure-boundary.md), [execution record](../evidence/afta-token-failure-boundary-2026-11-16.json) |
| 20: `FDP_RIP.2.1` | Applied 16 November: best-effort clearing of resolver-owned raw-token array on processing success/failure; full residual-data guarantee remains Gap | [20342bc346](https://github.com/PardasHealth/dhis2-core/commit/20342bc3467840a006afdf4596a024ea96e04458) | [Bounded buffer-hygiene trace](afta-token-secret-buffer.md), [execution record](../evidence/afta-token-secret-buffer-2026-11-16.json) |
| 20: `FDP_RIP.2.1` | Applied 16 November: both checksum validators clear their extracted arrays while retaining caller input; full residual-data guarantee remains Gap | [f9ab1c6c86](https://github.com/PardasHealth/dhis2-core/commit/f9ab1c6c86eb6cb22f866a479e42bb37b347e1bf) | [Checksum-copy trace](afta-token-checksum-buffers.md), [execution record](../evidence/afta-token-checksum-buffers-2026-11-16.json) |
| 20: `FDP_RIP.2.1` | Applied 16 November: hash/CRC helpers clear owned UTF-8 storage, preserve caller inputs and retain digest/token formats; full residual-data guarantee remains Gap | [469ad5a9d0](https://github.com/PardasHealth/dhis2-core/commit/469ad5a9d024e9a6f7cd6002b51101274e7cb30f) | [Hash/CRC buffer trace](afta-hash-crc-buffer-hygiene.md), [execution record](../evidence/afta-hash-crc-buffer-hygiene-2026-11-16.json) |
| 20: `FDP_RIP.2.1` | Applied 18 November: clear generation-owned components and unreturned plaintext on wrapper failure; retain successful caller output; full residual-data guarantee remains Gap | [2493199a48](https://github.com/PardasHealth/dhis2-core/commit/2493199a48ff5e85a5688177eccc33c8643edbd9) | [Generation ownership trace](afta-token-generation-buffer-hygiene.md), [execution record](../evidence/afta-token-generation-buffer-hygiene-2026-11-18.json) |

| 27: `FDP_SDI.2.1`; 28: `FDP_SDI.2.2` | Applied 18 November: opt-in signed audit records; verify before decoding, ERROR alert and blocked query/count on tampering; both rows now Partial evidence | [399eaa924a](https://github.com/PardasHealth/dhis2-core/commit/399eaa924a9f7bd24963cc225db07b651738b96d) | [Audit integrity trace](afta-audit-record-integrity.md), [execution record](../evidence/afta-audit-record-integrity-2026-11-18.json) |

| 49: `FPT_STM.1.1` | Applied 18 November: measure optional audit-buffer delays monotonically and order by actual expiry; trusted-time status remains Gap | [b8898fc224](https://github.com/PardasHealth/dhis2-core/commit/b8898fc224ef730cbf45329812e3e96c96633e54) | [Monotonic delay trace](afta-audit-monotonic-delay.md), [execution record](../evidence/afta-audit-monotonic-delay-2026-11-18.json) |

| 52: `FRU_FLT.1.1` | Applied 18 November: atomic optional audit-buffer capacity/deduplication and coordination with retry requeue; full fault tolerance remains Gap | [7318e85b9d](https://github.com/PardasHealth/dhis2-core/commit/7318e85b9d628c29d25705c96d0414e5264b948c) | [Buffer capacity trace](afta-audit-buffer-capacity.md), [execution record](../evidence/afta-audit-buffer-capacity-2026-11-18.json) |

| 52: `FRU_FLT.1.1` | Applied 21 November: reject missing/blank audit destinations so the scheduler retains failed events for retry; full fault tolerance remains Gap | [376bd531a2](https://github.com/PardasHealth/dhis2-core/commit/376bd531a2103e6ed0da6a98e1b74c36b11d130b) | [Destination failure trace](afta-audit-destination-failure.md), [execution record](../evidence/afta-audit-destination-failure-2026-11-21.json) |

| 1: `FAU_GEN.1.1`; 52: `FRU_FLT.1.1` | Applied 21 November: stop direct/buffered publication after audit payload serialization failure; safe failure marker; statuses unchanged | [532ef82f92](https://github.com/PardasHealth/dhis2-core/commit/532ef82f92eae405f042099435ef7102095e2269) | [Payload failure trace](afta-audit-payload-failure.md), [execution record](../evidence/afta-audit-payload-failure-2026-11-21.json) |

| 48: `FPT_TDC.1.1` | Applied 21 November: reject duplicate/trailing audit JSON before logging/storage; isolate strict reader and generic failure diagnostics; status remains Gap | [76ed5687b5](https://github.com/PardasHealth/dhis2-core/commit/76ed5687b5994391342a56df6668dd0da814ee1c) | [Message validation trace](afta-audit-message-validation.md), [execution record](../evidence/afta-audit-message-validation-2026-11-21.json) |

| 48: `FPT_TDC.1.1` | Applied 21 November: require message scope to match each receiving audit consumer before sinks; reject missing scope and null roots; status remains Gap | [29a53061e8](https://github.com/PardasHealth/dhis2-core/commit/29a53061e83a51c53d7573d407ea92dbd304ab7d) | [Destination scope trace](afta-audit-destination-scope.md), [execution record](../evidence/afta-audit-destination-scope-2026-11-21.json) |

| 2: `FAU_GEN.1.2`; 48: `FPT_TDC.1.1` | Applied 21 November: require audit type and reject numeric type/scope enums before sinks; statuses unchanged | [b80da28532](https://github.com/PardasHealth/dhis2-core/commit/b80da28532223c385729f1886f802cf5e9bc3365) | [Event type trace](afta-audit-event-type.md), [execution record](../evidence/afta-audit-event-type-2026-11-21.json) |

| 20: `FDP_RIP.2.1` | Applied 21 November: clear TOTP precheck-owned decoded secret bytes on valid/rejected input; full residual protection remains Gap | [8c36aaa0c5](https://github.com/PardasHealth/dhis2-core/commit/8c36aaa0c58db153945a8d513567e169d9c5ab8f) | [TOTP buffer trace](afta-totp-validation-buffer.md), [execution record](../evidence/afta-totp-validation-buffer-2026-11-21.json) |

| 20: `FDP_RIP.2.1` | Applied 23 November: clear enrollment-owned raw TOTP seed after encoding or encoder failure, before user mutation/persistence; full residual protection remains Gap | [46af98846e](https://github.com/PardasHealth/dhis2-core/commit/46af98846e7b30daf77de51e85baaa8f4f3bbae1) | [Enrollment buffer trace](afta-totp-enrollment-buffer.md), [execution record](../evidence/afta-totp-enrollment-buffer-2026-11-23.json) |

| 46: `FPT_FLS.1.1` | Applied 23 November: preserve TOTP failure rejection with fixed diagnostics and no attached decoder/verifier exceptions; full failure model remains open | [635df509a8](https://github.com/PardasHealth/dhis2-core/commit/635df509a80c9615d341828dd3c13a5f0bfbd949) | [Failure diagnostic trace](afta-totp-failure-diagnostics.md), [execution record](../evidence/afta-totp-failure-diagnostics-2026-11-23.json) |

| 46: `FPT_FLS.1.1` | Applied 23 November: retain QR failure E3026/empty response with fixed diagnostics and no attached provisioning exceptions; full failure model remains open | [b96f3a2d52](https://github.com/PardasHealth/dhis2-core/commit/b96f3a2d52c4f720f06796ae380fce2ab3485181) | [QR failure trace](afta-totp-qr-failure.md), [execution record](../evidence/afta-totp-qr-failure-2026-11-23.json) |

| 20: `FDP_RIP.2.1` | Applied 23 November: clear fingerprint-owned UTF-8 replay challenge bytes on hashing success/failure; retain fingerprint/replay behavior; full residual protection remains Gap | [bfbb87408c](https://github.com/PardasHealth/dhis2-core/commit/bfbb87408c6c92835cd90654815f511a9d7e8615) | [Replay buffer trace](afta-replay-fingerprint-buffer.md), [execution record](../evidence/afta-replay-fingerprint-buffer-2026-11-23.json) |

The counts above belong to separate, overlapping executions. **Do not add them**
to obtain a unique pass total. The email-challenge batch has 30 unit checks and 32 H2 API checks. The provisioning batch has 28 unit checks and 32 H2 API checks. The UID-binding batch has 42 unit checks and 103 H2 API checks; the preceding
atomic-counter batch separately includes six real Redis checks. Earlier records retain their original source hashes and outcomes; the
[generated register](afta-requirement-traceability.md) identifies sources changed
since each historical record. A previous test result is not silently upgraded to
proof for a later source version.

## 1. API-token failure event path

| Code | Owning symbols and invariant |
| --- | --- |
| [Dhis2ApiTokenFilter](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java#L197) | `doFilterInternal`, `authenticationFailed`: resolver and manager/constraint failures clear the security context, publish a sanitized failure authentication and stop before the protected chain. Only an already resolved owner name is retained; credentials are null. |
| [AuthenticationProviderConfig](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationProviderConfig.java#L123) | `authenticationEventPublisher`: explicitly maps invalid, expired and constraint-rejected token exceptions to a real application event. |
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
| [DefaultUserService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java#L1037) | `invalidateAllSessions`: call `SessionInformation.expireNow()` and retain the registry marker so an existing HTTP session cannot escape revocation merely because its registry entry disappeared. |
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

[AuthenticationListener](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationListener.java#L163)
uses `isOidcLogin`, `localOidcUser` and `logOidcAddress` from its success/failure
handlers. Login-stage and completed OAuth tokens now resolve the mapped
`DhisOidcUser` UID to its local account; section 12 records this later fix.
Optional HTTP details only affect debug address output. Unmapped
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
selected failure/reset rules and deployment acceptance remain open. The next
section records the separate atomic key-update improvement.

## 5. Atomic failure counters

| Code | Owning symbols and invariant |
| --- | --- |
| [Cache](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/cache/Cache.java#L83) | `update`: side-effect-free value transformation may retry; unsupported custom adapters fail explicitly instead of falling back to a non-atomic read/write. |
| [LocalCache](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/cache/LocalCache.java#L111) | `update`: entry processor serializes updates to the same key using cache2k's atomic entry operation. |
| [RedisCache](../../../dhis-2/dhis-support/dhis-support-system/src/main/java/org/hisp/dhis/cache/RedisCache.java#L157) | `update`, `UPDATE_IF_UNCHANGED`: compare original serialized bytes and atomically write the new value/expiry on the server; retry conflicts, with an explicit bounded failure. |
| [DefaultUserService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java#L1243) | `registerFailedLogin`, `registerRecoveryAttempt`, `registerFailed2FADisableAttempt`: use atomic updates and checked integer addition, preserving existing predicates, feature flags and resets. |
| [NoOpCache](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/cache/NoOpCache.java#L82) | `update`: preserve explicitly disabled-cache semantics; this path is not counter-enforcement evidence. |

[UserFailureCounterConcurrencyTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/user/UserFailureCounterConcurrencyTest.java)
reproduces three lost-count assertions against the preceding service: two concurrent
failures were counted as one. Its serial threshold/reset and disabled-setting
cases pass before and after the change.
[AtomicCacheUpdateTest](../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/cache/AtomicCacheUpdateTest.java)
checks concurrent local updates, mapper failure preservation and default/disabled
behavior.
[RedisAtomicCacheUpdateTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/cache/RedisAtomicCacheUpdateTest.java)
uses a disposable digest-pinned Redis container and existing application client
dependencies. It tests deterministic conflict retries, 200 concurrent increments,
a racing reset, serialization, expiry and mapper failure. It runs in a dedicated
unit-profile invocation because the H2 profile does not select untagged fixtures.

The 132-case execution combines 70 unit/cache checks, 56 H2 API checks and six
real Redis checks. The existing Redis-cache suite contributes 35 cases aggregated
from eleven nested JUnit reports; the enclosing zero-case report is not counted
again. Source hashes and all report hashes are preserved in the dated evidence.

Redis scripting permissions (`EVAL`/`EVALSHA`) are required. Custom counter-cache
adapters must implement the new atomic operation; no unsafe fallback is provided.
Full provider/reset policy, expiry/eviction behavior, cluster/failover and deployed
operational acceptance remain open. Multiple cache instances against one Redis
server are not a full multi-process application deployment.

## 6. Persisted current-user and MFA target identity

| Code | Owning symbols and invariant |
| --- | --- |
| [CurrentUserHandlerMethodArgumentResolver](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/mvc/CurrentUserHandlerMethodArgumentResolver.java#L70) | `resolveArgument`: resolve persisted `User` by authenticated UID. Never substitute a name owner. Required missing owners deny; optional owners return null. String/details snapshot contracts remain. |
| [TwoFactorController](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/TwoFactorController.java#L78) | Enrollment, enable/disable and QR handlers require the persisted account. Mutations use entity-based service overloads, with separate acting-user details for enable metadata. |
| [TwoFactorAuthService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java#L100) | Entity overloads and `reset2FA(User, UserDetails)` retain the resolved target through proof verification and mutation. Legacy name overloads remain wrappers. Failed-disable counting uses the target's name, matching its lock check. |

[CurrentUserIdentityResolverTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/mvc/CurrentUserIdentityResolverTest.java)
has five failing assertions in seven cases against the preceding resolver.
[TwoFactorIdentityMutationTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorIdentityMutationTest.java)
has three failures against the preceding reset/counter service paths.
[CurrentUserIdentityBindingTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/CurrentUserIdentityBindingTest.java)
has six HTTP failures against the preceding three classes: an ordinary stale
session receives a name-reuse account's profile UID/pending QR seed, or operates
MFA on the wrong name. It then verifies the correct owner after the patch.

The final batch passes 42 unit and 103 H2 API cases, including the existing profile,
MFA/QR/audit and login/token suites. These controlled fixtures do not establish a
live browser incident or universal session freshness. Same-UID factor/account
races, every indirect username consumer, retained snapshot authorities and the
complete selected policy remain open. See the dated evidence for exact classes,
source/report hashes, baseline failures and conditions.

## 7. TOTP provisioning URI data boundaries

`TwoFactorAuthUtils.generateTOTP2FAURL` encodes issuer, account and secret values
with `encodeProvisioningComponent`. The four parser cases cover ordinary input,
reserved delimiters in each label component, plus signs and Unicode account names.
The preceding utility errors in three of four cases; the patched utility passes
all four. See the [implementation and dated execution trace](afta-totp-provisioning-uri.md).
Scanner interoperability and the complete selected mechanism rules remain open.

## 8. Email disable challenge target continuity

`TwoFactorAuthService.disable2FA(User, String)` passes its selected account to
private `sendEmail2FACode(User)`. The compatibility username entry point resolves
once. The helper checks and updates the same account and sends to that recipient.
Two deterministic name-reassignment tests fail against the preceding service and
pass with the fix. They check original challenge/recipient, namesake isolation and
original verified-email enforcement. See the [code and execution trace](afta-email-challenge-identity.md).
Other initial username callers and same-account races remain open.

## 9. Email login challenge principal UID

`TwoFactorAuthenticationProvider.sendEmail2FACode(UserDetails)` calls
`TwoFactorAuthService.sendEmail2FACodeByUid(String)`. Missing/blank/deleted UIDs
fail without a username fallback; the entity helper retains recipient and update
identity. The provider contract assertion fails with the preceding provider and
the additive service helper available. Three new service cases cover renamed
owners, deleted owners and missing UIDs. See the [execution trace and limitations](afta-login-email-challenge-uid.md).
The latest batch passes 33 unit checks and 33 H2 login/MFA cases.
Password/factor-state locking during issuance and other caller contracts remain open.

## 10. Rejected MFA proof events and configured lockout

`AuthenticationProviderConfig.authenticationEventPublisher` maps the exact
`TwoFactorAuthenticationException` class to `AuthenticationFailureBadCredentialsEvent`.
Spring's exact-class lookup does not inherit its superclass mapping. Existing
audit and account-counter listeners now receive the rejection. The distinct
`TwoFactorCodeSentException` remains unmapped. Publisher baseline: two failures
in three cases. H2 baseline: two threshold failures in twelve cases. See the
[full execution trace](afta-mfa-failure-events.md) for final source/report hashes.
The final batch passes 76 unit and 46 H2 API cases.
Threshold/expiry and the lockout setting remain; full selected failure policy,
audit delivery/retention and deployed acceptance remain open.

## 11. Sanitized login success connection context

`TwoFactorAuthenticationProvider.authenticate` retains the parent's details in
its replacement token. Factor details are copied with address/session fields and
a null code. Real `ProviderManager` success-event cases cover password-only,
TOTP and email; all three fail against the preceding provider because details
are null. They also require the incoming request's supplied code to remain
unchanged. See the [execution trace](afta-login-success-context.md). Full event
content policy, other providers and deployed acceptance remain open.

## 12. Mapped OIDC event UID owner

**Selected source rows:** 29 (`FIA_AFL.1.1`), 37 (`FIA_USB.1.1`),
38 (`FIA_USB.1.2`), 39 (`FIA_USB.1.3`). Applied **16 November 2026**.

`AuthenticationListener.localOidcUser` resolves the principal UID. Success
bookkeeping retains that entity, avoiding another mutable-name lookup, and
failure counting follows its current name. Missing/deleted mapped owners have
no name fallback. Seven deterministic rename/reuse/deletion cases fail against
the preceding listener and pass with the fix, covering both OIDC token stages.
See the [per-row implementation and execution trace](afta-oidc-event-uid-binding.md).
All four rows remain partial; same-account name races and full session/provider
freshness remain open.

## 13. Token-processing failure boundary

**Selected source rows:** 1 (`FAU_GEN.1.1`), 29 (`FIA_AFL.1.1`). Applied
**16 November 2026**. `Dhis2ApiTokenFilter.doFilterInternal` now limits its
rejection catch to token processing. Success publication and downstream handling
run afterward. Two downstream exception regressions fail against the preceding
filter and pass with the fix, requiring exception identity, one success event,
no filter-produced failure and no token entry-point invocation. See the
[per-row code and execution trace](afta-token-failure-boundary.md). Both rows
remain partial; surrounding handlers and full event/failure policy remain open.

## 14. Gap row 20: resolver-owned raw-token array

**Selected source row:** 20 (`FDP_RIP.2.1`). Applied **16 November 2026**.
`ApiTokenResolver.resolve` overwrites its local raw-token character array in a
`finally` block after checksum/hash processing. Four controlled lifecycle cases
fail against the preceding resolver and pass with the change. They observe the
same array supplied to the generator under success and three failure outcomes.
See the [bounded trace and remaining resource boundary](afta-token-secret-buffer.md).
This is best-effort mutable-buffer hygiene. Immutable header/runtime copies,
physical JVM erasure and storage/replica/backup guarantees remain unproven;
row 20 stays **Gap / evidence not established**.

## 15. Gap row 20: validator-owned checksum copies

**Selected source row:** 20 (`FDP_RIP.2.1`). Applied **16 November 2026**.
`ApiKeyTokenGenerator` clears private checksum-validation code/checksum arrays
in `finally` blocks for both token versions. Six controlled cases fail against
the preceding implementation and pass with the change; they also require the
caller-owned token to remain unchanged. Public extraction and generation outputs
keep their ownership contract. See the [bounded trace](afta-token-checksum-buffers.md).
Row 20 remains **Gap**: byte/string/runtime copies and complete protected-resource
acceptance remain unproven. This adds a commit, not another distinct source row.

## 16. Gap row 20: hash/CRC encoding storage

**Selected source row:** 20 (`FDP_RIP.2.1`). Applied **16 November 2026**.
`HashUtils.hashOwnedBytes` and `hashChars` clear their own SHA-256/SHA-512 UTF-8
storage; character hashing avoids a second plaintext byte array. CRC calculation
clears its owned encoding array while preserving its existing byte range. Caller
inputs are retained. The 22 new cases have ten baseline lifetime failures and
twelve passing ownership/vector controls. See the [code and compatibility trace](afta-hash-crc-buffer-hygiene.md).
Row 20 remains **Gap**: physical JVM erasure and full protected-resource acceptance
remain unproven. This adds a commit to the same linked entry.

## 17. Gap row 20: token-generation ownership

**Selected source row:** 20 (`FDP_RIP.2.1`). Applied **18 November 2026**.
`ApiKeyTokenGenerator.generatePatToken` clears generation-owned random-code and
checksum components after assembly/failure. `generatePersonalAccessToken` clears
assembled plaintext only if no successful wrapper has taken ownership. The eleven
new cases have eight baseline cleanup failures and three passing ownership/real
compatibility controls. See the [generation trace](afta-token-generation-buffer-hygiene.md).
The H2 token-controller suite is also explicitly required in CI. Row 20 stays
**Gap**: returned caller outputs, runtime/immutable copies and complete protected
resource acceptance remain outside this proof.

## CI and maintaining the trace

The [fork regression workflow](../../../.github/workflows/cc-audit-regression.yml)
now explicitly selects `AuthenticationListenerTest` and
`UserSessionInvalidationTest` and requires their successful, nonempty reports.
The atomic-counter follow-up also requires `AtomicCacheUpdateTest`,
`UserFailureCounterConcurrencyTest` and the dedicated `RedisAtomicCacheUpdateTest`
report. The UID-binding follow-up also requires `CurrentUserIdentityResolverTest`,
`TwoFactorIdentityMutationTest`, `CurrentUserIdentityBindingTest` and the existing
`MeControllerTest` report. The buffer suites `ApiTokenSecretBufferTest`, `ApiTokenChecksumBufferTest`, `HashSecretBufferTest`, `CrcSecretBufferTest` and `ApiTokenGenerationBufferTest` are also required, together with the H2 `ApiTokenControllerTest` report. The publisher suite `TwoFactorFailureEventTest` and mapped-identity suite `OidcEventIdentityTest` are also required. `TwoFactorProvisioningUriTest` and `TwoFactorEmailChallengeIdentityTest` are also selected and required. Existing session/authentication API cases remain selected. Workflow syntax and
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

## 18. Audit-record integrity and response

**Selected source rows: 27 (`FDP_SDI.2.1`) and 28 (`FDP_SDI.2.2`).** The opt-in guard authenticates persisted record identity, metadata, JSONB attributes and compressed payload before decoding. A bad or absent tag emits `AUDIT_INTEGRITY_FAILURE` and blocks the matching query or count. See the [implementation, rollout and remaining acceptance](afta-audit-record-integrity.md). Both entries move from Gap to Partial evidence; full closure remains zero.

## 19. Monotonic audit-buffer delays

**Selected source row: 49 (`FPT_STM.1.1`).** Actual expiry ordering and monotonic elapsed time prevent wall-clock adjustment delays and head blocking by newer records. [Eight queue/scheduler checks and remaining acceptance](afta-audit-monotonic-delay.md) are recorded. Row 49 stays Gap.

## 20. Concurrent audit-buffer admission

**Selected source row: 52 (`FRU_FLT.1.1`).** Atomic admission preserves the 200-entry buffer bound under concurrent writers and shares the retry-processing monitor. [Code, 12 passing cases and remaining acceptance](afta-audit-buffer-capacity.md) are recorded. Row 52 stays Gap.

## 21. Audit destination failure propagation

**Selected source row: 52 (`FRU_FLT.1.1`).** Invalid destination mapping raises a publication failure, allowing the existing scheduler recovery path to retain the event. [Code, 19 passing cases and remaining acceptance](afta-audit-destination-failure.md) are recorded. Row 52 stays Gap.

## 22. Audit payload serialization failure

**Selected source rows: 1 (`FAU_GEN.1.1`) and 52 (`FRU_FLT.1.1`).** Payload construction errors propagate before direct publication or buffering; serializer exception content is excluded from the fixed failure marker and generic exception. [Code, 30 passing cases and remaining acceptance](afta-audit-payload-failure.md) are recorded. Statuses remain Partial and Gap respectively.

## 23. Audit message ambiguity rejection

**Selected source row: 48 (`FPT_TDC.1.1`).** Local strict parsing rejects duplicate fields and trailing content before the audit sinks. [Code, 15 passing checks and remaining acceptance](afta-audit-message-validation.md) are recorded. Full peer/schema/conflict policy is unestablished; row 48 remains Gap.

## 24. Audit destination and scope binding

**Selected source row: 48 (`FPT_TDC.1.1`).** Each concrete audit consumer binds the message claim to its receiving scope before sinks run. [Code, 39 passing checks and remaining acceptance](afta-audit-destination-scope.md) are recorded. Row 48 remains Gap.

## 25. Audit event type and enum validation

**Selected source rows: 2 (`FAU_GEN.1.2`) and 48 (`FPT_TDC.1.1`).** Require non-null event type and reject numeric enum values before sinks. [Code, 55 passing checks and remaining acceptance](afta-audit-event-type.md) are recorded. Statuses remain Partial and Gap.

## 26. TOTP validation-owned secret cleanup

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear the helper-owned length-validation array before proof verification or rejection. [Code, 44 passing checks and remaining acceptance](afta-totp-validation-buffer.md) are recorded. Full residual protection remains Gap.

## 27. TOTP enrollment-owned raw seed cleanup

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear raw random bytes after Base32 encoding or failure before mutation/persistence. [Code, 48 passing checks and remaining acceptance](afta-totp-enrollment-buffer.md) are recorded. Row 20 remains Gap.

## 28. TOTP failure diagnostics

**Selected source row: 46 (`FPT_FLS.1.1`).** Decoder/verifier errors still reject proofs; fixed diagnostics omit exception objects that may carry secrets. [Code, 50 passing checks and remaining acceptance](afta-totp-failure-diagnostics.md) are recorded. Row 46 remains Partial evidence.

## 29. TOTP QR failure diagnostics

**Selected source row: 46 (`FPT_FLS.1.1`).** Checked QR writer/I/O failures retain the existing error response with fixed, exception-free diagnostics. [Code, 53 passing checks and remaining acceptance](afta-totp-qr-failure.md) are recorded. Row 46 remains Partial evidence.

## 30. Replay fingerprint-owned challenge cleanup

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear the helper-owned UTF-8 challenge array after digest processing or failure. [Code, 72 passing checks and remaining acceptance](afta-replay-fingerprint-buffer.md) are recorded. Row 20 remains Gap.
