# FIA_UAU.5 — Multiple authentication mechanisms

ISO/IEC 15408-2:2026, clause 12.7.10. Trace update: 2026-11-09; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-uau-5)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [TwoFactorAuthenticationProvider.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)
- [TwoFactorReplayService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java)
- [TwoFactorAuthUtils.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java)
- [TwoFactorController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/TwoFactorController.java)

Symbols to inspect: `TwoFactorAuthenticationProvider`; `ApiTokenAuthManager`; `TwoFactorReplayService.consume`; `TwoFactorAuthUtils verification helpers`; `TwoFactorController.generateQRCode`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_UAU.5.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FIA_UAU.5.2

**Partial source mapping.** [TwoFactorAuthenticationProvider.java:171](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java#L171): The provider rejects unavailable/malformed enrolled factor types. Enabled login factors delegate to database-backed single-use validation before authentication can return. The delegated guard directly locks the primary user row and rejects a stale secret/type snapshot or deleted user at proof consumption. The consumption guard additionally requires a current matching password hash, enabled/local-login account state and non-expired account.

**Open:** Login-scoped provider correction only. Full AFTA provider-combination policy, other verification purposes, trusted clocks, recovery and deployed multi-node acceptance remain open. See ../afta-login-replay-protection.md. The 11 November freshness follow-up is traced in ../afta-factor-state-freshness.md. See ../afta-account-state-freshness.md for the bounded account-state follow-up.

## Evidence and limits

The provider rejects unavailable enrolled factors and requires a committed database-backed login challenge/step reservation. Targeted provider, H2/API and PostgreSQL tests cover replay and failure boundaries; see the dated 2026-11-09 replay evidence. Complete selected mechanism-combination coverage remains open. The MFA consumption guard rechecks the primary password hash, disabled/external-only flags and account expiry under its row lock; other providers/roles/sessions remain outside that check. Legacy QR GET/HEAD reads preserve a nonempty pending TOTP seed; explicit POST rotation and the bundled image route are retained. Initial GET state change and distributed enrollment races remain open.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ApiTokenAuthManagerTest.java](../../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java)
- [TwoFactorAuthenticationPolicyTest.java](../../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java)
- [TwoFactorLoginPolicyTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorLoginPolicyTest.java)
- [TwoFactorReplayServiceTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayServiceTest.java)
- [TwoFactorReplayPostgresTest.java](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayPostgresTest.java)
- [TwoFactorQrStabilityTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorQrStabilityTest.java)

Related boundary references, **not implementation coverage**:

- [DhisWebApiWebSecurityConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
- [TwoFactorAuthUtils.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java)
- [UserAccountController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/UserAccountController.java)
- [UsedTwoFactorCode.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/UsedTwoFactorCode.java)
- [R__PardasHealth_two_factor_login_replay.sql](../../../../dhis-2/dhis-support/dhis-support-db-migration/src/main/resources/org/hisp/dhis/db/migration/pardas/R__PardasHealth_two_factor_login_replay.sql)

## Work required before closure

Login downgrade and single-use implementation subgaps are corrected. Define full AFTA combination/cross-purpose rules and verify all provider/SSO paths, broader account/password/role/session freshness, clocks, database recovery, browser flows and deployed multi-node behavior. The component remains partial.

Proposed owner: Authentication / product security.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-14](../web-gap-solutions.md#g-14), [G-15](../web-gap-solutions.md#g-15).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
