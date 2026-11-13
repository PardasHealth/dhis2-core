# AFTA remediation: audit-review paging and optional filters

Date: **13 November 2026**. Selected rows 4, 5 and 7:
`FAU_SAR.1.1`, `FAU_SAR.1.2` and `FAU_SAR.3.1`. Partial evidence only.

## Defects and resulting behavior

The tracked-entity audit controller reversed its paging branches. Default and
explicit paged requests passed no pager to the audit service and omitted pager
metadata. Requests that disabled paging calculated response metadata without
applying those bounds to the query. A requested page could therefore retrieve
all matching records instead of the requested subset.

The controller now creates and forwards a pager only when paging is enabled.
Its page offset and size reach the service, and the response includes the
corresponding pager node. Explicit `paging=false` and `skipPaging=true` requests
remain unpaged, omit the count query and omit pager metadata. The existing
`skipPaging` precedence over `paging` is retained.

The optional `trackedEntities` filter also previously passed null into
`UID.toValueList`, which dereferences its input. Omitting that filter caused a
null-pointer exception before the query. The controller now normalizes it to an
empty set. Entity, user, operation, date and requested field selections remain
forwarded to their existing services.

## Requirement-to-code and test trace

| Selected requirement | Bounded implementation evidence |
| --- | --- |
| Row 4, `FAU_SAR.1.1` | Audit retrieval accepts the optional filter and forwards the requested page bounds. This does not establish complete authorized audit access. |
| Row 5, `FAU_SAR.1.2` | Response pager nodes agree with whether paging is enabled; actual serializers and every required readable field remain outside this unit evidence. |
| Row 7, `FAU_SAR.3.1` | Tests preserve entity, user, operation, date and field selections while applying paging. They do not execute the database predicates. |

- [AuditController.getTrackedEntityAudit](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/AuditController.java) contains both fixes.
- [AuditControllerPagingTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/controller/AuditControllerPagingTest.java) covers default paging, requested page/size, disabling paging, both flag-precedence cases, selection/field forwarding and an omitted optional entity filter.
- [HibernateTrackedEntityAuditStore](../../../dhis-2/dhis-tracker/src/main/java/org/hisp/dhis/tracker/audit/HibernateTrackedEntityAuditStore.java) already applies a supplied pager through `setFirstResult` and `setMaxResults`. This is source context; these controller tests mock the service rather than execute this store.
- [Fork CI](../../../.github/workflows/cc-audit-regression.yml) selects the new unit class and requires its executed, successful XML report.

## Execution evidence

The baseline controller at `5f2db7205f` produced **six assertion failures and one
error in seven cases**. The error was the missing optional-filter null-pointer
exception. Initial test setup exposed that exception in paging fixtures too;
the recorded rebaseline separates explicit empty-filter paging cases from the
omitted-filter case. Failed outcomes are retained in the evidence.

With both fixes, **30 cases pass, with no failures, errors or skips**:
seven audit-review cases, 16 API-token-manager cases and seven token-resolver
cases. These are real controller/manager/resolver unit executions with mocked
services, not HTTP, browser or database integration tests.

The [dated evidence record](../evidence/afta-audit-review-paging-2026-11-13.json)
contains source hashes, named cases, suite counts, commands and the existing
local dependency-cache provenance caveat. Implementation commit:
`efc15293821dcd9469160173abf8fcae3a02a05a`.

From `dhis-2`, using a supported JDK, Maven and approved dependencies:

```bash
mvn -B -ntp -pl dhis-web-api -am -Punit-test \
  -Dtest=AuditControllerPagingTest,ApiTokenAuthManagerTest,ApiTokenResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Require all three named Surefire reports to show executed passing cases.
The local run used Temurin 17.0.17+10, Maven 3.9.11 and the existing offline
cache, retaining the documented unchanged `sms-compression` source-build caveat.

## Remaining acceptance

All three selected rows remain partial and fit percentages are unchanged.
This patch does not change access-control policy or review authorization-sensitive
count totals, establish application-wide response limits, or prohibit explicit
unpaged queries. It does not prove SQL execution, real response serialization,
every audit-event type or deployed/evaluator acceptance. No hosted CI success is
inferred from changing the workflow. No additional AFTA requirements are added.

The subsequent [combined audit verification](afta-authentication-audit-records.md)
reruns these seven review cases after integrating the authentication-log fix.
The original implementation/evidence above remain preserved as historical records.
