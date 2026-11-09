# Keeping the security changes compatible with DHIS2 upstream

Upstream: <https://github.com/dhis2/dhis2-core>.
Fork: <https://github.com/PardasHealth/dhis2-core>.

The upstream repository, public interfaces and architecture remain authoritative.
Local security fixes should be small, independently reviewable patches. Future
merge conflicts cannot be guaranteed absent: upstream can edit the same methods.
The goal is to minimize conflicts and preserve the security behavior when resolving
those that occur.

## Current patch boundary

The Common Criteria review adds documentation under `docs/security/`, this guide,
a separate `README.fork.md`, and focused regression classes. It restores the
upstream README and existing integration-test files byte-for-byte to their
pre-review state. The 2026-11-09 MFA fix additionally adjusts only the successful
email-login fixture in `AuthenticationControllerTest` to explicitly enable and
restore the factor under test. Maven POMs, bundled app versions, dependency declarations,
repository identity, existing branches and remote URLs are unchanged.

The necessary production changes are confined to three classes (the third added on 2026-11-09):

| Patch | Production file / symbols | Behavior that must survive an upstream merge | Fork regression |
| --- | --- | --- | --- |
| CC-2026-01/02 | [ApiTokenAuthManager](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java): constructor, `authenticate`, `validateTokenExpiry` | Resolve current token/account/permissions/constraints for each authentication; use a fresh authentication object; reject malformed expiry. Do not restore the one-hour authentication snapshot. | [ApiTokenAuthManagerTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java), [ApiTokenStateAuthenticationTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/ApiTokenStateAuthenticationTest.java) |
| CC-2026-03 | [AuditScheduler](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java): `process` and explanatory Javadoc | A publication exception retains the failed and unprocessed events, surfaces the error, and permits later recovery. Successful earlier publications are not unnecessarily replayed. | [AuditSchedulerTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java) |
| AFTA-2026-01 / G-15 | [TwoFactorAuthenticationProvider](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java): `checkTwoFactorAuthentication`, `isTwoFactorTypeEnabled` | A globally disabled, unsupported or missing enrolled factor must reject login; never restore silent password-only downgrade. Enabled-factor verification and explicit recovery paths remain in the existing architecture. | [TwoFactorAuthenticationPolicyTest](../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java), [TwoFactorLoginPolicyTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorLoginPolicyTest.java) |

The PostgreSQL recovery regression is a separate
[AuditRepositoryFailureRecoveryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditRepositoryFailureRecoveryTest.java).
The [AuditStorageDeliveryRecoveryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditStorageDeliveryRecoveryTest.java)
adds a standalone broker/PostgreSQL fault fixture without changing upstream source
or POMs. The additional web-state regressions are separate from upstream's
`ApiTokenAuthenticationTest`; the recovery case is separate from
`AuditRepositoryTest`. This keeps unrelated additions out of upstream test files.
The existing fork-owned [CC workflow](../../.github/workflows/cc-audit-regression.yml)
runs these additions alongside the original regressions and checks report presence.

## Rules for future changes

1. Put fork guidance, evidence and configuration examples in separate files. Keep
   upstream README, build files and source formatting untouched unless a fix needs
   a specific edit.
2. Follow existing DHIS2 interfaces and services; do not introduce a parallel
   authentication/security implementation or shadow Spring beans to avoid a small
   upstream patch.
3. Keep each behavior change in a focused commit when committing reviewed work.
   Documentation and additional CI can be separate commits. Do not rewrite already
   shared fork history solely to reorganize this patch.
4. Maintain the traceability matrix, patch IDs and regression evidence. If upstream
   implements an equivalent fix, compare behavior and retain the regression tests;
   retire the local implementation only after its security properties still pass.
5. Do not add upstream source snapshots or entire generated app bundles to Git.
   Generated WARs, downloaded bundles and local dependency caches remain build
   artifacts. The checked-in app inventory is evidence of one build, not a change
   to upstream's app selection.

## Preparing an upstream integration

The security review did not fetch or merge upstream, change branches, or alter
remotes. `origin` remains the PardasHealth fork. An `upstream` remote was not present
when this guide was written. The following commands are for a future intentional
integration, after the current work is reviewed and committed and the working tree
is clean.

First inspect remotes. Add the official remote only if it is absent; verify any
existing `upstream` URL rather than replacing it blindly:

```bash
git remote -v
git remote add upstream https://github.com/dhis2/dhis2-core.git
git fetch upstream --prune
git branch -r
```

Choose the branch matching the fork's DHIS2 release/development stream. The reviewed
checkout reports `2.44-SNAPSHOT`; that alone is not permission to merge an arbitrary
future major version. `upstream/master` below is an example for a fork that is still
tracking that development stream; change the value before running it if necessary.

```bash
git status --short
upstream_ref=upstream/master
git merge-base HEAD "$upstream_ref"
git log --left-right --oneline "HEAD...$upstream_ref"
git switch -c integration/dhis2-upstream-review
git merge --no-commit --no-ff "$upstream_ref"
```

Inspect the proposed changes, especially the three production classes in the patch
ledger. Resolve each conflict by preserving both upstream behavior and the security
properties above. A blanket choice of `ours` or `theirs` can silently discard one
side's fixes. If the integration needs to be abandoned, `git merge --abort` is
appropriate while that merge is in progress, provided the clean-tree prerequisite
was observed. Never reset away uncommitted work to make a merge succeed.

Before completing the merge, run:

- The focused authentication, audit-buffer and PostgreSQL regressions in the
  [verification guide](../security/common-criteria-verification.md), including the
  new separate test classes.
- The normal upstream checks required by the files/dependencies being merged.
- The production web build and configured browser workflows; compare known
  baseline app errors rather than treating HTTP 200 alone as app health.

Record the upstream commit, resulting fork revision, configuration and actual test
results. Review and commit the merge only after these checks pass; publish through
the fork's normal review process. This guide does not perform a merge or authorize
an automatic push to either repository.
