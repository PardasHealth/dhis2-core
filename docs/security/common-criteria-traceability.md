# Common Criteria 2026: standard-to-code traceability

Review date: 2026-11-03. Baseline: `07d4f3f7ea` (DHIS2 `2.44-SNAPSHOT`).

**Assessment status: partial engineering evidence; no certification or conformance claim.**
This document maps candidate requirements to code and tests. It does not select an EAL,
Protection Profile (PP), or completed Security Target (ST). See the
[review and remaining gaps](common-criteria-review.md) and
[execution evidence](common-criteria-verification.md) before interpreting coverage.

## Look up a standard component ID

Use the [component-ID → code index](common-criteria-component-code-index.md) or
its [CSV version](common-criteria-component-code-index.csv) to find an exact key.
For example, [FMT_MSA.3 — element-by-element code and test trace](common-criteria/component-traces/FMT_MSA.3.md)
links FMT_MSA.3.1 and FMT_MSA.3.2 to the implementing methods and their limits.
The index includes all 261 component IDs from the supplied Parts 2 and 3. The
[component gap assessment](common-criteria-gap-assessment.md) gives each a baseline
disposition, required action and proposed owner. This replaces unreviewed inventory
placeholders with gap triage; it does not complete all element/operation mappings.
This overview's original candidate table is not an exhaustive implementation-coverage claim.
The [web-application scope](common-criteria/web-application-scope.md) records why
physical protections, specialized features and PP/composition activities are
excluded from this baseline, while platform-dependent web protections remain relevant.

## References and interpretation

The references are the supplied fifth editions of ISO/IEC 15408-1:2026,
15408-2:2026 and 15408-3:2026. Standard text and licensed PDFs are not copied into
this repository. Component identifiers and short titles below refer to the 2026
edition. In particular, FAU_STG numbering differs from earlier editions:

| 2026 component | Subject | Clause in Part 2 |
| --- | --- | --- |
| FAU_STG.1 | Audit data storage location | 8.8.6 |
| FAU_STG.2 | Protected audit data storage | 8.8.7 |
| FAU_STG.3 | Guarantees of audit data availability | 8.8.8 |
| FAU_STG.4 | Action in case of possible audit data loss | 8.8.9 |
| FAU_STG.5 | Prevention of audit data loss | 8.8.10 |

A source link means the implementation is relevant, not that every requirement
or every selection/assignment is satisfied. `Partial` means relevant code or
tests exist but full evidence is absent. `Open` means a decision or evidence is
missing. `Conditional` means applicability depends on the ST. Test existence is
separate from execution; only the verification document records executed results.
Paths and symbol names provide stable code trace points as line numbers change.

## Part 1: evaluation model and boundary

| Reference | Project trace | Assessment / completion needed |
| --- | --- | --- |
| Clauses 5–6: overview and general model | [README](../../README.md), [draft boundary and security problem](common-criteria-review.md#draft-evaluation-boundary) | Partial: product and environment identified; exact deployed version, configuration and app inventory must be frozen. |
| Clause 7: specifying requirements | Candidate Part 2 table below; [gap register](common-criteria-review.md#gap-register) | Open: complete selections, assignments, refinements and dependency rationale in an ST. |
| Clause 8: security components | Functional and assurance tables below | Partial: component references checked against the supplied 2026 editions; these are engineering candidates. |
| Clause 9: packages | [assurance decision](common-criteria-review.md#assurance-and-acceptance) | Open: no EAL/package selected; EAL/package definitions also require the appropriate Part 5 reference. |
| Clauses 10–11 and Annexes B–C: PPs and modular construction | No PP, PP-Module or PP-Configuration identified in the reviewed security documents | Conditional: identify applicable certified profile before claiming PP conformance. |
| Clause 12 and Annex D: Security Targets | [draft boundary, threats, assumptions and objectives](common-criteria-review.md#draft-evaluation-boundary) | Partial preparation only: not a completed or evaluated ST. |
| Clause 13: evaluation and results | [verification record](common-criteria-verification.md), [SECURITY.md](../../SECURITY.md) | Open: independent evaluation under ISO/IEC 18045 and the selected scheme remains external work. |
| Clause 14: composition | PostgreSQL, Artemis, Redis and optional identity providers in the draft boundary | Conditional: dependency use alone does not establish evaluated composition. |

## Part 2: all functional classes considered

This inventory covers all eleven functional classes. It is not an applicability
ruling for every component in the standard; components outside the candidate table
remain unselected pending ST analysis, rather than being silently marked passed.

| Class (clause) | Project relevance | Disposition |
| --- | --- | --- |
| FAU (8), security audit | Audit generation, SQL storage, filters, JMS persistence and recovery | Partial; detailed traces below. |
| FCO (9), communication | No reviewed evidence of cryptographic proof of origin/receipt | Conditional; application logs do not establish non-repudiation. |
| FCS (10), cryptographic support | Password hashing, token hashing/randomness, TLS/keys | Partial; algorithm/provider/configuration evidence required. |
| FDP (11), user data protection | Metadata sharing, roles, organisation-unit and tracker access | Partial; whole-API authorization coverage and environment storage controls required. |
| FIA (12), identification/authentication | Passwords, lockouts, 2FA, OIDC, LDAP, API tokens | Partial; token regressions addressed in this review. |
| FMT (13), security management | Roles, authority checks, account management and session revocation | Partial; exact management policy and administrator roles required. |
| FPR (14), privacy | Identifiable health data and user-attributed audit records | Conditional; no anonymity, pseudonymity, unlinkability or unobservability claim. |
| FPT (15), TSF protection | Security filter chains, failure propagation and timestamps | Partial; trusted time, platform protection and failure policy need operational evidence. |
| FRU (16), resource utilization | Connection pools, audit buffering and broker redelivery | Conditional; resource ceilings and fault-tolerance objectives not selected or load-verified. |
| FTA (17), TOE access | Login, idle timeout, session limits, termination and notices | Partial; deployment values and browser workflows require verification. |
| FTP (18), trusted path/channels | Browser HTTPS, database/broker transport and identity providers | Partial/environment-dependent; no claim from local HTTP tests. |

## Candidate functional requirement traces

| Component / Part 2 clause | Implementation trace and symbols | Test / evidence trace | Status and limit |
| --- | --- | --- | --- |
| FAU_GEN.1 / 8.4.7; FAU_GEN.2 / 8.4.8 | [AuditManager](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditManager.java); [AuthenticationLoggerListener.onApplicationEvent](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/AuthenticationLoggerListener.java) | [AuditManagerTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditManagerTest.java); [AuthenticationListenerTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/config/AuthenticationListenerTest.java) | Partial: event inventory, start/stop events, outcome fields, identity and trusted time need ST-wide review. |
| FAU_SAR.1 / 8.6.6; FAU_SAR.2 / 8.6.7; FAU_SAR.3 / 8.6.8 | [JdbcAuditRepository.query/count](../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java); [AuditController](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/AuditController.java) | [AuditRepositoryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditRepositoryTest.java); [AuditControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/AuditControllerTest.java) | Partial: bounded, parameterized queries and API tests are relevant; define authorized auditor access across all audit types. |
| FAU_SEL.1 / 8.7.6 | [AuditMatrixConfigurer](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/configuration/AuditMatrixConfigurer.java) | [AuditMatrixConfigurerTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/configuration/AuditMatrixConfigurerTest.java) | Partial: verify the deployed audit matrix and permitted configuration changes. |
| FAU_STG.1 / 8.8.6 | [ArtemisConfig.jmsTopicTemplate](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java); [AbstractAuditConsumer._consume](../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java) | [broker operations](common-criteria-artemis-external-operations.md) | Partial: declare database/broker boundary and external-channel selections. |
| FAU_STG.2 / 8.8.7 | [JdbcAuditRepository.delete/compression handling](../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java) | [AuditRepositoryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditRepositoryTest.java); [JdbcAuditRepositoryCompressionTest](../../dhis-2/dhis-support/dhis-support-audit/src/test/java/org/hisp/dhis/audit/JdbcAuditRepositoryCompressionTest.java) | Partial: query guards, corruption errors and a [restricted writer-role fixture](common-criteria/audit-delivery-failure-evidence.md) are tested; deployed authorization, retention privileges and tamper controls remain unverified. |
| FAU_STG.3 / 8.8.8 | [ArtemisConfig.jmsListenerContainerFactory/configureAuditRedelivery](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java); [AuditScheduler.process](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java) | [ArtemisAuditBrokerIntegrationTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/config/ArtemisAuditBrokerIntegrationTest.java); [AuditSchedulerTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java) | Partial: [integrated consumer/PostgreSQL recovery](common-criteria/audit-delivery-failure-evidence.md) and separate broker/buffer cases pass; retained-record metrics, disk exhaustion, multi-node behavior and full server outage recovery remain open. |
| FAU_STG.4 / 8.8.9 | [audit storage failure investigation](common-criteria-audit-storage-failure.md) | [failure verification plan](common-criteria/audit-storage-failure-verification.md) | Open: no verified capacity threshold, delivered alarm or operator response. |
| FAU_STG.5 / 8.8.10 | [DefaultAuditService.addAudit](../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/DefaultAuditService.java); [AbstractAuditConsumer._consume](../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java) | [DefaultAuditServiceFailureTest](../../dhis-2/dhis-support/dhis-support-audit/src/test/java/org/hisp/dhis/audit/DefaultAuditServiceFailureTest.java); [AbstractAuditConsumerFailureTest](../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AbstractAuditConsumerFailureTest.java) | Partial: exceptions propagate; policy for business operations on full storage is not selected or proven. |
| FCS_COP.1 / 10.4.6 | [PasswordEncoderConfig.passwordEncoder](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/PasswordEncoderConfig.java); [ApiKeyTokenGenerator.hashToken](../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java) | [PasswordManagerTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/PasswordManagerTest.java); [ApiKeyTokenGeneratorTest](../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java) | Partial: BCrypt and token hashing do not establish approved algorithms/key sizes/provider validation for a selected policy. |
| FCS_RNG.1 / 10.6.6; FIA_SOS.2 / 12.6.7 | [ApiKeyTokenGenerator.generatePersonalAccessToken](../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/apikey/ApiKeyTokenGenerator.java) | [ApiKeyTokenGeneratorTest](../../dhis-2/dhis-api/src/test/java/org/hisp/dhis/security/apikey/ApiKeyTokenGeneratorTest.java) | Partial: secure randomness path exists; entropy and platform RNG assurance not evaluated. |
| FDP_ACC.1 / 11.3.6; FDP_ACF.1 / 11.4.6 | [DefaultAclService.canRead/canWrite/canDataRead](../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java) | [AclServiceTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java); [MeControllerAclTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/MeControllerAclTest.java) | Partial: define subjects, objects, operations and exceptions; these tests do not prove all endpoints enforce the policy. |
| FIA_AFL.1 / 12.3.6 | [TwoFactorAuthenticationProvider.checkLockout](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java); [DefaultUserService](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultUserService.java) | [AuthenticationControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java) | Partial: specify thresholds, reset intervals and treatment across password, token and external authentication. |
| FIA_ATD.1 / 12.5.6; FIA_USB.1 / 12.9.6 | [UserDetailsImpl](../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/user/UserDetailsImpl.java); [ApiTokenAuthManager.authenticate](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java) | [ApiTokenAuthManagerTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java); [ApiTokenAuthenticationTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/ApiTokenAuthenticationTest.java) | Partial; CC-2026-01/02 fix stale attributes and shared request authentication. |
| FIA_SOS.1 / 12.6.6 | [DefaultPasswordValidationService](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/DefaultPasswordValidationService.java); [PasswordLengthValidationRule](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/PasswordLengthValidationRule.java) | [PasswordValidationServiceTest](../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/user/PasswordValidationServiceTest.java); [PasswordValidationRuleTest](../../dhis-2/dhis-services/dhis-service-core/src/test/java/org/hisp/dhis/user/PasswordValidationRuleTest.java) | Partial: confirm configured length, history, dictionary and reset-path enforcement. |
| FIA_UID.1 / 12.8.6; FIA_UAU.1 / 12.7.6 | [DhisWebApiWebSecurityConfig.configureMatchers](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java); [AuthenticationController.login](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/AuthenticationController.java) | [AuthenticationControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java); [LoginConfigCorsTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/LoginConfigCorsTest.java) | Partial: list public bootstrap/static/recovery endpoints as allowed pre-authentication actions; do not claim FIA_UAU.2 for those actions. |
| FIA_UAU.5 / 12.7.10 | [TwoFactorAuthenticationProvider](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/security/spring2fa/TwoFactorAuthenticationProvider.java); [ApiTokenAuthManager](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java) | [ApiTokenAuthManagerTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java) | Partial: reject PAT use when owner enables 2FA; external IdP/LDAP and recovery combinations need separate evidence. |
| FMT_MSA.1 / 13.5.6 | [DefaultSharingService.saveSharing](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/sharing/DefaultSharingService.java); [DefaultAclService.canManage/canMakeClassPublic/canMakeClassPrivate](../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java) | [SharingControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/SharingControllerTest.java); [AclServiceTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java) | Partial: sharing-management permissions exist; assigned SFP, attributes, allowed operations and roles require ST-wide definition and validation. |
| FMT_MSA.2 / 13.5.7 | [DefaultAclService.verifySharing](../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java); [AccessStringHelper.isValid](../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/AccessStringHelper.java); [SecurityCheck.check](../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/objectbundle/validation/SecurityCheck.java) | [AclServiceTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java); [SharingControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/SharingControllerTest.java) | Partial: syntax, data-sharing applicability and creator authority checks are relevant; the full set of secure attribute combinations and creation exceptions is not established. |
| FMT_MSA.3 / 13.5.8 | [HibernateIdentifiableObjectStore.save/checkPublicAccess](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/common/hibernate/HibernateIdentifiableObjectStore.java); [DefaultAclService.resetSharing/defaultPrivate/defaultPublic](../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java); [DefaultMetadataImportService.preCreateBundleObject](../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/DefaultMetadataImportService.java); [DashboardSchemaDescriptor.getSchema](../../dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/DashboardSchemaDescriptor.java) | [Detailed FMT_MSA.3.1/.2 code and named-test trace](common-criteria/component-traces/FMT_MSA.3.md); [AclServiceTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java) | Partial: defaults and authorized initial-sharing mechanisms exist. Defaults vary by class/authority; complete the selected policy, roles and creation-path coverage. |
| FMT_REV.1 / 13.7.6 | [ApiTokenAuthManager.authenticate](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManager.java); [UserRoleSessionInvalidationListener](../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/user/UserRoleSessionInvalidationListener.java) | [ApiTokenAuthManagerTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/security/apikey/ApiTokenAuthManagerTest.java); [UserRoleSessionInvalidationControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/UserRoleSessionInvalidationControllerTest.java) | Partial: token deletion/account changes are rechecked per authentication; cluster-wide database/cache visibility and in-flight operations need operational verification. |
| FMT_SMF.1 / 13.9.6; FMT_SMR.1 / 13.10.6 | [UserController](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/user/UserController.java); [SessionController](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/SessionController.java); [RequiresAuthority](../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/RequiresAuthority.java) | [SessionControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/SessionControllerTest.java); [UserAccountControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/UserAccountControllerTest.java) | Partial: document administrator/auditor separation and all management functions. |
| FPT_FLS.1 / 15.4.6 | [AbstractAuditConsumer._consume](../../dhis-2/dhis-services/dhis-service-audit-consumer/src/main/java/org/hisp/dhis/audit/AbstractAuditConsumer.java); [AuditScheduler.process](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/audit/AuditScheduler.java) | [AbstractAuditConsumerFailureTest](../../dhis-2/dhis-services/dhis-service-audit-consumer/src/test/java/org/hisp/dhis/audit/AbstractAuditConsumerFailureTest.java); [AuditSchedulerTest](../../dhis-2/dhis-support/dhis-support-artemis/src/test/java/org/hisp/dhis/artemis/audit/AuditSchedulerTest.java) | Partial: visible failures and retry retention are relevant; secure state and admitted failure types must be specified. |
| FPT_STM.1 / 15.14.6; FPT_STM.2 / 15.14.7 | [JdbcAuditRepository timestamp mapping](../../dhis-2/dhis-support/dhis-support-audit/src/main/java/org/hisp/dhis/audit/JdbcAuditRepository.java) | [AuditRepositoryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditRepositoryTest.java) | Partial: timestamp precision is relevant; authenticated time source, clock drift and timezone policy are environment evidence. |
| FTA_MCS.1 / 17.4.6; FTA_SSL.3 / 17.5.8; FTA_SSL.4 / 17.5.9 | [DhisWebApiWebSecurityConfig.configureMatchers](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java); [RedisSpringSessionConfig.sessionRepository](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/session/RedisSpringSessionConfig.java); [SessionController](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/security/SessionController.java) | [AuthenticationControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/AuthenticationControllerTest.java); [SessionControllerTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/security/SessionControllerTest.java) | Partial: idle timeout, logout, concurrency and both session stores require configuration-specific checks. |
| FTP_TRP.1 / 18.5.6; FTP_ITC.1 / 18.3.6 | [DhisWebApiWebSecurityConfig.setHttpHeaders](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java); [ArtemisConfig.jmsConnectionFactory](../../dhis-2/dhis-support/dhis-support-artemis/src/main/java/org/hisp/dhis/artemis/config/ArtemisConfig.java) | [deployment checks](common-criteria-review.md#deployment-and-web-application-checks) | Open: HTTPS/HSTS configuration alone is not trusted-channel proof; external Artemis uses TCP unless secured by the deployment. |

The added fork regressions are isolated in
[ApiTokenStateAuthenticationTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/ApiTokenStateAuthenticationTest.java)
and [AuditRepositoryFailureRecoveryTest](../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/audit/AuditRepositoryFailureRecoveryTest.java).
Upstream's existing integration-test files are unchanged by this review.

The [integrated audit delivery/storage tests](common-criteria/audit-delivery-failure-evidence.md)
connect the real listener/consumer to PostgreSQL and exercise bounded redelivery,
DLQ replay, connection loss and denied reads/deletion. Their scope supports
FAU_STG.2/3/5 and FPT_FLS.1 without asserting capacity alarms or conformance.

### Supporting browser protections

[CspFilter](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/CspFilter.java),
[DhisCorsProcessor](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/DhisCorsProcessor.java),
[LoginConfigCorsFilter](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/filter/LoginConfigCorsFilter.java)
and the SPA CSRF handler in
[DhisWebApiWebSecurityConfig](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java)
support protection of web interfaces. Relevant regression tests are
[CspFilterTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/filter/CspFilterTest.java),
[LoginConfigCorsFilterTest](../../dhis-2/dhis-web-api/src/test/java/org/hisp/dhis/webapi/filter/LoginConfigCorsFilterTest.java)
and [LoginConfigCorsTest](../../dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/security/LoginConfigCorsTest.java).
These mechanisms do not map one-to-one to a universal CC requirement. Associate
them with the ST's access-control/information-flow policy and vulnerability analysis.

## Part 3: assurance evidence and outstanding work

All nine assurance classes are accounted for. No assurance component is marked
satisfied solely because code or Git history exists.

| Class / candidate family or component | Evidence trace | Assessment |
| --- | --- | --- |
| APE / Part 3 clause 7 | No evaluated PP supplied | Conditional: required if a PP is developed/evaluated; identify applicable profile first. |
| ACE / clause 8 | No PP-Module or PP-Configuration supplied | Conditional: no modular configuration claim. |
| ASE / clause 9: ASE_INT.1, ASE_CCL.1, ASE_SPD.1, ASE_OBJ.2, ASE_REQ.2, ASE_TSS.1 | [draft scope, threats and objectives](common-criteria-review.md), candidate functional traces above | Open: complete ST, conformance claims, rationale, dependency closure and TOE summary specification for independent evaluation. |
| ADV / clause 10: ADV_ARC.1, ADV_FSP, ADV_TDS, ADV_IMP | [security configuration](../../dhis-2/dhis-web-api/src/main/java/org/hisp/dhis/webapi/security/config/DhisWebApiWebSecurityConfig.java), source links above, [API documentation reference](../../README.md) | Partial: code trace exists; full TSF interfaces, non-bypassability, domain separation, subsystem design and selected representation evidence remain. |
| AGD / clause 11: AGD_OPE.1, AGD_PRE.1 | [external broker operations](common-criteria-artemis-external-operations.md), [README](../../README.md), [deployment checks](common-criteria-review.md#deployment-and-web-application-checks) | Partial: create versioned evaluated installation/acceptance and administrator/user guidance for the final configuration. |
| ALC / clause 12: ALC_CMC, ALC_CMS, ALC_DEL, ALC_DVS, ALC_FLR, ALC_LCD, ALC_TAT | [CONTRIBUTING](../../CONTRIBUTING.md), [SECURITY](../../SECURITY.md), [Maven build](../../dhis-2/pom.xml), Git baseline and [CI](../../.github/workflows/cc-audit-regression.yml) | Partial: inventory, controlled builds, toolchain records, delivery integrity, development-site controls and flaw remediation evidence require organization-level review. |
| ATE / clause 13: ATE_COV, ATE_DPT, ATE_FUN.1, ATE_IND | Linked regression tests, [execution evidence](common-criteria-verification.md), [CI](../../.github/workflows/cc-audit-regression.yml) | Partial: exercised paths are documented; full TSF coverage, depth and independent testing are not established. |
| AVA / clause 14: AVA_VAN | [gap register](common-criteria-review.md#gap-register), [SECURITY](../../SECURITY.md), dependency patch inventory under `.patches/security/` | Partial: this targeted source review is not a complete vulnerability assessment or penetration test at a selected attack potential. |
| ACO / clause 15 | Database, broker and identity-provider boundaries in [draft scope](common-criteria-review.md#draft-evaluation-boundary) | Conditional: obtain component evaluation evidence and composition analysis if composed assurance is claimed. |

## Fork maintenance

See the [upstream integration guide](../fork/upstream-maintenance.md) for the two
necessary production patches and their regression checks. New fork tests and
documentation live in separate files to reduce overlap with upstream changes.

## Maintaining this trace

For every security change, update the relevant row, gap ID, affected symbols and
regression test. Record the exact tested revision/configuration and executed,
failed and skipped counts in the verification record. Keep historical CI links
labelled as historical. Never promote `Partial` or `Open` to conformance based on
a unit test alone. New or removed web applications require an inventory update and
workflow regression checks before the evaluation boundary can be frozen.
