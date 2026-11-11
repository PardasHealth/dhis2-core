# FCS_COP.1 — Cryptographic operation

ISO/IEC 15408-2:2026, clause 10.4.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fcs-cop-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [PasswordEncoderConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/PasswordEncoderConfig.java)
- [ApiKeyTokenGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java)

Symbols to inspect: `PasswordEncoderConfig.passwordEncoder`; `ApiKeyTokenGenerator.hashToken`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FCS_COP.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Encryption configuration, RSA key generation/loading and key publication are evidence candidates, not an approved cryptographic profile. Strict encryption-key startup checks and unchanged-cipher compatibility tests are a related configuration guard; algorithms and selected operations still require approval.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [PasswordManagerTest.java](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/PasswordManagerTest.java)
- [ApiKeyTokenGeneratorTest.java](../../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java)
- [HibernateEncryptionPolicyTest.java](../../../../dhis-2/dhis-support/dhis-support-hibernate/src/test/java/org/hisp/dhis/config/HibernateEncryptionPolicyTest.java)

Related boundary references, **not implementation coverage**:

- [HibernateEncryptionConfig.java](../../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java)
- [AuthorizationServerConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthorizationServerConfig.java)
- [KeyStoreUtil.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/KeyStoreUtil.java)
- [PublicKeysController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/PublicKeysController.java)

## Work required before closure

Password/token/PBE/signing operations exist; complete approved algorithm/parameter/provider correspondence is missing, and a legacy 3DES bean remains configured. Inventory active usages and migrate safely under a selected profile.

Proposed owner: Security / platform / data migration owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10), [G-13](../web-gap-solutions.md#g-13).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
