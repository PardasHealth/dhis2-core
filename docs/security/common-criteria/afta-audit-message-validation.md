# Audit message ambiguity rejection

Updated: **21 November 2026**. Branch: `work`.

**Selected source row: 48 (`FPT_TDC.1.1`). Status remains Gap.**

The audit consumer previously accepted duplicate JSON properties using the later value and ignored additional content after the first message object. Conflicting actor or scope properties could therefore be resolved silently, and two supplied JSON values could be treated as one successful delivery.

The consumer now creates a local ObjectReader with strict duplicate detection and failure on trailing tokens. Duplicate properties in the message or nested structured data and extra root values are rejected before audit logging or database persistence. Ordinary trailing whitespace is accepted. This does not mutate the shared ObjectMapper. String-valued serialized audit data remains opaque; this change does not recursively interpret that string as an independently agreed schema.

Parsing/encoding IO failures emit only `AUDIT_MESSAGE_REJECTED reason=INVALID_PAYLOAD action=FAIL_DELIVERY` and throw a generic `IllegalArgumentException` without the parser cause. This avoids forwarding payload-bearing parser diagnostics to JMS error handling. Existing storage failure propagation remains unchanged.

Implementation: [`76ed5687b5`](https://github.com/PardasHealth/dhis2-core/commit/76ed5687b5994391342a56df6668dd0da814ee1c).

| Code or check | Evidence |
| --- | --- |
| Strict reader and failure boundary | [AbstractAuditConsumer.java](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java) |
| Duplicate actor/scope/nested attributes, trailing object/array, valid structured and string data, shared mapper isolation | [Eight validation tests](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AuditMessageValidationTest.java) |
| Existing malformed-message and database failure handling | [Consumer failure tests](../../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AbstractAuditConsumerFailureTest.java) |
| PostgreSQL and JMS recovery compatibility, exact source/report hashes and commands | [Execution record](../evidence/afta-audit-message-validation-2026-11-21.json) |

**11 unit checks and four PostgreSQL/JMS recovery checks passed; no failures, errors or skips.** The corrected harness with old production code recorded five failures in the 11 unit checks. CI selects and requires the validation suite alongside existing audit regressions. Hosted CI success is not established here.

Duplicate-field producers will now fail delivery and enter the deployment's existing retry/dead-letter behavior instead of producing an audit record. This change does not choose a new poison-message, quarantine or availability policy. The complete trusted-peer identities and authorizations, required attributes and schemas, precedence across different valid messages, external alert delivery and full AFTA definitions/acceptance remain open. Row 48 is not upgraded solely from this parser fix.
