# Stored audit-record integrity: authenticate, alert and block reads

Updated: **18 November 2026**. Branch: `work`.

## Selected source rows and agreed first scope

| Selected source row | Requirement ID | Implemented behavior | Remaining acceptance |
| --- | --- | --- | --- |
| 27 | `FDP_SDI.2.1` | Opt-in keyed authentication detects modification of matching stored rows in the consolidated `audit` table. | Exact full clause, complete protected-data inventory, legacy audit tables, detection frequency, deletion/rollback/suppression and deployment acceptance. |
| 28 | `FDP_SDI.2.2` | A failed authentication emits a structured ERROR alert and blocks the entire query/count result. | External alert delivery/acknowledgement, incident recovery, key lifecycle, trusted history migration and production acceptance. |

The selected first scope is **audit records, with an alert and blocked reads**.
This implementation starts with the consolidated PostgreSQL `audit` table used by
`AuditRepository`/`AuditService`. It does not cover application configuration,
business tables, older changelog/audit tables or text log files. Both rows have
partial implementation evidence; neither is fully closed or certified.

## Record authentication and read boundary

`AuditIntegrityGuard` uses an independent 32-byte HMAC-SHA-256 key. A versioned
`v1:` tag authenticates the record ID, audit type/scope, stored timestamp, creator,
class, UID, code, attributes and compressed payload. Fields are length-prefixed,
with null distinct from an empty value; ID binding prevents simply copying a tag
from another record. Verification compares fixed-length MAC bytes with
`MessageDigest.isEqual`. This is keyed authentication, not an unkeyed checksum or
an assignment to an unspecified AFTA crypto iteration.

The repository reserves each ID before signing and signs its parameter snapshot,
not a second read of mutable caller fields. Signed timestamps use microsecond
precision, matching the database storage resolution. PostgreSQL normalizes the
attributes to its JSONB text representation before signing. This avoids key-order,
whitespace and numeric-notation differences or lossy floating-point parsing in the
signer. Changes to stored numeric scale that affect existing text attribute
filters are still authenticated. Enforcement requires PostgreSQL UTF8 storage.

Verification occurs **before enum conversion, decompression or attribute
materialization**. A missing, malformed, mismatched or unverifiable tag raises
`AuditIntegrityViolationException`; no result list is returned. Strict counts
also authenticate all matching records rather than bypassing checks with a raw
`COUNT(*)`. A late failure prevents the result/count from returning; it is not
silently skipped. Other selected records may already have been read internally,
but the public repository operation fails rather than returning a partial list.

The local alert has this form:

```text
AUDIT_INTEGRITY_FAILURE recordId=123 action=BLOCK_READ reason=authentication_failed
```

Only numeric identity and a fixed reason are included. Tests require ERROR level
and exclude payload, actor, key and tag values. Existing logging infrastructure
must route this event to operations. No email, webhook, SIEM delivery or alert
acknowledgement is established by this patch. Verification exceptions must not be
caught and converted to a successful empty result by downstream integrations.

## Configuration and rollout

Core defaults preserve existing deployments: integrity enforcement is **off**.
The normal injected repository receives a startup-created guard. The two-argument
repository constructor is retained for explicit legacy/manual integrations; use
the injected bean or the guard-aware constructor for enforcement.

For a deployment whose history and writers are ready for enforcement:

```properties
audit.database=on
audit.integrity.enabled=on
audit.integrity.key=REPLACE_WITH_AN_INDEPENDENT_BASE64_ENCODED_32_BYTE_RANDOM_KEY
```

The value is Base64 encoding of **32 bytes**, not a 32-character password. An
operator can generate a unique value using `openssl rand -base64 32`; do not use
the synthetic test keys or the encryption password. Keep the existing chosen key
in protected configuration and backups. The configuration catalog marks it
confidential. Enabled mode rejects missing, malformed and wrong-length keys at
startup, without echoing them. A length check does not establish entropy or
provider certification.

The additive repeatable migration creates nullable `audit.integritytag` storage.
It is idempotent and **does not sign existing history**. Disabled mode retains
legacy writes/reads, including tables without that column. Enabled mode signs new
single and batch writes and **rejects unsigned legacy rows** as well as corrupt
rows. There is no automatic trust-on-first-read, key fallback or blind backfill.

Before enabling on an existing installation, establish a trusted history
migration/archival plan and test it. This patch does not provide an operator
attestation or re-signing tool for existing data. Complete the schema migration,
upgrade every writer, and configure all participating nodes with the same chosen
key before enforcement. Old/off writers can create unsigned rows that enforcing
readers will block. A different valid key also blocks old records; key rotation,
re-signing and recovery need an independently tested procedure. No deployment,
key provisioning or modification of a live database was performed here.

Enforcement adds ID reservation/JSONB-normalization work to writes. Counts scan
and authenticate matching payloads instead of using a cheap database count.
Benchmark the selected deployment and retention volume; these tests do not prove
production latency or fault-tolerance limits.

## Code and verification trace

- [AuditIntegrityGuard](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/AuditIntegrityGuard.java): versioned authentication, independent key validation, alert and rejection.
- [JdbcAuditRepository](../../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java): signed single/batch writes and verified query/count paths.
- [AuditIntegrityConfig](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/config/AuditIntegrityConfig.java) and [configuration keys](../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java): startup binding, disabled compatibility default and confidential key.
- [Additive migration](../../../dhis-2/dhis-support/dhis-support-db-migration/src/main/resources/org/hisp/dhis/db/migration/pardas/R__PardasHealth_audit_integrity.sql): nullable tag column; no automatic history attestation.
- [Guard tests](../../../dhis-2/dhis-support/dhis-support-audit/src/test/java/org/hisp/dhis/audit/AuditIntegrityGuardTest.java): 29 key, field, version/null, boundary, wrong-key and concurrent-MAC cases.
- [Startup tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/config/AuditIntegrityConfigTest.java): three Spring/key/default/confidentiality checks.
- [PostgreSQL tests](../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditIntegrityPostgresTest.java): 21 real-storage cases with real migrations, tampering, recompressed payload forgery, copied tags, precise JSONB values, unsigned history, wrong keys, captured alerts and blocked counts/queries.
- [Dated execution evidence](../evidence/afta-audit-record-integrity-2026-11-18.json): exact implementation/source/report hashes, commands and compatibility executions.
- [Required CI workflow](../../../.github/workflows/cc-audit-regression.yml): selects and requires the guard, startup and PostgreSQL suites along with existing audit regressions. A committed workflow does not establish hosted CI success.

The storage red phase has the new guard/configuration, constructor and schema
scaffolding available, while the repository retains its previous unsigned
write/read behavior. **19 of 21 cases fail**, with legacy and JSONB-normalization
controls passing. This is an integration-wiring baseline, not a claim that a
complete old release contains the new API. The [test-only red-phase patch](../evidence/baselines/afta-audit-integrity-repository-red-phase.patch)
reproduces that repository source in an isolated checkout of the implementation
commit. Its resulting source hash and commands are in the evidence. Restore the
repository file afterward; the patch deliberately removes enforcement for tests
and must not be deployed.

## What remains unproven

Per-record checks apply to rows selected by a query. They do not continuously scan
unread records or prove detection of deletions, valid-record rollback, missing
history, whole-dataset replay or altered selector fields that omit a record from
a query. There is no external append-only anchor, periodic verifier or automatic
recovery in this change. Database schema, application/configuration and key access
remain trusted boundaries; an attacker controlling the application or key can
forge tags. Protect keys independently of a database-only attacker and use unique
keys per deployment.

The exact AFTA clause/version, all audit engines and access paths, trusted legacy
migration, alert transport, key lifecycle, production performance/failover and
evaluator acceptance remain open. These limitations prevent full row closure.

Implementation commit: [`399eaa924a`](https://github.com/PardasHealth/dhis2-core/commit/399eaa924a9f7bd24963cc225db07b651738b96d). Selected source rows 27–28 move from Gap to Partial evidence; no full requirement is closed.

Recorded validation: **39 unit cases passed**, **52 PostgreSQL cases passed with 2 existing skips**, and **11 H2 API authentication cases passed**. No failures or errors in these final runs. The 21 integrity PostgreSQL cases are included in the 52, not additional to them.
