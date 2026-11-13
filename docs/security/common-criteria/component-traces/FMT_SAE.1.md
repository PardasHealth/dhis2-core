# FMT_SAE.1 — Time-limited authorization

ISO/IEC 15408-2:2026, clause 13.8.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fmt-sae-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

Symbols to inspect: `ApiTokenAuthManager.validateTokenExpiry`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FMT_SAE.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FMT_SAE.1.2

**Partial source mapping.** [ApiTokenAuthManager.java:113](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L113): validateTokenExpiry rejects absent expiry and expiry at or before the current application clock before creating authenticated token state.

**Open:** This traces rejection of expired personal access tokens only. Roles permitted to set expiry (.1), other security attributes and trusted time remain open.

## Evidence and limits

Fresh PAT state and account/token expiry checks provide partial evidence; cross-node visibility, all attributes and trusted time remain open.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [DefaultUserService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java)

## Work required before closure

Token/account/session expiry exists, but who may set lifetime, all affected attributes and expiry/revocation rules are not fully mapped. Test boundary times with a trusted time source.

Proposed owner: Authentication / distributed platform.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-10](../web-gap-solutions.md#g-10).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
