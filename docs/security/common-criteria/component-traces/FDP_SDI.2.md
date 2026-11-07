# FDP_SDI.2 — Stored data integrity monitoring and action

ISO/IEC 15408-2:2026, clause 11.15.7. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-sdi-2)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_SDI.2.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FDP_SDI.2.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Audit serialization and restricted-role tests do not prove general stored-data tamper monitoring.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [JdbcAuditRepository.java](../../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java)

## Work required before closure

No complete stored-data tamper detection plus selected action is demonstrated. Specify alarms/recovery and test persistent tampering and detection failure.

Proposed owner: Persistence / security architecture.

Solution plans: [G-04](../web-gap-solutions.md#g-04), [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
