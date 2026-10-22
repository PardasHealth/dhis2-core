# External Artemis audit retention and recovery runbook

Status: operator deployment guidance and verification checklist; **not** evidence of a successful external-broker test.

## Deployment prerequisites

External Artemis is configured outside DHIS2. Enable persistent journal storage, durable shared topic subscriptions, TLS and authentication, restricted management access, backups, and alert delivery. DHIS2 uses the configured external broker host, port, username and password. Confirm network reachability from every DHIS2 node. Do not assume the embedded broker address settings are applied remotely.

## Artemis broker.xml example

Apply the following settings to the four audit topic addresses in the external broker's `<address-settings>` section. Merge with the site's existing policy and validate against the deployed Artemis version.

```xml
<address-setting match="dhis2.topic.metadata">
  <dead-letter-address>DLQ</dead-letter-address>
  <expiry-address>ExpiryQueue</expiry-address>
  <max-delivery-attempts>5</max-delivery-attempts>
  <redelivery-delay>1000</redelivery-delay>
</address-setting>
<address-setting match="dhis2.topic.aggregate">
  <dead-letter-address>DLQ</dead-letter-address>
  <expiry-address>ExpiryQueue</expiry-address>
  <max-delivery-attempts>5</max-delivery-attempts>
  <redelivery-delay>1000</redelivery-delay>
</address-setting>
<address-setting match="dhis2.topic.tracker">
  <dead-letter-address>DLQ</dead-letter-address>
  <expiry-address>ExpiryQueue</expiry-address>
  <max-delivery-attempts>5</max-delivery-attempts>
  <redelivery-delay>1000</redelivery-delay>
</address-setting>
<address-setting match="dhis2.topic.api">
  <dead-letter-address>DLQ</dead-letter-address>
  <expiry-address>ExpiryQueue</expiry-address>
  <max-delivery-attempts>5</max-delivery-attempts>
  <redelivery-delay>1000</redelivery-delay>
</address-setting>
```

Provision durable ANYCAST queues named `DLQ` and `ExpiryQueue` with the broker's supported address/queue configuration. Restrict access to authorized audit operators. Avoid changing global `#` retry policy or tracker job destinations.

## Required monitoring and alarms

- Track DLQ depth and oldest-message age; alert immediately when audit events enter the DLQ.
- Monitor journal filesystem free space, broker paging, disk-full/blocking status, producer send failures, and durable subscription backlog.
- Alert on broker unavailability and disconnected DHIS2 nodes; validate alert routing and escalation contacts.
- Define environment-specific warning/critical thresholds, maximum acceptable audit backlog and recovery time in the deployment record. No universal capacity threshold is asserted here.
- Preserve logs, alert timestamps, broker configuration version, event identifiers, and recovery actions as evaluation evidence.

## Controlled recovery procedure

1. Declare the incident, preserve a copy of the DLQ messages and relevant broker/application logs, and restrict operator access.
2. Correct the underlying database, storage, or network failure. Confirm the audit consumer is healthy.
3. Inventory dead-lettered events and establish stable identifiers. Check whether each event was already persisted in the audit repository; do not assume replay is idempotent.
4. Replay a small, approved batch to the **original audit topic**, preserving message body and required headers, using an audited broker management operation. Avoid direct database insertion and uncontrolled bulk replay.
5. Verify stored audit records, duplicates, ordering assumptions, and DLQ depth. Stop replay on unexpected duplication or parsing errors.
6. Record incident timeline, approved replay operator, counts (pending/replayed/failed), and independent verification before closure.

## Verification matrix for external broker

| Scenario | Evidence required | Status |
| --- | --- | --- |
| Publish persistent event, restart external broker, consume from durable subscription | Broker logs, message ID, stored audit record | Not tested |
| Fail audit database writes and observe transactional redelivery | Five receive attempts, listener error logs | Not tested |
| Keep failure active and inspect DLQ | Original payload, queue depth, delivery count | Not tested |
| Recover database and replay one event | Audit record and duplicate check | Not tested |
| Run two DHIS2 nodes sharing subscription | Single logical delivery, reconnect/failover behavior | Not tested |
| Simulate storage pressure in isolated staging | Paging/blocking behavior and alert evidence | Not tested |

Do not run disk-exhaustion or forced-failure scenarios in production. The automated embedded Artemis restart regression is separate evidence and does not establish external broker equivalence.
