# Common Criteria audit storage failure investigation (FAU_STG.3 / FAU_STG.4)

Status: **partial source-level evidence only**. No claim of Common Criteria conformance or end-to-end delivery guarantee.

## Confirmed source path

1. Audit messages are received by `@JmsListener` implementations in `dhis-service-audit-consumer` (metadata, API, aggregate and tracker).
2. Each calls `AbstractAuditConsumer._consume(TextMessage)`.
3. When `AUDIT_DATABASE` is enabled, `_consume` invokes `AuditService.addAudit`.
4. `DefaultAuditService.addAudit` delegates to `JdbcAuditRepository.save`, which calls `SimpleJdbcInsert.executeAndReturnKey`.

Before this PR, `AbstractAuditConsumer` caught **all** persistence exceptions and logged them without rethrowing. This can make a listener invocation appear successful even though the audit row was not saved. The PR rethrows unexpected processing failures after logging. Malformed payload deserialization failures are now rethrown as `IllegalArgumentException`; poison-message disposition still requires a defined dead-letter policy and bounded redelivery.

## Important limitations

- Previously, `ArtemisConfig.jmsTopicTemplate` set `DeliveryMode.NON_PERSISTENT`. This PR changes it to `DeliveryMode.PERSISTENT`, but that alone does **not** establish durable topic subscriptions or broker restart recovery.
- This PR configures `ArtemisConfig.jmsListenerContainerFactory` with `setSessionTransacted(true)`, allowing the JMS receive to roll back on listener failure. The listener factory now also requests shared durable subscriptions via `setSubscriptionDurable(true)` and `setSubscriptionShared(true)`. These settings require broker-backed verification of subscription naming, cluster behavior and redelivery. Transactional acknowledgment alone cannot establish recovery.
- Embedded Artemis config defines DLQ and ExpiryQueue, but this does not prove that audit messages will be routed to either.
- The producer can optionally use an in-memory queue (`AUDIT_USE_IN_MEMORY_QUEUE_ENABLED`); its crash-loss behavior remains to be tested.
- No verified storage capacity threshold, alert delivery, retry bound, poison-message isolation, or post-restart replay evidence exists.

## Required follow-up before asserting FAU_STG.3 / FAU_STG.4

1. Define the Security Target's permitted behavior on full storage and database outage (block operations, preserve pending audit messages, or explicitly accept loss).
2. Test database outage and storage exhaustion with event IDs and compare published, consumed, persisted and dead-letter counts.
3. Verify that the configured shared durable topic subscriptions are created correctly for all four audit destinations, including multiple application nodes, and establish bounded redelivery with DLQ. Persistent delivery and transacted listener sessions are configured in this PR, but need broker-backed failure, recovery and restart tests.
4. Test invalid payloads separately from transient database failures; ensure malformed messages cannot create infinite retries or disappear without evidence.
5. Verify alert thresholds, alert delivery and operator runbooks; document capacity limits and retention.
6. Check the audit feature flags for configurations where database persistence is disabled.

## Evidence in this PR

- `DefaultAuditServiceFailureTest` verifies that service-layer write failures propagate.
- `AbstractAuditConsumerFailureTest` verifies that persistence exceptions and malformed payload errors escape the consumer method.
- Both are unit-level checks. They **do not** exercise a real Artemis broker or PostgreSQL outage.
