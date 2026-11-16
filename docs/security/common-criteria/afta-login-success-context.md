# Login success connection context without submitted MFA proof

Date: **16 November 2026**. Branch: `work`. Supporting selected row 2,
`FAU_GEN.1.2`; the complete audit-content requirement remains partial.

After password and factor checks, `TwoFactorAuthenticationProvider.authenticate`
created a replacement token to add Spring's password factor authority. It did
not copy the parent's authentication details. Success events and the returned
session authentication consequently lost available request connection context.

The replacement now retains the parent's details. For
`TwoFactorWebAuthenticationDetails`, it creates a new details object with the
captured remote address, session ID and resolved IP, and a null MFA code. This
preserves context for success listeners without copying the submitted OTP into
the returned authentication. Other details keep their existing Spring object
contract. The incoming request details are not modified.

Trace:

- [Provider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java): `authenticate`, construction of `completed` and sanitized factor details.
- [Provider regression](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java): real `ProviderManager`, provider and success-event publisher with mocked account/replay dependencies. Three cases cover password-only, TOTP and email. The result and emitted event must retain address/session fields with a null code, while the input retains its supplied value.
- [Execution evidence](../evidence/afta-login-success-context-2026-11-16.json): implementation commit, baseline, commands and source/report hashes.
- [Required fork CI](../../../.github/workflows/cc-audit-regression.yml): the extended provider policy suite is already selected and required to produce a passing nonempty report.

Against the preceding provider, all three new cases fail because the result's
details are null; the existing 21 cases pass. The final execution also includes
the existing audit, listener, publisher and H2 login/MFA suites. This is not a
live browser or production audit-delivery acceptance test.

The captured session ID describes request details at authentication time, before
any subsequent session-fixation strategy changes the session. This fix does not
select the complete AFTA event field policy, refresh session permissions, alter
proxy trust or change MFA proof validation. Other authentication providers and
credential-bearing details types require their own review. No complete row or
certification claim follows from these regression cases.

Final verification: **112 Java cases passed** (66 unit and 46 H2 API), with
zero failures, errors or skips. The three baseline failures remain separately
recorded. These executions overlap previous inventories and are not additive.
