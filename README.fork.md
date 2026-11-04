# PardasHealth DHIS2-Core fork

This repository retains DHIS2-Core's upstream architecture and shared Git history.
The original [README](README.md) describes the upstream project.

Fork-specific security work:

- [Web review guide: scope, evidence, gaps and solutions](docs/security/common-criteria/README.md)
- [Common Criteria 2026 standard-to-code traceability](docs/security/common-criteria-traceability.md)
- [Exact standard component ID → code lookup](docs/security/common-criteria-component-code-index.md)
- [All 261 component gap dispositions and required closure](docs/security/common-criteria-gap-assessment.md)
- [Web-application scope and reasons for exclusions](docs/security/common-criteria/web-application-scope.md)
- [FMT_MSA.3 code and test trace](docs/security/common-criteria/component-traces/FMT_MSA.3.md)
- [Review, fixes and remaining gaps](docs/security/common-criteria-review.md)
- [Solutions, acceptance checks and compatibility plans](docs/security/common-criteria/web-gap-solutions.md)
- [Proposed evaluation configuration and decisions](docs/security/common-criteria/evaluation-decisions.md)
- [Test/build/browser evidence](docs/security/common-criteria-verification.md)
- [Maintaining this fork and integrating upstream changes](docs/fork/upstream-maintenance.md)

These documents are engineering evidence, not Common Criteria certification.
All component IDs now have a baseline gap disposition. Complete element-level
coverage, deployment acceptance and formal evaluation remain open; see the gap
assessment before interpreting a code link as coverage.
