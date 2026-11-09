# Encryption and two-factor authentication: source findings and closure plan

Date: 2026-11-04. Baseline: `07d4f3f7ea` plus the recorded two-class patch.
These are source-review findings, with conditional impact and explicit remaining
verification. No production configuration or user ciphertext was supplied. No
exploit, deployment compromise or completed cryptographic evaluation is claimed.

## G-13: encryption configuration and migration

Candidate components: FCS_CKM.5, FCS_COP.1, FDP_SDC.1/2; also the key lifecycle
and management requirements selected in the ST.

Source: [HibernateEncryptionConfig](../../../dhis-2/dhis-support/dhis-support-hibernate/src/main/java/org/hisp/dhis/config/HibernateEncryptionConfig.java).

| Symbol | Observed behavior | Gap |
| --- | --- | --- |
| `init`, `getConnectionProperty` | The encryption password has a built-in fallback when configuration is missing or empty. | An evaluated deployment cannot infer unique, secret key provisioning from enabling encryption. Actual deployed key settings and affected data are unknown. |
| `tripleDesStringEncryptor` | A bean configures `PBEWithSHA1AndDESede` with fixed salt; its comment describes legacy SystemSettings use. | Active use and migration state need inventory. Bean presence alone does not establish that every stored value uses this cipher. |
| `aes128StringEncryptor`, `hibernateEncryptors` | AES PBE with random salt is configured; the Hibernate registry maps the AES bean. | This does not establish coverage of all stored health data, approved derivation parameters, authenticated encryption or key lifecycle. |

Closure requires an encrypted-field/ciphertext inventory, actual bean use,
selected provider/algorithm/parameters and explicit secret provisioning. Design a
versioned migration with read compatibility, backup validation, re-encryption,
rotation and rollback tests before removing legacy reads or changing keys. A
configuration-only key replacement can make existing ciphertext unreadable.
Do not copy existing secrets into this report, test fixtures or logs.

Verify fresh installation without a key, empty-key configuration, upgrade with
old ciphertext, wrong-key failure, partial migration/restart and recovery. Select
the intended failure behavior in the evaluated profile; no such migration tests
were executed in this documentation follow-up.

## G-14: single-use and replay evidence

Candidate components: FIA_UAU.4 and FPT_RPL.1, if selected. FIA_UAU.5 and trusted
time requirements are also relevant to the protocol definition.

Sources:

- [TwoFactorAuthUtils](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java): `verifyEmail2FACode` checks expiry and equality; `verifyTOTP2FACode` validates format/secret and calls `Totp.verify`.
- [TwoFactorAuthenticationProvider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java): `validate2FACode` returns after successful validation without recording consumption in that path.
- [TwoFactorAuthService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java): `generateEmail2FACode` creates a six-digit code with a 15-minute lifetime; `sendEmail2FACode` stores its encoded value on the user.

No used-code/accepted-time-step state was identified in those reviewed login
paths. This is a gap in establishing single use, not a dynamic demonstration of
two complete successful logins. Time-limited validity and single-use acceptance
are separate properties. Other protocols and library internals require their
own review; reusable passwords/PATs are not automatically failures of a
single-use requirement whose selected mechanism might be different.

If single use is required, design atomic acceptance state shared by all nodes.
Email-code verification should consume the specific issued challenge once;
TOTP verification should reject reuse of an already accepted time step under
the selected window/skew policy. Avoid a process-local cache as the only guard.
Keep enrollment, ordinary login and administrative recovery semantics explicit.

Required verification: repeated use before expiry, concurrent same-code requests,
cross-node races, expiry/skew boundaries, replacement challenges, database/cache
failure and restart. No replay or race test was executed in this follow-up.

## G-15: disabled-factor-type policy

**2026-11-09 implementation update:** the provider downgrade described below has
been corrected. Disabled, unsupported or missing enrolled factor types now reject
login. Enabled types still validate their codes. See the
[remediation/test record](afta-remediation-progress.md). The following paragraphs
describe the original 2026-11-04 finding; full provider/recovery acceptance remains open.

Candidate component: FIA_UAU.5, together with the selected account/role policy.

Source: [TwoFactorAuthenticationProvider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java),
`checkTwoFactorAuthentication`, `isTwoFactorTypeEnabled`, `authenticate`.

The provider first checks the user's enrollment and requires form-based details
for a user with 2FA enabled. It calls `validate2FACode` only when the enrolled
factor type is enabled globally. If that type is disabled, this branch does not
check the code and authentication continues after the password check. Role-based
enrollment checking does not itself validate the factor.

The practical impact depends on factor configuration, enrollment, enabled login
paths and the intended administrative meaning of disabling a factor type. No
universal authentication bypass or deployed exploit is claimed.

Specify one policy: deny such logins until an authorized recovery/migration, or
an explicitly designed fallback with equivalent required assurance. A secure
profile should not silently treat enrollment as successful second-factor proof.
Document emergency recovery so changing the flag does not strand administrators.

Then test TOTP/email enrollment states against each global flag, required-role
constraints, missing/wrong/valid codes, alternate providers, PAT denial rules,
browser messaging and recovery. Preserve generic failure handling and complete
the normal login regressions. The 2026-11-09 follow-up records the provider/API
change and its tests; it does not close replay, browser or multi-node acceptance.
