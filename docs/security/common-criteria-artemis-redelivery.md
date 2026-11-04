# Common Criteria audit broker redelivery policy

## Embedded Artemis policy

The embedded broker configures a dead-letter address (`DLQ`), expiry address
(`ExpiryQueue`), five maximum delivery attempts, and a one-second redelivery delay.
The retry limit and delay are applied only to the four named audit topics; the `#` fallback retains the existing DLQ and expiry address settings for other destinations.

A listener exception should roll back a transacted JMS receive. After repeated
unsuccessful deliveries, the broker is configured to route the message to the
dead-letter queue rather than retry indefinitely. The original policy description
was configuration evidence; [integrated PostgreSQL failure and replay tests](common-criteria/audit-delivery-failure-evidence.md)
now exercise the production listener/consumer path for the documented scenarios.
External broker, full-storage and multi-node acceptance still require separate evidence.

## External broker deployments

External Artemis deployments are **not** configured by this Java code. Operators
must independently configure equivalent `address-setting` rules on their broker,
including `dead-letter-address`, `max-delivery-attempts`, and
`redelivery-delay`, and provision the DLQ queue with restricted access.

## Required verification before FAU_STG.3 / FAU_STG.4 / FAU_STG.5 sign-off

1. Verify all four audit topic subscriptions exist and remain durable after
   disconnect and restart, including multiple DHIS2 nodes.
2. Inject an audit database write failure and confirm rollback and redelivery.
3. Keep the failure active, confirm five attempts and DLQ routing, and inspect
   the original payload and failure metadata.
4. Recover the database, replay the dead-letter message safely, and check for
   duplicate audit records.
5. Test embedded and external broker restart, disk exhaustion, and alarm
   generation. Confirm operational ownership of DLQ monitoring and replay.

## Risk notes

The audit-specific address matches avoid changing retry limits for tracker job
queues and other non-audit destinations. Broker persistence must
be enabled and backed by durable storage for crash recovery. A dead-letter
queue without monitoring and recovery procedures is not sufficient for
Common Criteria compliance.

## 2026 reference and current review

For ISO/IEC 15408-2:2026, availability is FAU_STG.3, possible-loss action is
FAU_STG.4, and full-storage behavior is FAU_STG.5. See the
[consolidated traceability matrix](common-criteria-traceability.md) and
[current verification record](common-criteria-verification.md). Earlier statements
about absent evidence describe the original investigation, not a new test result.
