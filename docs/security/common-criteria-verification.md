# Common Criteria review: execution evidence

Review date: 2026-11-03. Baseline: `07d4f3f7ea` plus the working-tree changes
listed in [the review](common-criteria-review.md). No certification claim.

Current validated inventory: **211 passed, 3 existing skips, 0 failures/errors,
26 classes**. Historical executions and their limits are retained below.

## Environment and provenance

- Maven 3.9.11; locally provisioned Eclipse Temurin JDK 17.0.17+10 for compilation.
- The initial host Java 21 runtime lacked a full `javac`; its compile attempt
  failed before tests. This is not a passing baseline.
- Maven Central and OSGeo dependencies resolved through the configured proxy.
- The environment disallows JitPack access. For local verification only, the
  unmodified `dhis2/sms-compression` tag `0.3.2` sources were fetched from GitHub
  and compiled/tested using a temporary Maven descriptor with dependencies from
  the release's `build.gradle`. The result was installed into the local dependency
  cache as `com.github.dhis2:sms-compression:0.3.2`. No project source or dependency versions
  were changed; no stubs were used. This source build is not asserted binary-identical
  to JitPack's artifact; release CI should use its normal approved artifact.
- Docker checks use the managed local daemon and disposable test containers.

## Failure reproduction

`AuditSchedulerTest` on the original scheduler: 4 tests, 2 failures, 0 errors,
0 skipped. Both failure/recovery cases found the queue empty after a producer
exception instead of retaining the pending events. After the scheduler fix,
all 4 cases passed in the subsequent build.

`ApiTokenAuthManagerTest` on the original manager: 10 tests, 9 failures,
0 errors, 0 skipped. Failures covered account changes, owner/token deletion,
updated constraints and request isolation. The pre-existing token-expiry check passed.
Two malformed-token cases were then added (missing expiry and missing owner).

## Executed regressions

- Focused unit suite: **83 tests, 0 failures/errors/skipped** across 15 classes.
  This includes 12 API-token state/isolation cases, 4 buffer cases, 4 broker
  configuration/restart/redelivery cases, 7 audit compression/write-failure cases,
  27 password/session unit cases, and 29 other authentication/browser-filter cases.
- Web-controller H2 suite: **36 tests, 0 failures/errors/skipped** across
  `ApiTokenAuthenticationTest` (13), `AuthenticationControllerTest` (16),
  `LoginConfigCorsTest` (4), `SessionControllerTest` (2), and
  `UserRoleSessionInvalidationControllerTest` (1). Tests include valid PAT use,
  account disablement after prior successful use, changed method constraints,
  invalid/expired tokens, form login, session controls and login CORS bootstrap.
- These web tests use the Spring security/filter/controller stack with H2. They
  do not establish browser rendering or PostgreSQL equivalence.

- PostgreSQL suite: initially **87 executed/passed, 3 skipped**, 0 failures/errors.
  `AclServiceTest`: 61 total, 60 passed, 1 existing disabled null-owner case
  (`DHIS2-17768`). `PasswordManagerTest`: 1 passed. `AuditRepositoryTest` initially:
  28 total, 26 passed, 2 existing disabled 200,000-row performance cases.
- After adding deterministic database rejection/recovery coverage,
  `AuditRepositoryTest` was rerun: **29 total, 27 passed, 2 skipped**, 0 failures/errors.
  The new rejected-write/recovery case passed against PostgreSQL 16/PostGIS in a
  disposable Testcontainers database. It does not prove disk-full or network-outage
  behavior or committed-record preservation across restart.
- Current unique-case total across these suites: **207 passed, 3 skipped**,
  0 failures/errors. The repeated audit run is not counted twice.

## Production build and runtime checks

The production executable WAR build succeeded with the `embedded,unit-test`
profiles. `ApiTokenAuthManagerTest` was rerun after the final permission-isolation
assertions/formatting: all 12 cases passed. All 30 declared browser app bundles
were downloaded, their ZIP integrity checked, and registered with `appState=OK`.
Bundle versions, upstream commits (when published) and SHA-256 hashes are recorded
in the evidence inventory linked below.

The built WAR started successfully on Java 21, accessed at a loopback test endpoint,
using isolated PostgreSQL 16/PostGIS, a filesystem file store, embedded persistent
Artemis, database metadata auditing enabled and the volatile audit buffer disabled.
This local HTTP smoke profile has CSRF at its default off setting and is not the
evaluated production TLS/CSRF profile.

Chromium/Playwright checks:

- Login app rendered, signed in with the disposable database's initial account,
  reached Dashboard, and retrieved authenticated `/api/me` with HTTP 200 and no
  page errors during that login flow.
- All 30 app entry pages responded HTTP 200. That is an entry-page check, not a
  claim that every application workflow works.
- The first empty-database check found four page-error apps. Adding a test root
  organisation unit and assigning it to the isolated administrator removed the
  errors in the legacy event reports and event visualizer apps. Messaging and
  Interpretations still reported JavaScript errors. See the baseline comparison below.
- API datastore create/read/update/delete and post-delete absence checks passed.
- A real persisted API token authenticated successfully; after deletion, reuse
  returned HTTP 401. No plaintext token or browser session is stored in repository
  evidence.
- Optional custom-logo/datastore probes returned HTTP 404 where no custom data
  existed. The custom-logo JSON endpoint intentionally returns 404 without a
  configured custom file; these are distinct from uncaught page errors.
- App Hub returned HTTP 503 and a scheduled version check could not reach its
  external service in this restricted environment. Those external-dependent
  workflows were not verified.

Build command (from `dhis-2`):

```bash
mvn -B -ntp -pl dhis-web-server -am -Pembedded,unit-test \
  -Dtest=ApiTokenAuthManagerTest -Dsurefire.failIfNoSpecifiedTests=false package
```

The local build additionally used the configured JVM proxy and CA trust for
bundled-app downloads. The WAR is generated under
`dhis-2/dhis-web-server/target/dhis.war`; generated binaries are not committed.

## Static verification

All changed Java files passed the repository's Spotless check. Workflow YAML and
both embedded report-verification Python scripts parsed successfully. Relative
document links resolved, and all 35 referenced functional component IDs were
found in the supplied 2026 Part 2. This verifies references, not conformance.

## Reproduction commands

From `dhis-2`, using a supported JDK and Maven plus an accessible Docker daemon:

```bash
mvn -B -ntp -pl dhis-web-api,dhis-services/dhis-service-audit-consumer -am -Punit-test \
  -Dtest=ApiTokenAuthManagerTest,ApiTokenResolverTest,AuditSchedulerTest,JdbcAuditRepositoryCompressionTest,DefaultAuditServiceFailureTest,AbstractAuditConsumerFailureTest,ArtemisAuditBrokerIntegrationTest,ArtemisAuditRedeliveryTest,CspFilterTest,LoginConfigCorsFilterTest,AuthenticationListenerTest,UserSessionInvalidationTest,UserRoleSessionInvalidationListenerTest,PasswordValidationServiceTest,PasswordValidationRuleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -B -ntp -pl dhis-test-web-api -am -Pintegration-h2-test \
  -Dtest=ApiTokenAuthenticationTest,ApiTokenStateAuthenticationTest,AuthenticationControllerTest,LoginConfigCorsTest,SessionControllerTest,UserRoleSessionInvalidationControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -B -ntp -pl dhis-test-integration -am -Pintegration-test \
  -Dtest=AuditRepositoryTest,AuditRepositoryFailureRecoveryTest,AuditStorageDeliveryRecoveryTest,PasswordManagerTest,AclServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Local invocations additionally selected the workspace Maven settings/cache and
provisioned JDK described above. Inspect Surefire XML reports, rather than only
the Maven exit code, to confirm every expected class actually ran. The expanded
[CI workflow](../../.github/workflows/cc-audit-regression.yml) checks required
report presence and rejects failures, errors and fully skipped classes. The
workflow has been edited locally; no new hosted CI result is claimed.

## Baseline comparison for browser findings

Both modified production classes (`ApiTokenAuthManager` and `AuditScheduler`)
were restored from baseline Git source into a temporary class directory, compiled
and placed first on the runtime classpath. The baseline replay used the same
dependencies, bundle snapshot, disposable database and app configuration. Both
Messaging and Interpretations reproduced the same errors. This is a controlled
comparison of the changed backend code, not a separate certified baseline build.

The Messaging bundle dereferences `configuration.feedbackRecipients.id` without
a null guard, while the fresh database has no feedback recipient group. Configuring
a disposable feedback-recipient group removed that page error in the final
patched-runtime run. The
Interpretations `v1.2.0` bundle's `index.html` explicitly loads `268.bundle.js` as
a page script; that chunk calls worker-only `importScripts`. This remaining error is
not resolved by the authentication or audit-buffer changes. The current published
default-branch Interpretations bundle was inspected as well and contains the same
worker-script reference, so it was not substituted for the pinned release.

Machine-readable [test/build/browser evidence](evidence/review-2026-11-03.json)
and [app versions, source commits and hashes](evidence/web-app-inventory-2026-11-03.json)
are retained in the project. Sensitive runtime configuration, cookies and tokens
are excluded. Remaining browser acceptance work is tracked in G-09/G-12.

Final patched-runtime recheck with the organisation-unit and feedback-recipient
fixtures: **30/30 entry pages returned HTTP 200; 29/30 reported no JavaScript page
errors**. The remaining Interpretations worker-script error reproduces with the
original backend classes and is tracked in G-12. Full browser workflow acceptance
and formal Common Criteria approval remain open; neither is implied by these
smoke results. The temporary runtime and disposable database were stopped after
verification; the built WAR remains in the ignored build directory.

## Upstream compatibility follow-up

To reduce future merge overlap, the two new API-token integration cases were moved
to `ApiTokenStateAuthenticationTest`, and the new PostgreSQL rejection/recovery
case to `AuditRepositoryFailureRecoveryTest`. The upstream README and the original
integration-test files were restored to their pre-review contents. Fork entry-point
documentation is in `README.fork.md`; the upstream maintenance guide identifies
the two required production patches. No branch, remote, dependency or bundled-app
selection was changed. Historical counts above describe the original execution
layout; follow-up results for the separate classes are recorded in the evidence.

Follow-up execution after isolation: `ApiTokenAuthenticationTest` (11) and
`ApiTokenStateAuthenticationTest` (2) passed all **13 web cases**.
`AuditRepositoryTest` (28 total, 26 passed, 2 existing disabled performance cases)
and `AuditRepositoryFailureRecoveryTest` (1 passed) passed **27 PostgreSQL cases**.
Both separate test files passed Spotless. The current evidence contains **25 test
classes, 207 passed cases and 3 existing skips**; the previous class allocation is
retained as historical evidence. Production sources and the previously built WAR
are unchanged by this follow-up, so the earlier production/browser results retain
their stated scope.

## Integrated audit delivery and database faults

`AuditStorageDeliveryRecoveryTest`: **4 passed, 0 failures/errors/skips** in its
standalone execution. It uses the production common Jackson configuration, audit
consumer, audit service/repository and transacted/shared/durable listener factory
with a real broker and disposable PostgreSQL. See the
[detailed scenarios and limits](common-criteria/audit-delivery-failure-evidence.md).
The existing SQL rejection/savepoint case and upstream repository regressions are
also run together with this new fixture before recording final combined results.
No additional production source, dependency or app-bundle change was needed.

An initial combined invocation could not initialize its second PostgreSQL
container because the shared Docker filesystem ran out of space; the new class
failed during setup, not in its audit cases. Redundant generated WAR intermediates
were removed while preserving the executable WAR, and the isolated fault fixture
now uses a bounded 512 MiB tmpfs data volume. That infrastructure incident is not
an audit disk-exhaustion test or evidence for FAU_STG.4/5.

Final combined run after the fixture storage adjustment: **33 total cases,
31 passed, 2 existing performance skips, 0 failures/errors** across the upstream
repository suite and both fork recovery classes. The new standalone test file
passes Spotless; the fork CI now requires nine audit test-class reports. The
current complete evidence inventory is **211 passed and 3 existing skips across
26 classes**. Production-source hashes and the retained WAR hash are unchanged
by this test/evidence extension.

## Component-ID lookup documentation

The [component-ID → code index](common-criteria-component-code-index.md) and its
[CSV copy](common-criteria-component-code-index.csv) enumerate all **155 Part 2
functional IDs and 106 Part 3 assurance IDs** from the supplied 2026 documents.
There are **37 functional candidate code/evidence traces** and **10 assurance
artifact candidate traces** in the original selective matrix. The 2026-11-04
[gap assessment](common-criteria-gap-assessment.md) now gives all 261 components
an evidence basis, gap/action and proposed owner. It corrects five omitted
composite assurance components. This is inventory completeness and component gap
triage, not complete element-level implementation coverage.

The [FMT_MSA.3 element-level trace](common-criteria/component-traces/FMT_MSA.3.md)
identifies exact default-initialization/creation/authorization methods and code
excerpts for FMT_MSA.3.1 and FMT_MSA.3.2. Eight named supporting `AclServiceTest`
cases were checked against the existing passing Surefire XML. No new test run or
production behavior change is claimed for this documentation-only follow-up.

## 2026-11-04 documentation consistency verification

The documentation follow-up adds component dispositions and source findings for
encryption configuration and two-factor authentication. The standard identifier,
clause and short-title sets were compared with headings extracted from all supplied
Part 2 and Part 3 PDFs: 155 functional and 106 assurance components, including
`ASE_COMP.1`, `ADV_COMP.1`, `ALC_COMP.1`, `ATE_COMP.1` and `AVA_COMP.1`.

Regenerate the index, CSV and gap report from the manually maintained disposition
JSON with `python docs/security/tools/render_common_criteria_review.py`. Use
`--check` to validate required fields, unique IDs/counts, source/test file existence
and generated-document consistency. This checker does not determine conformance.

No production sources or test sources changed during this follow-up. Existing
source hashes and the retained production WAR were checked against the recorded
2026-11-03 evidence. The 211 passes and three skips are previous execution
evidence, not newly executed results. Encryption migration, single-use replay,
factor-toggle browser behavior and a complete independent evaluation remain open.

## 2026-11-04 web-application verification extension

Following the user's web-application scope direction, existing upload/app/query
tests were executed with the unchanged source and test code. The two offline
Maven invocations used Temurin 17.0.17+10 and Maven 3.9.11 with the existing
workspace cache. The JDK archive checksum was verified against its release file.

| Suite | Total | Passed | Skipped | Failures/errors |
| --- | ---: | ---: | ---: | ---: |
| `FileResourceUtilsTest` | 6 | 6 | 0 | 0 |
| `SqlViewUtilsTest` | 5 | 5 | 0 | 0 |
| `AppControllerTest` | 24 | 23 | 1 | 0 |
| `FileResourceControllerTest` | 9 | 9 | 0 | 0 |
| `SqlViewControllerTest` | 8 | 8 | 0 | 0 |
| **Extension total** | **52** | **51** | **1** | **0** |

The existing skipped case is `AppControllerTest.redirectLocationTest`, marked
deprecated pending root-path serving of bundled app overrides. It is not a pass.
Passing app cases include encoded/direct/parent path traversal and semicolon
bypass rejection, ZipSlip/ZipBomb handling, legitimate nested resources and
unauthorized app reload. The [machine-readable execution record](evidence/review-2026-11-04.json)
contains all case names, counts, skip reason, source and report hashes.

The first controller invocation failed during Spring context setup because
the sandbox denied socket creation required by the broker's available-port
finder. No controller test body established behavior in that run. The rerun
with network permission for the test command passed; no application/test code
was changed to bypass setup.

Reproduce from `dhis-2`, adding local toolchain/settings/cache options as needed:

```bash
mvn -o -B -ntp -pl dhis-web-api,dhis-services/dhis-service-administration -am -Punit-test \
  -Dtest=FileResourceUtilsTest,SqlViewUtilsTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -o -B -ntp -pl dhis-test-web-api -am -Pintegration-h2-test \
  -Dtest=AppControllerTest,FileResourceControllerTest,SqlViewControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Offline mode requires the existing dependency cache; remove it when resolving
dependencies in a normally connected development environment. The embedded
broker needs permission to create local sockets.

Across both dated records, the evidence inventory is **262 passed, 4 existing
skips, 0 failures/errors, 31 classes** (266 total cases). This is the union of
recorded suites, not a single complete-suite run. The original 211 passes and
three skips remain historical evidence. Production-source and retained WAR
hashes still match that successful build. No new WAR/browser run is asserted.

The [web attack-surface review](common-criteria/web-attack-surface-review.md)
identifies what these tests cover and what remains open. These checks do not
close all injection, file-domain authorization, browser-app or dependency gaps.


## 2026-11-09 AFTA authentication remediation

The enrolled-factor downgrade in checklist row 36 / FIA_UAU.5.2 / G-15 is
corrected in the existing provider. The new provider-policy suite reproduces
eight failures on the original implementation and passes all 19 cases after
the patch. Six new full-security-chain H2 API checks and 43 existing
authentication/two-factor controller cases also pass (49 web cases total, no
failures/errors/skips). The successful email fixture now explicitly enables and
restores the factor under test; it previously depended on the downgrade.

These 68 targeted cases overlap some earlier suites and are not added blindly
to the historical 262-pass total. They do not establish complete AFTA fit or
new browser, recovery, replay or multi-node acceptance. Details, commands and
limits: [remediation progress](common-criteria/afta-remediation-progress.md) and
[dated evidence](evidence/afta-remediation-2026-11-09.json).


## 2026-11-09 login replay protection follow-up

Login proof acceptance now requires a committed database reservation. Targeted
provider, deterministic H2 transaction/race, real security-chain API and isolated
PostgreSQL/Flyway tests exercise the implementation. These overlap earlier
classes and must not be added blindly to the earlier passing-case totals.
The migration is an additive repeatable script in the existing Flyway root,
under a fork-specific subdirectory; no numbered upstream migration is changed.

See [implementation and upgrade limits](common-criteria/afta-login-replay-protection.md)
and [source hashes, final test counts and packaging evidence](evidence/afta-replay-protection-2026-11-09.json).
This does not establish other-purpose single use, reliable time, snapshot
recovery, full deployed multi-node/browser acceptance or complete AFTA fit.

## 11 November 2026: factor-state freshness follow-up

The [factor-state trace](common-criteria/afta-factor-state-freshness.md) records
primary-row validation under a lock before login-proof consumption. The new
[evidence inventory](evidence/afta-factor-state-freshness-2026-11-11.json) records
102 passing targeted checks, successful embedded WAR packaging with existing
browser assets, packaged-class identity and PDF dependency verification. The
counts overlap earlier inventories; deployed browser/multi-node acceptance and
full AFTA requirements remain open.

## 11 November 2026: optional external broker TLS

The [broker TLS trace](common-criteria/afta-external-broker-tls.md) and
[evidence inventory](evidence/afta-external-broker-tls-2026-11-11.json) record
118 passing targeted cases, including a real negotiated-TLS JMS exchange and
certificate/credential/transport rejection. Embedded WAR packaging, changed
packaged-class identity and the PDF check pass with existing browser assets.
This is conditional evidence for one link, not site deployment acceptance or
full `FPT_ITT.1.1` closure. The scoped item moves to Partial evidence; charts and
trace counts are regenerated from the assessment data.

## 11 November 2026: current account state during MFA consumption

The [account-state trace](common-criteria/afta-account-state-freshness.md) and
[evidence inventory](evidence/afta-account-state-freshness-2026-11-11.json) record
114 passing authentication/database cases and a six-failure deterministic
regression comparison against the preceding guard. PostgreSQL tests cover
locked-row ordering for password/disabled/external-only/expiry updates. WAR
packaging with existing browser assets, guard/result-record/provider byte identity
and PDF checks pass. Existing sessions, other providers and complete selected
session policy remain open. Checklist dispositions are unchanged.

## 11 November 2026: explicit platform encryption-key startup policy

The [explicit-key trace](common-criteria/afta-explicit-encryption-key.md) and
[evidence inventory](evidence/afta-explicit-encryption-key-2026-11-11.json) record
112 passing core checks, 24 platform tests and four isolated entrypoint render
checks. WAR packaging, packaged-class identity and PDF checks pass. Strict-mode
startup rejects missing/default keys; cipher/key migration and AFTA crypto
approval remain open. No existing key or site ciphertext is changed.

## 11 November 2026: stable pending TOTP QR reads

The [QR trace](common-criteria/afta-pending-qr-stability.md) and
[evidence inventory](evidence/afta-pending-qr-stability-2026-11-11.json) record
109 passing targeted authentication checks. Eight QR cases reproduce two
seed-stability failures on the preceding handler and pass with the fix.
WAR/controller identity, unchanged packaged profile ZIP and PDF checks pass.
Initial legacy GET still changes state; full CSRF and enrollment-race acceptance
remain open. Checklist dispositions are unchanged.

## 13 November 2026: authentication audit record fields

The [audit-field trace](common-criteria/afta-authentication-audit-records.md) and
[evidence inventory](evidence/afta-authentication-audit-records-2026-11-13.json)
record 174 passing combined audit, token, review and authentication/API checks. A
14-case prior-listener subset reproduces 13 failures; the final audit suite has
15 passing cases. The 12 November token-owner/traceability changes are preserved.
WAR/listener/token-manager byte identity and PDF checks pass. Required-event
policy, durable storage and deployed acceptance remain open; percentages do
not change for this improvement within already partial items.

## 15 November 2026: API-token failure audit events

The [token failure audit trace](common-criteria/afta-token-failure-audit.md) and
[dated evidence](evidence/afta-token-failure-audit-2026-11-15.json) record 62 passing
checks across five token/filter, authentication-listener and audit-message suites,
with no failures/errors/skips. The new filter/listener cases produce six assertion
failures among 24 cases against the preceding production code. Patched production
files were restored and the final 62-case rerun passed. The actual configured
Spring publisher emits the new token failure events; token failures retain safe
identity/connection fields and do not count as failed password/MFA proofs.

The initial host runtime could not compile Java release 17, so a workspace JDK
was provisioned. JitPack denied the existing SMS dependency; its unchanged 0.3.2
upstream source tag was built/tested into the local cache using the same documented
source-build workaround. No dependency declarations changed. Evidence records
these conditions rather than asserting a cold release build. No WAR, hosted CI,
production deployment or complete AFTA requirement closure is claimed. Checklist
statuses and totals are unchanged; prior suite counts are not summed with these
overlapping executions.

## 15 November 2026: authentication audit address source

The [address-source trace](common-criteria/afta-authentication-address-source.md)
and [dated evidence](evidence/afta-authentication-address-source-2026-11-15.json)
record 88 passing checks across six authentication/audit/token suites, with no
failures/errors/skips. Four new audit-address assertions fail against the preceding
HTTP-request detail constructor (20 audit cases total). The patched constructor
uses the servlet-resolved address rather than raw forwarding headers. Existing
explicit-value/session constructors and the earlier token-failure audit events
are preserved.

The tests establish request-constructor/message behavior in controlled fixtures;
they do not verify production proxy ingress, collector trust or complete AFTA
coverage. Counts overlap earlier records and are not added to them. The existing
workspace JDK/source-built SMS dependency conditions remain. No WAR packaging,
hosted CI or new deployment acceptance is claimed. Checklist dispositions remain
unchanged.

## 15 November 2026: global session invalidation

The [global revocation trace](common-criteria/afta-global-session-invalidation.md)
and [dated evidence](evidence/afta-global-session-invalidation-2026-11-15.json)
record **31 passing checks**: ten core session/role-revocation units and 21 H2
session/authentication API cases, with no final failures/errors/skips. Four new
core assertions fail against the preceding global-invalidation service, including
an existing HTTP session reaching the chain with status 200 because its registry
entry was removed rather than marked expired. The patched real-filter fixture
invalidates that session and clears authentication on its next request.

The initial combined H2 run had one legacy fixture assertion failure: the login
session test counted retained expired principals as active logins. It now still
requires exactly one active admin principal, filtering by nonexpired sessions.
The final 21-case rerun passes. Session-controller positive/negative tests retain
the existing superuser authority boundary. The workflow now includes the core
invalidation suite and requires its successful report; YAML/embedded Python and
changed-file formatting checks pass. No hosted CI result is asserted.

The username-based unsupported-enumeration fallback is tested with a mocked
registry, not an external Redis deployment. Complete selected timing, concurrent
new/in-flight requests, all provider/browser paths, Redis/cross-node acceptance
and full AFTA row 39 closure remain open. No new WAR or deployment is claimed.
These overlapping suite counts are not added to earlier evidence inventories.

## 15 November 2026: OIDC event account association

The [lifecycle-listener trace](common-criteria/afta-oidc-event-account-association.md)
and [dated evidence](evidence/afta-oidc-event-account-association-2026-11-15.json)
record **81 passing checks**: 52 lifecycle/token/audit units and 29 H2 login/token
API cases, with no failures/errors/skips. Eight new assertions fail among 19
listener cases against the preceding implementation. Missing/non-HTTP OIDC
details no longer interrupt bookkeeping; completed OAuth events use a resolved
local DHIS2 identity rather than an external subject. Unmapped success events do
not mutate local namesakes. Existing token failure isolation and session fixes
are retained.

Unit cases use real local OIDC-principal wrappers and a real listener/meter
registry, with mocked authentication events and account services. H2 cases cover
existing form/token paths, not a live external IdP/browser flow. Workflow syntax,
changed-file formatting and checklist/evidence validators pass. The lifecycle
suite is now explicitly required by the workflow, but no hosted CI, WAR or new
deployment result is asserted. Distributed counter atomicity, complete selected
policies and full rows 29/37 acceptance remain open. Overlapping executions are
not added to earlier evidence inventories.

## 15 November 2026: atomic failure counters

The [atomic-counter trace](common-criteria/afta-atomic-failure-counters.md) and
[dated evidence](evidence/afta-atomic-failure-counters-2026-11-15.json) record **132
passing checks**: 70 unit/cache, 56 H2 API and six real Redis cases, with no final
failures/errors/skips. The existing Redis-cache suite's 35 cases are aggregated
from eleven nested JUnit reports; the enclosing zero-case report adds nothing.
Three of five service assertions fail against the preceding get/put counter code,
each reproducing two simultaneous failures recorded as one. Threshold/reset and
disabled-setting cases pass before and after the correction.

The new cache operation uses local entry processing or Redis serialized-byte
compare-and-set with configured write expiry and bounded conflict retries.
Login/recovery/MFA-disable counters use checked addition without changing the
existing thresholds or feature flag. Real Redis tests cover a deterministic
conflict, concurrent increments and reset, JDK serialization, expiry, eternal
entries and failed mapper preservation. Multiple cache instances use one
Lettuce factory against a disposable digest-pinned Redis server; this is not
full application cluster/failover acceptance.

An initial script-result serializer compile issue and missing runtime Lettuce
in the lower-level fixture were corrected. The real Redis fixture now uses the
application test module's existing client dependencies. Because the H2 profile
does not select untagged fixtures, a dedicated unit-profile invocation executes
its six cases and CI requires the resulting report. No production dependency or
Maven declaration changed. Redis scripting permission and custom cache-adapter
support are new operational compatibility requirements. Full provider/reset and
failure/alert policies, deployed acceptance, WAR packaging and hosted CI success
remain unverified. Earlier overlapping inventories are not added to this run.

## 15 November 2026: current-user and MFA target UID binding

The [current-user/MFA UID trace](common-criteria/afta-current-user-uid-binding.md)
and [dated evidence](evidence/afta-current-user-uid-binding-2026-11-15.json) record
**145 passing checks**: 42 unit and 103 H2 API cases, with no failures/errors/skips.
Separate baselines reproduce three service mutation/counter failures, five of
seven resolver failures, and six of six real HTTP failures. The two unchanged
String/UserDetails resolver-contract cases pass before and after the patch.

The HTTP baseline uses an ordinary session retaining a retired username while an
administrator renames that account and assigns the old name to another account.
It returns the other account's profile UID and pending QR seed in controlled
fixtures. Patched entity resolution follows the authenticated UID; self-MFA
mutations pass that resolved entity to service overloads, and reset does not
repeat mutable-name lookup. Required missing persisted owners deny. Failed-disable
counting uses the target name matched by that invocation's lock check.

Existing profile, MFA/QR/audit, login and token suites pass. New reports plus the
existing profile suite are required in CI; workflow syntax, changed-file formatting
and AFTA evidence validation pass locally. No live external IdP/browser, Redis
session or full multi-node acceptance, new WAR or hosted CI result is claimed.
Snapshot permissions and other indirect name consumers, same-UID state races and
complete AFTA operation definitions remain open. Earlier overlapping test counts
are not added to this execution.

## 16 November: email MFA disable challenge target

The [email challenge trace](common-criteria/afta-email-challenge-identity.md) and
[dated evidence](evidence/afta-email-challenge-identity-2026-11-16.json) record two
failing baseline cases, then 30 unit and 32 H2 API passes with the selected-account
helper. The new suite is required by fork CI; no hosted run or full clause closure
is asserted. These overlap previous execution inventories and are not additive.

## 16 November: login email challenge UID

The [login challenge UID trace](common-criteria/afta-login-email-challenge-uid.md)
and [dated evidence](evidence/afta-login-email-challenge-uid-2026-11-16.json) bind
the login provider to UID-based issuance. The baseline keeps the preceding provider
with the additive service helper available: one of 26 cases fails on the old name
call. Final unit and H2 counts are recorded in the evidence; no hosted run, live
concurrent exploit or complete clause closure is asserted. Results overlap prior inventories.

## 16 November: rejected MFA proof events

The [MFA failure-event trace](common-criteria/afta-mfa-failure-events.md) and
[dated execution evidence](evidence/afta-mfa-failure-events-2026-11-16.json) record
two publisher baseline failures and two H2 lockout baseline failures. An exact
custom exception mapping enables the existing failure listeners. Successful
challenge issuance remains excluded. Final suite counts/hashes are in the evidence;
these overlap earlier inventories and do not establish full selected policy or
production audit delivery. No hosted CI execution is claimed.

## 16 November: sanitized login success details

The [success-context trace](common-criteria/afta-login-success-context.md) and
[dated evidence](evidence/afta-login-success-context-2026-11-16.json) preserve three
failing baseline cases among 24 provider-policy tests. The replacement success
token now retains address/session fields while clearing the submitted OTP in a
copy. Final suite counts and source/report hashes are recorded in the evidence.
Results overlap earlier inventories; production audit delivery and full clause
closure are not established. No hosted CI execution is claimed.

## 16 November: OIDC event UID owner

**Selected source rows: 29, 37, 38 and 39.** The
[per-row UID trace](common-criteria/afta-oidc-event-uid-binding.md) and
[dated evidence](evidence/afta-oidc-event-uid-binding-2026-11-16.json) record seven
failing baseline cases against the preceding listener and the final execution.
Mapped OIDC events now retain the UID-resolved account for bookkeeping.
These controlled fixtures do not establish live IdP/browser acceptance or complete
session freshness; all four rows remain partial. Results overlap prior inventories.

## 16 November: token failure boundary

**Selected source rows: 1 (`FAU_GEN.1.1`), 29 (`FIA_AFL.1.1`).**
The [boundary trace](common-criteria/afta-token-failure-boundary.md) and
[dated record](evidence/afta-token-failure-boundary-2026-11-16.json) preserve
two failing baseline cases among fifteen token-filter tests. The catch now
excludes downstream handling and success publication. Exact final suites/hashes
are in the evidence. Results overlap earlier inventories; surrounding handler
acceptance, full event policy and complete row closure are not established.

## 16 November: gap-row 20 token-buffer hygiene

**Selected source row: 20 (`FDP_RIP.2.1`).** The
[buffer-lifecycle trace](common-criteria/afta-token-secret-buffer.md) and
[dated evidence](evidence/afta-token-secret-buffer-2026-11-16.json) record four
baseline failures and final unit/H2 suites. Overwriting the resolver-owned array
is best-effort hygiene only: JVM physical erasure, immutable copies and all
protected storage/resources remain unproven. Row 20 stays Gap. Final counts
overlap previous inventories; no full closure or deployed memory acceptance is claimed.

## 16 November: gap-row 20 checksum copies

**Selected source row: 20 (`FDP_RIP.2.1`), still Gap.** The
[checksum-copy trace](common-criteria/afta-token-checksum-buffers.md) and
[dated evidence](evidence/afta-token-checksum-buffers-2026-11-16.json) record six
baseline failures for both token versions and final compatibility/regression
executions. The private validator clears its own extracted arrays and preserves
caller input. This is observable Java-array hygiene, not a JVM physical-erasure
or complete residual-resource guarantee. Execution counts overlap prior records.

## 16 November: gap-row 20 hash/CRC buffers

**Selected source row: 20 (`FDP_RIP.2.1`), still Gap.** The
[UTF-8 buffer trace](common-criteria/afta-hash-crc-buffer-hygiene.md) and
[dated evidence](evidence/afta-hash-crc-buffer-hygiene-2026-11-16.json) record ten
baseline lifetime failures among 22 new cases, with twelve ownership/vector
controls passing. SHA and CRC helpers now clear their owned encoding storage.
Real digest/token regressions and caller-ownership assertions retain compatibility
coverage. This does not prove physical JVM erasure or full resource protection;
counts overlap earlier executions and no complete requirement is closed.

## 18 November: gap-row 20 token generation ownership

**Selected source row: 20 (`FDP_RIP.2.1`), still Gap.** The
[generation trace](common-criteria/afta-token-generation-buffer-hygiene.md) and
[dated evidence](evidence/afta-token-generation-buffer-hygiene-2026-11-18.json)
record eight cleanup baseline failures among eleven new cases. Successful caller
output remains retained; generation temporaries and unreturned plaintext are
cleared. The final run includes token create/controller and authentication/state
regressions. Counts overlap earlier inventories; JVM physical erasure and full
protected-resource acceptance remain unproven.
