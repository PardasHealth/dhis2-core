# FAU_ARP.1 — Security alarms

ISO/IEC 15408-2:2026, clause 8.3.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fau-arp-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FAU_ARP.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Authentication failure logging and lockout are related controls; they do not establish a general audit analysis or alarm service.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [AuthenticationLoggerListener.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/AuthenticationLoggerListener.java)
- [DefaultUserService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java)

## Work required before closure

No verified security-alarm delivery/action policy. Select triggering events, recipients and actions; exercise threshold crossing and failed notification.

Proposed owner: Security engineering / operations.

Solution plans: [G-03](../web-gap-solutions.md#g-03), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
