# Rejected MFA proof failure events and account lockout

Date: **16 November 2026**. Branch: `work`. Supporting selected rows 1–3,
29–30 and 36 (`FAU_GEN.1.1/.2`, `FAU_GEN.2.1`, `FIA_AFL.1.1/.2`,
`FIA_UAU.5.2`). No complete row is closed.

`TwoFactorAuthenticationException` extends Spring's `BadCredentialsException`,
but `DefaultAuthenticationEventPublisher` matches exception classes exactly.
The configured publisher did not map this custom class, so a rejected MFA proof
could fail authentication without publishing a failure event to the audit and
account-counter listeners. The existing threshold mechanism therefore did not
count these failures even when account lockout was enabled.

`AuthenticationProviderConfig.authenticationEventPublisher` now maps this exact
exception to `AuthenticationFailureBadCredentialsEvent`. Existing listeners can
record the failed attempt and increment the existing atomic login counter. The
threshold, expiry and opt-in lockout setting remain unchanged. The separate
`TwoFactorCodeSentException` remains unmapped: issuing an email challenge after
password validation is not itself a rejected MFA proof.

Trace:

- [Publisher configuration](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationProviderConfig.java): explicit custom exception mapping.
- [Failure listener](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/AuthenticationListener.java): `handleAuthenticationFailure` dispatch to existing login counter.
- [Publisher regression](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/TwoFactorFailureEventTest.java): real configured publisher, both enabled factor types, exact event/authentication/exception identity and actual listener dispatch to a mocked counter service. A separate case requires no credential-failure event for successful challenge issuance.
- [HTTP policy regressions](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorLoginPolicyTest.java): repeated rejected proofs reach the configured account threshold for both types and the next login returns `ACCOUNT_LOCKED`; repeated email challenge issuance does not lock the account. Fixtures restore the setting and reset counters.
- [Execution record](../evidence/afta-mfa-failure-events-2026-11-16.json): source/report hashes, baseline outcomes and reproduction commands.
- [Fork CI](../../../.github/workflows/cc-audit-regression.yml): requires the new publisher suite and the existing H2 policy suite to produce successful nonempty reports.

These are controlled configured-publisher and H2 HTTP results. They do not prove
production audit delivery/retention, full audit content for every mechanism,
complete rate-limiting or deployed multi-node acceptance. The exception also
represents configured-factor rejection and issuance conflicts; those existing
failure paths now reach the same listeners. Policy-specific distinctions and
the full AFTA failure-handling rules remain open.

Final verification: **122 Java cases passed** (76 unit and 46 H2 API), with
zero failures, errors or skips. The dated record preserves the four baseline
failures separately. Counts overlap earlier records and are not additive.
