# Audit destination and scope binding

Updated: **21 November 2026**. Branch: `work`.

**Selected source row: 48 (`FPT_TDC.1.1`). Status remains Gap.**

The four audit consumers previously trusted the message's claimed scope regardless of the receiving destination. For example, the metadata consumer could persist a message claiming API scope while applying the metadata sink settings. Missing scope was also accepted; null root records failed inconsistently later in processing.

Each concrete consumer now passes its fixed expected scope to the common processing method. The parsed record must be non-null and its scope must match before data conversion, audit logging or persistence. Mismatches, missing scope and null roots use the existing invalid-payload failure marker and generic exception, allowing the configured delivery failure handling to run. Valid matched messages retain their actor and data. There is no unchecked one-argument processing helper in this repository; its protected signature and test fixtures were updated.

| Receiving consumer/topic | Required scope |
| --- | --- |
| Metadata | `METADATA` |
| Aggregate | `AGGREGATE` |
| Tracker | `TRACKER` |
| API | `API` |

Implementation: [`29a53061e8`](https://github.com/PardasHealth/dhis2-core/commit/29a53061e83a51c53d7573d407ea92dbd304ab7d).

| Code or check | Evidence |
| --- | --- |
| Scope check before sinks | [AbstractAuditConsumer.java](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java) |
| Fixed scope at each listener | [Consumer sources](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/consumers) |
| All 12 scope mismatches, four missing scopes, four null roots and four matched deliveries | [24 destination tests](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AuditDestinationScopeTest.java) |
| Existing strict JSON, failure propagation and PostgreSQL/JMS recovery compatibility | [Exact source/report hashes and commands](../evidence/afta-audit-destination-scope-2026-11-21.json) |

**35 unit checks and four PostgreSQL/JMS recovery checks passed; no failures, errors or skips.** The corrected new harness with old production code recorded 20 failures in 35 unit cases. Previously permissive successful-message fixtures were updated to carry the required scope. CI selects and requires the destination suite; hosted CI success is not established here.

Existing producers in this repository include scope in their messages. External/legacy producers that omit or misroute it must correct their messages; these deliveries now fail and follow the deployment's existing retry/dead-letter policy. This fix does not authenticate a publisher, change broker permissions, choose a quarantine/availability policy, or define all attribute schemas and cross-peer precedence. A publisher with valid access can still claim a matching scope. Full trusted-peer definitions, external alert delivery and AFTA acceptance remain open; row 48 stays Gap.
