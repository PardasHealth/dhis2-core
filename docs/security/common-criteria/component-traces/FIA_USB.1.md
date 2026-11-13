# FIA_USB.1 — User-subject binding

ISO/IEC 15408-2:2026, clause 12.9.6. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fia-usb-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [UserDetailsImpl.java](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/user/UserDetailsImpl.java)
- [ApiTokenAuthManager.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java)

Symbols to inspect: `UserDetailsImpl`; `ApiTokenAuthManager.authenticate`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FIA_USB.1.1

**Partial source mapping.** [ApiTokenAuthManager.java:61](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L61): Authentication constructs a fresh ApiTokenAuthenticationToken using the persisted token and current UserDetails for its owner. The token owner and reloaded account must have the same nonblank stable UID; see the 12 November owner-binding record.

**Open:** The selected user-attribute list, other authentication mechanisms and complete subject lifecycle are not specified by this mapping.

### FIA_USB.1.2

**Partial source mapping.** [ApiTokenAuthManager.java:80](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L80): Before binding a token to a user, validateAndCreateUserDetails checks owner existence and current enabled, credential-expiry, lock, account-expiry and 2FA state. The token owner and reloaded account must have the same nonblank stable UID; see the 12 November owner-binding record.

**Open:** This is a partial initial-association rule trace for personal access tokens; selected ST rules and browser/OIDC subjects require separate checks.

### FIA_USB.1.3

**Partial source mapping.** [ApiTokenAuthManager.java:61](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java#L61): The authentication path avoids a process-local snapshot and reloads current owner information on each token request. The token owner and reloaded account must have the same nonblank stable UID; see the 12 November owner-binding record.

**Open:** This supports changes between personal access token requests; it does not prove invalidation or propagation for every subject or deployment node.

## Evidence and limits

Current-user/token lookup and lockout paths were reviewed with the recorded authentication tests; all providers and nodes are not covered.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ApiTokenAuthManagerTest.java](../../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java)
- [ApiTokenAuthenticationTest.java](../../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/ApiTokenAuthenticationTest.java)

Related boundary references, **not implementation coverage**:

- [DefaultUserService.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java)

## Work required before closure

Fresh PAT principals are tested, but complete user-to-subject association/update rules across sessions, jobs and providers remain open. Specify binding attributes and all change/revocation tests.

Proposed owner: Authentication engineering.

Solution plans: [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
