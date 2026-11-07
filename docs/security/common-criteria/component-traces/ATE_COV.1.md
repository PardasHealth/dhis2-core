# ATE_COV.1 — Evidence of coverage

ISO/IEC 15408-3:2026, clause 13.2.3. Trace update: 2026-11-07; component disposition baseline: 2026-11-04.

**Status: Assurance evidence incomplete.** Boundary/artifact references; element mapping open. No conformance claim.

[All component pages](README.md) · [Component disposition](../../common-criteria-gap-assessment.md#ate-cov-1)

## Web scope

Related to web application; component/level selection requires ST completion. Web/API security behavior, supporting deployment or relevant software assurance; source review does not select every alternative component level. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions.

## Code candidates

None established in this review.

## Element trace

Identifiers below inventory the obligations; consult the licensed standard for their exact text. Partial mappings establish only the stated behavior and retain the stated limitations.

### ATE_COV.1.1C

**Unmapped — Evidence content.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ATE_COV.1.1E

**Unmapped — Evaluator activity.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

### ATE_COV.1.1D

**Unmapped — Developer delivery.** No individual implementation or artifact mapping has been established for this element in this review. The component candidates above must not be treated as proof for it. Complete the element-specific trace, applicable ST operations and verification.

## Evidence and limits

Recorded regression evidence totals 262 passes and four existing skips across two dated records and 31 classes; it is not a single full-suite run or complete selected SFR/TSFI coverage.

Test sources (execution and scope are recorded separately in the [verification record](../../common-criteria-verification.md)):

None established in this review.

Related boundary references, **not implementation coverage**:

None established in this review.

## Work required before closure

Build the required coverage analysis from selected requirements/TSFI to tests/results; resolve omissions and justify samples or rigorous completeness. Required artifact: Evidence of coverage.

Proposed owner: Security assurance owner / evaluator.

Solution plans: [G-08](../web-gap-solutions.md#g-08), [G-11](../web-gap-solutions.md#g-11).

Select the component and its operations in the agreed ST, resolve dependencies, map every selected element to exact code/configuration/process evidence, and verify positive and negative cases in the frozen deployment. Assurance elements require the corresponding developer and evaluator artifacts.
