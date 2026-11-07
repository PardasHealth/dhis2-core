# FAU_GEN.2 — User identity association

ISO/IEC 15408-2:2026, clause 8.4.8. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fau-gen-2)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [AuditManager.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditManager.java)
- [AuthenticationLoggerListener.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/AuthenticationLoggerListener.java)

Symbols to inspect: `AuditManager`; `AuthenticationLoggerListener.onApplicationEvent`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FAU_GEN.2.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Audit generation, query/storage and broker configuration were reviewed with the recorded audit failure tests.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AuditManagerTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditManagerTest.java)
- [AuthenticationListenerTest.java](../../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java)

Related boundary references, **not implementation coverage**:

- [JdbcAuditRepository.java](../../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java)
- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)

## Work required before closure

User identity is recorded on selected paths, but service/background/failed-auth identities are not exhaustively mapped. Trace every selected event to its attributable subject.

Proposed owner: Security engineering / operations.

Solution plans: [G-02](../web-gap-solutions.md#g-02), [G-03](../web-gap-solutions.md#g-03), [G-04](../web-gap-solutions.md#g-04).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
