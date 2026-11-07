# FPT_SSP.2 — Mutual trusted acknowledgement

ISO/IEC 15408-2:2026, clause 15.13.7. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-ssp-2)

## Web scope

Related when distributed application parts are enabled. Embedded/external broker, cache and multi-node topology determine the internal TSF/data transfer boundary. Retain these obligations for each enabled distributed configuration; single-node assumptions do not justify ignoring external links. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_SSP.2.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FPT_SSP.2.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

JMS receipt acknowledgement is related, but does not establish authenticated receipt of unmodified TSF data or mutual knowledge of status.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)
- [CacheInvalidationListener.java](../../../../dhis-2/dhis-support/dhis-support-cache-invalidation/src/main/java/org/hisp/dhis/cacheinvalidation/redis/CacheInvalidationListener.java)

## Work required before closure

No mutually trusted acknowledgement/status protocol is demonstrated. Define acknowledgement-of-acknowledgement/state convergence and test loss, replay and reordering.

Proposed owner: Distributed platform / security architecture.

Solution plans: [G-06](../web-gap-solutions.md#g-06), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
