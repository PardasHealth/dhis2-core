# FAU_GEN.2 — User identity association

ISO/IEC 15408-2:2026, clause 8.4.8. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

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

**Partial source mapping.** [AuthenticationLoggerListener.java:80](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/AuthenticationLoggerListener.java#L80): Known local OIDC/PAT account names are selected independently of detail shape; attempted password/OIDC names are retained when local identity is unavailable. Credentials/token keys are not serialized.

**Open:** Selected authentication-message paths only. Timestamp layout/trusted time, full event and identity coverage, destinations/retention and actual AFTA operations remain open. Forwarded addresses depend on a trusted ingress policy. See ../afta-authentication-audit-records.md.

## Evidence and limits

Audit generation, query/storage and broker configuration were reviewed with the recorded audit failure tests. Authentication log-field regressions cover available failure addresses, typed optional details, Unicode identities and escaped value boundaries. Logging layout/time, required-event policy and durable destination remain separate obligations.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [AuditManagerTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditManagerTest.java)
- [AuthenticationListenerTest.java](../../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java)
- [AuthenticationAuditRecordTest.java](../../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/AuthenticationAuditRecordTest.java)

Related boundary references, **not implementation coverage**:

- [JdbcAuditRepository.java](../../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java)
- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)

## Work required before closure

User identity is recorded on selected paths, but service/background/failed-auth identities are not exhaustively mapped. Trace every selected event to its attributable subject.

Proposed owner: Security engineering / operations.

Solution plans: [G-02](../web-gap-solutions.md#g-02), [G-03](../web-gap-solutions.md#g-03), [G-04](../web-gap-solutions.md#g-04).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
