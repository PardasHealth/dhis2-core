# FIA_SOS.2 — TSF Generation of secrets

ISO/IEC 15408-2:2026, clause 12.6.7. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-sos-2)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [ApiKeyTokenGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java)

Symbols to inspect: `ApiKeyTokenGenerator.generatePersonalAccessToken`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_SOS.2.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FIA_SOS.2.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Secret generation and account policy paths are candidates; provider strength and all credential lifecycle paths are not established.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ApiKeyTokenGeneratorTest.java](../../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java)

Related boundary references, **not implementation coverage**:

- [CodeGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CodeGenerator.java)
- [TwoFactorAuthService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java)
- [DefaultUserService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java)

## Work required before closure

Generated codes/tokens use random helpers, but selected strength/format, privileged access and entropy assurance are missing. Verify generation and all distribution/storage paths.

Proposed owner: Authentication / security engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
