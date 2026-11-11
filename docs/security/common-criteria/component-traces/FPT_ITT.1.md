# FPT_ITT.1 — Basic internal TSF data transfer protection

ISO/IEC 15408-2:2026, clause 15.9.7. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Partial element mappings; remaining elements open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fpt-itt-1)

## Web scope

Related when distributed application parts are enabled. Embedded/external broker, cache and multi-node topology determine the internal TSF/data transfer boundary. Retain these obligations for each enabled distributed configuration; single-node assumptions do not justify ignoring external links. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [ArtemisConfig.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java)
- [ExternalArtemisConnectionFactory.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ExternalArtemisConnectionFactory.java)
- [ConfigurationKey.java](../../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java)

Symbols to inspect: `ArtemisConfig.jmsConnectionFactory`; `ExternalArtemisConnectionFactory.create`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FPT_ITT.1.1

**Partial source mapping.** [ExternalArtemisConnectionFactory.java:46](../../../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ExternalArtemisConnectionFactory.java#L46): NATIVE broker configuration can require server-authenticated TLS 1.2/1.3, certificate chain and hostname validation without plaintext fallback. Local JMS tests exercise negotiated TLS message transfer and certificate/credential/transport rejection.

**Open:** Optional broker path only, conditional on the TOE boundary. TLS is opt-in; embedded transport is unchanged. Other internal links, deployed configuration, rotation/recovery and exact AFTA selections remain open. See ../afta-external-broker-tls.md.

## Evidence and limits

The optional external broker path supports verified TLS and targeted Netty/JMS positive/negative tests. This is conditional evidence for one link, not the complete internal transfer boundary.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

- [ExternalArtemisConnectionFactoryTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/config/ExternalArtemisConnectionFactoryTest.java)
- [ArtemisExternalTlsIntegrationTest.java](../../../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/config/ArtemisExternalTlsIntegrationTest.java)

Related boundary references, **not implementation coverage**:

- [CacheInvalidationListener.java](../../../../dhis-2/dhis-support/dhis-support-cache-invalidation/src/main/java/org/hisp/dhis/cacheinvalidation/redis/CacheInvalidationListener.java)

## Work required before closure

Select the internal boundary/threats and verify every configured broker, Redis, database and other selected link. Production deployment, certificate rotation/recovery and complete internal TSF-data protection remain open.

Proposed owner: Distributed platform / deployment owner.

Solution plans: [G-06](../web-gap-solutions.md#g-06), [G-08](../web-gap-solutions.md#g-08).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
