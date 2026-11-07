# FCS_RNG.1 — Random number generation

ISO/IEC 15408-2:2026, clause 10.6.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fcs-rng-1)

## Web scope

Related; application and platform evidence required. A web application relies on JVM/OS/provider randomness, key memory handling and trusted time. These are supporting responsibilities rather than ignored hardware features; identify allocation and evidence in the deployment profile. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [ApiKeyTokenGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java)

Symbols to inspect: `ApiKeyTokenGenerator.generatePersonalAccessToken`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FCS_RNG.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FCS_RNG.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

JDK SecureRandom use was identified; the selected provider, entropy construction and platform evidence have not been established.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ApiKeyTokenGeneratorTest.java](../../../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java)

Related boundary references, **not implementation coverage**:

- [CodeGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CodeGenerator.java)
- [TwoFactorAuthService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java)

## Work required before closure

Random-number helpers exist; required output quality, distribution, entropy and platform assurance are not established. Select the construction and supply statistical/entropy and failure evidence.

Proposed owner: Security / JVM platform owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
