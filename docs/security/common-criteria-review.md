# DHIS2-Core Common Criteria review and remediation

Date: 2026-11-03. Baseline commit: `07d4f3f7ea`.
References: supplied ISO/IEC 15408 Parts 1, 2 and 3, fifth editions (2026).

**Result: engineering review and targeted remediation; formal approval remains open.**
Follow-up on 2026-11-04: all **261** component IDs now have a baseline
[gap disposition](common-criteria-gap-assessment.md), including the five composite
assurance components omitted by the earlier inventory. This completes inventory
gap triage, not every normative element's implementation review or certification.
The user's subsequent scope direction is DHIS2 as a web application. The
[web scope/exclusion rationale](common-criteria/web-application-scope.md) explains
which specialized activities/features are outside this profile and which
deployment protections remain relevant. Every component disposition records its
own scope reason. The [web attack-surface review](common-criteria/web-attack-surface-review.md)
adds file/app/query and browser-security paths without claiming full penetration coverage.
The [traceability matrix](common-criteria-traceability.md) links standard components
to source symbols and tests. The [verification record](common-criteria-verification.md)
separates executed checks from planned acceptance work.

## Draft evaluation boundary

This is an ST preparation aid, not an evaluated ST or a selected assurance package.

- Product: the checked-out DHIS2-Core `2.44-SNAPSHOT` Java application, including
  the API, security configuration, audit producer/consumer/storage, embedded web
  server and the web-application bundling/serving mechanism.
- Interfaces: browser login and app bootstrap, session-authenticated API calls,
  Basic/PAT/JWT/OIDC authentication as configured, account/role management, audit
  read/write paths, PostgreSQL and Artemis connectivity.
- Assets: health records and metadata, credentials and tokens, role/sharing data,
  sessions, security configuration and attributable audit records.
- Environment dependencies: OS/JVM, PostgreSQL, persistent broker storage, optional
  Redis, reverse proxy/TLS, trusted clock, external LDAP/OIDC providers, deployment
  secrets and administrator procedures. Decide which components enter the TOE
  rather than assuming their security is inherited.
- Web applications: [apps-to-bundle.json](../../dhis-2/dhis-web-server/apps-to-bundle.json)
  references separate `d2-ci` repositories, often mutable default branches. Their
  full source and browser workflows are not all in this checkout. Freeze versions,
  hashes and enabled apps before an evaluation or release acceptance claim.
- No production instance, production database or specific deployed configuration
  was supplied. Testing must use isolated data and accounts.

### Draft security problem and objectives

| Threat / assumption | Objective | Candidate requirements / evidence |
| --- | --- | --- |
| T.STALE_ACCESS: revoked accounts, permissions or token constraints remain effective | O.CURRENT_AUTH: authenticate against current account/token state and isolate each request | FIA_ATD.1, FIA_USB.1, FMT_REV.1; CC-2026-01/02 |
| T.UNAUTHORIZED_DATA: users access data or management functions outside their roles/sharing scope | O.ACCESS: enforce server-side identity, role, sharing and organisation-unit policies | FIA_UID.1, FIA_UAU.1, FDP_ACC.1, FDP_ACF.1, FMT_SMR.1 |
| T.AUDIT_LOSS: storage/broker failures erase evidence or silently acknowledge unsaved events | O.AUDIT: preserve attributable evidence under the selected failure/retention policy | FAU_GEN.1/2, FAU_STG.1–5, FPT_FLS.1; CC-2026-03 and existing audit work |
| T.SESSION_ABUSE: stolen, fixed, expired or administratively revoked sessions continue to authorize | O.SESSION: rotate sessions on login and enforce limits, expiry and logout | FTA_MCS.1, FTA_SSL.3/4; authentication/session regressions |
| T.CHANNEL: untrusted clients or network actors intercept/alter security-sensitive traffic | O.CHANNEL: authenticated encrypted channels and browser interface protection | FTP_TRP.1, FTP_ITC.1; deployment-specific TLS/CORS/CSRF/CSP evidence |
| A.ADMIN: authorized administrators follow controlled installation, retention and recovery procedures | OE.ADMIN: restrict privileged access and record operational actions | AGD_PRE.1, AGD_OPE.1, ALC evidence; role/retention policy remains to be finalized |
| A.PLATFORM: OS, database, broker, clock and identity services meet the agreed configuration | OE.PLATFORM: least privilege, persistent storage, backups, trusted time and protected transport | deployment configuration and independent platform evidence |

These are proposed inputs. Complete the ST's threat/assumption/objective rationale,
SFR assignments/selections and dependency analysis before claiming conformance.

## Remediations in this review

### CC-2026-01: stale API-token authorization (high)

`ApiTokenAuthManager.authenticate` previously returned an authentication object
from a process-local cache for up to one hour. Cache hits checked expiry but did
not re-read account status, role/group permissions or the persisted token.
Consequently account disablement, 2FA enablement, permission changes and token
updates/deletion on another node could leave a token usable with old rights.

Remediation: resolve the persisted token and create current `UserDetails` on every
authentication. Retain existing expiry, owner, account and 2FA checks. This also
makes request constraints use the current token rather than a cached snapshot.
The local authentication cache is removed from this manager; no API response
schema or configured authentication method changes. The tradeoff is additional
lookup/hydration work for repeat PAT requests. Measure production load before
release; a future cache needs reliable distributed invalidation and bounded
revocation semantics rather than reinstating the previous behavior.

Evidence: `ApiTokenAuthManagerTest` covers account disable/lock/expiry, credentials
expiry, 2FA enablement, removed owner/token, replaced constraints and token expiry
after successful authentication. `ApiTokenStateAuthenticationTest` exercises the web
security filter with persisted users and tokens. Database replication/cache
visibility and already-running requests remain operational concerns.

### CC-2026-02: shared mutable request authentication (medium)

The same cached `ApiTokenAuthenticationToken` was reused across requests. The
filter writes request details and the authenticated flag onto that object.
Concurrent requests could therefore share/overwrite authentication details.

Remediation: each authentication returns a fresh object with current principal
and authorities. The isolation regression checks distinct instances, fresh
principal, no carried request details and an unset authenticated flag before the
filter completes request-constraint validation.

### CC-2026-03: audit batch loss on producer failure (high when buffer enabled)

`AuditScheduler.process` previously drained all ready events into a local batch.
If a publish threw, the failed event and the rest of that batch were absent from
the queue and never retried.

Remediation: poll and publish one event at a time; requeue the failed event and
propagate the exception. Unprocessed events remain queued. Serialize concurrent
processor invocations. Tests cover first/middle failure, recovery without
replaying acknowledged earlier events, normal publication and unexpired delay.

Limits: the optional queue is still volatile; a JVM crash can lose buffered
messages. A producer may send successfully then throw on an uncertain
acknowledgment; retries can duplicate that event. This is not an exactly-once or
crash-durability claim. The queue should remain disabled in a profile requiring
durable pre-broker retention. Broker/DLQ/database recovery are separate tests.

### CC-2026-04: incorrect 2026 standard trace references (documentation)

Existing audit documents mapped storage availability/loss to earlier-edition
component numbers. Corrected references now use Part 2 clauses 8.8.6–8.8.10 and
FAU_STG.1–5. Historical test evidence is preserved with its original scope/date.

## Gap register

The [web gap solution plan](common-criteria/web-gap-solutions.md) expands every
G-01–G-15 into evidence status, proposed owner, solution, acceptance checks and
compatibility/migration considerations. W-01–W-10 cover additional web-specific
coverage gaps. A proposed solution is not an implemented fix or a passing test.

| ID | Priority | Current gap | Required closure / owner |
| --- | --- | --- | --- |
| G-01 | Blocking conformance | Web-application scope is directed by the user, but no complete ST, selected PP/assurance package/scheme or frozen evaluated deployment exists | Product/security owner and evaluator finalize these; use documented web-scope exclusions and complete component operations and dependency rationale. |
| G-02 | High | Audit database persistence and embedded broker persistence default off; volatile buffer is optional | Deployment owner enable required audit scopes/storage and persistent volumes; keep volatile buffer off for durable profile; prove post-restart retention. Defaults are not changed globally because they serve different DHIS2 deployments. |
| G-03 | High | Integrated JMS/PostgreSQL rejection and recovery now pass, but no verified disk-capacity threshold/alarm or selected policy for business operations when audit storage fills | Security/operations owners select FAU_STG.3/4/5 behavior and retention metrics; implement/test it under real PostgreSQL and broker failure/exhaustion. |
| G-04 | High | A restricted-role fixture blocks modification/deletion, but deployed auditor/retention privileges, database least privilege and tamper detection remain unverified | Define auditor/admin roles, enforce selected access policy throughout entry points, test least-privilege identities and protected storage. |
| G-05 | High | CSRF defaults off; actual app compatibility with enabled CSRF and selected TLS/cookie/proxy settings is unverified | Define the evaluated browser profile; enable supported protections and verify login, SPA token refresh, writes, logout and external integrations with actual app versions. |
| G-06 | High | External broker connection is configured as TCP; TLS, client authentication and network isolation depend on environment | Provide deployment trusted-channel design and verification for browser, database, broker, Redis and identity-provider traffic. |
| G-07 | Medium | Forwarded client-address headers are trusted by authentication/IP constraint code | Reverse proxy must strip client-supplied forwarded headers, set authoritative values, and be the only application ingress; exercise direct-access/spoofing rejection in that deployment. |
| G-08 | Medium | API/account/session/ACL source and selected tests do not establish complete TSF coverage | Enumerate every security-relevant endpoint/action and link negative/positive tests, including tracker and object-level access. |
| G-09 | Blocking release acceptance | Independently versioned browser apps and mutable bundle references | Freeze app hashes and run critical browser workflows for each enabled app against the patched build. |
| G-12 | Web acceptance | The pinned Interpretations app loads a worker chunk as a page script; a Messaging null configuration error was isolated to missing feedback recipients | Baseline replay reproduces both page errors. The required Messaging fixture is configured in the isolated test instance; repair and validate the independently bundled Interpretations app in its owning source repository before claiming all web workflows work. |
| G-10 | Assurance | No full crypto/provider, entropy, key lifecycle, trusted-time or secure-deletion evidence | Security/platform owners complete applicable FCS/FPT/FDP selections and supply test/configuration evidence. |
| G-11 | Assurance | Development environment, delivery controls, complete vulnerability assessment and independent tests are outside this source review | Complete selected ALC/ADV/AGD/ATE/AVA evidence with organization and evaluator. |
| G-13 | High, conditional on encrypted-data use/configuration | Field-encryption configuration has a built-in password fallback and a legacy 3DES/fixed-salt bean; active field/bean use and deployment key settings are not fully inventoried | Security/platform/data owners inventory ciphertext and keys; require explicit secret provisioning in the evaluated profile and design tested re-encryption, backup and rollback before changing existing keys or ciphers. See the [source findings](common-criteria/crypto-and-two-factor-gaps.md#g-13-encryption-configuration-and-migration). |
| G-14 | Login single-use implemented; wider acceptance open | A database constraint now reserves email login challenges and matched TOTP steps; independent transactions prevent caller rollback from reopening a code. | Define cross-purpose rules and verify account/factor freshness, clocks, protected database state, recovery and full deployed multi-node/browser behavior. See [implementation/evidence](common-criteria/afta-login-replay-protection.md). |
| G-15 | Provider fix implemented; deployment acceptance open | On 2026-11-09 the provider was changed to reject disabled, unsupported or missing enrolled factor types; enabled factors still require validation. Targeted provider/API regressions are recorded. | Deploy and verify the change, authorized recovery, alternate providers and multi-node behavior. Full AFTA FIA_UAU.5.2 coverage remains partial; see [remediation progress](common-criteria/afta-remediation-progress.md). |

## Follow-up review status and next closure order

The [machine-readable component assessment](evidence/component-gap-assessment-2026-11-04.json)
and [code index](common-criteria-component-code-index.md) separate implementation
candidates from related source boundaries. Every component has an explicit gap
and action; none is declared conformant or silently excluded. Some code paths
remain untraced at element level. Missing evidence is not proof that code is absent.

1. Agree the ST boundary, deployment, enabled apps, SFR operations and assurance
   package (G-01). Record justified exclusions and dependencies. Requirements at
   alternative levels and PP/composition requirements are not all mandatory.
   The [evaluation decision worksheet](common-criteria/evaluation-decisions.md)
   supplies concrete proposals and identifies missing deployment/owner inputs.
2. Validate the G-15 provider fix and resolve G-13–14 with the concrete source findings and compatibility/migration
   plan, then verify authentication and encrypted-data upgrades/recovery.
3. Freeze the release/app/dependency inventory. Resolve the independently bundled
   Interpretations error and execute critical workflows, including the selected
   CSRF/TLS/session configuration. Entry-page checks do not cover all app workflows.
4. Verify deployed channels, authoritative proxy ingress, database privileges,
   audit durability/capacity actions and multi-node revocation/state consistency.
5. Complete element-level code/configuration/process traces and selected assurance
   artifacts, then obtain independent evaluation and scheme approval.

No production code, dependencies, bundle selection, branches or remotes changed
during the 2026-11-04 documentation follow-up. The prior build/test results apply
only to their recorded source hashes and test configuration; they are not new runs.
The subsequent [web verification extension](common-criteria-verification.md#2026-11-04-web-application-verification-extension)
adds 51 passing cases and one existing skip in five original upload/app/query
suites. The combined dated evidence inventory is 262 passes and four skips in
31 classes. It does not close the complete web or independent-evaluation gaps.

## Deployment and web-application checks

Use an isolated deployment built from the patched checkout and pinned application
bundles. Record version/hash/configuration and results, including failures.

1. Build the production web artifact, start it against disposable PostgreSQL and
   the intended broker/session store, and verify health and login bootstrap.
2. Verify login success/failure, password reset, configured 2FA/IdP paths, session
   rotation, idle expiry, logout and administrator session revocation.
3. Verify valid PAT reads/writes, method/IP/referrer constraints, expiry,
   post-disable denial, permission downgrade and deletion on each node.
4. Open every enabled app. Verify static assets, navigation, data reads/writes and
   denied actions for a least-privilege user. Test with the evaluated CSRF setting
   and allowed CORS origins, including preflight and denied foreign origins.
5. Verify HTTPS/certificates, HSTS, cookies, forwarded-header trust and blocked
   direct ingress; validate outbound trusted channels and credential handling.
6. Compare published, persisted and DLQ audit identifiers through transient
   database loss, full storage, broker restart and recovery; test alert delivery
   and retention privileges. Reconcile duplicates and all missing events.
7. Record PostgreSQL and Redis/non-Redis session results separately; do not infer
   one from the other. Protect test logs from passwords, tokens and health data.

## Assurance and acceptance

The engineering fixes can be reviewed and merged independently of certification.
Acceptance requires a successful production build and the agreed application
regressions. Formal conformance additionally requires the completed ST, all
selected SFR/SAR evidence, approved environment guidance, independent evaluation
and the selected certification scheme's decision. Parts 1–3 alone do not supply
all evaluation methodology/package references (see Part 1 normative references;
ISO/IEC 18045 and relevant additional ISO/IEC 15408 parts may be needed).

## Additional audit failure verification

The [integrated audit delivery/storage evidence](common-criteria/audit-delivery-failure-evidence.md)
adds real PostgreSQL privilege rejection and connection termination, production
consumer/listener rollback, bounded DLQ routing and recovery checks. All four
standalone cases passed. This follow-up adds tests, CI and evidence only; the two
production patches and existing upstream files remain as described in the
[fork maintenance guide](../fork/upstream-maintenance.md).
