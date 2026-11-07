# FCS_RBG.5 — Random bit generation (combining entropy sources)

ISO/IEC 15408-2:2026, clause 10.5.10. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fcs-rbg-5)

## Web scope

Related; application and platform evidence required. A web application relies on JVM/OS/provider randomness, key memory handling and trusted time. These are supporting responsibilities rather than ignored hardware features; identify allocation and evidence in the deployment profile. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FCS_RBG.5.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

JDK SecureRandom use was identified; the selected provider, entropy construction and platform evidence have not been established.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [CodeGenerator.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/common/CodeGenerator.java)
- [TwoFactorAuthService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java)

## Work required before closure

No entropy-combiner assurance was located. Specify composition, per-source entropy assumptions and failure/degradation tests.

Proposed owner: Security / JVM platform owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
