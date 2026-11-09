# DHIS2 web review: gaps, solutions and acceptance

Date: 2026-11-04. Baseline: `07d4f3f7ea`, DHIS2 `2.44-SNAPSHOT`, plus the
recorded two-class security patch. Scope follows the user's direction to review
DHIS2 as a web application and explain exclusions.

This is the remediation plan for the [review register](../common-criteria-review.md#gap-register),
[component dispositions](../common-criteria-gap-assessment.md) and
[web attack surfaces](web-attack-surface-review.md). **Solutions below are proposed,
not implemented or passed unless explicitly identified as existing evidence.**
The documentation covers the retained web concerns and the known evidence gaps;
it does not establish exhaustive source coverage, a complete penetration test or
Common Criteria approval. See [scope/exclusions](web-application-scope.md).

## Evidence and prioritization

| Category | Meaning in this plan |
| --- | --- |
| Fixed, with targeted evidence | The PAT stale-state/shared-authentication defects and audit-buffer batch-loss defect have the recorded production changes and regressions. They are not open implementation tasks here; complete deployment/coverage remains open. |
| Confirmed source behavior | A named source path has the described behavior. Severity can depend on configuration, selected security policy and actual use; a source finding alone is not a successful exploit. |
| Confirmed compatibility issue | A browser error was reproduced under the documented fixture/baseline comparison. This does not establish a security exploit. |
| Deployment or policy gap | The actual configuration or required behavior is not supplied/finalized. A proposed setting is not proof it is deployed or sufficient. |
| Evidence/coverage gap | Review or testing is incomplete. No vulnerability, absence of a feature or failed requirement is inferred solely from missing evidence. |

Prioritize exposed authentication/data-protection behavior, then secure deployment
and functional acceptance, then complete the selected assurance evidence. A
priority is an engineering recommendation, not a CVSS score or evaluation verdict.
The proposed owners are roles; no assignment to a person is implied.

## Scope and acceptance parameters

The application boundary includes browser/API security, app serving/installation,
configured identity mechanisms, sharing/roles, import/export, files, audit and
recovery. Platform services stay explicit dependencies. The scope document
explains excluded physical protections, PP/composition activities and specialized
features not selected in the baseline profile; these are not passing results.

Before acceptance, owners must record the release/app hashes, enabled login and
session mechanisms, node topology, sensitive-data classes, auditable events and
retention policy. They must set measurable revocation delay, allowed clock skew,
audit warning/delivery deadlines, acceptable loss, recovery time and capacity
limits. These are **acceptance parameters**, not invented DHIS2 configuration
keys. Where no value is selected, the corresponding acceptance remains open.

## Gaps in the existing register

<a id="g-01"></a>
### G-01 — evaluated configuration and requirement selection

- **State/owner:** policy and assurance gap; product security and evaluator. Blocks conformance, not preparation of the engineering review.
- **Solution:** freeze the web/API release, enabled app/dependency versions, deployment and optional providers; complete the ST, operations and dependency rationale. Select a PP only if applicable/required and select the assurance package. Do not require every alternative assurance level or every component in the catalog simultaneously.
- **Acceptance:** every selected normative element has code/configuration/process evidence and a test/evaluator reference; every exclusion has its scope reason; all artifacts identify the same release/configuration. Use the [decision worksheet](evaluation-decisions.md) and [FMT_MSA.3 trace](component-traces/FMT_MSA.3.md) as inputs, not as an accepted ST.
- **Compatibility:** documentation first; no branch, dependency or app changes are needed to choose the boundary. Revisit exclusions if requirements change.

<a id="g-02"></a>
### G-02 — durable audit deployment

- **State/owner:** configuration and durability evidence gap; deployment and audit owners. High where durable audit is required. Candidates: `FAU_GEN.1/2`, `FAU_STG.1–3`.
- **Source/evidence:** [ConfigurationKey](../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java), [ArtemisConfig](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java); existing [audit fault results](audit-delivery-failure-evidence.md).
- **Solution:** select required event scopes/types and durable audit destinations; enable database auditing as required, broker persistence and durable subscriptions, and persist broker/database volumes. Keep the optional in-memory pre-broker queue off for a profile that cannot accept its crash-loss window. Document the transaction/acknowledgement boundary and duplicate reconciliation; persistence does not imply exactly-once publication.
- **Acceptance:** publish uniquely identified selected events, crash/restart each persistence component and reconcile retained, duplicated, pending and missing identifiers against the chosen loss policy. Repeat for each enabled node/broker topology. The tmpfs/nonpersistent fault fixture does not satisfy persistent-media crash acceptance.
- **Compatibility:** verify destination/schema/retention capacity before enabling additional scopes. Preserve upstream defaults for other deployments; place the evaluated profile in deployment guidance/configuration management.

<a id="g-03"></a>
### G-03 — audit capacity and storage-full actions

- **State/owner:** policy plus unimplemented/unverified capacity behavior; security and operations. High. Candidates: `FAU_STG.3–5`, `FPT_FLS.1`.
- **Solution:** define what happens to affected business operations if audit cannot be retained; define early warning thresholds, rate/backlog monitoring, DLQ ownership and notification deadlines. Provide monitoring and any necessary application behavior for that policy. Retention must not silently erase required evidence to keep writes succeeding.
- **Acceptance:** constrain real broker/PostgreSQL storage in an isolated environment, trigger warning then exhaustion, verify delivered notification and the selected stop/queue/other permitted behavior, and reconcile events after recovery. Also exercise failed alert delivery. Existing privilege rejection/connection termination and a Docker disk-space setup incident are not disk-full behavior tests.
- **Compatibility:** model clinical availability and audit loss together; do not silently choose shutdown or overwrite for all sites. Back up configuration and use a bounded recovery/replay runbook before restoring traffic.

<a id="g-04"></a>
### G-04 — audit reader, writer and retention authorization

- **State/owner:** deployed privilege and integrity evidence gap; database, security and audit operations. High. Candidates: `FAU_SAR.1–3`, `FAU_STG.2`, `FMT_MTD.1`.
- **Source:** [JdbcAuditRepository](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java). The restricted-role test fixture rejects UPDATE/DELETE; it is not a deployed grants review.
- **Solution:** separate application writer, authorized reviewer, retention and schema-owner privileges. Define emergency administration and retention approval, protect backups and exported records, and add tamper-evidence controls if required by the selected threat model. Account for migration privileges without granting them permanently to runtime users.
- **Acceptance:** capture effective grants and test API/direct-store reads, writes, modification and deletion with each role, including unauthorized users and retention boundary cases. Demonstrate review/retention actions are attributable and failures are visible.
- **Compatibility:** test migrations with a separate migration identity and operational recovery before reducing runtime grants; retain the required INSERT/SELECT/query behavior.

<a id="g-05"></a>
### G-05 — browser CSRF, cookies, CORS and CSP profile

- **State/owner:** confirmed configurable source behavior with incomplete deployed/browser evidence; browser integration and security owners. High. Candidates: `FIA_UAU`, `FTP_TRP.1`, `AVA_VAN`.
- **Source:** [DhisWebApiWebSecurityConfig.filterChain](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java), [CspFilter](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/CspFilter.java). `http.security.csrf.enabled` defaults off; the existing browser run used that local HTTP configuration.
- **Solution:** use the supported SPA CSRF token handling in the evaluated browser profile; define allowed origins and intended cookie attributes, framing and script policies. Inventory machine-client exceptions by authentication mechanism and threat model rather than broadly disabling protection. Configure the actual HTTPS boundary; cookie/HSTS flags alone do not provide TLS.
- **Acceptance:** valid app writes and refresh/login/logout succeed; missing/invalid tokens on protected state-changing requests fail without mutation. Verify attacker-origin form/fetch/preflight cases, intended cross-origin clients, cookie attributes and CSP behavior in real browsers for every enabled app. Recheck account linking, reset and enrollment actions.
- **Compatibility:** validate each app/client against a staged profile first. Preserve token acquisition/refresh and documented integrations; do not globally enable a setting and claim compatibility from unit tests alone.

<a id="g-06"></a>
### G-06 — trusted channels and external service boundaries

- **State/owner:** source/deployment gap; platform/network and integration security. High. Candidates: `FTP_ITC.1`, `FTP_PRO.1–3`, `FTP_TRP.1`, `FDP_UCT/UIT`, `FPT_ITC/ITI/ITT`.
- **Source:** [embedded Main](../../../dhis-2/dhis-web-server/src/main/java/org/hisp/dhis/web/tomcat/Main.java), [ArtemisConfig.jmsConnectionFactory](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java); external broker configuration currently constructs a TCP URL.
- **Solution:** enumerate browser/proxy, database, broker, Redis, identity and other outbound links; specify endpoint authentication, encrypted transport, protocol/options, key/certificate management and network isolation. Determine which library-supported options are exposed; implement missing client configuration where required. Network isolation alone is not a claimed encryption mechanism.
- **Acceptance:** inspect actual negotiated connections; test wrong peer/hostname, expired/untrusted certificates, invalid credentials, downgrade/fallback and unavailable peers. Verify secrets are not logged. Apply the [external broker runbook](../common-criteria-artemis-external-operations.md) only after validating client capability and deployed versions.
- **Compatibility:** stage certificate/credential overlap and rotation; preserve endpoint verification. Do not disable TLS validation to restore connectivity or assume embedded broker settings configure an external broker.

<a id="g-07"></a>
### G-07 — authoritative client address and proxy ingress

- **State/owner:** confirmed forwarded-header trust boundary with unverified deployment; network/platform and authentication owners. Medium, potentially high if IP-based constraints are security-critical. Candidates: `FIA_AFL.1`, `FTA_TSE.1`, `AVA_VAN`.
- **Source:** [ForwardedIpAwareWebAuthenticationDetails](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/ForwardedIpAwareWebAuthenticationDetails.java).
- **Solution:** make the reverse proxy the only application ingress, strip client-supplied forwarding headers and set a canonical address from the connection/approved proxy chain. Define IPv4/IPv6, multiple-proxy and malformed-header semantics. Add application trust configuration if the topology cannot guarantee this boundary.
- **Acceptance:** direct application access is denied; forged single/multi-value headers from untrusted clients cannot satisfy a PAT/IP restriction, evade an IP-dependent policy or falsify the authoritative audit identity. Verify legitimate proxy chains and both address families.
- **Compatibility:** preserve configured trusted proxies/load balancers and validate rolling changes before removing direct paths; health checks require their own restricted route.

<a id="g-08"></a>
### G-08 — complete authorization and TSF interface coverage

- **State/owner:** evidence gap; API, tracker, access-control and security owners. Medium in the existing register; prioritize each sensitive unverified path by exposure/impact. Candidates: `FDP_ACC/ACF`, `FMT_MSA.1–4`, `ADV_FSP`, `ATE_COV`.
- **Source:** [DefaultAclService](../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java), [SecurityCheck](../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/objectbundle/validation/SecurityCheck.java), [SharingMapper](../../../dhis-2/dhis-tracker/src/main/java/org/hisp/dhis/tracker/imports/preheat/mappers/SharingMapper.java).
- **Solution:** inventory routes, verbs, formats, background actions and controlled objects with their identity/role/sharing/organisation-unit rules. Map creation defaults, authorized overrides, bulk/import/export, jobs and privileged exceptions to code and negative/positive tests. A hidden UI control is not enforcement.
- **Acceptance:** for each selected action, test unauthenticated, wrong role, wrong owner/sharing/organisation-unit, revoked identity and permitted identity; verify denial leaves no data mutation or background side effect. Include tracker associations, alternate formats and object-ID substitution. Complete the [FMT_MSA.3 trace](component-traces/FMT_MSA.3.md) across object types.
- **Compatibility:** preserve legitimate public bootstrap/recovery actions explicitly; do not apply a global deny rule without enumerating required unauthenticated operations.

<a id="g-09"></a>
### G-09 — frozen app/dependency release and workflow acceptance

- **State/owner:** release reproducibility/acceptance gap; release engineering and app owners. Blocks the claim that all enabled apps work. Candidates: `ALC_TDA/TAT/CMS`, `ATE_FUN`, `AVA_VAN`.
- **Source:** [apps-to-bundle.json](../../../dhis-2/dhis-web-server/apps-to-bundle.json), [recorded app inventory](../evidence/web-app-inventory-2026-11-03.json).
- **Solution:** freeze exact core/app/dependency revisions and artifact hashes for the acceptance build, retain provenance/SBOM and resolve mutable/SNAPSHOT inputs and the recorded source-built dependency caveat. Keep normal upstream tracking separate from the reviewed release manifest.
- **Acceptance:** regenerate the intended artifact set or explain controlled non-reproducibility; compare hashes and run every enabled app's critical read/write/denial workflow with the evaluated security settings. Test static assets, navigation, refresh and cache upgrade. A 200 entry page or absence of console errors does not prove a complete workflow.
- **Compatibility:** avoid replacing all upstream bundle references merely to freeze one evidence snapshot. Apply intentional app/version changes as small reviewed release changes, with rollback artifacts and hashes.

<a id="g-10"></a>
### G-10 — crypto, entropy, key/time lifecycle and deletion evidence

- **State/owner:** source/platform assurance gap; security, JVM/OS, database and retention owners. Candidates: `FCS_*`, `FPT_STM.1/2`, `FDP_IRC/RIP/SDC/SDI`.
- **Source:** [AuthorizationServerConfig.jwkSource](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthorizationServerConfig.java), [CodeGenerator](../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CodeGenerator.java), [encryption finding](crypto-and-two-factor-gaps.md#g-13-encryption-configuration-and-migration).
- **Solution:** inventory each cryptographic purpose, algorithm/parameters/provider and key source, storage, access, rotation and destruction; select appropriate platform entropy evidence and trusted clock controls. Define retained/deleted data across database pages, files, replicas, caches and backups. Ordinary deletion/GC is not proven secure erasure, and SecureRandom use is not a provider certification.
- **Acceptance:** verify configured providers/parameters, generation failure handling, signing-key restart/rotation and verification overlap, expiration/skew/clock rollback behavior, key access and destruction/retention evidence. Deliberately approve ephemeral signing keys only for configurations whose lifecycle semantics permit them.
- **Compatibility:** coordinate signing-key rotations with token consumers and encryption changes with G-13; verify backup restores under the required retained keys before deleting keys or media.

<a id="g-11"></a>
### G-11 — development, delivery and independent assurance

- **State/owner:** organizational/evaluator evidence gap; development security, release engineering and evaluator. Candidates: selected `ADV/AGD/ALC/ATE/AVA` components.
- **Solution:** provide TSFI/design/implementation correspondence, secure preparation/operation guidance, controlled CM items/build tools, development controls, flaw handling, authentic delivery and vulnerability research. Arrange required independent testing at the selected attack potential. Identify evidence held outside this repository rather than assuming Git establishes the process.
- **Acceptance:** the selected artifact checklist is complete, version-consistent and reviewed; vulnerabilities have triage/fix/retest records; release recipients can verify provenance/integrity; evaluator independence and test scope are evidenced. Existing author-run regressions do not become independent tests by being linked here.
- **Compatibility:** keep fork evidence in separate documents and focused tests. Follow the [upstream maintenance guide](../../fork/upstream-maintenance.md); do not rewrite shared history, change remotes or promise conflict-free future merges.

<a id="g-12"></a>
### G-12 — Interpretations and fixture-dependent browser errors

- **State/owner:** confirmed compatibility issue for the recorded Interpretations bundle; app/release owners. Messaging and legacy event-app failures were fixture-dependent. Candidate: release/functional acceptance, not an inferred security SFR failure.
- **Evidence:** [browser/baseline comparison](../common-criteria-verification.md) and app inventory identify the tested versions. The Interpretations worker chunk was loaded as a page script and raised `importScripts is not defined` with both original and patched production classes.
- **Solution:** repair the owning app's build/HTML chunk inclusion so worker code runs in its intended worker context; inspect its source and generated manifest before choosing the exact change. Build a traceable corrected artifact, then intentionally update/pin the tested bundle. Do not hide the console error, add a global fake `importScripts`, or patch generated WAR files as the maintainable fix. Document required organisation-unit/feedback-recipient fixtures separately from defects.
- **Acceptance:** verify the worker and app logic, interpretation list/create/read/update and denied operations under the evaluated profile; repeat baseline regression and asset/cache-upgrade checks. A removed script tag alone is not proof the worker-dependent feature still works.
- **Compatibility:** preserve the original bundle for rollback; keep its version/hash change isolated. No app-source repair or corrected artifact is currently implemented in this checkout.

<a id="g-13"></a>
### G-13 — built-in encryption fallback and legacy cipher migration

- **State/owner:** confirmed source configuration with conditional data/configuration impact; security/platform/data migration owners. High when used without an appropriate secret/profile. Candidates: `FCS_CKM.5`, `FCS_COP.1`, `FDP_SDC.1/2`.
- **Source:** [HibernateEncryptionConfig](../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java), detailed [finding/migration rationale](crypto-and-two-factor-gaps.md#g-13-encryption-configuration-and-migration).
- **Solution:** inventory actual encrypted fields, bean use, ciphertext versions and key configuration; require explicitly provisioned secrets for the evaluated profile. Design versioned legacy-read/re-encryption support and approved provider/parameters before removing fallback or old cipher reads. Do not put live keys/ciphertext in evidence or logs.
- **Acceptance:** fresh/missing/empty/wrong key cases have selected behavior; upgrade reads old data, migration can resume after interruption, backup/restore and rollback retain readability, and rotated keys meet the chosen lifecycle. Test actual affected data types, not only an encrypt/decrypt helper round trip.
- **Compatibility:** changing the password alone can make existing ciphertext unreadable. Preserve protected backups and required old keys through the approved migration window; never claim a silent key replacement is a safe fix.

<a id="g-14"></a>
### G-14 — single-use 2FA acceptance and replay

- **State/owner:** confirmed lack of consumption in the reviewed helper/provider path, with selected single-use semantics and end-to-end replay evidence still open; authentication/distributed-data owners. High if single-use/replay resistance is required. Candidates: `FIA_UAU.4`, `FPT_RPL.1`.
- **Source:** [TwoFactorAuthUtils](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java), [TwoFactorAuthenticationProvider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java); [detailed evidence](crypto-and-two-factor-gaps.md#g-14-single-use-and-replay-evidence).
- **Solution:** define challenge scope, retry, skew and consumption point. Atomically consume an email challenge or record an accepted TOTP step using authoritative state shared across nodes; bind it to the intended user/challenge/purpose. Specify failure and rollback behavior. A local-only replay cache is insufficient for a distributed guarantee.
- **Acceptance:** a fresh challenge succeeds once, immediate reuse and simultaneous same-code attempts fail after one success, including across nodes; test expiry/skew, resend/replacement, enrollment versus login, storage outage and restart. Verify unsuccessful password authentication does not incorrectly consume another user's challenge.
- **Compatibility:** distinguish login/enrollment/recovery purposes and test existing clients. Design transactional state/migration using existing DHIS2 service/store conventions; no new replay schema or successful replay test is asserted here.

<a id="g-15"></a>
### G-15 — enrolled factor disabled globally

- **State/owner:** provider downgrade corrected; complete deployment/provider/recovery acceptance remains open. Product security/authentication and recovery owners. Candidate: `FIA_UAU.5`.
- **Implementation update, 2026-11-09:** the existing provider now rejects disabled, unsupported or missing enrolled factor types before accepting authentication; enabled types still require code validation. See the [remediation and test record](afta-remediation-progress.md). The earlier branch behavior is preserved as a historical [source finding](crypto-and-two-factor-gaps.md#g-15-disabled-factor-type-policy).
- **Solution:** select explicit semantics. Recommended secure-profile behavior is to deny enrolled-factor login when the factor is unavailable, unless a separately authorized, attributable recovery procedure applies. Implement the policy in the existing provider; do not silently treat enrollment as factor proof or add a parallel authentication subsystem.
- **Acceptance:** cover every enrollment/type/global-flag/role-required combination with absent/wrong/valid codes and the relevant alternative providers. Verify denial, useful generic UI feedback, authorized recovery and normal login/PAT behavior. Exercise the browser flow, not only the private helper.
- **Compatibility:** plan administrator recovery before changing behavior; disabling a type no longer permits password-only login for enrolled accounts. Record the chosen fallback/fail-closed policy so an upgrade does not accidentally strand users or weaken MFA.

## Additional web coverage gaps and solutions

These are subdivisions of G-08/G-09/G-10/G-11, not newly demonstrated exploits.
They prevent a broad web-security claim even when the selected regression suites
pass. The following source/test references are starting points; test presence is
not an execution claim.

<a id="w-01"></a>
### W-01 — XSS, HTML/script handling and framing

**State:** coverage gap; browser/app security owner; candidates `AVA_VAN`, selected
data/channel policy. **Solution:** trace untrusted values from input to HTML/DOM,
URL, script and template sinks; use context-specific encoding and vetted
sanitization for intentionally rich content, with a compatible CSP/framing policy.
Include translations, interpretations, filenames and imported metadata. Start at
[CspFilter](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/CspFilter.java)
and [HeaderUtils](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/utils/HeaderUtils.java),
then inspect separately maintained app sources. **Acceptance:** stored/reflected/
DOM payloads stay inert for ordinary and privileged viewers, legitimate rich
content still renders, and disallowed framing fails in the evaluated browsers.
Existing CSP filter tests do not establish these application-sink properties.

<a id="w-02"></a>
### W-02 — file-domain access and malicious upload/download handling

**State:** partial source/tests with remaining policy/coverage; file/storage and
access-control owners; candidates `FDP_ACC/ACF/ITC/ETC`, `FRU_RSA`, `AVA_VAN`.
**Solution:** map every file domain and metadata/content/signed-URL route to
authorization, allowed content behavior, size/type handling, safe filenames and
response headers. Do not assume the generic route's rule covers other routes.
Apply content isolation and scanning if the selected policy requires them; bound
image/decompression processing and temporary storage. Start at
[FileResourceController](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/FileResourceController.java)
and [FileResourceUtils](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/utils/FileResourceUtils.java).
**Acceptance:** test cross-user/domain ID substitution, ownership/assignment,
expired signed links, MIME/extension mismatch, active content, header filenames,
large/decompression inputs and valid uploads for each backend. Keep database and
blob cleanup/recovery consistent. The six utility/nine controller passes are a
subset, not a domain-complete access or malware-isolation proof.

<a id="w-03"></a>
### W-03 — SQL/query/filter injection and view privilege

**State:** partial source/tests; query/persistence security owner; candidates
`FDP_ACC/ACF`, `FMT_MTD`, `AVA_VAN`. **Solution:** inventory query inputs by SQL
value, identifier, expression, ordering and filter context. Bind values and
allow-list/parse structural choices; review validation on every path. Validate
view-author/use permissions, protected-table settings and least-privilege/read-only
database identities. Start at [SqlViewUtils](../../../dhis-2/dhis-services/dhis-service-administration/src/main/java/org/hisp/dhis/sqlview/SqlViewUtils.java),
[DefaultSqlViewService](../../../dhis-2/dhis-services/dhis-service-administration/src/main/java/org/hisp/dhis/sqlview/DefaultSqlViewService.java)
and [HibernateSqlViewStore](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/sqlview/hibernate/HibernateSqlViewStore.java).
**Acceptance:** invalid input cannot alter query structure, access protected
objects or perform writes; test encoded/malformed/alternate-format inputs and
valid reports. Textual substitution alone is not an established exploit. The
five utility/eight controller passes do not replace real PostgreSQL negative tests.

<a id="w-04"></a>
### W-04 — app archive, resource and supply-chain trust

**State:** 23 passing controller cases/one existing skip with wider backend and
provenance coverage open; app/release/storage owners; candidates `FMT_MOF/MTD`,
`FDP_ACC`, `FRU_RSA`, `AVA_VAN`. **Solution:** retain path/ZipSlip/ZipBomb validation,
restrict installation authority and trusted app provenance, and bound extraction,
parallel buffering and cleanup. Review encoded/proxy normalization and blob/local
backend behavior at [AppController](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/AppController.java)
and [BlobStoreAppStorageService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/appmanager/BlobStoreAppStorageService.java).
**Acceptance:** archive/path attacks cannot escape the app namespace, exceed the
chosen resource policy or leave a usable partial install; valid nested resources
and upgrade/rollback work. Treat authorized same-origin app code as privileged
code, not as harmless uploaded content. Record why the skipped redirect assertion
is not currently exercised; do not convert it to a pass.

<a id="w-05"></a>
### W-05 — SSRF and outbound credential boundaries

**State:** incomplete outbound-call inventory, not a confirmed SSRF exploit;
integration/platform security owner; candidates `FTP_ITC`, `FDP_ACC`, `AVA_VAN`.
**Solution:** inventory fetch/import/webhook/identity/App Hub destinations and
which values are attacker-controlled. Validate allowed schemes/destinations,
redirects, address resolution and egress; prevent credentials from crossing
origins or reaching host/metadata/internal services contrary to policy. Preserve
configured legitimate integrations. Start at [DefaultAppManager](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/appmanager/DefaultAppManager.java)
and each enabled integration, rather than claiming this one source is exhaustive.
**Acceptance:** isolate test destinations and verify denied loopback/private/
link-local/redirect/DNS cases according to the selected policy, allowed services,
TLS peer failures and timeouts. Do not disable trust checks as a connectivity fix.

<a id="w-06"></a>
### W-06 — OAuth/OIDC and alternate identity-provider protocols

**State:** incomplete enabled-provider evidence; authentication/integration owner;
candidates `FIA_UAU/UID/USB`, `FMT_REV`, `FTP_ITC`, `AVA_VAN`. **Solution:** record
enabled flows/clients, redirect URI and client registration rules, state/nonce/
PKCE requirements where appropriate, issuer/audience/signature/algorithm checks,
account linkage, token exposure, refresh/revocation and logout semantics. Start at
[AuthorizationServerConfig](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthorizationServerConfig.java)
and the configured provider paths. Existing [PKCE](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/OAuth2PkceEnforcementTest.java)
and [token redaction](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/OAuth2TokenRedactionTest.java)
tests are candidates, not part of the recorded 31-class execution inventory.
**Acceptance:** reject mismatched state/nonce/redirect/issuer/audience/keys and
required missing/wrong PKCE values, verify legitimate provider login/link/logout,
and ensure secrets/runtime authorization state do not leak through APIs/import/
export/logs. Cover all enabled providers; local-password/PAT success is not proxy
evidence for federation.

<a id="w-07"></a>
### W-07 — distributed sessions, revocation and recovery

**State:** selected local regressions only; session/distributed platform owner;
candidates `FTA_MCS/SSL`, `FMT_REV`, `FPT_TRC/RCV`, `FRU_FLT`. **Solution:** define
which sessions/tokens/attributes must converge across nodes and within what time;
use supported session/invalidation mechanisms and authoritative reconciliation.
Specify behavior on lost/reordered invalidations, Redis/database outage and
restart. Review [RedisSpringSessionConfig](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/RedisSpringSessionConfig.java),
[NonRedisSessionConfig](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/NonRedisSessionConfig.java)
and [CacheInvalidationListener](../../../dhis-2/dhis-support/dhis-support-cache-invalidation/src/main/java/org/hisp/dhis/cacheinvalidation/redis/CacheInvalidationListener.java).
**Acceptance:** test session fixation/rotation, idle/concurrency boundaries,
logout/disable/role downgrade and old-cookie replay on each supported topology,
including partition/reconnect and concurrent changes. Preserve normal session
continuity only within the chosen policy. Fresh PAT lookup fixes its cache defect;
it does not prove all replicated TSF data is consistent.

<a id="w-08"></a>
### W-08 — resource exhaustion and abusive workloads

**State:** unverified deployment limits, not a measured denial-of-service result;
capacity/security owner; candidates `FRU_RSA/FLT`, `FIA_AFL`, `AVA_VAN`.
**Solution:** define request/body/header/upload/decompression, paging/export,
query/job concurrency, timeout and queue/pool limits across proxy, server and
backends. Separate authorized large reports from abusive workloads; protect
authentication/recovery endpoints and bound temporary files/memory. Inspect
[ConfigurationKey](../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java)
and the actual deployment, not only a global upload setting.
**Acceptance:** under controlled isolated load, over-limit operations fail with
bounded resource use and no unexpected work, unrelated authorized users retain
the agreed service level, and recovery needs no destructive reset. Record actual
limits, metrics and workload. A passing archive unit/controller case does not
prove application-wide availability or minimum resource reservations.

<a id="w-09"></a>
### W-09 — sensitive data, logs, caches, exports and retention

**State:** incomplete data-flow/privacy evidence; product privacy, data and
security owners; candidates `FDP_ACC/ETC/IRC/RIP/SDC/SDI`, `FAU_GEN`, `AVA_VAN`.
**Solution:** inventory patient/credential/security data through responses,
errors, logs, audit, browser/service-worker caches, downloads, exports, temporary
files, backups and replicas. Minimize/redact by context, enforce access and
retention, and choose cache/content handling appropriate to sensitive responses.
An exclusion of specialized `FPR_*` guarantees does not waive confidentiality or
legal/privacy obligations. Use G-04/G-10/G-13 for protection/deletion/key design.
**Acceptance:** a denied or different user cannot recover protected content from
an alternate route, export, cache or error; secrets are absent from permitted
logs/telemetry; selected retention/deletion works through backups/restore under
the approved policy. Verify useful diagnostics and authorized clinical reports
still work. No complete log/response/backup privacy audit is currently claimed.

<a id="w-10"></a>
### W-10 — malformed requests, errors and privileged management routes

**State:** incomplete API surface and negative-test coverage; API/security owner;
candidates `FIA_UID/UAU`, `FDP_ACC/ITC`, `FMT_MOF/MTD`, `AVA_VAN`.
**Solution:** inventory verbs/content types/encodings/serialization and bulk/
asynchronous entry points. Define schema/domain validation, allowed writable
properties, management authority and safe error behavior; apply the same controls
to direct, import and job paths. Trace [SecurityCheck](../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/objectbundle/validation/SecurityCheck.java)
and [SystemSettingsController](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/SystemSettingsController.java)
with the full route inventory rather than treating them as the only entry points.
**Acceptance:** duplicate/unexpected fields, forbidden property assignment,
invalid verbs/media/encoding and unauthorized management requests do not change
data or enqueue privileged work; errors expose no stack/SQL/secret details to
unauthorized callers. Verify partial failure/transaction semantics and normal
client validation feedback. No new vulnerability is inferred from a route simply
being unreviewed.

## Implementation sequence and release acceptance

1. Freeze the web profile and acceptance parameters (G-01/G-09). Keep an isolated
   baseline build, data fixtures and rollback artifacts; no production patient
   records or credentials belong in tests/evidence.
2. Resolve configuration-dependent authentication/key behavior and the owning
   app defect with explicit compatibility plans (G-12–15). Make small changes in
   existing services/providers and separately owned app sources. Keep migrations,
   dependencies, configuration and behavior changes reviewable independently.
3. Validate deployed browser/proxy/channels/audit/storage controls (G-02–07), then
   complete route/data-flow and negative-test coverage (G-08, W-01–10). Exercise
   failures and recoverability as well as valid clinical workflows.
4. Complete selected component elements, process evidence and independent work
   (G-10/11). Retest affected code and app workflows after each actual change;
   broaden only when changes/failures/unresolved risk justify it.
5. Release acceptance requires all in-profile critical workflows passing, selected
   security behavior verified, open risks explicitly dispositioned by authorized
   owners, and reproducible/versioned evidence. Formal Common Criteria approval
   additionally requires the selected scheme/evaluator process.

Evidence for any closure must identify gap/element, core/app/source hashes,
non-secret configuration, topology, procedure, expected/actual result, case/report
identifiers and residual limits. Update the [verification record](../common-criteria-verification.md)
and component disposition only when that evidence exists. A documented solution,
passed subset or temporarily hidden app error does not close the gap.

The recorded inventory remains **262 passes and four existing skips across 31
classes** over two dated records. No tests, production behavior, dependencies,
app bundle selection, branches, remotes or GitHub publication changed while
writing this solution plan. No new test run or completed solution is asserted.
