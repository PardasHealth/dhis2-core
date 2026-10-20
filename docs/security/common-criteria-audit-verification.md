# Common Criteria audit hardening — verification

This document describes how to verify the draft audit-security changes in PR #1. **No successful build or test execution has yet been observed.**

## Scope

- CC-004: bound SQL filters, including JSON attribute keys/values.
- CC-005: timestamp precision preserved by `ResultSet.getTimestamp`.
- CC-006: explicit failure on corrupt audit payload and serialization errors.
- CC-007: bulk deletion requires a valid bounded date range plus UID or code.

## Run in a DHIS2 checkout

Prerequisites: supported JDK, Maven, Docker daemon accessible to Testcontainers, and dependency repositories configured for this DHIS2 build.

From the `dhis-2` directory:

```bash
mvn -version
docker info
mvn -pl dhis-test-integration -am -Pintegration-test \
  -Dtest=AuditRepositoryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

The correct profile in `dhis-2/pom.xml` is `integration-test` (not `integration`). Inspect the effective Maven configuration if the command fails. **Do not treat a successful Maven invocation as evidence unless the test report shows that AuditRepositoryTest actually ran.**

## Evidence to collect

1. Record the exact commit SHA and Java/Maven versions.
2. Save the full Maven output and Surefire/Failsafe XML reports.
3. Verify the named audit tests were executed and count passed/failed/skipped.
4. Confirm the test database is isolated from production.
5. Review SQL parameterization with PostgreSQL (including malicious JSON attribute keys and values).
6. Confirm deleting a bounded UID does not remove unrelated rows.
7. Check that corrupt compressed payloads raise a visible error rather than returning null.

## Remaining review items

- Authorization for audit-retention deletion is **not implemented** by the query guard.
- Bulk deletion by UID/code and range is not subject to a maximum deletion count.
- Throwing on malformed historic payloads may affect existing consumers; review failure handling and alerting.
- No CI status or test result is available at the time this document was added.
