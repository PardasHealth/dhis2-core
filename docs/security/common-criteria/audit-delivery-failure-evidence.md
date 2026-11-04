# Audit delivery and storage failure: integrated evidence

Date: 2026-11-03. Status: **tested engineering behavior, partial CC evidence**.
References: ISO/IEC 15408-2:2026 FAU_STG.2 (8.8.7), FAU_STG.3 (8.8.8),
FAU_STG.5 (8.8.10), and FPT_FLS.1 (15.4.6). No FAU_STG.4 capacity-alarm claim.

## Source path under test

[AuditStorageDeliveryRecoveryTest](../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditStorageDeliveryRecoveryTest.java)
uses a dedicated PostgreSQL 16/PostGIS Testcontainers database with a bounded
512 MiB memory-backed data volume, separate schemas
and a non-owner login role for each case. It creates the audit table from the
project's original audit-table migration. Tests do not grant the writer UPDATE
or DELETE; administrative operations use a separate test-only connection.

The broker cases use the production
[ArtemisConfig](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)
topic template, redelivery policy and transacted/shared/durable listener factory,
the production
[MetadataAuditConsumer](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/consumers/MetadataAuditConsumer.java),
[DefaultAuditService](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/DefaultAuditService.java)
and [JdbcAuditRepository](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java).
Serialization uses `JacksonObjectMapperConfig.configureMapper`, matching the
project's common mapper configuration. Configuration flags and filesystem lookup
are test doubles; broker delivery and database persistence are real.

The listener has an explicit test subscription, an isolated in-VM broker acceptor,
and a broker with persistence disabled. Tests wait for the actual subscription
binding before publishing. These choices isolate the test from application broker
instances; they do not verify production annotation scanning, multi-node
subscription naming, network TLS or broker crash durability.

## Executed cases and assertions

| Test method | Injected failure | Checked outcome |
| --- | --- | --- |
| `transientInsertFailureRollsBackReceiveAndPersistsAfterRecovery` | Revoke INSERT from the audit writer before publishing one event; restore it after the first listener error | Listener receives a database exception; pending UID has no row during failure; receive is redelivered; pending UID is persisted once after recovery; an earlier autocommitted record remains; no DLQ record is left. |
| `sustainedInsertFailureReachesDeadLetterQueueAndCanBeReplayed` | Keep INSERT revoked through the configured retry bound | Exactly five deliveries with JMS delivery counts 1–5, all failing in database persistence; original payload reaches DLQ; no pending row is fabricated; after INSERT restoration and explicit replay, one pending row exists and the earlier record remains. |
| `terminatedDatabaseConnectionFailsReadsAndWritesAndFreshConnectionRecovers` | PostgreSQL terminates the precise writer backend after an autocommitted insert | Subsequent insert, count and query on that dead connection throw database exceptions; a fresh connection reads the earlier committed record and accepts new writes. Failed reads do not return a fabricated zero/empty list. |
| `appendOnlyDatabaseRoleCannotModifyOrDeleteAndDeniedReadsAreVisible` | Writer has SELECT/INSERT only; then SELECT is temporarily revoked | Repository deletion and direct UPDATE fail; original author and record remain; denied count/query throw; restoring SELECT restores readable results. |

The broker cases start with one committed `earlier` record and publish one
`pending` event. During failure the total remains one and the pending count zero;
after recovery the total is two and each UID count one. This is a reconciliation
of the controlled test events, not a claim about all application audit traffic.

## Reproduce

From `dhis-2`, with a supported JDK, Maven and Docker:

```bash
mvn -B -ntp -pl dhis-test-integration -am -Pintegration-test \
  -Dtest=AuditRepositoryTest,AuditRepositoryFailureRecoveryTest,AuditStorageDeliveryRecoveryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Require a non-skipped Surefire report for all three named classes. The fork-owned
[security regression workflow](../../../.github/workflows/cc-audit-regression.yml)
includes these cases and validates required reports. Local execution and source
hashes are recorded in the [verification record](../common-criteria-verification.md)
and [machine-readable evidence](../evidence/review-2026-11-03.json). No hosted CI
run is asserted from editing the workflow.

## Remaining boundaries

- This does not exhaust database/broker disk space, deliver capacity alerts,
  select retention metrics, or define whether business operations are admitted
  when audit storage fills. FAU_STG.3/4/5 still need ST selections and deployment
  evidence for those conditions.
- The broker receive and JDBC insert are separate transactions. A crash after
  database commit but before JMS acknowledgment can duplicate an event; these
  tests do not establish exactly-once delivery or distributed-transaction safety.
- The memory-backed test volume deliberately avoids consuming the shared Docker
  disk during this fault fixture. It is not evidence of durable media or crash
  recovery.
- Connection termination is a real PostgreSQL session failure. It is not a test
  of all network partitions, full server restart, connection-pool recovery or
  committed-record survival across power loss.
- The restricted role is a fixture. The tests do not prove that production DHIS2
  credentials have those privileges, or that administrator/retention access and
  tamper detection satisfy the selected policy.
- All faults are confined to disposable test resources. Original upstream source,
  dependencies, application settings and browser bundles are unchanged by this
  evidence extension.
