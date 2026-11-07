# FDP_SDC.1 — Stored data confidentiality

ISO/IEC 15408-2:2026, clause 11.14.7. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Partial implementation evidence.** Component code candidates; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#fdp-sdc-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

- [HibernateEncryptionConfig.java](../../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java)

Symbols to inspect: `HibernateEncryptionConfig.aes128StringEncryptor/hibernateEncryptors`. This component-level list is not a mapping to every element below.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### FDP_SDC.1.1

**Unmapped — Functional requirement.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Field encryption configuration is a candidate only; coverage of all selected stored user data and safe key migration remain open.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

None established in this review.

## Work required before closure

Field encryption covers selected data, not a complete selected stored-data inventory. Include database, files, backups, caches and temporary data and verify key provisioning.

Proposed owner: Security / platform / data migration owner.

Solution plans: [G-10](../web-gap-solutions.md#g-10), [G-13](../web-gap-solutions.md#g-13).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
