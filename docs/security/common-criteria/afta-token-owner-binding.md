# AFTA remediation: bind API tokens to the owner's stable identity

Date: **12 November 2026**. Selected rows 37–39,
`FIA_USB.1.1`, `FIA_USB.1.2` and `FIA_USB.1.3`.

## Defect and intended behavior

`ApiTokenAuthManager` reloads current user details by the token owner's username
before accepting an API token. Previously it checked that a returned account
existed and passed account-state checks, but did not compare that account's UID
with the token owner. A stale owner name or a concurrent username rename/reuse
could therefore return a different account without this manager rejecting the
identity mismatch. This is a source-level missing invariant; no deployed
cross-account exploit is asserted.

The manager must require a nonblank owner UID and reject a loaded account whose
UID differs from that owner. This check happens before creating the request's
authentication token. A valid rename with an unchanged UID remains supported;
disabled, locked, expired and MFA-enabled owner restrictions remain enforced.

## Code and test trace

- [ApiTokenAuthManager.validateAndCreateUserDetails](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java): validate owner existence and stable identity, reload current details transactionally, compare UIDs, then enforce the existing account-state checks.
- [ApiTokenAuthManagerTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java): mismatch after prior authentication, missing owner identity, missing loaded identity, legitimate rename, current permissions, request isolation, account restrictions, token deletion, expiry and updated constraints.
- [Fork regression workflow](../../../.github/workflows/cc-audit-regression.yml): the existing authentication unit command selects this class and requires its non-skipped successful report.

## Verification

On the preceding manager, the updated suite ran 16 cases with three assertion
failures: mismatched identity, missing owner UID and missing reloaded UID were
all accepted. The other 13 cases passed, including the valid rename case.

With the fix, **23 targeted tests pass, with no failures, errors or skips**:
16 `ApiTokenAuthManagerTest` cases and seven `ApiTokenResolverTest` cases. The
real production manager and resolver run against mocked account/token services;
this does not dynamically demonstrate a database/cache race or a deployed exploit.

Source hashes, suite counts, baseline failed methods and dependency provenance
are in the [dated evidence record](../evidence/afta-token-owner-binding-2026-11-12.json).
The implementation commit is `7cdefca5c4834e01c66a3d3f77b724d588aa9ab7`.
Maven 3.9.11 and checksum-verified Temurin 17.0.17+10 were used. The JitPack
restriction required the previously documented local source-build workaround
for unchanged `sms-compression` 0.3.2; no dependency version was changed and
binary equivalence to JitPack's artifact is not asserted.

From `dhis-2`, with a supported JDK, Maven and approved dependencies:

```bash
mvn -B -ntp -pl dhis-web-api -am -Punit-test \
  -Dtest=ApiTokenAuthManagerTest,ApiTokenResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Require both named Surefire XML reports to show executed, successful cases.

## Remaining boundaries

This establishes a bounded PAT owner-binding invariant, not full selected-clause
closure. Username-based reload may safely deny a stale renamed owner until its
current name is visible. It does not guarantee completion ordering against later
account updates, revoke browser sessions, define every selected security
attribute, or cover SSO, background jobs and every multi-node provider. UID reuse
through deliberate administrative imports and deployment cache isolation remain
separate acceptance concerns. No schema, crypto format or external service
change is required.
