# FPT_FLS.1 — Failure with preservation of secure state

ISO/IEC 15408-2:2026, clause 15.4.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-fls-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [AbstractAuditConsumer.java](../../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java)
- [AuditScheduler.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java)

Symbols to inspect: `AbstractAuditConsumer._consume`; `AuditScheduler.process`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_FLS.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Selected audit rejection, rollback, retry and recovery paths are tested; this does not establish secure recovery or fault tolerance of the whole TOE.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AbstractAuditConsumerFailureTest.java](../../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AbstractAuditConsumerFailureTest.java)
- [AuditSchedulerTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java)

Related boundary references, **not implementation coverage**:

- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)
- [JdbcAuditRepository.java](../../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java)

## Work required before closure

Audit failures propagate/retry on tested paths; secure state for all selected TOE failures is not defined. Specify preserved invariants and test failure transitions including unavailable storage.

Proposed owner: Reliability / security / operations.

Solution plans: [G-02](../web-gap-solutions.md#g-02), [G-03](../web-gap-solutions.md#g-03), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
