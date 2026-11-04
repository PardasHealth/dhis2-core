# Common Criteria audit-storage failure verification plan

Status: Partially executed verification work; remaining capacity/operational checks are proposed. No compliance claim until selected requirements and evaluation evidence are reviewed.

## Scope

ISO/IEC 15408-2:2026 FAU_STG.2 (protected audit data storage), FAU_STG.3 (guarantees of audit data availability), FAU_STG.4 (action in case of possible audit data loss), and FAU_STG.5 (prevention of audit data loss). Confirm the selected Security Target's exact assignments and selections before assigning pass/fail.

## Test matrix

| Scenario | Setup | Expected evidence |
| --- | --- | --- |
| Database unavailable | Stop the PostgreSQL container before an audit write | Error propagation, observable health/alert, and documented operation outcome |
| Audit insert rejected | Revoke INSERT from the audit writer in a disposable test database | No silent success, explicit exception, audit failure signal |
| Storage capacity exhausted | Use a constrained disposable PostgreSQL volume | Threshold notification, documented action, no unreported data loss |
| Audit query under failure | Interrupt database connectivity during a read | Explicit failure and no fabricated empty result |
| Recovery | Restore database and repeat audit writes | Previously committed records preserved and new writes succeed |
| Concurrent failures | Multiple writers while storage fails | Deterministic failure policy and no unsupported success acknowledgements |
| Authorized deletion | Attempt audit deletion with least-privilege identity | Enforced permission boundary; privileged retention behavior documented |

## Implementation guardrails

- Do not disable auditing or suppress database exceptions to make tests pass.
- Do not assert that all security operations must fail closed without an approved Security Target and operational policy.
- Use disposable Testcontainers databases, not production resources.
- Keep the two existing disabled 200k-insert performance tests out of routine CI; optionally run in a dedicated scheduled performance workflow.
- Record test names, run URL, commit SHA, PostgreSQL version, expected/actual outcomes, and remaining gaps.
- Treat passing CI as test evidence, not Common Criteria certification.

## Definition of done

1. Implement deterministic fault injection and recovery integration tests.
2. Verify they execute in CI (not skipped), with XML test reports.
3. Review the Security Target selections for FAU_STG.2/3/4/5.
4. Update the Common Criteria assessment sheet and dashboard with links to source and CI evidence.
5. Only promote a requirement to verified after evidence review.

Current source traces and executed evidence are maintained in the
[consolidated traceability matrix](../common-criteria-traceability.md) and
[verification record](../common-criteria-verification.md).

## Added deterministic database rejection case

`AuditRepositoryFailureRecoveryTest.testRejectedWritePreservesEarlierRecordsAndRecovers` installs
a transaction-scoped PostgreSQL CHECK constraint to reject a known test UID. It
asserts that the repository raises a database exception, restores the test
transaction with a savepoint, checks that the earlier record remains, removes
the constraint and verifies that writes recover. Test transaction rollback
cleans up the schema/data. This exercises real database rejection and recovery;
it does not simulate a lost connection, fill a volume, establish preservation of
committed records across restart, or prove a capacity alarm/business-operation
policy. See the current verification record for the execution result.

## Integrated delivery and PostgreSQL failure checks

[Audit delivery/storage failure evidence](audit-delivery-failure-evidence.md)
records four additional executed cases using the real consumer, transacted
listener factory and database. They cover transient/sustained INSERT denial,
DLQ replay, actual backend-connection termination, denied reads and append-only
writer privileges. This closes the earlier separation between broker-only and
repository-only checks for those controlled scenarios. Disk exhaustion, capacity
alarms, external broker/multi-node behavior and the selected business-operation
failure policy remain unverified.
