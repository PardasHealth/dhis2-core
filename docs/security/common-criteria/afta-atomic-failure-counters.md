# AFTA remediation: atomic failure counters

Date: **15 November 2026**. Selected rows 29/30 (`FIA_AFL.1.1` and
`FIA_AFL.1.2`). Scope: concurrent updates to the existing login, recovery and
MFA-disable counters, preserving the existing lockout predicates and settings.
This is partial implementation evidence, not complete AFTA acceptance.

## Defect and correction

Each failure previously read an integer from its cache and later wrote the
incremented value. Concurrent attempts could read the same old value and both
write the same next value. Three deterministic service regressions reproduce
two simultaneous failures being counted as one, independently for password
login, account recovery and MFA disable.

`Cache.update` provides an atomic read/modify/write operation. The local cache
uses a cache2k entry processor. Redis reads the original serialized bytes,
evaluates a side-effect-free update function and uses a Lua compare-and-set to
commit only if the stored bytes are still the same, or the key is still absent.
On conflict the value is read again and the function is retried. Successful
writes apply the cache's configured write expiry, retaining the existing JDK
serialization and key regions. Redis retries are bounded at 64 attempts; exhausted
contention, serializer failures or missing results raise an exception instead of
reporting an uncommitted value.

`DefaultUserService.registerFailedLogin`, `registerRecoveryAttempt` and
`registerFailed2FADisableAttempt` use the operation with checked integer addition.
A counter cannot wrap to a negative number on integer overflow. Successful-login
and MFA-disable resets retain their existing per-key invalidation behavior.
The existing threshold constants, recovery comparison, feature flag, 15-minute
cache configuration and endpoint/credential policy are unchanged.

## Compatibility and deployment

The cache interface extension has a default unsupported operation for custom
implementations, preserving compatibility for cache users that do not call it.
The built-in local, Redis and disabled caches implement it. Custom adapters used
for security counters must implement atomic update; there is deliberately no
non-atomic fallback. The disabled cache still evaluates against its default
without retaining state, as expected for explicitly disabled caching/test setups.
It is not evidence of lockout enforcement in that configuration.

Redis credentials must permit scripting (`EVAL`/`EVALSHA`) as well as the existing
key reads and writes. A denied script or Redis outage propagates a failure; no
process-local lock or non-atomic write is substituted. Such infrastructure or
sustained-contention failures can surface an error response rather than the
usual credentials failure. The selected operational failure/alert policy still
needs deployment acceptance. No existing cache values are cleared or migrated,
and no production dependency version or Maven declaration changes are required.

## Regression boundaries

- `UserFailureCounterConcurrencyTest` forces the old reads to coincide using a
  barrier around real local-cache reads. The preceding service has three failures
  in five cases; serial threshold/reset and disabled-setting cases pass.
- `AtomicCacheUpdateTest` checks 800 concurrent local increments, mapper failure
  preservation, absent defaults and the intentionally disabled cache behavior.
- `RedisAtomicCacheUpdateTest` uses a disposable, digest-pinned Redis 7.4 container
  and the application's existing Lettuce/JDK serialization dependencies. Separate
  cache instances exercise a deterministic conflict, 200 concurrent increments,
  a reset racing with an update, unchanged serialized values, write-expiry reset,
  eternal entries and mapper failure. The fixture is in the web-test module so it
  uses the existing runtime client dependencies.
- Existing Redis-cache, authentication, session and MFA regression suites are
  retained. Final execution passes **132 checks**: 70 unit/cache checks, 56 H2 API
  cases and six real Redis cases, with no final failures/errors/skips. The existing
  Redis-cache suite's 35 cases are the sum of eleven nested reports; its zero-case
  enclosing report is not counted twice. Commands, image identity and source hashes are in the
  [dated execution record](../evidence/afta-atomic-failure-counters-2026-11-15.json).

The first Redis fixture attempt in the lower-level system module could not load
the runtime Lettuce client and did not execute its test cases. The fixture was
moved to the application test module; no new client dependency was added. An
earlier compile attempt also corrected the typed script-result serializer.
The H2 profile also did not select the untagged Redis fixture. A dedicated
unit-profile invocation and CI step now execute and require its six-case report.
These setup/selection follow-ups are separate from the preceding-service race comparison.

## Remaining acceptance

A single real Redis server with multiple cache instances establishes the tested
atomic key-update behavior, not a full multi-process application/Redis-cluster
or failover deployment. Cache expiry, eviction, resets and explicitly disabled
caching still affect retained state under their existing semantics. Full selected
provider counting/reset rules, exact AFTA thresholds, recovery, operational alarms
and deployed acceptance remain open. The rows and checklist percentages do not
move to fully closed.
