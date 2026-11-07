# FMT_MSA.4 — Security attribute value inheritance

ISO/IEC 15408-2:2026, clause 13.5.9. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fmt-msa-4)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FMT_MSA.4.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Sharing defaults, inheritance and import mapping provide candidate traces. FMT_MSA.3 additionally has an element-level document.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [DefaultAclService.java](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java)
- [DefaultCascadeSharingService.java](../../../../dhis-2/dhis-services/dhis-service-reporting/src/main/java/org/hisp/dhis/dashboard/impl/DefaultCascadeSharingService.java)
- [SharingMapper.java](../../../../dhis-2/dhis-tracker/src/main/java/org/hisp/dhis/tracker/imports/preheat/mappers/SharingMapper.java)

## Work required before closure

Cascade/import mappings are candidates, but inheritance and override rules are not fully specified. Define parent/child propagation, exceptions and authorized modifications and test them.

Proposed owner: Access-control engineering / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
