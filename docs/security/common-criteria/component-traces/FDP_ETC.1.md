# FDP_ETC.1 — Export of user data without security attributes

ISO/IEC 15408-2:2026, clause 11.6.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-etc-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DefaultMetadataExportService.java](../../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/DefaultMetadataExportService.java)

Symbols to inspect: `DefaultMetadataExportService.getMetadata/validate`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_ETC.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FDP_ETC.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Export services are candidate enforcement paths; complete attribute and authorization semantics are not mapped.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [DefaultDataExportService.java](../../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/datavalueset/DefaultDataExportService.java)

## Work required before closure

Export paths exist; removal of security attributes and enforcement of the selected export policy across formats is not fully mapped. Test unauthorized exports and attribute stripping.

Proposed owner: Data exchange / access-control engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
