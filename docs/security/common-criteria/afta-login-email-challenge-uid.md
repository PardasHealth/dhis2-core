# Login email challenge uses authenticated UID

Date: **16 November 2026**. Branch: `work`. Supporting selected rows 36–39:
`FIA_UAU.5.2`, `FIA_USB.1.1/.2/.3`. All remain partial.

The email login provider passed the password-authenticated principal's username
to the challenge service. A name reassigned after principal creation could select
another account for challenge delivery and secret replacement. The preceding
[disable challenge fix](afta-email-challenge-identity.md) removed a second lookup
within disable, but left this initial login lookup contract unchanged.

`TwoFactorAuthenticationProvider.sendEmail2FACode(UserDetails)` now passes the
principal UID to `TwoFactorAuthService.sendEmail2FACodeByUid(String)`. The new
transactional entry point rejects null/blank UIDs, loads the account by UID and
uses the existing entity helper. Missing accounts fail without username fallback.
Existing username callers retain their public compatibility method. Factor-state
and verified-email checks, challenge lifetime, delivery format and login proof
consumption are unchanged.

Trace:

- [Login provider](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java): `sendEmail2FACode(UserDetails)`.
- [Challenge service](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java): `sendEmail2FACodeByUid(String)` and private entity helper.
- [Provider policy tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationPolicyTest.java): missing-code login must call the UID method and never the name method. The synthetic principal now has an explicit UID.
- [Challenge identity tests](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorEmailChallengeIdentityTest.java): UID issuance retains the renamed owner and recipient despite a supplied namesake; deleted owners and null/blank UIDs cannot issue mail. The preceding disable challenge cases remain.
- [Dated execution evidence](../evidence/afta-login-email-challenge-uid-2026-11-16.json): exact implementation, source/report hashes, regression scope and commands. Both suites are already selected and required by the [fork workflow](../../../.github/workflows/cc-audit-regression.yml).

The baseline deliberately retains the preceding provider while making the additive
UID service method available to compile the new contract assertions. It establishes
that the old provider calls the name method; it is not a full pre-patch deployment
or concurrent HTTP exploit. Service tests model name reuse deterministically using
mocked lookup/delivery boundaries with real challenge generation and formatting.
Final verification passes 33 unit and 33 H2 API cases, including login challenge
replacement and subsequent proof acceptance. Eight traceability validator cases
also pass. These executions overlap prior evidence and are not additive.

UID routing does not lock password/account/factor state during issuance. Same-UID
state races, delivery/persistence failure coordination, other name-based callers,
full external-provider policy and deployment acceptance remain open. Login proof
consumption has its own primary-state checks; this change does not broaden those
guarantees or close a complete AFTA requirement.
