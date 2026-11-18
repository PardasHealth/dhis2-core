# Audit-buffer monotonic timing

Updated: **18 November 2026**. Branch: `work`.

**Selected source row: 49 (`FPT_STM.1.1`). Status remains Gap.**

The optional in-memory audit buffer previously used wall-clock milliseconds for elapsed time and compared only configured delay values. A newer item with a shorter delay could become the queue head even when an older item had already expired. A backward wall-clock change could also postpone publication.

`QueuedAudit` now uses `System.nanoTime()` for elapsed time and orders entries by actual monotonic deadlines. The public constructor and scheduler delay remain unchanged. A package-private shared ticker supports deterministic tests. Deadline subtraction preserves ordinary short queue intervals across the monotonic counter's signed wraparound. This is an in-process elapsed-time control; it does not provide UTC timestamps or survive restart.

Implementation: [`b8898fc224`](https://github.com/PardasHealth/dhis2-core/commit/b8898fc224ef730cbf45329812e3e96c96633e54).

| Code or check | Evidence |
| --- | --- |
| Deadline creation, delay and ordering | [QueuedAudit.java](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/QueuedAudit.java) |
| Expired older event versus newer shorter delay; equal configured delays; nanosecond expiry; counter wraparound | [Four queue tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/QueuedAuditTest.java) |
| Publish failure retention, recovery, early-publication prevention and successful drain | [Four scheduler tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java) |
| Exact commit, source/report hashes and command | [Execution record](../evidence/afta-audit-monotonic-delay-2026-11-18.json) |

**Eight tests passed; no failures, errors or skips.** CI selects and requires both suites. No pre-fix execution or hosted CI success is claimed.

Remaining acceptance includes the agreed time authority, synchronization, clock permissions, drift, rollback/skew/failure behavior for audit timestamps, sessions and tokens, and deployment evidence. The optional buffer remains disabled by default and loses buffered events on JVM crash. This change does not close row 49 or establish fault tolerance.
