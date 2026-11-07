# ADV_ARC.1 — Security architecture description

ISO/IEC 15408-3:2026, clause 10.2.4. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Assurance evidence incomplete.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#adv-arc-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)

Symbols to inspect: `security configuration`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### ADV_ARC.1.1D

**Unmapped — Developer delivery.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.1E

**Unmapped — Evaluator activity.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.1C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.2D

**Unmapped — Developer delivery.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.2C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.3D

**Unmapped — Developer delivery.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.3C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.4C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ADV_ARC.1.5C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Source/configuration show architectural pieces but no complete TSF security architecture argument.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

None established in this review.

## Work required before closure

Provide domain separation, self-protection, initialization and non-bypass arguments linked to TSFI and selected SFRs. Required artifact: Security architecture description.

Proposed owner: Security assurance owner / evaluator.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-11](../web-gap-solutions.md#g-11).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
