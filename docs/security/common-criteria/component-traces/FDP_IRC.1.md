# FDP_IRC.1 — Information retention control

ISO/IEC 15408-2:2026, clause 11.9.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-irc-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_IRC.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FDP_IRC.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Deletion/retention paths are related boundaries; ordinary deletion does not prove irrecoverable erasure of media, replicas or residual memory.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [FileResourceRetentionStrategy.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/fileresource/FileResourceRetentionStrategy.java)
- [DefaultDeletionManager.java](../../../../dhis-2/dhis-support/dhis-support-system/src/main/java/org/hisp/dhis/system/deletion/DefaultDeletionManager.java)

## Work required before closure

Retention/deletion paths do not prove irreversible untraceable erasure or blocked access pending erasure. Define retained data, deadlines, backups/replicas and verify erasure plus pending-access controls.

Proposed owner: Data retention / database / platform owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
