# Email MFA disable challenge target continuity

Date: **16 November 2026**. Branch: `work`. Supporting evidence for selected
rows 37–39 (`FIA_USB.1.1/.2/.3`), which remain partial.

`TwoFactorAuthService.disable2FA(User, String)` already retained the resolved
account when checking a supplied proof and resetting MFA. Its empty-code email
branch still called `sendEmail2FACode(String)`, performing another username lookup.
A reassigned name could redirect challenge validation, delivery and persistence
to a different account after the original target was selected.

The empty-code branch now passes its selected `User` to a private challenge helper.
That helper checks factor state and verified email, creates the challenge, sends
to the selected account and persists that same object. The public username method
remains as a compatibility wrapper that resolves once and delegates. No endpoint,
challenge lifetime, delivery format, lock threshold or authorization rule changes.

Trace:

- [TwoFactorAuthService](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthService.java): `disable2FA(User, String)`, `sendEmail2FACode(String)` and private `sendEmail2FACode(User)`.
- [TwoFactorEmailChallengeIdentityTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorEmailChallengeIdentityTest.java): sequential name lookups return different accounts, deterministically modeling name reassignment. Real challenge generation and email formatting run with mocked lookup, settings and delivery boundaries. Assertions require the original recipient, changed original secret, unchanged namesake secret, a single lookup and original-account verified-email enforcement.
- [CI workflow](../../../.github/workflows/cc-audit-regression.yml): selects the new suite and requires a successful nonempty report.
- [Execution record](../evidence/afta-email-challenge-identity-2026-11-16.json): implementation commit, source/report hashes, baseline and reproduction commands.

Both regression cases fail against the preceding production service. One leaves
the selected account's challenge unchanged while updating the namesake; the other
sends a challenge despite the selected account's unverified email. Both pass with
the helper change. Existing MFA unit and H2 API cases are included in the dated
execution record. This deterministic service fixture is not a live concurrent
exploit, a real email delivery or deployed browser acceptance.

This follows the [current-user UID fix](afta-current-user-uid-binding.md).
It closes the repeated lookup in the disable-challenge branch only. Initial
username lookup contracts used by other callers, same-UID factor changes during
challenge issuance, email/persistence transaction failure coordination,
cross-purpose replay and full AFTA identity/provider rules remain open.
