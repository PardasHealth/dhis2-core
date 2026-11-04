# Evaluation decisions needed to close the review

Prepared: 2026-11-04. Status: proposed inputs, not an accepted Security Target.
This worksheet makes the remaining scope decisions reviewable. The existing
engineering fixes and tests do not depend on treating these proposals as approved.
The user has directed a web-application focus and documented exclusion of other
features; the [scope rationale](web-application-scope.md) applies that direction.
Specific deployment, crypto, factor/recovery and assurance choices below remain open.

Common Criteria evaluates a specified TOE against selected, completed requirements.
It does not require a web application to implement every component in Parts 2/3.
For example, privacy observer guarantees, physical resistance, alternative
assurance levels and PP/composition evaluation activities need scope decisions.
The [261-component gap assessment](../common-criteria-gap-assessment.md) retains
these decisions explicitly instead of claiming blanket approval or exclusion.

| Decision | Concrete proposal | Still needed |
| --- | --- | --- |
| Product/version | DHIS2-Core baseline `07d4f3f7ea` with the recorded security patch, exact dependency and enabled-app hashes. Include the browser applications in compatibility/release acceptance. | Freeze a release commit/build; resolve mutable/SNAPSHOT inputs and the locally built dependency provenance caveat. Select which app source is inside the evaluated TOE. |
| Deployment | HTTPS reverse proxy with blocked direct ingress; controlled PostgreSQL, persistent Artemis volumes and trusted clock. Treat Redis and external identity providers as explicit optional evaluated configurations. | Supply actual topology, versions, TLS/certificate configuration, secrets provisioning and administrative procedures. Decide which platform parts are inside the TOE versus justified environment responsibilities. |
| Authentication | Current account/token state; specified revocation timing; explicit password/provider/2FA combination rules. Recommend denying an enrolled factor when its type is unavailable unless a documented authorized recovery procedure applies. | Decide G-14/15 single-use, skew, fallback and emergency recovery semantics, then implement/test cross-node behavior and browser compatibility. |
| Authorization | Server-enforced roles, sharing and organisation-unit rules with documented privileged exceptions and object-specific defaults. | Complete SFP/endpoint/object mapping. Select FMT_MSA.3 defaults per object type rather than assuming all existing metadata is private. |
| Audit | Attributable selected security events, protected durable storage, restricted review/retention, capacity warnings and explicit storage-full behavior. | Select events, retention, loss/durability metrics, recipients and whether affected business operations stop when evidence cannot be retained. Run persistent crash/capacity tests. |
| Encryption | Explicitly provisioned secrets and selected approved crypto profile; inventory encrypted fields and support safe legacy-data migration. | Decide algorithms/providers/parameters and key lifecycle; prove backup, upgrade, re-encryption and rollback without losing readable data (G-13). |
| Browser security | Verify supported CSRF protection and secure cookies/CSP/CORS with every enabled app and login/integration path. | Supply intended origins/proxy profile, resolve Interpretations bundle defect in its owning source and execute critical workflows beyond page loading. |
| PP and assurance | Select a recognized scheme/PP and an assurance package with the evaluator; retain the source review as preparatory evidence. | Product owner/evaluator identify mandatory PP, target assurance level, attack potential and required delivery/development/independent artifacts. No EAL or PP is currently claimed. |

After these decisions, complete SFR assignments/selections, dependencies and
threat/objective rationale, then create an element-level implementation and
test/evidence map for the selected requirements. The existing `FMT_MSA.3` trace
is an example of that depth, with its remaining gaps disclosed. Source links,
test totals and this worksheet alone do not close a selected requirement.
