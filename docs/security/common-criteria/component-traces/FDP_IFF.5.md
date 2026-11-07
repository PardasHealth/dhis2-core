# FDP_IFF.5 — No illicit information flows

ISO/IEC 15408-2:2026, clause 11.8.10. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-iff-5)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_IFF.5.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Object ACL and browser-origin controls were considered; they are not a formally specified information-flow policy or covert-channel analysis.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [DefaultAclService.java](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java)
- [DhisCorsProcessor.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/DhisCorsProcessor.java)

## Work required before closure

No evidence eliminating all illicit flows in the selected scope was located. Supply comprehensive channel analysis and elimination evidence.

Proposed owner: Security architecture / product owner.

Solution plans: [G-01](../web-gap-solutions.md#g-01), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
