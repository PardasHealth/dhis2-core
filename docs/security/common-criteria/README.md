# DHIS2 web-application Common Criteria review guide

Review baseline: `07d4f3f7ea`, DHIS2 `2.44-SNAPSHOT`, plus the recorded security
patches. Original documentation date: 2026-11-04. Current implementation follow-ups
are summarized in the [progress snapshot](afta-remediation-progress.md#current-progress-snapshot).

The engineering package documents web scope, component dispositions, code/test
traces, known findings and proposed solutions. **It is not a claim that the full
source, all app workflows or all selected normative elements passed evaluation.**
Formal approval and the open web-security/compatibility gaps remain unresolved.

## Current AFTA progress

Start with the [current progress snapshot and charts](afta-remediation-progress.md#current-progress-snapshot),
[remaining gap acceptance](afta-remediation-progress.md#remaining-gaps-and-acceptance),
and [complete selected assessment](afta-selected-requirements-review.md).
The snapshot is generated from the authoritative checklist and linked implementation records.
Dated follow-ups preserve their historical counts; they are not the current status summary.

## Document guide

| Review question | Document |
| --- | --- |
| Are token-generation buffers cleared without erasing returned tokens? | [Row 20: generation ownership and failed-wrapper cleanup](afta-token-generation-buffer-hygiene.md) |
| Are hash/CRC encoding buffers cleared? | [Row 20: owned UTF-8 buffers and compatibility checks](afta-hash-crc-buffer-hygiene.md) |
| Are token-validator copies cleared too? | [Row 20: checksum-buffer cleanup and caller ownership](afta-token-checksum-buffers.md) |
| What residual-data gap work has started? | [Row 20: token buffer cleanup and remaining resource scope](afta-token-secret-buffer.md) |
| Can downstream failures be mistaken for token rejection? | [Token failure boundary and selected source rows](afta-token-failure-boundary.md) |
| Which account receives mapped OIDC event bookkeeping? | [UID binding, selected source rows and regression evidence](afta-oidc-event-uid-binding.md) |
| Does login success retain request context without an OTP? | [Sanitized success details and regression evidence](afta-login-success-context.md) |
| Do rejected MFA proofs reach audit and account lockout? | [Failure-event mapping and HTTP regressions](afta-mfa-failure-events.md) |
| How is an email login challenge bound to the principal? | [Authenticated UID routing and regression evidence](afta-login-email-challenge-uid.md) |
| How is the email disable challenge bound to its selected account? | [Target continuity and regression evidence](afta-email-challenge-identity.md) |
| How are TOTP provisioning values escaped? | [URI component encoding and regression evidence](afta-totp-provisioning-uri.md) |
| Where is the recent requirements-to-code trace? | [Methods, regression cases, implementation commits and evidence](afta-code-traceability.md) |
| How are persisted current-user/MFA targets protected against name reuse? | [Authenticated UID routing, entity-bound mutations and HTTP regressions](afta-current-user-uid-binding.md) |
| How are concurrent failure counters updated? | [Local/Redis atomicity, tests and required Redis permissions](afta-atomic-failure-counters.md) |
| Which AFTA gaps have implementation changes? | [Remediation progress](afta-remediation-progress.md), [login replay protection and upgrade notes](afta-login-replay-protection.md) |
| How are API tokens bound to their owner? | [PAT owner UID validation, regressions and evidence](afta-token-owner-binding.md) |
| How do token IP constraints handle forwarding headers? | [Servlet-address enforcement, proxy compatibility and evidence](afta-token-ip-constraints.md) |
| How does tracked-entity audit review apply paging and filters? | [Audit-review fixes, seven regressions and evidence](afta-audit-review-paging.md) |
| What is the result for the user-selected AFTA list? | [Selected AFTA requirements, gaps and percentages](afta-selected-requirements-review.md) |
| How does each selected AFTA row trace to code, tests and dated evidence? | [AFTA-only traceability register](afta-requirement-traceability.md), [CSV](afta-requirement-traceability.csv), [current work and acceptance](afta-remediation-progress.md#current-progress-snapshot) |
| What source excerpt identifies the selected list? | [AFTA source provenance and transcription limits](afta-source-provenance.md) |
| What should management know? | [Plain-language gap report](management-gap-report.md) |
| What is in scope, and why are features excluded? | [Web scope and exclusions](web-application-scope.md) |
| Which code covers a key such as `FMT_MSA.3`? | [Component-to-code index](../common-criteria-component-code-index.md), [CSV](../common-criteria-component-code-index.csv), [199 individual component pages and exclusions](component-traces/README.md), [detailed FMT_MSA.3 element trace](component-traces/FMT_MSA.3.md) |
| What is the disposition of every standard component? | [261-component gap assessment](../common-criteria-gap-assessment.md), [machine-readable dispositions](../evidence/component-gap-assessment-2026-11-04.json) |
| What was fixed, and which gaps are open? | [Review and prioritized register](../common-criteria-review.md) |
| What should be done, and how will closure be tested? | [G-01–G-15 and W-01–W-10 solutions and acceptance](web-gap-solutions.md) |
| Which web attack surfaces were considered? | [Web attack-surface review](web-attack-surface-review.md) |
| What are the exact encryption and MFA findings? | [Source findings and migration/recovery concerns](crypto-and-two-factor-gaps.md) |
| Which tests/build/browser checks actually ran? | [Verification record](../common-criteria-verification.md), [original execution evidence](../evidence/review-2026-11-03.json), [web extension evidence](../evidence/review-2026-11-04.json) |
| What configuration/assurance decisions remain? | [Decision worksheet](evaluation-decisions.md), [Part 1 model and traceability](../common-criteria-traceability.md) |
| How should the fork continue tracking upstream? | [Upstream maintenance guide](../../fork/upstream-maintenance.md) |

## Reading the conclusions

All **155 Part 2** and **106 Part 3** IDs have a baseline scope/gap disposition.
The inventory corrects five previously omitted composite assurance components.
There are **199 related component candidates**, including platform/distributed
and alternative assurance levels, and **62 excluded activities or specialized
features not selected in this baseline profile**. These counts are scope triage,
not 199 mandatory implemented requirements or 62 passing requirements. Every row
has a reason; no physical/platform responsibility is assumed to disappear.

The code index distinguishes **45 code/artifact candidate rows** from related
boundary references. Candidate code is partial evidence. Most rows are not
complete element-by-element implementation mappings; selected operations,
dependencies and coverage still need an ST and supporting evidence.

The production patch fixes stale PAT authorization/shared authentication objects
and audit-buffer loss on publication failure. The 2026-11-09 provider fix also
rejects disabled enrolled second factors. Login replay reservation is also implemented; see the remediation progress record. The replay follow-up also changes the OTP utility and adds two internal persistence/service classes plus an additive migration;
other additions are focused tests, fork CI and documentation. No change to
dependencies or app selection is implied by this review package.

The original November 3–4 test inventory contains **262 passes, four existing skips, 31 classes**
(266 total cases). The web extension adds 51 passes/one skip to the original
evidence; this is not one full-suite invocation. A production build and selected
browser/API checks were recorded. The Interpretations bundle error, broader
workflows, production TLS/CSRF/topology behavior, encryption migration, 2FA policy/
replay behavior and complete vulnerability/independent evidence remain open.
Execution records preserve conditions at capture time, including publication
status; Git history and branch state determine subsequent publication.

The component-traces folder now inventories the normative element IDs for all
199 retained components. Additional partial element mappings cover PAT
expiry, revocation, subject binding and logout. Unmapped elements are explicitly
open. This expansion does not change the implementation coverage conclusions.

## Maintaining this package

Edit component dispositions in the JSON source, then regenerate the index, CSV
and gap report and individual trace pages from the repository root. Element IDs
and partial mappings are maintained in
`../evidence/component-trace-elements-2026-11-07.json`; the FMT_MSA.3 page remains
manually maintained. Run:

```bash
python docs/security/tools/render_common_criteria_review.py
python docs/security/tools/render_common_criteria_review.py --check
```

The checker validates counts, identifiers, required disposition fields,
source/test existence, solution-anchor coverage and generated consistency. It
does not prove Common Criteria conformance. Review source changes against the
recorded hashes, rerun affected security and browser tests when behavior changes,
and preserve dated evidence rather than rewriting failed/skipped outcomes as
passes. Follow the solution plan's closure record requirements before changing
an open gap's status.

- [11 November: current factor state at login consumption](afta-factor-state-freshness.md).

- [11 November: optional external broker TLS and code trace](afta-external-broker-tls.md).

- [11 November: current account state during MFA login](afta-account-state-freshness.md).

- [11 November: explicit encryption-key startup guard](afta-explicit-encryption-key.md).

- [11 November: stable pending TOTP QR reads](afta-pending-qr-stability.md).

- [13 November: authentication audit record fields](afta-authentication-audit-records.md).

## Audit-record integrity follow-up — 18 November 2026

Selected source rows **27 (`FDP_SDI.2.1`) and 28 (`FDP_SDI.2.2`)** now have [opt-in audit integrity implementation and evidence](afta-audit-record-integrity.md). Preserve the fork repeatable migration, confidential independent signing-key configuration and pre-decode verification when integrating upstream changes. Defaults remain off; do not sign historical records without trusted provenance. Both rows have partial evidence, with rollout and full acceptance open.

- [Row 49: monotonic audit-buffer timing, code and tests](afta-audit-monotonic-delay.md). Trusted-time acceptance remains open.

- [Row 52: concurrent audit-buffer capacity and recovery](afta-audit-buffer-capacity.md). Whole-application fault-tolerance acceptance remains open.

- [Row 52: audit destination failures and retained-event recovery](afta-audit-destination-failure.md). Full fault-tolerance acceptance remains open.

- [Rows 1/52: audit payload serialization failure boundary](afta-audit-payload-failure.md). Full acceptance remains open.

- [Row 48: reject ambiguous audit-message JSON](afta-audit-message-validation.md). Full peer/schema/conflict-policy acceptance remains open.

- [Row 48: audit destination/scope binding](afta-audit-destination-scope.md). Full publisher trust and schema policy remain open.

- [Rows 2/48: audit event type and enum validation](afta-audit-event-type.md). Full audit schema and peer-policy acceptance remain open.

- [Row 20: TOTP validation-owned secret buffer cleanup](afta-totp-validation-buffer.md). Full residual-protection acceptance remains open.

- [Row 20: TOTP enrollment-owned raw seed cleanup](afta-totp-enrollment-buffer.md). Full residual-protection acceptance remains open.

- [Row 46: TOTP failure diagnostics without secret-bearing exceptions](afta-totp-failure-diagnostics.md). Full failure-model acceptance remains open.

- [Row 46: TOTP provisioning QR failure diagnostics](afta-totp-qr-failure.md). Full failure-model acceptance remains open.

- [Row 20: replay fingerprint-owned challenge buffer cleanup](afta-replay-fingerprint-buffer.md). Full residual-protection acceptance remains open.

- [Row 20: TOTP QR-owned buffer cleanup](afta-qr-owned-buffers.md). Full residual-protection acceptance remains open.

- [Row 20: audit payload codec-owned buffer cleanup](afta-audit-codec-buffers.md). Full residual-protection acceptance remains open.

- [Row 20: generated secret character ownership](afta-generated-secret-characters.md). Full residual-protection acceptance remains open.

- [Rows 20/46/48: PKCE S256 failure and derived parameter policy](afta-pkce-failure-policy.md). Remove reserved PKCE extras for generated requests; full acceptance remains open.
