# API-token authentication failure boundary

Date applied: **16 November 2026**. Branch: `work`.
Implementation: [38ffff2be6](https://github.com/PardasHealth/dhis2-core/commit/38ffff2be6e2768a8d6730c68a5b5d0a5f88ab26).

## Selected source rows

| Selected source row | Requirement ID | Behavior changed by this fix |
| --- | --- | --- |
| 1 | `FAU_GEN.1.1` | A downstream authentication exception no longer causes this filter to emit a second event classifying the valid token as rejected. |
| 29 | `FIA_AFL.1.1` | This filter no longer converts downstream credential/account exceptions into token-owner failure events that can reach the login counter. |

Both remain **Partial evidence**. Source row numbers refer to the original
numbered checklist; the requirement IDs identify its security elements.

## Defect and implementation

`Dhis2ApiTokenFilter.doFilterInternal` enclosed token authentication, success
publication and `filterChain.doFilter` within one `AuthenticationException` catch.
A downstream credential or account exception was consequently swallowed, the
context cleared, and the token entry point invoked. A failure event could follow
the already published success even though the token itself had authenticated.

The catch now covers token processing only. On token rejection, the existing
failure handler still publishes the sanitized failure, clears authentication,
invokes the entry point and stops. Success publication and downstream processing
run after that boundary. Their exceptions retain surrounding filter-chain
handling rather than being reclassified by this token filter.

Trace:

- [Dhis2ApiTokenFilter](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java): `doFilterInternal`, boundary before success publication and downstream invocation.
- [ApiTokenIpConstraintTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenIpConstraintTest.java): two new cases throw `BadCredentialsException` or `LockedException` from the downstream chain. The exact exception must propagate, only the success event may be published by this filter, the chain runs once, and this filter must not clear context or invoke its 401 entry point. Existing 13 token-resolution/constraint/failure cases remain.
- [Dated evidence](../evidence/afta-token-failure-boundary-2026-11-16.json): implementation commit, selected source rows, baseline, commands and exact source/report hashes.
- [Fork CI](../../../.github/workflows/cc-audit-regression.yml): the extended token suite is already selected and required to produce a passing nonempty report.

Against the preceding filter, both new cases fail because their exceptions are
swallowed; the original 13 cases pass. This controlled filter fixture does not
assert that every downstream exception comes from an HTTP endpoint or that all
surrounding handlers have passed deployment acceptance. Audit delivery/retention,
the complete selected event policy and failure-handling policy remain open.

Final verification: **86 Java cases passed** (57 unit and 29 H2 API), with
zero failures, errors or skips. The two baseline failures remain separately
recorded. Counts overlap earlier execution inventories and are not additive.
