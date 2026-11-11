# AFTA hardening: require an explicit encryption key in the platform

Date: **11 November 2026**. Related review finding: G-13.
**Startup configuration protection, not cryptographic approval.**

## For management

Core historically uses a built-in encryption password if none is configured.
The platform already generates and retains a separate encryption secret. It now
also enables a startup check that rejects a missing/blank password or the known
built-in fallback. A broken key configuration stops startup instead of silently
using that fallback.

The change validates the existing key. It does not rotate it, rewrite ciphertext,
change algorithms or establish that all health data is encrypted. The platform
uses the same retained secret in development and production. Never replace that
secret simply to make a startup error disappear: existing encrypted settings may
become unreadable.

## Code and requirement trace

| Artifact | What it establishes |
| --- | --- |
| [HibernateEncryptionConfig.init](../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java) | When strict mode is enabled, rejects a missing, empty, whitespace-only or built-in default password before encryption beans can be used. Valid configured keys are not trimmed or changed; error text does not include the rejected value. |
| [ConfigurationKey](../../../dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ConfigurationKey.java) | Adds `encryption.require_explicit_password`; the existing encryption password remains confidential. Core's compatibility default is `off`. |
| [HibernateEncryptionPolicyTest](../../../dhis-2/dhis-support/dhis-support-hibernate/src/test/java/org/hisp/dhis/config/HibernateEncryptionPolicyTest.java) | Checks invalid-key rejection, Spring startup failure, exact key preservation, explicit-key ciphertext compatibility for existing AES/legacy encryptors, and legacy fallback read compatibility when strict mode is off. |
| [Platform entrypoint](https://github.com/PardasHealth/dhis2-platform/blob/main/container/entrypoint.sh) | Writes `encryption.require_explicit_password = on` beside the existing retained secret; neither deployment mode disables it. |

The supplied AFTA list references encryption through `FCS_COP.1.1(1)` and `(2)`
(rows 13/14) and `(3)` and `(4)` (rows 65/66). Their exact operations, algorithms
and parameters remain unspecified. This startup guard is a related implementation
candidate; it is **not assigned to an unknown iteration or treated as approval**.
Key destruction and residual-information requirements are not implemented by
checking a startup password. All checklist dispositions and percentages remain
unchanged.

## Configuration and upgrade

For the updated platform, use the normal backed-up update path. It retains the
existing encryption secret and writes strict mode on every startup. Upgrade the
WAR along with the platform: an older core that does not recognize the new key
cannot enforce it. The check requires no database migration.

For a separately managed core deployment, strict mode is selected with:

```properties
encryption.require_explicit_password=on
encryption.password=REPLACE_WITH_THE_EXISTING_PROTECTED_NONDEFAULT_KEY
```

This is a template, not a replacement-key instruction. Keep the existing key and
its protected backups. A non-default key check is not an entropy, algorithm,
provider or key-management certification. Sites must still provision a strong
secret and control access and recovery.

If existing ciphertext uses the built-in fallback, keep the legacy core profile
until its encrypted-field inventory and tested migration/recovery plan exist.
Core preserves the original fallback when strict mode is off. Simply enabling
strict mode or substituting a random key does not migrate old ciphertext. The
updated platform does not provide a bypass for its strict setting.

## Verification and limits

**112 core checks pass, with no failures, errors or skips:** 11 encryption-policy,
21 provider-policy and 80 H2 guard/API cases. Platform verification has **24
passing setup/update/mode tests** and four isolated entrypoint render checks.
The WAR build and PDF check pass; changed packaged classes match the compiled
classes. Existing browser assets are reused. Counts overlap earlier inventories
and are not added to them.

Exact test counts, source hashes, retained-key configuration checks and build
results are in the [dated evidence record](../evidence/afta-explicit-encryption-key-2026-11-11.json).
Cipher compatibility tests use synthetic settings and the unchanged algorithms;
they do not establish a successful migration of a site's data.

Four isolated runs of the actual platform entrypoint cover repeated production
and development starts, requiring strict mode, the same synthetic retained key,
private configuration-file permissions, no secret output and the expected CSRF
setting. A Java stand-in inspects generated configuration; these are not real
DHIS2 starts. Existing setup/update/mode tests also retain their secret-preservation
checks. No user's secret, database, application container or volume is changed.

Remaining work includes the field/ciphertext inventory, approved algorithm and
provider selections, retirement of legacy cryptography, tested key rotation and
re-encryption, backup recovery, deployed startup acceptance and exact AFTA clauses.
Strict startup validation closes none of these broader obligations.

Core implementation commit: `27a7c8ddf6099e5c724194a30a416d9d91344e87`.
