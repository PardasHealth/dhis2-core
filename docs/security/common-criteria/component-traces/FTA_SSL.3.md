# FTA_SSL.3 — TSF-initiated termination

ISO/IEC 15408-2:2026, clause 17.5.8. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fta-ssl-3)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
- [RedisSpringSessionConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/RedisSpringSessionConfig.java)
- [SessionController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/SessionController.java)

Symbols to inspect: `DhisWebApiWebSecurityConfig.configureMatchers`; `RedisSpringSessionConfig.sessionRepository`; `SessionController`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FTA_SSL.3.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Session, logout, concurrency and PAT-constraint controls provide candidates; Redis and non-Redis behavior and all selected policy cases require verification.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AuthenticationControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java)
- [SessionControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/SessionControllerTest.java)

Related boundary references, **not implementation coverage**:

- [NonRedisSessionConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/NonRedisSessionConfig.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

## Work required before closure

Idle expiry configuration is partial evidence; actual timing, refresh behavior and every Redis/non-Redis session path remain open. Test inactivity boundary and stale-cookie denial.

Proposed owner: Session / authentication / product security.

Solution plans: [G-05](../web-gap-solutions.md#g-05), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
