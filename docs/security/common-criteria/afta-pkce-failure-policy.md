# PKCE S256 failure policy and derived parameter protection

Updated: **23 November 2026**. Branch: `work`.

| **Selected source rows** | Bounded change | Status |
| --- | --- | --- |
| 20 (`FDP_RIP.2.1`) | Clear the custom hash helper's owned ASCII verifier bytes | Gap |
| 46 (`FPT_FLS.1.1`) | Stop generated PKCE requests on S256 failure | Partial evidence |
| 48 (`FPT_TDC.1.1`) | Protect generated PKCE fields from contradictory extra parameters | Gap |

The resolver previously fell back to a plain verifier challenge if custom SHA-256 lookup failed, and generic provider extras could overwrite derived PKCE fields. Spring's default resolver also generates PKCE, so turning off the DHIS2-specific customization did not remove that downgrade/override boundary.

Requests with generated PKCE now require `S256`; a custom missing algorithm raises `PKCE S256 is unavailable`, and a weak generated framework request raises `PKCE S256 is required`. Neither returns an authorization request. The custom helper computes the hash before adding its verifier/challenge fields, leaving supplied maps untouched on hash failure. Those two fixed errors do not attach provider exceptions or verifier values. The custom hash helper clears its owned ASCII bytes on success/failure.

For generated PKCE, `code_challenge`, `code_challenge_method` and `code_verifier` are rejected in `extra_request_parameters` with `PKCE parameters cannot be overridden`. **Remove these entries from affected provider configuration.** Unrelated hints such as `prompt` remain supported. The guard also protects Spring-generated fields when the custom `enable_pkce` flag is off. It does not introduce a requirement that every provider/grant use PKCE or define every possible extra OAuth parameter.

Implementation: [`e3d4f49dd2`](https://github.com/PardasHealth/dhis2-core/commit/e3d4f49dd2158875c8caae9add912d30326e2c7a).

| Code or check | Evidence |
| --- | --- |
| Request policy, derived-field guard and owned verifier cleanup | [DhisCustomAuthorizationRequestResolver.java](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/DhisCustomAuthorizationRequestResolver.java) |
| RFC7636 vector; one matching challenge/method in actual URL; no verifier in URL; custom/framework failure; reserved overrides; nonce; unchanged maps and owned array cleanup | [12 resolver checks](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/oidc/OidcPkceFailurePolicyTest.java) |
| Existing provider builder/parser compatibility, exact sources/reports and command | [Execution record](../evidence/afta-pkce-failure-policy-2026-11-23.json) |

The nine-check pre-fix policy baseline recorded seven failures and two normal S256 controls passing. Additional nonce/buffer/URL assertions were added afterward. **22 final cases passed, with no failures, errors or skips**: 12 resolver checks and ten existing provider/config checks. Read-scope fixtures isolate digest failures from separate nonce hashing; a normal OpenID-scope control retains nonce data. CI selects and requires all three suites; hosted CI success is not claimed.

This proves local request construction and failure boundaries, not live IdP/token exchange or complete deployed filter/session behavior. Full provider/peer policy, other parameter schemas, selected fault states and AFTA acceptance remain open. Immutable verifier strings, framework/provider copies and physical erasure remain unproven. No selected requirement is fully closed.
