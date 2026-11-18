# Audit-buffer concurrent capacity and recovery

Updated: **18 November 2026**. Branch: `work`.

**Selected source row: 52 (`FRU_FLT.1.1`). Status remains Gap.**

Concurrent writers previously performed separate capacity, duplicate and insertion operations on the optional in-memory audit buffer. A thread-safe queue did not make that sequence atomic. A pre-fix run observed **202 entries despite the 200-entry limit**. A processing failure could also reclaim a removed entry's slot after an admission had filled it.

Admission now shares the scheduler processing monitor: capacity and duplicate checks plus insertion form one critical section, coordinated with failure requeue. Direct overflow publication runs outside the admission block and retains caller-visible failure propagation. Buffered records are not evicted to make space for overflow. Existing duplicate behavior at a full queue is retained: the incoming event is published directly.

Implementation: [`7318e85b9d`](https://github.com/PardasHealth/dhis2-core/commit/7318e85b9d628c29d25705c96d0414e5264b948c).

| Code or check | Evidence |
| --- | --- |
| Atomic admission and existing processing/requeue | [AuditScheduler.java](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java) |
| 400 distinct events, contention for the final slot, simultaneous duplicate admission, overflow failure, existing retry/drain behavior | [Eight scheduler tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java) |
| Existing monotonic expiry compatibility | [Four queue tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/QueuedAuditTest.java) |
| Exact commit, source/report hashes and commands | [Execution record](../evidence/afta-audit-buffer-capacity-2026-11-18.json) |

**12 cases passed; no failures, errors or skips.** With the new tests and old production code, one of 12 failed on the capacity overrun. Concurrency schedules vary; an earlier run passed, and no claim is made that every old-code execution reproduces the race. CI already selects and requires both suites. Hosted CI success is not established here.

The buffer remains optional and disabled by default. It does not survive JVM crash, guarantee exactly-once delivery, or automatically retry a failed direct overflow publication. Admission may wait behind the existing synchronized processing operation and broker call; production throughput and broker timeouts need deployment validation. Selected whole-application degraded functions and database, disk, broker and node fault classes remain undefined/unproven. This bounded fix does not close row 52.
