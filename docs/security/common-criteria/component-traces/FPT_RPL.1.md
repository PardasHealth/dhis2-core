# FPT_RPL.1 — Replay detection

ISO/IEC 15408-2:2026, clause 15.12.6. Trace update: 2026-11-09; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-rpl-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_RPL.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FPT_RPL.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

The 2026-11-09 selected AFTA follow-up implements login-only proof reservations under FIA_UAU.5.2. This is related context for this component, not a full component-specific trace or an assertion that all mechanisms/data flows meet its selected operations.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [TwoFactorAuthUtils.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java)
- [TwoFactorAuthenticationProvider.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java)
- [TwoFactorReplayService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java)

## Work required before closure

This component is not added to the user-selected AFTA list by this fix. If selected separately, establish its precise mechanism/data scope and element mapping, including other purposes, context binding, clocks, recovery and failure policy. Do not infer absent code or a passing requirement from the current trace status.

Proposed owner: Authentication / protocol engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-14](../web-gap-solutions.md#g-14).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
