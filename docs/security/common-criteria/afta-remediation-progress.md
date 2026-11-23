# AFTA remediation implementation progress

Updated: **23 November 2026**. Scope: the [user-selected AFTA checklist](afta-selected-requirements-review.md).
This records implemented changes separately from the remaining requirements.
It is not AFTA/Common Criteria approval or a statement that all listed gaps are closed.

<!-- AFTA_CURRENT_PROGRESS_START -->

## Current progress snapshot

Latest recorded implementation: **23 November 2026**. Branch: `work`.

**45 distinct implementation commits** are linked to **27/78 primary entries (34.6%)**, plus **1/3 optional entries**. A linked fix records bounded implementation work; every primary entry retains acceptance work.

| Requirement evidence status | Primary entries | Percentage |
| --- | ---: | ---: |
| Partial evidence | 45 | 57.7% |
| Gap / evidence not established | 8 | 10.3% |
| Definition or edition unresolved | 25 | 32.1% |
| Fully closed | 0 | 0.0% |

![Current AFTA manager summary](charts/afta-selected-dashboard.png?v=progress-2026-11-23-45)

![Recorded implementation activity](charts/afta-implementation-progress.png?v=progress-2026-11-23-45)

Implementation activity increases when another distinct code commit is linked with dated evidence. The linked-entry count increases when a previously unlinked primary entry receives a follow-up. Repeated fixes to one entry increase activity without closing its remaining requirements. Evidence status changes only when the assessment supports that change; partial evidence is not a compliance or effort percentage.

### Work recorded on 23 November 2026

Test totals belong to each linked execution record and overlap across fixes; do not sum them as unique tests.

| **Selected source rows** | Implementation | Detailed trace and execution record |
| --- | --- | --- |
| 20 (`FDP_RIP.2.1`) | [fdf52396c2](https://github.com/PardasHealth/dhis2-core/commit/fdf52396c23f065c8ef542c28799415cc54701e1) | [Audit payload codec-owned buffer cleanup](afta-audit-codec-buffers.md) |
| 20 (`FDP_RIP.2.1`) | [e98b283c8b](https://github.com/PardasHealth/dhis2-core/commit/e98b283c8be740d62e39bad3b87152b3532da930) | [Generated secret character ownership](afta-generated-secret-characters.md) |
| 20 (`FDP_RIP.2.1`), 46 (`FPT_FLS.1.1`), 48 (`FPT_TDC.1.1`) | [e3d4f49dd2](https://github.com/PardasHealth/dhis2-core/commit/e3d4f49dd2158875c8caae9add912d30326e2c7a) | [PKCE S256 failure policy and derived parameter protection](afta-pkce-failure-policy.md) |
| 20 (`FDP_RIP.2.1`) | [bfbb87408c](https://github.com/PardasHealth/dhis2-core/commit/bfbb87408c6c92835cd90654815f511a9d7e8615) | [Replay fingerprint-owned challenge buffer cleanup](afta-replay-fingerprint-buffer.md) |
| 20 (`FDP_RIP.2.1`) | [b5313faa57](https://github.com/PardasHealth/dhis2-core/commit/b5313faa57d714e6721404f3f59d926a5928ca88) | [TOTP QR-owned buffer cleanup](afta-qr-owned-buffers.md) |
| 20 (`FDP_RIP.2.1`) | [46af98846e](https://github.com/PardasHealth/dhis2-core/commit/46af98846e7b30daf77de51e85baaa8f4f3bbae1) | [TOTP enrollment-owned raw seed cleanup](afta-totp-enrollment-buffer.md) |
| 46 (`FPT_FLS.1.1`) | [635df509a8](https://github.com/PardasHealth/dhis2-core/commit/635df509a80c9615d341828dd3c13a5f0bfbd949) | [TOTP failure diagnostics without secret-bearing exceptions](afta-totp-failure-diagnostics.md) |
| 46 (`FPT_FLS.1.1`) | [b96f3a2d52](https://github.com/PardasHealth/dhis2-core/commit/b96f3a2d52c4f720f06796ae380fce2ab3485181) | [TOTP provisioning QR failure diagnostics](afta-totp-qr-failure.md) |

### Remaining gaps and acceptance

**8 primary entries remain Gap.** Existing bounded fixes remain linked; the following acceptance work is still open.

| **Selected source row** | Requirement | Remaining acceptance |
| --- | --- | --- |
| 20 | `FDP_RIP.2.1` | Define the protected resources and platform boundary; demonstrate allocation/deallocation protection for memory, files, media, replicas and backups. The linked buffer-hygiene changes do not establish complete protected-resource coverage. |
| 48 | `FPT_TDC.1.1` | Define trusted peers, attribute schemas and conflict/precedence rules; verify contradictory and malformed security-data exchange. |
| 49 | `FPT_STM.1.1` | Document clock authority, synchronization, access control and allowed drift; test rollback, skew and failure for audit/session/token behavior. |
| 52 | `FRU_FLT.1.1` | Define required degraded functions/fault classes; demonstrate service/data behavior under the chosen database, broker, disk and node failures. |
| 56 | `FTA_TAH.1.1` | Confirm this AFTA element definition and demonstrate its required user-visible history, timing and preservation/acknowledgement rules. Existing last-login storage alone is insufficient. |
| 57 | `FTA_TAH.1.2` | Confirm this AFTA element definition and demonstrate its required user-visible history, timing and preservation/acknowledgement rules. Existing last-login storage alone is insufficient. |
| 58 | `FTA_TAH.1.3` | Confirm this AFTA element definition and demonstrate its required user-visible history, timing and preservation/acknowledgement rules. Existing last-login storage alone is insufficient. |
| 59 | `FTA_TSE.1.1` | Select the required user/time/location/provider conditions and prove every allowed/denied session-establishment path. |

The **25 unresolved primary entries** need the exact AFTA clauses, edition or selections. The **45 partial entries** still need their remaining coverage, deployment and acceptance proof. Rows 27–28 have partial evidence for the consolidated audit-table control; their complete acceptance remains open.

Use the [selected assessment](afta-selected-requirements-review.md), [row-by-row code/test register](afta-requirement-traceability.md) and [authoritative checklist data](../evidence/afta-selected-checklist-2026-11-07.json) for every entry and its exact evidence boundary. The dated sections below preserve the implementation history.

<!-- AFTA_CURRENT_PROGRESS_END -->

## 23 November follow-up: PKCE failure and parameter policy

**Selected source rows: 20 (`FDP_RIP.2.1`), 46 (`FPT_FLS.1.1`), 48 (`FPT_TDC.1.1`).** Generated PKCE fails closed on S256 downgrade, rejects reserved extra overrides and clears custom verifier bytes. [Code, 22 passing checks and operator compatibility requirements](afta-pkce-failure-policy.md) are recorded. Statuses remain Gap/Partial/Gap. Activity now records **45 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November gap follow-up: generated secret characters

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear email-code and shared secure-token wrapper-owned random characters after conversion; retain output strings, expiry and caller character ownership. [Code and 76 passing checks](afta-generated-secret-characters.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **44 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November gap follow-up: audit codec buffers

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear codec-owned plaintext and current compressed output allocations, preserving caller payloads and legacy error/conversion behavior. [Code, 40 unit checks and 25 PostgreSQL/JMS checks](afta-audit-codec-buffers.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **43 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November gap follow-up: QR-owned buffers

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear temporary QR UTF-8 input, current PNG backing allocation and owned bit matrix, preserving returned PNG and existing error/normalization behavior. [Code and 66 passing checks](afta-qr-owned-buffers.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **42 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November gap follow-up: replay fingerprint challenge bytes

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear the fingerprint-owned UTF-8 challenge array after digest success/failure, retaining digest and replay behavior. [Code, 59 unit checks and 13 PostgreSQL replay checks](afta-replay-fingerprint-buffer.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **41 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November follow-up: TOTP QR failure diagnostics

**Selected source row: 46 (`FPT_FLS.1.1`).** Preserve checked QR-generation failure response (E3026 and empty image) while omitting exception diagnostics that could carry the provisioning URI. [Code and 53 passing checks](afta-totp-qr-failure.md) include real PNG decode compatibility. Row 46 remains Partial evidence. At this follow-up, activity reached **40 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November follow-up: TOTP failure diagnostics

**Selected source row: 46 (`FPT_FLS.1.1`).** Preserve proof rejection while emitting fixed decoder/verifier failure diagnostics without attached exceptions. [Code and 50 passing checks](afta-totp-failure-diagnostics.md) are recorded. Row 46 remains Partial evidence. At this follow-up, activity reached **39 implementation commits across 27/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 23 November gap follow-up: TOTP enrollment raw seed

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear enrollment-owned random seed bytes after encoding or encoder failure, before user mutation/persistence. [Code and 48 passing checks](afta-totp-enrollment-buffer.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **38 implementation commits across 26/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: TOTP validation secret bytes

**Selected source row: 20 (`FDP_RIP.2.1`).** Clear the helper-owned decoded TOTP precheck array on accepted/rejected input, preserving independent OTP-library verification. [Code and 44 passing checks](afta-totp-validation-buffer.md) are recorded. Row 20 remains Gap. At this follow-up, activity reached **37 implementation commits across 26/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: audit event type and enums

**Selected source rows: 2 (`FAU_GEN.1.2`) and 48 (`FPT_TDC.1.1`).** Reject numeric type/scope enums and require event type before audit sinks. [Code, 51 unit checks and four PostgreSQL/JMS checks](afta-audit-event-type.md) are recorded. Statuses remain Partial and Gap. At this follow-up, activity reached **36 implementation commits across 26/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: audit destination/scope binding

**Selected source row: 48 (`FPT_TDC.1.1`).** All four receiving audit consumers now reject mismatched/missing scopes and null root records before sinks run. [Code, 35 unit checks and four PostgreSQL/JMS checks](afta-audit-destination-scope.md) are recorded. Full publisher trust/schema policy remains Gap. At this follow-up, activity reached **35 implementation commits across 26/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: audit message ambiguity

**Selected source row: 48 (`FPT_TDC.1.1`).** Reject duplicate fields and trailing JSON before audit sinks, without changing the shared mapper. [Code, 11 unit checks and four PostgreSQL/JMS checks](afta-audit-message-validation.md) are recorded. Full peer/schema/conflict policy remains Gap. At this follow-up, activity reached **34 implementation commits across 26/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: audit payload failures

**Selected source rows: 1 (`FAU_GEN.1.1`) and 52 (`FRU_FLT.1.1`).** Serialization errors now stop publication instead of producing a null payload, with a fixed failure marker that excludes serializer exception details. [Code and 30 passing tests](afta-audit-payload-failure.md) cover all scopes and both direct/buffered paths. Statuses remain Partial and Gap. At this follow-up, activity reached **33 implementation commits across 25/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 21 November gap follow-up: audit destination failures

**Selected source row: 52 (`FRU_FLT.1.1`).** Missing or blank destinations now propagate publication failure, allowing the optional scheduler to retain the pending event. [Code and 19 passing tests](afta-audit-destination-failure.md) include retry after in-process mapping repair. Row 52 remains Gap. At this follow-up, activity reached **32 implementation commits across 25/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 18 November gap follow-up: concurrent audit-buffer capacity

**Selected source row: 52 (`FRU_FLT.1.1`).** Made optional audit-buffer capacity and duplicate admission atomic with processing/requeue. A pre-fix concurrent run reached 202 entries against the 200 limit; all 12 queue/scheduler cases pass after the fix. See the [code trace and evidence](afta-audit-buffer-capacity.md). Row 52 remains Gap. At this follow-up, activity reached **31 implementation commits across 25/78 primary entries**, plus one optional entry; status stays **45 partial, 8 gaps, 25 unresolved; 0 closed**.

## 18 November gap follow-up: monotonic audit-buffer delays

**Selected source row: 49 (`FPT_STM.1.1`).** Corrected expiry ordering and wall-clock-dependent elapsed time in the optional audit buffer. [Eight tests and code trace](afta-audit-monotonic-delay.md) cover queue expiry and scheduler retry compatibility. Row 49 remains Gap; current status totals stay **45 partial, 8 gaps, 25 unresolved; 0 closed**. At this follow-up, activity reached **30 implementation commits across 24/78 primary entries**, plus one optional entry.

## 18 November gap follow-up: audit-record integrity

**Selected source rows: 27 (`FDP_SDI.2.1`) and 28 (`FDP_SDI.2.2`).** Implemented opt-in keyed integrity tags for the consolidated `audit` table, verification before decoding, ERROR alerts and blocked reads/counts on tampering. See the [code, execution evidence and rollout conditions](afta-audit-record-integrity.md). Both rows move **Gap → Partial evidence**; default-off deployment, trusted legacy-history migration, other audit stores and remaining acceptance stay open. Current primary status: **45 partial, 8 gaps, 25 unresolved; 0 closed**. At this follow-up, linked activity reached **29 commits across 23/78 primary entries**, plus one optional entry.

## 18 November gap follow-up: generation-owned token storage

**Selected source row: 20 (`FDP_RIP.2.1`).** Token generation now clears its
random-code/checksum components after assembly or failure, and clears assembled
plaintext when wrapper creation fails before ownership transfer. Successfully
returned plaintext remains available to the caller. See the
[generation ownership trace](afta-token-generation-buffer-hygiene.md).
Row 20 stays Gap; full physical/runtime/storage residual protection is unproven.
This adds another implementation commit to the same linked primary entry.

## 16 November gap follow-up: hash/CRC encoding storage

**Selected source row: 20 (`FDP_RIP.2.1`).** SHA-256/SHA-512 string/character
helpers and CRC processing now clear their own encoded plaintext storage. Caller
byte/character inputs retain their ownership contract. Real digest vectors and
token checks retain compatibility coverage; see the [UTF-8 buffer trace](afta-hash-crc-buffer-hygiene.md).
This is best-effort buffer hygiene; row 20 stays Gap. The commit count rises for
an already linked row, without increasing the distinct primary-entry count.

## 16 November gap follow-up: validator-owned token copies

**Selected source row: 20 (`FDP_RIP.2.1`).** Both token checksum validators
now clear their extracted code/checksum arrays after valid, rejected and throwing
validation outcomes, retaining caller-owned input. See the
[six-case lifecycle trace](afta-token-checksum-buffers.md). Row 20 stays Gap: this
is best-effort buffer hygiene, not a JVM or full protected-resource erasure proof.
This adds a commit to an already linked row rather than another covered entry.

## 16 November gap follow-up: resolver-owned raw-token buffer

**Selected source row: 20 (`FDP_RIP.2.1`).** The resolver overwrites its mutable
raw-token array after checksum/hash processing on success and failure. See the
[buffer lifecycle trace and four baseline cases](afta-token-secret-buffer.md).
This is best-effort hygiene only; immutable request/runtime copies and the
complete memory/file/media/replica/backup boundary remain unproven. Row 20 stays
**Gap / evidence not established**. The progress chart records the bounded work
without changing the full-requirement evidence/closure totals.

## 16 November follow-up: token failure boundary

**Selected source rows: 1 (`FAU_GEN.1.1`), 29 (`FIA_AFL.1.1`).**
The token filter now handles only its own token-processing exceptions as token
rejections. Success publication and downstream processing run outside that catch.
See the [per-row boundary trace](afta-token-failure-boundary.md). Both rows remain partial.

## 16 November follow-up: OIDC event UID binding

**Selected source rows: 29 (`FIA_AFL.1.1`), 37 (`FIA_USB.1.1`),
38 (`FIA_USB.1.2`), 39 (`FIA_USB.1.3`).** The listener resolves mapped principals
by UID and retains that entity for success bookkeeping; failures use its current
name. Missing/deleted mapped owners have no name fallback. See the
[per-row change trace](afta-oidc-event-uid-binding.md). All four rows remain partial.

## 16 November follow-up: login success connection context

The replacement success token now retains request address/session context, using
a copied factor-details object with the submitted OTP cleared. The incoming
request object remains unchanged. See the [success-context trace](afta-login-success-context.md).
This supports row 2; full audit content and deployment acceptance remain open.

## 16 November follow-up: rejected MFA proof events

The publisher explicitly maps the custom MFA rejection exception to a credential
failure event, enabling existing audit and atomic account-counter listeners.
Successful email challenge issuance remains separate. See the
[publisher and HTTP lockout trace](afta-mfa-failure-events.md).
Selected rows 1–3, 29–30 and 36 remain partial; complete failure policy is open.

## 16 November follow-up: email login challenge UID

The login provider now routes email challenge issuance by the principal UID.
Missing/deleted owners fail without a mutable-name fallback, while the existing
entity helper retains the selected recipient and secret update. See the
[login UID trace and regression scope](afta-login-email-challenge-uid.md).
Rows 36–39 remain partial; issuance state races and full policy remain open.

## 16 November follow-up: email disable challenge identity

The empty-code email MFA disable branch now retains its selected account through
verified-email checks, challenge generation, delivery and persistence, avoiding
a repeated mutable-name lookup. See the [target-continuity trace](afta-email-challenge-identity.md).
Rows 37–39 remain partial; other caller and same-account race limits remain open.

## 16 November follow-up: TOTP provisioning URI

The provisioning generator now percent-encodes issuer, account and secret values
to keep URI delimiters inside their data components. See the
[code, parser regressions and execution evidence](afta-totp-provisioning-uri.md).
This supports rows 35–36 without closing the full mechanism requirements.

## 15 November follow-up: current-user and MFA UID binding

Rows 29 and 37–39 have a concrete name-reuse fix. Persisted current-user entity
arguments now follow the authenticated UID, and self-MFA handlers pass that entity
to the service without repeating name resolution. Reset retains its selected
entity, and failed-disable counting uses the target's current name. Required
missing owners are denied without a namesake fallback. See the
[identity-routing trace and baseline HTTP reproduction](afta-current-user-uid-binding.md).
Full session/provider attribute freshness and same-account races remain open.

## 15 November follow-up: atomic failure counters

Rows 29 and 30 have a concurrent-counting fix. Login, recovery and MFA-disable
failures now use atomic local/Redis cache updates rather than a separate read
and write. The configured thresholds, feature flag and cache expiry remain.
See the [counter implementation, compatibility and Redis permission requirements](afta-atomic-failure-counters.md).
Complete provider/reset policy and deployed cluster/failover acceptance remain open.

## 15 November follow-up: OIDC event account association

Rows 29 and 37 have a lifecycle-listener fix: optional OIDC HTTP details no
longer interrupt failed/successful-login bookkeeping, and completed OAuth events
use their resolved local DHIS2 identity rather than an external subject name.
Unmapped success events do not mutate a local namesake. See the [bounded event
association trace](afta-oidc-event-account-association.md). Provider acceptance and full selected row definitions remain open; the later
atomic-counter follow-up addresses the separate per-key update race.

## 15 November follow-up: global session invalidation

Row 39 has a concrete revocation fix: global invalidation retains expired
session markers so the session filter can reject an existing session's next
request. The admin sessions API now uses the shared corrected service and retains
its existing authority requirement. See the [implementation and deployment limits](afta-global-session-invalidation.md).
The row remains partial; full selected revocation timing and cross-node/provider
acceptance are not established.

## 15 November follow-up: authentication address provenance

Row 2 has a further audit-field fix: password/MFA and HTTP Basic request details
now use the servlet-resolved connection address instead of raw forwarding headers.
This prevents direct request headers from supplying or erasing that audit field.
See the [address-source trace and proxy compatibility conditions](afta-authentication-address-source.md).
Explicit-value/session constructors and the earlier API-token event fix remain.
The row stays partial pending the selected event policy and deployed evidence.

## 15 November follow-up: API-token failure audit events

Rows 1–3 now have explicit application-event mappings for invalid, expired and
constraint-rejected API tokens. Resolver failures join the same audit path;
resolved owner names and servlet connection details are retained without the
token secret or full account object. Token rejections do not increment password
failure counters. See the [implementation and verification boundary](afta-token-failure-audit.md).
The selected rows remain partial.

## Latest follow-up: authentication audit records

Authentication event logging now retains available failure addresses and Unicode
identities, handles optional details without unsafe casts, and escapes supplied
line/field boundaries consistently. See the [audit-field trace and remaining
storage/policy limits](afta-authentication-audit-records.md).

## 13 November follow-up: token IP constraints and raw forwarding headers

Rows 16, 17 and 19 have a concrete access-control fix: token IP rules now use
the servlet remote address rather than trusting raw `X-Forwarded-For`. Two
baseline assertions reproduce the header-trust defect; all 37 targeted
filter/token/audit-review cases pass with the fix. Proxy deployments need trusted
server-side client-address resolution before accepting IP-constrained tokens.
See the [implementation, compatibility notes and execution evidence](afta-token-ip-constraints.md).
The selected rows remain partial.

## 13 November follow-up: audit-review paging and optional filters

Selected rows 4, 5 and 7 have two concrete controller fixes: requested paging
now reaches the audit query and response metadata; omitting the optional entity
filter no longer causes a null-pointer exception. Seven new cases reproduce six
failed assertions and one error on the preceding controller. With the fixes,
all 30 targeted audit-review and token cases pass. See the
[code/test trace, dated evidence and remaining acceptance](afta-audit-review-paging.md).
The rows remain partial; complete AFTA fit is not asserted.

## 12 November follow-up: stable PAT owner binding

Rows 37–39 now have a concrete identity-binding fix: a token's reloaded account
must have the same stable UID as its owner. Missing or mismatched identities
reject; a legitimate rename retains access. Three baseline denial assertions
failed before the fix; all 23 targeted manager/resolver cases pass after it.
See the [code, tests, execution evidence and remaining boundaries](afta-token-owner-binding.md).
The selected rows remain partial; full checklist percentages do not change.

## 11 November follow-up: stable pending TOTP QR reads

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
| Integrity/residual protection, reliable time, fault tolerance and access history | Audit-record integrity now has the bounded implementation above; the remaining selected guarantees need designs, deployment and acceptance evidence per checklist row. |
| Extensions, iterations and edition mismatches | Exact AFTA document/version and clause definitions remain missing. No behavior or algorithm is assigned to an unknown extension/crypto iteration by guesswork. |

For current checklist totals and implementation activity, see the
[current progress snapshot](#current-progress-snapshot). The external broker transport tests
add partial evidence for one previously untraced item. They do not close the whole
item. Earlier MFA fixes corrected subgaps within an already partial requirement.
