# FIA_AFL.1 — Authentication failure handling

ISO/IEC 15408-2:2026, clause 12.3.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-afl-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [TwoFactorAuthenticationProvider.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
- [DefaultUserService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java)

Symbols to inspect: `TwoFactorAuthenticationProvider.checkLockout`; `DefaultUserService`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_AFL.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FIA_AFL.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Current-user/token lookup and lockout paths were reviewed with the recorded authentication tests; all providers and nodes are not covered.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AuthenticationControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java)

Related boundary references, **not implementation coverage**:

- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

## Work required before closure

Lockout exists, but selected thresholds, counters, reset behavior, all providers and distributed operation are not fully verified. Test policy and bypass cases across nodes.

Proposed owner: Authentication engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
