# AFTA remediation: enforce token IP constraints without raw-header trust

Date: **13 November 2026**. Selected rows 16, 17 and 19:
`FDP_ACF.1.1`, `FDP_ACF.1.2` and `FDP_ACF.1.4`. Partial evidence only.

## Defect and behavior

`Dhis2ApiTokenFilter` previously preferred the raw `X-Forwarded-For` header to
the servlet remote address when checking a token's allowed IP list. A caller
with a valid token who could reach the backend could supply an allowed address
in that header and satisfy the constraint from a disallowed address. A header
could also wrongly reject an allowed servlet address. This is a reproduced
filter defect, not evidence of exploitation in a deployed network.

The filter now uses `HttpServletRequest.getRemoteAddr()` exclusively for this
rule. The token's stored allowed IPs, owner validation and other constraints
retain their existing semantics. Raw forwarding headers neither satisfy nor
override the IP decision. Tokens without IP constraints retain their behavior.

## Proxy compatibility and deployment requirement

IP-constrained tokens behind a reverse proxy may now be denied until the server
resolves the client address into the servlet remote address. Configure that
rewriting only through a container/proxy integration that validates trusted proxy
peers and sanitizes forwarding headers. The application does not infer trust from
the mere presence of a header. Adding a shared proxy IP to an allowlist permits
all clients arriving through that proxy; it does not preserve a client-IP rule.

This change does not configure Tomcat, the gateway, network access or other
authentication helpers. A server layer that trusts arbitrary forwarding headers
can still supply an untrusted servlet address. Deployment verification must
include direct backend access, trusted/untrusted peers, header replacement and
multiple proxy hops before asserting end-to-end provenance.

## Requirement-to-code and test trace

| Selected requirement | Bounded evidence |
| --- | --- |
| Row 16, `FDP_ACF.1.1` | The existing token IP policy uses the servlet-reported address as its request attribute rather than an unchecked raw header. |
| Row 17, `FDP_ACF.1.2` | Allowed/disallowed address decisions retain the token rule, including interaction with a method constraint. |
| Row 19, `FDP_ACF.1.4` | A disallowed address cannot satisfy an IP restriction by supplying an allowed address in the raw header; the chain is not invoked and authentication is absent. |

- [Dhis2ApiTokenFilter.validateIp](../../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/Dhis2ApiTokenFilter.java) contains the address-source fix.
- [ApiTokenIpConstraintTest](../../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenIpConstraintTest.java) exercises the real filter and resolver with servlet request/response fixtures and mocked account authentication, failure entry point, event publisher and chain.
- [Fork CI](../../../.github/workflows/cc-audit-regression.yml) runs the new class and requires its executed successful XML report.

The seven new cases cover spoofed headers, ordinary allowed/denied addresses,
a raw header attempting to override an allowed servlet address, an IPv6 address,
a token without IP constraints and a matching IP that still fails a method rule.

## Executed evidence

On the baseline filter at `22c2c5ba0a`, seven cases ran with **two failures** and
no errors or skips: header spoofing was accepted, and a raw header overrode an
allowed servlet address. With the fix, **37 targeted cases pass**, with no
failures, errors or skips: seven IP-constraint, 16 token-manager, seven resolver
and seven audit-review cases. Shared earlier suites are not counted as new
independent evidence.

Source hashes, failed method names, suite counts, commands and environment limits
are in the [dated record](../evidence/afta-token-ip-constraints-2026-11-13.json).
Implementation commit: `e7035884301d1ade648bf5424f560c539df128e2`.
The run used Temurin 17.0.17+10, Maven 3.9.11 and the existing offline dependency
cache, retaining the documented unchanged `sms-compression` source-build caveat.

From `dhis-2` with approved dependencies:

```bash
mvn -B -ntp -pl dhis-web-api -am -Punit-test \
  -Dtest=ApiTokenIpConstraintTest,ApiTokenAuthManagerTest,ApiTokenResolverTest,AuditControllerPagingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Require all four named reports to show executed passing cases. These are servlet
unit checks, not a live HTTP/proxy, database or browser run. The broader selected
access-control requirements remain partial. Other login/audit/lockout IP helpers,
all selected attributes/data types and deployed/evaluator acceptance remain open.
