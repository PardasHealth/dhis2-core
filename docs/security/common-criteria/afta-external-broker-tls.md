# AFTA remediation: protect the external audit-broker connection

Date: **11 November 2026**. Checklist row 47, `FPT_ITT.1.1`; related gap G-06.
**Partial evidence for one optional connection; not AFTA approval.**

## For management

The application could previously configure an external audit/message broker only
through a plain TCP connection. It can now use TLS to encrypt that connection
and verify the broker's certificate and hostname. Invalid certificates and
credentials reject the connection; the client does not retry using plaintext.
The default embedded broker continues to use its existing in-process transport.

The setting must be enabled and configured on every participating application
node. This change does not prove that a deployed site is using TLS. Other
internal connections and operational certificate management still require work.

Row 47 moves from a gap to **partial evidence** because a concrete path now has
implementation and transport tests. The primary checklist therefore has
**43/78 partial entries (55.1%), 10 gaps (12.8%) and 25 needing definitions
(32.1%)**. **0/78 are fully closed; all 78 still have open obligations.** These
are evidence percentages, not a security score or estimate of effort remaining.
See the updated [AFTA charts](afta-selected-requirements-review.md).

## Configuration

Only `artemis.mode=NATIVE` uses this TLS client. Configure the broker's own TLS
acceptor, certificate chain, broker permissions and persistence separately. In
`dhis.conf`, use the actual broker DNS name matching its certificate and its TLS
port. The following is a template, with secrets supplied through the site's
protected configuration process:

```properties
artemis.mode=NATIVE
artemis.host=broker.example.org
artemis.port=61617
artemis.username=REPLACE_WITH_BROKER_USER
artemis.password=REPLACE_WITH_PROTECTED_BROKER_PASSWORD
artemis.ssl.enabled=on
artemis.ssl.truststore.path=/opt/dhis2/certs/broker-trust.p12
artemis.ssl.truststore.type=PKCS12
artemis.ssl.truststore.password=REPLACE_WITH_PROTECTED_TRUSTSTORE_PASSWORD
```

The truststore contains reviewed CA certificates, not the broker's private key.
Mount it read-only and make it readable by the application identity. The default
format is PKCS12; configure the format explicitly if using another supported JDK
store. Without an explicit store, the JDK provider's default trust anchors apply.
A store password without a store path is rejected. An explicit store/path password
with TLS disabled is rejected to avoid silently using plaintext.

The client uses the JDK TLS provider, enables TLS 1.2/1.3, requires hostname
verification and does not provide trust-all or hostname-bypass settings. TLS
parameters are passed as structured transport options, not interpolated into a
broker URI. Forced transport SSL parameters prevent Artemis SSL system-property
overrides from weakening this profile. The truststore password is marked
confidential in the existing configuration-key mechanism; no password is added
to the application log or this report.

The compatibility default remains `artemis.ssl.enabled=off`. Explicitly enable
TLS for a protected NATIVE deployment. Embedded mode ignores these external TLS
options. No application-client certificate/mutual TLS feature is introduced.

## Code trace

| Artifact | Contribution to `FPT_ITT.1.1` |
| --- | --- |
| [ExternalArtemisConnectionFactory.create](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ExternalArtemisConnectionFactory.java) | Constructs the external Netty transport with mandatory certificate/hostname verification when TLS is enabled, explicit protocol limits and no plaintext fallback. |
| [ArtemisConfig.jmsConnectionFactory](../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java) | Uses the helper for NATIVE connections and preserves broker username/password authentication and embedded in-VM behavior. |
| [ConfigurationKey](../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java) | Defines the four TLS/truststore configuration keys and marks the truststore password confidential. |
| [ExternalArtemisConnectionFactoryTest](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/config/ExternalArtemisConnectionFactoryTest.java) | Checks secure options, compatibility defaults, truststore option handling, credential preservation, confidentiality metadata and embedded isolation. |
| [ArtemisExternalTlsIntegrationTest](../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/config/ArtemisExternalTlsIntegrationTest.java) | Uses real local TCP/TLS sockets, a generated CA/leaf chain and a password-protected broker to test message transfer and certificate/credential/transport failure. |

## Verification and limits

**118 targeted checks pass, with no failures, errors or skips:** 24 broker/audit
cases (16 new TLS/factory cases), 21 provider-policy cases and 73 H2 guard/API
cases. WAR packaging succeeds using existing browser assets. The three changed
runtime classes match the compiled classes inside the WAR; the PDF dependency
load check passes. Counts overlap earlier inventories and are not added to them.

The local broker fixture is reached through the same NATIVE client configuration
as an external broker. A positive test sends and receives a JMS message and
inspects the negotiated TLS session. Negative tests cover wrong hostname,
unrelated CA, expired CA-signed leaf, missing truststore, invalid broker password,
TLS client to plaintext broker and plaintext client to TLS-only broker.

Existing broker restart/durable delivery, poison-message recovery and audit-buffer
regressions are retained. Exact counts, source hashes, authentication regressions
and WAR packaging results are in the
[dated evidence record](../evidence/afta-external-broker-tls-2026-11-11.json).
The test keys/certificates and broker data are temporary and are not deployed or
committed. The initial positive fixture lacked queue-creation permission; its
permission was corrected before the passing run.

This does not establish full `FPT_ITT.1.1` fit. Final TOE boundaries determine
whether the broker is an internal part or a trusted external entity. All selected
links, including database/Redis and alternate enabled clients, need their own
configuration and evidence. Site deployment, certificate rotation/revocation,
revocation checking policy, restore, monitoring and sustained availability are
not established by these local tests. TLS algorithm/provider approval and the
undefined AFTA TLS/X.509 extensions are not inferred from this implementation.

Implementation commit: `a3f6a77f45260ebd11c44b574739e8418d928dd2`.
