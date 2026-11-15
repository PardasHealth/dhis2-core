# AFTA remediation: authentication audit address source

Date: **15 November 2026**. Selected checklist row 2, `FAU_GEN.1.2`.
Partial audit-field evidence; no full AFTA requirement closure.

## Change and compatibility

The HTTP-request constructor of `ForwardedIpAwareWebAuthenticationDetails`
previously preferred raw `X-Forwarded-For` to the servlet remote address. A client
could supply a forged address, an arbitrary comma-separated chain, or an empty
header that erased an available address. This affected the password/MFA and HTTP
Basic detail subclasses and the authentication audit listener.

The constructor now retains the address already captured by
`WebAuthenticationDetails` from the servlet request. It does not parse raw
`X-Forwarded-For` or `Forwarded` headers. Existing explicit-value constructors,
setters, serialized session shape, MFA-code handling and event formatting remain
unchanged. The historical class name is retained for compatibility.

Behind a proxy, configure trusted server/container client-address resolution and
prevent untrusted direct ingress before relying on an original-client address.
Without that configuration, the connection address can correctly identify the
proxy rather than the original browser. This patch does not authenticate a
container's forwarding configuration, establish proxy trust, or enforce IP-based
lockout. The existing provider's temporary lockout decision remains username-based.

## Verification

Five new cases in `AuthenticationAuditRecordTest` exercise actual emitted log
messages from real HTTP request detail construction: password failure with a
forged address, Basic success with a forwarded chain, an empty forwarded header,
a servlet-resolved address conflicting with both header forms, and a request
without forwarding headers. They also check that an MFA proof is not logged.
The existing explicit-value/Unicode/field-escaping cases are preserved.

Against the preceding constructor, the expanded 20-case audit suite has four
assertion failures and no errors/skips. The missing-header case and existing
cases continue to pass. The patched run passes **88 checks across six suites**, with no
failures/errors/skips. Execution and source identities are recorded
in the [dated evidence inventory](../evidence/afta-authentication-address-source-2026-11-15.json).
Token failure events introduced earlier on 15 November retain their ordinary
servlet details; this change does not replace or undo that event mapping fix.

The module formatter was applied, then unrelated pre-existing license-header-only
edits were restored. The changed audit test retains its original license header;
no formatting-only changes to replay or provider-policy code are included.

## Remaining acceptance

The selected event inventory, field coverage across all providers, trusted time,
protected log transport/storage, retention and production proxy/collector
acceptance remain open. Explicitly supplied or deserialized detail values still
need a trusted source. The checklist totals and row disposition are unchanged.
