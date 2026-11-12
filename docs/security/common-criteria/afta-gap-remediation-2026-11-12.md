# Selected AFTA gaps: 12 November 2026 follow-up

Scope: `afta-selected-requirements-review.csv` and its authoritative JSON source
only. Preserve all 82 source rows, the row 80/81 duplicate and optional rows
63/64/66. No additional Common Criteria components are added to the work list.

## Changes made

The [requirement traceability register](afta-requirement-traceability.md) and
[CSV](afta-requirement-traceability.csv) connect each original row to its existing
code or boundary references, remaining acceptance work and explicitly linked
implementation follow-ups. Six rows have follow-up execution records linked in
the checklist: 13, 14, 36, 47, 65 and 66. The crypto iteration references remain
boundary context; their definitions remain unresolved. Other rows explicitly
show that item-specific execution evidence is not linked in this register;
this does not mean the repository contains no related tests or evidence.

For linked follow-ups, the register resolves implementation commits to dated
JSON records and recorded test classes to source files. It preserves historical
hashes, lists files that have since changed, and separately lists suite test
sources without a recorded hash. Historical records are not rewritten to imply
current verification. Shared suites are not summed as independent test passes.

The fork regression workflow now validates the AFTA checklist and generated
traceability with Python, including negative checks for missing evidence,
failed/skipped evidence, stale source anchors, changed source hashes and inferred
closure. It also includes the existing `TwoFactorControllerTest` and
`UserTwoFactorAuditControllerTest` in the H2 authentication command and required
report checks. These suites were present in the row 36 remediation evidence but
omitted from that CI command. This fixes a regression-coverage gap for enrollment
and its audit behavior; it is not a new authentication implementation.

## Verification performed in this follow-up

From the repository root:

```bash
python3 docs/security/tools/render_afta_selected_review.py --check
python3 docs/security/tools/render_afta_traceability.py --check
python3 -m unittest discover -s docs/security/tools -p test_afta_traceability.py
git diff --check
```

The two generation checks pass; eight validator tests pass. These are document
and evidence-integrity checks, not Java security tests. No new Java execution,
hosted CI result, deployed browser acceptance or full AFTA item closure is
claimed. CI must actually execute the newly selected existing suites and retain
their XML reports before new regression evidence can be recorded.

The workflow now also runs on pushes to the `work` branch. Previously its
pull-request/manual-only triggers meant that publishing directly to `work`
did not automatically collect regression evidence. Enabling this trigger does
not itself establish a successful hosted run.

## Remaining requirements and blockers

The primary work list remains 78 unique entries not explicitly optional:
43 partial, ten gaps and 25 definition/edition questions; none fully closed.
Use each row's `remaining_acceptance` in the register as its closure criterion.

The user's subsequent [source excerpt](afta-source-provenance.md) confirms the
selected identifiers, product/organization headers and visible page markers.
The exact AFTA source title/version, extension clauses, crypto iterations and
assignments/selections have not been supplied. Rows 9–12 also have an edition
numbering conflict; row 22 has a provisional identifier normalization and rows
80/81 repeat an identifier. Do not substitute ISO edition numbering or guess
cryptographic operations to close these items.

All ten gap rows need decisions or deployment evidence before implementation
can establish the selected guarantee:

| Source rows | Required input before closure |
| --- | --- |
| 20 | Protected residual-data resources, disposal/reuse boundary and backup/media responsibilities. |
| 27–28 | Stored data covered by integrity protection; tamper scenarios; approved detection, alert and recovery policy. |
| 48 | Trusted peer inventory, security-attribute schema and conflict/precedence contract. |
| 49 | Authoritative clock, permitted drift, synchronization/failure policy and evaluated deployment. |
| 52 | Required degraded functions, fault classes and service/data acceptance limits. |
| 56–58 | Exact previous-access-history clauses and required presentation/acknowledgement behavior. |
| 59 | Selected user/time/location/provider session-establishment conditions. |

Partial rows also require the selected production configuration and item-specific
positive/negative execution evidence. Audit full-storage behavior and retention,
MFA recovery/cross-purpose use, production CSRF-aware login, trusted connections
and key lifecycle retain their existing acceptance work. Introducing arbitrary
policies or changing cryptographic formats would not establish these requirements.

When definitions arrive, implement and test each bounded behavior change in the
existing owning service, add its dated execution record and implementation
follow-up to the selected JSON, then regenerate both AFTA reports. Keep failed
and skipped outcomes, source revision, configuration, test identities and residual
limits. Promote a full requirement only after its complete acceptance review.
