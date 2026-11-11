# AFTA remediation: keep a pending TOTP QR seed stable

Date: **11 November 2026**. Related selected checklist row 36, `FIA_UAU.5.2`.
**Enrollment-flow hardening, not full AFTA or CSRF approval.**

## For management

The bundled user-profile app loads its TOTP QR image from a deprecated GET
endpoint. That endpoint previously began enrollment again on every request.
Refreshing the image could therefore replace a pending seed that the user had
already scanned, leaving the image and pending enrollment out of step.

The endpoint now reuses a nonempty pending TOTP seed. Repeated GET/HEAD reads do
not replace it. The first legacy image request still starts enrollment, and an
explicit enrollment POST still rotates an unconfirmed seed. An already enabled
factor is not changed. The bundled app version and assets are retained.

This corrects one enrollment behavior within an already partial requirement.
Checklist percentages are unchanged: **43/78 partial (55.1%), 10 gaps (12.8%) and
25 needing definitions (32.1%); none fully closed**.

## Code trace and client compatibility

[TwoFactorController.generateQRCode](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/TwoFactorController.java),
the deprecated `/api/2fa/qrCode` GET handler, now calls enrollment only when the
current user does not already have a nonempty `ENROLLING_TOTP` seed. It otherwise
uses the existing `generateQRCode` service to render the pending seed. That
service continues to enforce the global TOTP setting and enrollment state.
An empty/missing seed retains the previous legacy repair behavior.

The supported `POST /api/2fa/enrollTOTP2FA` still starts or explicitly restarts
pending enrollment; `qrCodePng` and `qrCodeJson` remain read-only paths. Successful
`POST /api/2fa/enable` still requires a valid proof. No authentication bypass,
new provider, migration, dependency or browser-asset change is introduced.

The existing packaged `user-profile-app.zip` was inspected: its application
JavaScript uses a versioned `/api/${version}/2fa/qrCode` image URL. The fix retains
that route and initial GET behavior rather than requiring an unreviewed frontend
upgrade. The API tests include a versioned repeated request. Static client usage
is evidence of compatibility intent, not a real browser acceptance test.

## Regression tests

**109 targeted authentication checks pass, with no failures, errors or skips:**
21 provider-policy cases and 88 H2 guard/API cases, including eight new QR cases.
The QR suite produced **two failures out of eight** on the preceding controller:
repeated GET and HEAD replaced the seed. The corrected controller passes all
eight. WAR packaging, packaged controller identity and PDF checks pass; the
inspected profile ZIP is byte-identical inside the WAR's embedded server JAR.
Counts overlap earlier inventories and are not added to them.

[TwoFactorQrStabilityTest](../../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/TwoFactorQrStabilityTest.java)
exercises the real controller/service path with H2 and MockMvc:

- Initial legacy GET creates a pending seed and a decodable QR containing it.
- A repeated versioned GET preserves the scanned seed; that proof can complete enrollment.
- HEAD does not replace an existing pending seed.
- Explicit enrollment POST still rotates an unconfirmed seed.
- Reading an enabled factor rejects without changing the factor.
- Globally disabled TOTP rejects pending reads without changing the seed.
- Supported PNG/JSON reads retain the POST-created seed.
- A missing pending seed retains the existing legacy repair path.

Counts, baseline comparison, code/asset hashes and packaging checks are in the
[dated evidence record](../evidence/afta-pending-qr-stability-2026-11-11.json).
The existing authentication, MFA policy and replay regressions are also run.

## Remaining boundaries

The legacy GET still changes state on initial enrollment or missing-seed repair.
This patch does **not** make it a safe/read-only HTTP method or close the full
CSRF issue. Removing that behavior requires a coordinated user-profile client
migration to the supported POST/read-only routes. Production CSRF remains enabled
in the platform; its separately documented browser-login blocker is unchanged.

The check is not a new distributed enrollment lock: concurrent initial requests,
explicit rotations, enable/disable races and stale request snapshots require
separate ordering and acceptance evidence. Cross-purpose proof consumption,
recovery, trusted time and deployed browser/multi-node acceptance also remain
open. Existing authenticated sessions are not revoked by this change.

Implementation commit: `bb1acc0c8d7ab63502aa641d01feb7c8fa8012a6`.
