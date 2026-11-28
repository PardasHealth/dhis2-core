# OIDC authorization security parameter protection

Updated: **28 November 2026**. Branch: `work`.

| **Selected source rows** | Bounded change | Status |
| --- | --- | --- |
| 46 (`FPT_FLS.1.1`) | Reject conflicting configuration before returning an authorization request | Partial evidence |
| 48 (`FPT_TDC.1.1`) | Keep generated correlation and registered grant fields authoritative over provider extras | Gap |

Provider `extra_request_parameters` previously could supply `state`, `nonce`, `client_id`, `redirect_uri`, `response_type` or `scope`. That allowed the outgoing authorization URL to conflict with the request's stored correlation fields or client registration. Existing PKCE checks protected only the PKCE fields.

The resolver now rejects these six exact, case-sensitive keys with `Authorization security parameters cannot be overridden`. The fixed exception excludes configuration values and has no attached cause. Rejection happens before custom PKCE generation and before returning an authorization request, for both resolver entry points, OAuth and OpenID scopes, and custom PKCE on/off. The existing PKCE policy remains in place. Provider hints such as `prompt`, `login_hint` and `hd` remain supported.

**Deployment compatibility:** remove these six fields from `oidc.provider.<id>.extra_request_parameters` before upgrading. Configure client ID, redirect URI and scopes through the provider/client-registration settings; let the framework generate state/nonce and retain its authorization-code response type. An affected configuration now rejects login initiation instead of emitting contradictory parameters. The check runs when a request is resolved, not at configuration startup.

Implementation: [`be26248818`](https://github.com/PardasHealth/dhis2-core/commit/be2624881835aff359ded8877c1e29326f00cf46).

| Code or check | Evidence |
| --- | --- |
| `SECURITY_PARAMETERS` and `customizeAuthorizationRequest` | [Resolver](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisCustomAuthorizationRequestResolver.java) |
| Six protected fields × two PKCE modes × OAuth/OpenID × both entry points; eight normal outgoing-URL controls | [56 regression cases](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/OidcAuthorizationParameterPolicyTest.java) |
| Existing PKCE S256, buffer ownership, provider builder and config parser compatibility | [Execution record](../evidence/afta-oidc-authorization-parameters-2026-11-28.json) |

The unchanged resolver failed all **48 rejection cases**, while all eight normal controls passed. After the fix, **78 checks passed with zero failures, errors or skips**: 56 new cases, 12 existing PKCE cases and ten provider/configuration cases. Normal controls check the actual decoded outgoing URL retains one state, client ID, redirect URI, response type and scope, plus generated OpenID nonce and provider hints. CI selects and requires the new suite; hosted CI success is not claimed.

This establishes a local request-construction consistency rule. Live IdP/callback/token exchange, authorization-endpoint URL query configuration, other provider-specific schemas, full trusted-peer policy and complete secure-state failure coverage remain open. Rows 46 and 48 retain their previous statuses; no full AFTA requirement is closed.
