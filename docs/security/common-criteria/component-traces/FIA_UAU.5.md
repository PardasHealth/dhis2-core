# FIA_UAU.5 — Multiple authentication mechanisms

ISO/IEC 15408-2:2026, clause 12.7.10. Trace update: 2026-11-09; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-uau-5)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [TwoFactorAuthenticationProvider.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

Symbols to inspect: `TwoFactorAuthenticationProvider`; `ApiTokenAuthManager`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_UAU.5.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FIA_UAU.5.2

**Partial source mapping.** [TwoFactorAuthenticationProvider.java:168](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java#L168): The existing two-factor provider rejects enrolled logins when the globally configured factor is disabled, unsupported or missing, rather than accepting a password-only downgrade. Enabled types continue through factor validation.

**Open:** This traces the disabled-factor policy correction only. Full selected AFTA/provider-combination rules, replay protection, browser recovery, SSO and multi-node acceptance remain open. See ../afta-remediation-progress.md for dated evidence.

## Evidence and limits

Multiple provider paths are identified. On 2026-11-09 the existing provider was changed to reject globally disabled enrolled factors, with focused policy and API regressions; complete selected provider-combination coverage remains open.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ApiTokenAuthManagerTest.java](../../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java)
- [TwoFactorAuthenticationPolicyTest.java](../../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java)
- [TwoFactorLoginPolicyTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorLoginPolicyTest.java)

Related boundary references, **not implementation coverage**:

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
- [TwoFactorAuthUtils.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java)
- [UserAccountController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/UserAccountController.java)

## Work required before closure

The disabled enrolled-factor password-only downgrade is corrected. Define remaining AFTA combination/replay rules and verify every selected provider, browser recovery and multi-node flow; this component remains partially evidenced, not fully closed.

Proposed owner: Authentication / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-14](../web-gap-solutions.md#g-14), [G-15](../web-gap-solutions.md#g-15).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
