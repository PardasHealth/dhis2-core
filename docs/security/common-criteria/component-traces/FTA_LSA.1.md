# FTA_LSA.1 — Limitation on scope of selectable attributes

ISO/IEC 15408-2:2026, clause 17.3.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fta-lsa-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FTA_LSA.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Session, logout, concurrency and PAT-constraint controls provide candidates; Redis and non-Redis behavior and all selected policy cases require verification.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
- [RedisSpringSessionConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/RedisSpringSessionConfig.java)
- [NonRedisSessionConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/NonRedisSessionConfig.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

## Work required before closure

PAT request constraints are candidates, but full restrictions on selectable session security attributes are not mapped. Specify attributes, authorizing conditions and negative selection tests.

Proposed owner: Session / authentication / product security.

Solution plans: [G-05](../web-gap-solutions.md#g-05), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
