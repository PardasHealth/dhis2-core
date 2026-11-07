# FCS_CKM.6 — Timing and event of cryptographic key destruction

ISO/IEC 15408-2:2026, clause 10.3.11. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: No direct implementation trace established.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fcs-ckm-6)

## Web scope

Related; application and platform evidence required. A web application relies on JVM/OS/provider randomness, key memory handling and trusted time. These are supporting responsibilities rather than ignored hardware features; identify allocation and evidence in the deployment profile. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FCS_CKM.6.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### FCS_CKM.6.2

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Encryption configuration, RSA key generation/loading and key publication are evidence candidates, not an approved cryptographic profile.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

- [HibernateEncryptionConfig.java](../../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java)
- [AuthorizationServerConfig.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthorizationServerConfig.java)
- [KeyStoreUtil.java](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/oidc/KeyStoreUtil.java)
- [PublicKeysController.java](../../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/PublicKeysController.java)

## Work required before closure

No proof of key destruction timing or erasure across JVM, disk, backups and replicas was located. Define destruction triggers, method and evidence for every selected key class.

Proposed owner: Security / platform / data migration owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10), [G-13](../web-gap-solutions.md#g-13).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
