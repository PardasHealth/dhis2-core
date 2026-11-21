# Audit payload serialization failure boundary

Updated: **21 November 2026**. Branch: `work`.

**Selected source rows: 1 (`FAU_GEN.1.1`) and 52 (`FRU_FLT.1.1`).** Row 1 remains Partial evidence; row 52 remains Gap. Neither is fully closed.

The audit object factory previously caught a Jackson serialization error, logged its full stack trace and returned null. `AuditManager` then continued to direct publication or optional buffering without a successfully serialized payload. Serializer exception text may include audited values.

The factory now emits only `AUDIT_SERIALIZATION_FAILURE action=ABORT_PUBLICATION` and throws `IllegalStateException("Unable to serialize audit payload")` without the serializer's cause. The exception reaches the existing manager boundary before the producer or scheduler is called. It prevents this failure from being reported as a successful payload construction. Valid JSON, including Unicode, retains its format across METADATA, TRACKER, AGGREGATE and API scopes.

Implementation: [`532ef82f92`](https://github.com/PardasHealth/dhis2-core/commit/532ef82f92eae405f042099435ef7102095e2269).

| Code or check | Evidence |
| --- | --- |
| Serializer failure and fixed marker | [DefaultAuditObjectFactory.java](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/legacy/DefaultAuditObjectFactory.java) |
| Four valid scopes, four failing scopes, direct/buffered manager paths | [Ten payload tests](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditPayloadSerializationTest.java) |
| Existing manager, destination, scheduler and monotonic queue behavior | [Dated execution record with exact source/report hashes](../evidence/afta-audit-payload-failure-2026-11-21.json) |

**30 tests passed, with no failures, errors or skips.** With the same tests and old production code, six of 30 failed. The new checks use a real ObjectMapper and a getter that throws a synthetic private value. They confirm generic exception text, no attached cause, no producer/scheduler call on failure, and valid JSON compatibility. CI now selects and requires the payload suite and existing manager suite alongside audit regressions; hosted CI success is not established here.

This aborts publication of the failed event; it does not create a durable copy, retry policy, dead-letter queue or replacement success record. Callers may already handle exceptions; business-operation/transaction behavior needs its own selected policy and end-to-end proof. Arbitrary unchecked serializer failures are not assigned new recovery behavior. Full event inventory, upstream exception handling, live broker persistence, deployed log/alert delivery, fault classes and AFTA acceptance remain open.
