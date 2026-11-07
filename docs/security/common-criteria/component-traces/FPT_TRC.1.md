# FPT_TRC.1 — Internal TSF consistency

ISO/IEC 15408-2:2026, clause 15.17.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-trc-1)

## Web scope

Related when distributed application parts are enabled. Embedded/external broker, cache and multi-node topology determine the internal TSF/data transfer boundary. Retain these obligations for each enabled distributed configuration; single-node assumptions do not justify ignoring external links. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_TRC.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FPT_TRC.1.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Cache invalidation and fresh PAT lookup are partial controls; reliable TSF state convergence under partition, loss and reordering is not proven.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [CacheInvalidationListener.java](../../../../dhis-2/dhis-support/dhis-support-cache-invalidation/src/main/java/org/hisp/dhis/cacheinvalidation/redis/CacheInvalidationListener.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

## Work required before closure

Invalidation messaging and fresh PAT lookup do not prove all replicated TSF data remains consistent. Test partitions, missed/reordered updates, reconnect and bounded reconciliation.

Proposed owner: Distributed platform / authentication engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
