# TOTP provisioning URI component encoding

Date: **16 November 2026**. Branch: `work`. Supporting code evidence for selected
rows 35–36 (`FIA_UAU.5.1/.2`); both remain partial. This fixes provisioning data
boundaries, without selecting the missing AFTA mechanism-combination policy.

`TwoFactorAuthUtils.generateTOTP2FAURL` previously interpolated the username and
normalized application title into an `otpauth` URI. Only spaces in the title were
escaped. Reserved characters could produce invalid URIs or change path, query
and fragment interpretation. This is a controlled malformed-input reproduction,
not evidence that an attacker can edit another account's username or system title.

The method now UTF-8 percent-encodes each issuer, account and secret component.
The literal label separator and query delimiters remain structural. Spaces use
`%20` and literal plus signs use `%2B`, so URI and query parsers preserve the
original values. Existing application-title trimming, ASCII normalization and
`DHIS2_` prefix remain. Ordinary email usernames may have an escaped `@` in the
raw URI; the decoded account identity is unchanged.

Code and regression trace:

- [Provisioning generator](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/twofa/TwoFactorAuthUtils.java): `generateTOTP2FAURL`, `encodeProvisioningComponent`.
- [URI regression cases](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/twofa/TwoFactorProvisioningUriTest.java): ordinary account, account delimiters, issuer delimiters and Unicode account. Assertions parse the URI and query, check the decoded values, require exactly two query parameters and reject a fragment.
- [Required CI selection and report gate](../../../.github/workflows/cc-audit-regression.yml): the new four-case suite is selected and must have a successful nonempty report.
- [Dated execution evidence](../evidence/afta-totp-provisioning-uri-2026-11-16.json): implementation commit, source/report hashes, commands and baseline outcome.

Against the preceding production utility, four cases ran: the ordinary case
passed and three malformed/Unicode cases errored while parsing the URI. With the
fix, all four pass. Existing factor policy, identity mutation and H2 provisioning
regressions are recorded separately in the execution evidence. No live phone
scanner, full browser flow, production deployment or hosted CI result is claimed.
Cross-purpose replay, enrollment races, title-policy choices and full selected
mechanism acceptance remain open.
