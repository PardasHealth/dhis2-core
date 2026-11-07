# FIA_API.1 — Authentication proof of identity

ISO/IEC 15408-2:2026, clause 12.4.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-api-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_API.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Signing-key and HTTP configuration are related to TOE identity; the selected TOE-to-peer authentication mechanism is not established.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [AuthorizationServerConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthorizationServerConfig.java)
- [Main.java](../../../../dhis-2/dhis-web-server/src/main/java/org/hisp/dhis/web/tomcat/Main.java)

## Work required before closure

User authentication is not TOE identity proof. Define the TOE entity, peer-facing authentication mechanism and required properties; verify peer validation and credential lifecycle.

Proposed owner: Security / deployment owner.

Solution plans: [G-06](../web-gap-solutions.md#g-06), [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
