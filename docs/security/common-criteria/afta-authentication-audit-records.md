# AFTA remediation: reliable authentication audit fields

Date: **13 November 2026**. Selected checklist rows 1–3:
`FAU_GEN.1.1`, `FAU_GEN.1.2`, `FAU_GEN.2.1`.
**Partial event-generation evidence, not complete audit or AFTA approval.**

## For management

Authentication logs should retain who attempted or completed an operation and
available connection context. The listener previously omitted the client address
on failures, could throw while reading missing/unexpected details, and discarded
non-ASCII identity characters. A separate switch-user message did not apply the
same field protection as other messages.

The listener now extracts identity, address and outcome independently. Optional
HTTP details are checked by type. Unicode identities are retained; line/control
characters and field separators are escaped so supplied values cannot introduce
new log lines or semicolon-delimited fields. Ordinary event labels and INFO
logging remain unchanged.

These fixes improve already partial audit requirements. The checklist percentages
remain **43/78 partial (55.1%), 10 gaps (12.8%) and 25 needing definitions (32.1%)**.
No complete requirement is closed. Durable storage and the selected event policy
still require separate work.

## Code trace

[AuthenticationLoggerListener](../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/AuthenticationLoggerListener.java):

| Symbol | Contribution |
| --- | --- |
| `onApplicationEvent` | Honors the actual INFO logging level, retains existing duplicate/fixation exclusions and escapes switch-user actor/target values. |
| `logAuthenticationEvent` | Resolves known local OIDC/PAT identities independently of details; retains an attempted OIDC name if no local identity is available. Captures forwarded or ordinary HTTP address on success/failure without unchecked casts. |
| `logMessage`, `formatIpAddress`, `escapeLogField` | Preserve Unicode identity text, escape CR/LF/control/format characters and semicolon/backslash delimiters, and apply the same value handling to identity, address, exception and switch target. |

Known local usernames are used when available; failed password events can carry
an attempted name, not a verified account identity. Missing identity/address data
is not invented. Token keys, credentials and whole principal/claims objects are
not read or serialized to form these fields. Existing provider exception messages
remain an input; this is not a universal credential-redaction guarantee.

Escaped values use `\u` hex sequences, or `\U` for supplementary control/format
code points. Literal backslashes are escaped to distinguish supplied text from
these sequences. The semicolon field separator remains reserved. Unicode text
can now remain visible where the old ASCII-only filter discarded it. Consumers
should parse record boundaries before decoding field escapes; event/field labels
remain stable.

## Regression verification

**167 combined checks pass, with no failures, errors or skips:** 15 audit-message,
21 provider-policy, 23 token manager/resolver, seven audit-review and 101 H2 authentication/API cases.
A preceding 14-case listener subset reproduced 13 failures on the original code;
an extra compatibility case preserves attempted OIDC identities in the final
15-case suite. Counts overlap earlier inventories and are not added to them.
WAR packaging, packaged listener/token-manager identity and PDF checks pass using
existing browser assets. The newer 12 November token-owner and traceability
commits and the later audit-review paging fix are preserved. The latter
seven-case suite was rerun after integration; duplicate executions are not
counted twice.

[AuthenticationAuditRecordTest](../../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/security/AuthenticationAuditRecordTest.java)
captures actual emitted log messages. Cases cover failure addresses/reasons,
ordinary HTTP details, OIDC identities with missing/unexpected/forwarded details,
completed OAuth login, PAT details/token non-disclosure, attempted Unicode names,
line/field injection, switch-user targets, backslash/format-character handling,
unknown local OIDC identity, duplicate exclusion and disabled INFO logging.

The capture fixture restores the prior named logger configuration. It does not
change application logging defaults. The dated
[evidence record](../evidence/afta-authentication-audit-records-2026-11-13.json)
records the preceding-listener comparison, final counts, source hashes and build
checks. Existing password/MFA/API regressions are retained.

## Operation and remaining limits

No migration, dependency, endpoint, credential or browser asset change is needed.
An evaluated deployment must still enable INFO for this logger and configure the
intended protected destination, timestamp layout and retention. The listener does
not make logs durable, sign them, provide complete event coverage or implement a
storage-full policy. Timestamp/clock guarantees remain outside this message fix.

A forwarded address is available context, not automatically an authenticated
network identity: trusted-proxy ingress and direct-access restrictions still
apply. All identity-provider paths, event selections, error-message redaction,
collector/parser behavior and deployed acceptance remain open. Switch-user's
existing two-message behavior is retained; no exactly-once delivery is claimed.

Implementation commit: `8dd8a28bf27272e56db522a640b55ee74113d425`.
