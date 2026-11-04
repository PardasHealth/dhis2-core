# DHIS2 web-application scope and exclusions

Date: 2026-11-04. User direction: review DHIS2 as a web application, ignore other
features and document why. This scopes the engineering review. It does not select
an assurance level, claim a Protection Profile or certify the product.

The [component gap assessment](../common-criteria-gap-assessment.md) and its
[machine-readable source](../evidence/component-gap-assessment-2026-11-04.json)
record a scope disposition and reason for **each of the 261 components**. An
exclusion is scoped to this application/profile, not a statement that the standard
component can never apply to a web application. Revisit a feature exclusion if
business requirements, deployments or the eventual PP/ST require it.

## Retained web-application concerns

The review retains browser and API authentication, MFA, sessions, authorization,
sharing/defaults, input/output handling, data import/export, files and app
installation, audit, cryptography, secure administration and recovery. It also
retains software development, tests, guidance, vulnerability analysis and delivery
evidence relevant to an application evaluation. Alternative assurance levels remain
candidates until an assurance package is selected; they are not all mandatory.

HTTPS/TLS, proxy trust, database and broker protection, Redis/multi-node consistency,
provider entropy, trusted time, OS secret storage and backups support web security.
They are **not ignored** simply because the Java application uses another service
or platform to provide them. Assign their responsibilities explicitly and verify
the deployed boundary. Distributed requirements apply to each enabled topology.

## Excluded activities and specialized features

| Components | Scope decision and why | Security work still retained |
| --- | --- | --- |
| `FPT_PHP.1–3`, `FPT_EMS.1` | Physical tamper and emanation resistance are outside the Java/browser application TOE and belong to hosting/hardware. | Hosting assumptions, logical access, application timing/error leakage and deployment security. |
| All `APE_*`, `ACE_*` | This task evaluates an application; it does not author/evaluate a new PP, PP-Module or PP-Configuration. | Applicable ST (`ASE_*`) work and compliance with any subsequently selected existing PP. |
| `ASE_COMP.1`, `ADV_COMP.1`, `ALC_COMP.1`, `ATE_COMP.1`, `AVA_COMP.1`, all `ACO_*` | No formally composed/composite TOE using base-component evaluation evidence is selected. Ordinary use of libraries/services is not that evaluation approach. | Third-party inventory, provenance, vulnerabilities, interface protection and integration tests. |
| `FMT_LIM.1–2` | No specialized manufacturing/test-mode capability or lifecycle-availability guarantee is selected. | Production debug/test attack surface and administrative capability/access review. |
| `FCO_NRO.1–2`, `FCO_NRR.1–2`, `FDP_DAU.1–2` | No third-party-verifiable origin, receipt or data-validity evidence service is selected. Authenticated access and audit are different guarantees. | Transport/data integrity, input validation, actor attribution and audit protection. Revisit for signed clinical records or legally verifiable receipts. |
| All `FPR_*` | No special anonymous, pseudonymous, unlinkable or unobservable operation guarantee is selected for the baseline authenticated/auditable application. | Patient confidentiality, data exposure and access control. Privacy-law, minimization and de-identification needs are not waived; confirm them with the product/privacy owner. |
| `FCS_RBG.6` | DHIS2 consumes platform randomness; this scope does not provide an evaluated random-bit generation service. | Strength/entropy/provider behavior for generated application secrets; other RBG constructions depend on the selected provider, not blanket exclusion. |
| `FTA_SSL.1–2` | The baseline profile uses idle termination and explicit logout rather than a separate application screen-lock/unlock feature. | `FTA_SSL.3/4`, invalid-session denial, cookie/session protection and browser behavior. Revisit for shared-workstation locking requirements. |
| `FRU_PRS.1–2`, `FRU_RSA.2` | No subject-priority service or guaranteed minimum resource reservation is selected. | Maximum quotas, capacity/availability and denial-of-service resistance. Revisit if clinical service priorities or reservations are required. |

Information flow, retention, residual data, stored-data protection, replay, fault
tolerance and runtime security testing remain related review candidates. They
are not automatically excluded on the ground that this is a web application.
Their exact operations and allocation need an ST and deployment decision.

## Web verification boundary

The existing record establishes a production build, selected authentication/audit/
ACL tests and browser/API checks under an isolated local profile. It does not
establish every enabled app workflow, a production TLS/CSRF profile, full tracker
object authorization, comprehensive injection/file-upload testing or a current
complete dependency/penetration assessment. See the [web attack-surface review](web-attack-surface-review.md)
and the [gap register](../common-criteria-review.md#gap-register). Do not interpret
an exclusion as a passing test or mark the remaining web gaps closed.
The [solution plan](web-gap-solutions.md) supplies concrete closure and acceptance
work for every registered gap and the additional web coverage concerns.
