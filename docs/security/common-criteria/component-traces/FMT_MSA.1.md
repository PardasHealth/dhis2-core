# FMT_MSA.1 — Management of security attributes

ISO/IEC 15408-2:2026, clause 13.5.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fmt-msa-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DefaultSharingService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/sharing/DefaultSharingService.java)
- [DefaultAclService.java](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java)

Symbols to inspect: `DefaultSharingService.saveSharing`; `DefaultAclService.canManage/canMakeClassPublic/canMakeClassPrivate`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FMT_MSA.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Sharing defaults, inheritance and import mapping provide candidate traces. FMT_MSA.3 additionally has an element-level document.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [SharingControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/SharingControllerTest.java)
- [AclServiceTest.java](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java)

Related boundary references, **not implementation coverage**:

- [DefaultCascadeSharingService.java](../../../../dhis-2/dhis-services/dhis-service-reporting/src/main/java/org/hisp/dhis/dashboard/impl/DefaultCascadeSharingService.java)
- [SharingMapper.java](../../../../dhis-2/dhis-tracker/src/main/java/org/hisp/dhis/tracker/imports/preheat/mappers/SharingMapper.java)

## Work required before closure

Sharing-management access checks exist, but complete attribute/role/action mapping is absent. Specify who may set/change each selected security attribute.

Proposed owner: Access-control engineering / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
