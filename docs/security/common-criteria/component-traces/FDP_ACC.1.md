# FDP_ACC.1 — Subset access control

ISO/IEC 15408-2:2026, clause 11.3.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-acc-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DefaultAclService.java](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java)

Symbols to inspect: `DefaultAclService.canRead/canWrite/canDataRead`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_ACC.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Server-side sharing/access checks and existing ACL regression evidence cover selected operations, not every TSF interface.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AclServiceTest.java](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java)
- [MeControllerAclTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/MeControllerAclTest.java)

Related boundary references, **not implementation coverage**:

- [SecurityCheck.java](../../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/objectbundle/validation/SecurityCheck.java)

## Work required before closure

ACL checks and selected tests exist, but subjects/objects/operations in the chosen subset are not fully enumerated. Map enforcement and bypass/negative cases for that subset.

Proposed owner: Access-control engineering / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
