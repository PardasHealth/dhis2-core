# FPT_FLS.1 — Failure with preservation of secure state

ISO/IEC 15408-2:2026, clause 15.4.6. Trace update: 2026-11-09; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-fls-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [AbstractAuditConsumer.java](../../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java)
- [AuditScheduler.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java)
- [TwoFactorReplayService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java)

Symbols to inspect: `AbstractAuditConsumer._consume`; `AuditScheduler.process`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_FLS.1.1

**Partial source mapping.** [TwoFactorReplayService.java:69](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorReplayService.java#L69): Login factor acceptance requires a successful durable replay reservation; duplicate keys reject and storage exceptions propagate before authentication returns. Targeted H2 and PostgreSQL tests exercise this failure boundary.

**Open:** One identified failure case, not the complete selected AFTA failure list or whole-application secure-state proof. Reliable time, restore and broader fault tolerance remain open.

## Evidence and limits

Audit retry/failure paths and MFA replay-reservation failure provide bounded examples. A denied or unavailable replay write prevents the provider from returning authentication in targeted tests; these do not establish all secure-state failure transitions.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AbstractAuditConsumerFailureTest.java](../../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AbstractAuditConsumerFailureTest.java)
- [AuditSchedulerTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java)
- [TwoFactorReplayServiceTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayServiceTest.java)
- [TwoFactorReplayPostgresTest.java](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/twofa/TwoFactorReplayPostgresTest.java)

Related boundary references, **not implementation coverage**:

- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)
- [JdbcAuditRepository.java](../../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java)

## Work required before closure

Select every relevant failure class and secure invariant. MFA replay storage failure is covered by targeted tests; broader storage, time, restore, provider, session and whole-application failure acceptance remains open.

Proposed owner: Reliability / security / operations.

Solution plans: [G-02](../web-gap-solutions.md#g-02), [G-03](../web-gap-solutions.md#g-03), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
