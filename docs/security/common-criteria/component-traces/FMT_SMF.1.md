# FMT_SMF.1 — Specification of Management Functions

ISO/IEC 15408-2:2026, clause 13.9.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fmt-smf-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [UserController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/user/UserController.java)
- [SessionController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/SessionController.java)
- [RequiresAuthority.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/RequiresAuthority.java)

Symbols to inspect: `UserController`; `SessionController`; `RequiresAuthority`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FMT_SMF.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Administrative configuration interfaces are candidates; a complete TSF management/data/role inventory is missing.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [SessionControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/SessionControllerTest.java)
- [UserAccountControllerTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/UserAccountControllerTest.java)

Related boundary references, **not implementation coverage**:

- [ConfigurationController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/ConfigurationController.java)
- [SystemSettingsController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/SystemSettingsController.java)

## Work required before closure

Management interfaces exist, but no complete selected security-management-function inventory is supplied. Link every required function to authorization, interfaces and tests.

Proposed owner: Administration / security engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
