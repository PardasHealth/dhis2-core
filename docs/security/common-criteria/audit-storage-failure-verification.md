# Common Criteria audit-storage failure verification plan

Status: Proposed verification work; no compliance claim until implementation and CI evidence are reviewed.

## Scope

ISO/IEC 15408-2:2026 FAU_STG.2 (availability of stored audit records), FAU_STG.3 (action in case of possible audit data loss), and FAU_STG.4 (prevention of audit data loss). Confirm the selected Security Target's exact assignments and selections before assigning pass/fail.

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
3. Review the Security Target selections for FAU_STG.2/3/4.
4. Update the Common Criteria assessment sheet and dashboard with links to source and CI evidence.
5. Only promote a requirement to verified after evidence review.
