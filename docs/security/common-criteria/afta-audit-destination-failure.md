# Audit destination failure propagation

Updated: **21 November 2026**. Branch: `work`.

**Selected source row: 52 (`FRU_FLT.1.1`). Status remains Gap.**

`AuditProducerSupplier.publish` previously logged an error and returned normally when an audit scope had no destination. The optional scheduler therefore treated the publication as successful and discarded the event. Empty destinations had the same behavior; whitespace-only destinations were passed to the message manager.

Missing, empty or whitespace-only destinations now raise `IllegalStateException` without passing the event to the message manager. The existing scope-only error log is retained; the exception includes no event payload. The scheduler's existing failure handler retains the event and propagates the failure. Direct and overflow callers also receive the error. Valid destinations still send once, and underlying send failures propagate unchanged. The standard configured mapping already covers all audit scopes; this change makes a broken mapping visible rather than acknowledging it.

Implementation: [`376bd531a2`](https://github.com/PardasHealth/dhis2-core/commit/376bd531a2103e6ed0da6a98e1b74c36b11d130b).

| Code or check | Evidence |
| --- | --- |
| Destination validation and failure boundary | [AuditProducerSupplier.java](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditProducerSupplier.java) |
| Missing/null/empty/blank mappings, valid send and failure identity, real scheduler retention and retry after mapping repair | [Seven producer tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditProducerSupplierTest.java) |
| Eight scheduler and four monotonic queue compatibility cases | [Scheduler tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java), [queue tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/QueuedAuditTest.java) |
| Commands and exact implementation/source/report hashes | [Execution record](../evidence/afta-audit-destination-failure-2026-11-21.json) |

**19 tests passed, with no failures, errors or skips.** With the same tests and old production code, six of 19 failed. CI now selects and requires the producer suite as well as the existing scheduler and queue suites. Hosted CI success is not claimed.

The recovery test repairs an in-process mapping to exercise retry; it does not implement live configuration reload. This optional memory buffer still loses pending events on JVM crash. An unresolved first mapping failure can stop that processing pass until repair; this change does not introduce a quarantine policy. Direct/overflow failure retry belongs to the caller, and broker acknowledgments can remain uncertain. Live broker delivery, durable recovery, selected degraded functions, other application fault classes and full AFTA acceptance remain open.
