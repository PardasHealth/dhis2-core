# Common Criteria audit broker redelivery policy

## Embedded Artemis policy

The embedded broker configures a dead-letter address (`DLQ`), expiry address
(`ExpiryQueue`), five maximum delivery attempts, and a one-second redelivery delay.
These settings apply to the embedded broker's `#` address match.

A listener exception should roll back a transacted JMS receive. After repeated
unsuccessful deliveries, the broker is configured to route the message to the
dead-letter queue rather than retry indefinitely. **This is a configuration
claim, not an end-to-end test result.**

## External broker deployments

External Artemis deployments are **not** configured by this Java code. Operators
must independently configure equivalent `address-setting` rules on their broker,
including `dead-letter-address`, `max-delivery-attempts`, and
`redelivery-delay`, and provision the DLQ queue with restricted access.

## Required verification before FAU_STG.3 / FAU_STG.4 sign-off

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

The `#` setting also applies to non-audit destinations on the embedded
broker. A narrower audit-specific address policy should be used if other
message types require different retry semantics. Broker persistence must
be enabled and backed by durable storage for crash recovery. A dead-letter
queue without monitoring and recovery procedures is not sufficient for
Common Criteria compliance.
