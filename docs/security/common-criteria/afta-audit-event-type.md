# Audit event type and enum validation

Updated: **21 November 2026**. Branch: `work`.

**Selected source rows: 2 (`FAU_GEN.1.2`) and 48 (`FPT_TDC.1.1`).** Statuses remain Partial evidence and Gap respectively; neither is fully closed.

The audit consumer previously accepted a missing/null event type. Jackson also accepted JSON numbers as enum ordinals, silently interpreting an input such as `auditType: 0` as an event type and a numeric scope as a scope name. Audit records could therefore be stored without an event type or with values selected by enum declaration order rather than the named message contract.

The local reader now enables `FAIL_ON_NUMBERS_FOR_ENUMS`, and the pre-sink validation requires a non-null type. Invalid input uses the existing fixed rejection marker and generic exception before audit logging or storage. The shared ObjectMapper remains unchanged. Named type/scope values and matched destinations retain their behavior; this does not add a new event-type taxonomy.

Implementation: [`b80da28532`](https://github.com/PardasHealth/dhis2-core/commit/b80da28532223c385729f1886f802cf5e9bc3365).

| Code or check | Evidence |
| --- | --- |
| Local enum policy and required type | [AbstractAuditConsumer.java](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java) |
| Numeric type/scope and absent/null type across all four consumers, plus destination regression cases | [40 destination/input tests](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AuditDestinationScopeTest.java) |
| Valid JSON and shared mapper isolation | [Eight message tests](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AuditMessageValidationTest.java) |
| Exact source/report hashes, existing failure tests and PostgreSQL/JMS recovery | [Execution record](../evidence/afta-audit-event-type-2026-11-21.json) |

**51 unit checks and four PostgreSQL/JMS checks passed; no failures, errors or skips.** The pre-fix run recorded 16 failures in the 51 unit checks. Existing CI already selects and requires the affected suites; hosted CI success is not claimed.

External/legacy producers using numeric ordinals or omitting event type now fail delivery and must send valid named values. Retry/dead-letter handling follows the existing deployment policy. This does not define every required audit field, authenticate publishers, select cross-peer precedence rules, establish deployed alert delivery or prove complete event coverage. Full selected AFTA definitions and acceptance remain open.
