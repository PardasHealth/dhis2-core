# FIA_UID.1 — Timing of identification

ISO/IEC 15408-2:2026, clause 12.8.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-uid-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
- [AuthenticationController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/AuthenticationController.java)

Symbols to inspect: `DhisWebApiWebSecurityConfig.configureMatchers`; `AuthenticationController.login`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_UID.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FIA_UID.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Filter-chain permissions, multiple providers, 2FA verification and account management are candidate paths; complete authentication protocol coverage is outstanding.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AuthenticationControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java)
- [LoginConfigCorsTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/LoginConfigCorsTest.java)

Related boundary references, **not implementation coverage**:

- [TwoFactorAuthenticationProvider.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
- [TwoFactorAuthUtils.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java)
- [UserAccountController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/UserAccountController.java)

## Work required before closure

Permitted actions before identification are not fully selected. Enumerate public actions and verify identity binding before all other actions.

Proposed owner: Authentication / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-14](../web-gap-solutions.md#g-14), [G-15](../web-gap-solutions.md#g-15).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
