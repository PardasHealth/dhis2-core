# Resolver-owned API-token buffer cleanup

Date applied: **16 November 2026**. Branch: `work`.
Implementation: [20342bc346](https://github.com/PardasHealth/dhis2-core/commit/20342bc3467840a006afdf4596a024ea96e04458).

## Selected source rows

| Selected source row | Requirement ID | Bounded change | Overall status |
| --- | --- | --- | --- |
| 20 | `FDP_RIP.2.1` | Overwrite the resolver-owned raw-token character array after checksum/hash processing, on success and failure. | **Gap / evidence not established** for complete residual-information protection. |

## Implementation and evidence

`ApiTokenResolver.resolve` extracted a raw token into a temporary `char[]` and
left it intact after checksum verification or hashing. The array now runs through
a `finally` block that fills it with zero characters before returning or throwing.
The resulting hash, request acceptance rules and public error handling remain.
Empty/unmatched headers do not allocate this raw-token buffer.

- [ApiTokenResolver](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolver.java): `resolve`, local array and `finally` cleanup.
- [ApiTokenSecretBufferTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenSecretBufferTest.java): four cases capture the actual array supplied to checksum processing and observe zero contents after success, checksum rejection, checksum error or hashing error. Static generator mocks expose the array reference and control failures; only synthetic input is used.
- [ApiTokenResolverTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenResolverTest.java): existing real checksum/hash and malformed/missing header cases remain selected.
- [Dated execution evidence](../evidence/afta-token-secret-buffer-2026-11-16.json): implementation commit, selected source row, baseline, commands and source/report hashes.
- [Fork workflow](../../../.github/workflows/cc-audit-regression.yml): explicitly selects the new suite and requires a successful nonempty report.

All four new cases fail against the preceding resolver because the captured array
retains raw characters. They test the resolver's observable buffer lifecycle,
not the JVM's physical memory or garbage collector.

## Remaining gap and next acceptance work

This is **best-effort mutable-buffer hygiene**, not a secure-erasure guarantee.
The servlet header, regex/group strings, generator internals and runtime copies
can retain immutable data. Compiler/JIT behavior, heap dumps, swap, process memory,
files, storage media, replicas and backups are not proven cleared by `Arrays.fill`.

The selected protected-resource inventory, allocation/deallocation rules,
application/platform responsibilities and deployed residual-data tests remain
undefined. Row 20 therefore stays Gap and the requirement-status totals do not
change. The implementation-progress chart records one additional linked change
and one additional primary entry with a bounded follow-up; neither is a closure
or compliance percentage.

Final verification: **74 Java cases passed** (61 unit and 13 H2 API), with
zero failures, errors or skips. Four baseline failures remain separately recorded.
The progress chart now records 25 linked implementation commits affecting 21/78
primary entries; this row remains Gap. Execution counts overlap earlier records.
