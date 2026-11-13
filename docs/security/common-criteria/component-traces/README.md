# Component trace documents

199 components retained for web-application review have individual pages. 62 conditional or excluded components are recorded below with their rationale. These are alternative candidates for an eventual Security Target, not a mandatory combined package.

**Trace depth varies.** FMT_MSA.3 has a separately maintained element/code/test analysis. Other pages expose the existing code candidates, boundary references, test sources and gaps. A small set additionally contains source-reviewed partial element mappings. An unmapped element remains open; creating its page does not establish coverage. Part 3 requires developer, content and evaluator artifacts, not merely source code.

Element identifiers come from the supplied 2026 standards. Requirement text is not reproduced. The element inventory does not select ST operations or close dependencies. Source line anchors refer to the reviewed checkout; revalidate after upstream updates.

See the [component gap assessment](../../common-criteria-gap-assessment.md), [verification record](../../common-criteria-verification.md) and [scope](../web-application-scope.md). Regenerate with `python3 docs/security/tools/render_common_criteria_review.py`; add `--check` to validate.

| Component | Trace depth |
| --- | --- |
| [FAU_ARP.1](FAU_ARP.1.md) — Security alarms | Boundary/artifact references; element mapping open |
| [FAU_GEN.1](FAU_GEN.1.md) — Audit data generation | Partial element mappings; remaining elements open |
| [FAU_GEN.2](FAU_GEN.2.md) — User identity association | Partial element mappings; remaining elements open |
| [FAU_SAA.1](FAU_SAA.1.md) — Potential violation analysis | Boundary/artifact references; element mapping open |
| [FAU_SAA.2](FAU_SAA.2.md) — Profile based anomaly detection | Boundary/artifact references; element mapping open |
| [FAU_SAA.3](FAU_SAA.3.md) — Simple attack heuristics | Boundary/artifact references; element mapping open |
| [FAU_SAA.4](FAU_SAA.4.md) — Complex attack heuristics | Boundary/artifact references; element mapping open |
| [FAU_SAR.1](FAU_SAR.1.md) — Audit review | Component code candidates; element mapping open |
| [FAU_SAR.2](FAU_SAR.2.md) — Restricted audit review | Component code candidates; element mapping open |
| [FAU_SAR.3](FAU_SAR.3.md) — Selectable audit review | Component code candidates; element mapping open |
| [FAU_SEL.1](FAU_SEL.1.md) — Selective audit | Component code candidates; element mapping open |
| [FAU_STG.1](FAU_STG.1.md) — Audit data storage location | Component code candidates; element mapping open |
| [FAU_STG.2](FAU_STG.2.md) — Protected audit data storage | Component code candidates; element mapping open |
| [FAU_STG.3](FAU_STG.3.md) — Guarantees of audit data availability | Component code candidates; element mapping open |
| [FAU_STG.4](FAU_STG.4.md) — Action in case of possible audit data loss | Boundary/artifact references; element mapping open |
| [FAU_STG.5](FAU_STG.5.md) — Prevention of audit data loss | Component code candidates; element mapping open |
| [FCS_CKM.1](FCS_CKM.1.md) — Cryptographic key generation | Component code candidates; element mapping open |
| [FCS_CKM.2](FCS_CKM.2.md) — Cryptographic key distribution | Boundary/artifact references; element mapping open |
| [FCS_CKM.3](FCS_CKM.3.md) — Cryptographic key access | Component code candidates; element mapping open |
| [FCS_CKM.5](FCS_CKM.5.md) — Cryptographic key derivation | Component code candidates; element mapping open |
| [FCS_CKM.6](FCS_CKM.6.md) — Timing and event of cryptographic key destruction | Boundary/artifact references; element mapping open |
| [FCS_COP.1](FCS_COP.1.md) — Cryptographic operation | Component code candidates; element mapping open |
| [FCS_RBG.1](FCS_RBG.1.md) — Random bit generation (RBG) | Boundary/artifact references; element mapping open |
| [FCS_RBG.2](FCS_RBG.2.md) — Random bit generation (external seeding) | Boundary/artifact references; element mapping open |
| [FCS_RBG.3](FCS_RBG.3.md) — Random bit generation (internal seeding - single source) | Boundary/artifact references; element mapping open |
| [FCS_RBG.4](FCS_RBG.4.md) — Random bit generation (internal seeding - multiple sources) | Boundary/artifact references; element mapping open |
| [FCS_RBG.5](FCS_RBG.5.md) — Random bit generation (combining entropy sources) | Boundary/artifact references; element mapping open |
| [FCS_RNG.1](FCS_RNG.1.md) — Random number generation | Component code candidates; element mapping open |
| [FDP_ACC.1](FDP_ACC.1.md) — Subset access control | Component code candidates; element mapping open |
| [FDP_ACC.2](FDP_ACC.2.md) — Complete access control | Boundary/artifact references; element mapping open |
| [FDP_ACF.1](FDP_ACF.1.md) — Security attribute-based access control | Component code candidates; element mapping open |
| [FDP_ETC.1](FDP_ETC.1.md) — Export of user data without security attributes | Component code candidates; element mapping open |
| [FDP_ETC.2](FDP_ETC.2.md) — Export of user data with security attributes | Component code candidates; element mapping open |
| [FDP_IFC.1](FDP_IFC.1.md) — Subset information flow control | Boundary/artifact references; element mapping open |
| [FDP_IFC.2](FDP_IFC.2.md) — Complete information flow control | Boundary/artifact references; element mapping open |
| [FDP_IFF.1](FDP_IFF.1.md) — Simple security attributes | Boundary/artifact references; element mapping open |
| [FDP_IFF.2](FDP_IFF.2.md) — Hierarchical security attributes | Boundary/artifact references; element mapping open |
| [FDP_IFF.3](FDP_IFF.3.md) — Limited illicit information flows | Boundary/artifact references; element mapping open |
| [FDP_IFF.4](FDP_IFF.4.md) — Partial elimination of illicit information flows | Boundary/artifact references; element mapping open |
| [FDP_IFF.5](FDP_IFF.5.md) — No illicit information flows | Boundary/artifact references; element mapping open |
| [FDP_IFF.6](FDP_IFF.6.md) — Illicit information flow monitoring | Boundary/artifact references; element mapping open |
| [FDP_IRC.1](FDP_IRC.1.md) — Information retention control | Boundary/artifact references; element mapping open |
| [FDP_ITC.1](FDP_ITC.1.md) — Import of user data without security attributes | Component code candidates; element mapping open |
| [FDP_ITC.2](FDP_ITC.2.md) — Import of user data with security attributes | Component code candidates; element mapping open |
| [FDP_ITT.1](FDP_ITT.1.md) — Basic internal transfer protection | Boundary/artifact references; element mapping open |
| [FDP_ITT.2](FDP_ITT.2.md) — Transmission separation by attribute | Boundary/artifact references; element mapping open |
| [FDP_ITT.3](FDP_ITT.3.md) — Integrity monitoring | Boundary/artifact references; element mapping open |
| [FDP_ITT.4](FDP_ITT.4.md) — Attribute-based integrity monitoring | Boundary/artifact references; element mapping open |
| [FDP_RIP.1](FDP_RIP.1.md) — Subset residual information protection | Boundary/artifact references; element mapping open |
| [FDP_RIP.2](FDP_RIP.2.md) — Full residual information protection | Boundary/artifact references; element mapping open |
| [FDP_ROL.1](FDP_ROL.1.md) — Basic rollback | Boundary/artifact references; element mapping open |
| [FDP_ROL.2](FDP_ROL.2.md) — Advanced rollback | Boundary/artifact references; element mapping open |
| [FDP_SDC.1](FDP_SDC.1.md) — Stored data confidentiality | Component code candidates; element mapping open |
| [FDP_SDC.2](FDP_SDC.2.md) — Stored data confidentiality with dedicated method | Boundary/artifact references; element mapping open |
| [FDP_SDI.1](FDP_SDI.1.md) — Stored data integrity monitoring | Boundary/artifact references; element mapping open |
| [FDP_SDI.2](FDP_SDI.2.md) — Stored data integrity monitoring and action | Boundary/artifact references; element mapping open |
| [FDP_UCT.1](FDP_UCT.1.md) — Basic data exchange confidentiality | Boundary/artifact references; element mapping open |
| [FDP_UIT.1](FDP_UIT.1.md) — Data exchange integrity | Boundary/artifact references; element mapping open |
| [FDP_UIT.2](FDP_UIT.2.md) — Source data exchange recovery | Boundary/artifact references; element mapping open |
| [FDP_UIT.3](FDP_UIT.3.md) — Destination data exchange recovery | Boundary/artifact references; element mapping open |
| [FIA_AFL.1](FIA_AFL.1.md) — Authentication failure handling | Component code candidates; element mapping open |
| [FIA_API.1](FIA_API.1.md) — Authentication proof of identity | Boundary/artifact references; element mapping open |
| [FIA_ATD.1](FIA_ATD.1.md) — User attribute definition | Component code candidates; element mapping open |
| [FIA_SOS.1](FIA_SOS.1.md) — Verification of secrets | Component code candidates; element mapping open |
| [FIA_SOS.2](FIA_SOS.2.md) — TSF Generation of secrets | Component code candidates; element mapping open |
| [FIA_UAU.1](FIA_UAU.1.md) — Timing of authentication | Component code candidates; element mapping open |
| [FIA_UAU.2](FIA_UAU.2.md) — User authentication before any action | Boundary/artifact references; element mapping open |
| [FIA_UAU.3](FIA_UAU.3.md) — Unforgeable authentication | Boundary/artifact references; element mapping open |
| [FIA_UAU.4](FIA_UAU.4.md) — Single-use authentication mechanisms | Boundary/artifact references; element mapping open |
| [FIA_UAU.5](FIA_UAU.5.md) — Multiple authentication mechanisms | Partial element mappings; remaining elements open |
| [FIA_UAU.6](FIA_UAU.6.md) — Re-authenticating | Boundary/artifact references; element mapping open |
| [FIA_UAU.7](FIA_UAU.7.md) — Protected authentication feedback | Boundary/artifact references; element mapping open |
| [FIA_UID.1](FIA_UID.1.md) — Timing of identification | Component code candidates; element mapping open |
| [FIA_UID.2](FIA_UID.2.md) — User identification before any action | Boundary/artifact references; element mapping open |
| [FIA_USB.1](FIA_USB.1.md) — User-subject binding | Partial element mappings; remaining elements open |
| [FMT_MOF.1](FMT_MOF.1.md) — Management of security functions behaviour | Component code candidates; element mapping open |
| [FMT_MSA.1](FMT_MSA.1.md) — Management of security attributes | Component code candidates; element mapping open |
| [FMT_MSA.2](FMT_MSA.2.md) — Secure security attributes | Component code candidates; element mapping open |
| [FMT_MSA.3](FMT_MSA.3.md) — Static attribute initialization | Maintained element/code/test analysis |
| [FMT_MSA.4](FMT_MSA.4.md) — Security attribute value inheritance | Boundary/artifact references; element mapping open |
| [FMT_MTD.1](FMT_MTD.1.md) — Management of TSF data | Component code candidates; element mapping open |
| [FMT_MTD.2](FMT_MTD.2.md) — Management of limits on TSF data | Boundary/artifact references; element mapping open |
| [FMT_MTD.3](FMT_MTD.3.md) — Secure TSF data | Boundary/artifact references; element mapping open |
| [FMT_REV.1](FMT_REV.1.md) — Revocation | Partial element mappings; remaining elements open |
| [FMT_SAE.1](FMT_SAE.1.md) — Time-limited authorization | Partial element mappings; remaining elements open |
| [FMT_SMF.1](FMT_SMF.1.md) — Specification of Management Functions | Component code candidates; element mapping open |
| [FMT_SMR.1](FMT_SMR.1.md) — Security roles | Component code candidates; element mapping open |
| [FMT_SMR.2](FMT_SMR.2.md) — Restrictions on security roles | Boundary/artifact references; element mapping open |
| [FMT_SMR.3](FMT_SMR.3.md) — Assuming roles | Boundary/artifact references; element mapping open |
| [FPT_FLS.1](FPT_FLS.1.md) — Failure with preservation of secure state | Partial element mappings; remaining elements open |
| [FPT_INI.1](FPT_INI.1.md) — TSF initialization | Boundary/artifact references; element mapping open |
| [FPT_ITA.1](FPT_ITA.1.md) — Inter-TSF availability within a defined availability metric | Boundary/artifact references; element mapping open |
| [FPT_ITC.1](FPT_ITC.1.md) — Inter-TSF confidentiality during transmission | Boundary/artifact references; element mapping open |
| [FPT_ITI.1](FPT_ITI.1.md) — Inter-TSF detection of modification | Boundary/artifact references; element mapping open |
| [FPT_ITI.2](FPT_ITI.2.md) — Inter-TSF detection and correction of modification | Boundary/artifact references; element mapping open |
| [FPT_ITT.1](FPT_ITT.1.md) — Basic internal TSF data transfer protection | Partial element mappings; remaining elements open |
| [FPT_ITT.2](FPT_ITT.2.md) — TSF data transfer separation | Boundary/artifact references; element mapping open |
| [FPT_ITT.3](FPT_ITT.3.md) — TSF data integrity monitoring | Boundary/artifact references; element mapping open |
| [FPT_RCV.1](FPT_RCV.1.md) — Manual recovery | Boundary/artifact references; element mapping open |
| [FPT_RCV.2](FPT_RCV.2.md) — Automated recovery | Boundary/artifact references; element mapping open |
| [FPT_RCV.3](FPT_RCV.3.md) — Automated recovery without undue loss | Boundary/artifact references; element mapping open |
| [FPT_RCV.4](FPT_RCV.4.md) — Function recovery | Boundary/artifact references; element mapping open |
| [FPT_RPL.1](FPT_RPL.1.md) — Replay detection | Boundary/artifact references; element mapping open |
| [FPT_SSP.1](FPT_SSP.1.md) — Simple trusted acknowledgement | Boundary/artifact references; element mapping open |
| [FPT_SSP.2](FPT_SSP.2.md) — Mutual trusted acknowledgement | Boundary/artifact references; element mapping open |
| [FPT_STM.1](FPT_STM.1.md) — Reliable time stamps | Component code candidates; element mapping open |
| [FPT_STM.2](FPT_STM.2.md) — Time source | Boundary/artifact references; element mapping open |
| [FPT_TDC.1](FPT_TDC.1.md) — Inter-TSF basic TSF data consistency | Boundary/artifact references; element mapping open |
| [FPT_TEE.1](FPT_TEE.1.md) — Testing of external entities | Boundary/artifact references; element mapping open |
| [FPT_TRC.1](FPT_TRC.1.md) — Internal TSF consistency | Boundary/artifact references; element mapping open |
| [FPT_TST.1](FPT_TST.1.md) — TSF self-testing | Boundary/artifact references; element mapping open |
| [FRU_FLT.1](FRU_FLT.1.md) — Degraded fault tolerance | Boundary/artifact references; element mapping open |
| [FRU_FLT.2](FRU_FLT.2.md) — Limited fault tolerance | Boundary/artifact references; element mapping open |
| [FRU_RSA.1](FRU_RSA.1.md) — Maximum quotas | Boundary/artifact references; element mapping open |
| [FTA_LSA.1](FTA_LSA.1.md) — Limitation on scope of selectable attributes | Boundary/artifact references; element mapping open |
| [FTA_MCS.1](FTA_MCS.1.md) — Basic limitation on multiple concurrent sessions | Component code candidates; element mapping open |
| [FTA_MCS.2](FTA_MCS.2.md) — Per user attribute limitation on multiple concurrent sessions | Boundary/artifact references; element mapping open |
| [FTA_SSL.3](FTA_SSL.3.md) — TSF-initiated termination | Component code candidates; element mapping open |
| [FTA_SSL.4](FTA_SSL.4.md) — User-initiated termination | Partial element mappings; remaining elements open |
| [FTA_TAB.1](FTA_TAB.1.md) — Default TOE access banners | Boundary/artifact references; element mapping open |
| [FTA_TAH.1](FTA_TAH.1.md) — TOE access history | Boundary/artifact references; element mapping open |
| [FTA_TSE.1](FTA_TSE.1.md) — TOE session establishment | Boundary/artifact references; element mapping open |
| [FTP_ITC.1](FTP_ITC.1.md) — Inter-TSF trusted channel | Boundary/artifact references; element mapping open |
| [FTP_PRO.1](FTP_PRO.1.md) — Trusted channel protocol | Boundary/artifact references; element mapping open |
| [FTP_PRO.2](FTP_PRO.2.md) — Trusted channel establishment | Boundary/artifact references; element mapping open |
| [FTP_PRO.3](FTP_PRO.3.md) — Trusted channel data protection | Boundary/artifact references; element mapping open |
| [FTP_TRP.1](FTP_TRP.1.md) — Trusted path | Boundary/artifact references; element mapping open |
| [ASE_INT.1](ASE_INT.1.md) — ST introduction | Boundary/artifact references; element mapping open |
| [ASE_CCL.1](ASE_CCL.1.md) — Conformance claims | Boundary/artifact references; element mapping open |
| [ASE_SPD.1](ASE_SPD.1.md) — Security problem definition | Boundary/artifact references; element mapping open |
| [ASE_OBJ.1](ASE_OBJ.1.md) — Security objectives for the operational environment | Boundary/artifact references; element mapping open |
| [ASE_OBJ.2](ASE_OBJ.2.md) — Security objectives | Boundary/artifact references; element mapping open |
| [ASE_ECD.1](ASE_ECD.1.md) — Extended components definition | Boundary/artifact references; element mapping open |
| [ASE_REQ.1](ASE_REQ.1.md) — Direct rationale security requirements | Boundary/artifact references; element mapping open |
| [ASE_REQ.2](ASE_REQ.2.md) — Derived security requirements | Boundary/artifact references; element mapping open |
| [ASE_TSS.1](ASE_TSS.1.md) — TOE summary specification | Boundary/artifact references; element mapping open |
| [ASE_TSS.2](ASE_TSS.2.md) — TOE summary specification with architectural design summary | Boundary/artifact references; element mapping open |
| [ADV_ARC.1](ADV_ARC.1.md) — Security architecture description | Component code candidates; element mapping open |
| [ADV_FSP.1](ADV_FSP.1.md) — Basic functional specification | Boundary/artifact references; element mapping open |
| [ADV_FSP.2](ADV_FSP.2.md) — Security-enforcing functional specification | Boundary/artifact references; element mapping open |
| [ADV_FSP.3](ADV_FSP.3.md) — Functional specification with complete summary | Boundary/artifact references; element mapping open |
| [ADV_FSP.4](ADV_FSP.4.md) — Complete functional specification | Boundary/artifact references; element mapping open |
| [ADV_FSP.5](ADV_FSP.5.md) — Complete semi-formal functional specification with additional error information | Boundary/artifact references; element mapping open |
| [ADV_FSP.6](ADV_FSP.6.md) — Complete semi-formal functional specification with additional formal specification | Boundary/artifact references; element mapping open |
| [ADV_IMP.1](ADV_IMP.1.md) — Implementation representation of the TSF | Boundary/artifact references; element mapping open |
| [ADV_IMP.2](ADV_IMP.2.md) — Complete mapping of the implementation representation of the TSF | Boundary/artifact references; element mapping open |
| [ADV_INT.1](ADV_INT.1.md) — Well-structured subset of TSF internals | Boundary/artifact references; element mapping open |
| [ADV_INT.2](ADV_INT.2.md) — Well-structured internals | Boundary/artifact references; element mapping open |
| [ADV_INT.3](ADV_INT.3.md) — Minimally complex internals | Boundary/artifact references; element mapping open |
| [ADV_SPM.1](ADV_SPM.1.md) — Formal TSF model | Boundary/artifact references; element mapping open |
| [ADV_TDS.1](ADV_TDS.1.md) — Basic design | Boundary/artifact references; element mapping open |
| [ADV_TDS.2](ADV_TDS.2.md) — Architectural design | Boundary/artifact references; element mapping open |
| [ADV_TDS.3](ADV_TDS.3.md) — Basic modular design | Boundary/artifact references; element mapping open |
| [ADV_TDS.4](ADV_TDS.4.md) — Semi-Formal modular design | Boundary/artifact references; element mapping open |
| [ADV_TDS.5](ADV_TDS.5.md) — Complete semi-formal modular design | Boundary/artifact references; element mapping open |
| [ADV_TDS.6](ADV_TDS.6.md) — Complete semi-formal modular design with formal high-level design presentation | Boundary/artifact references; element mapping open |
| [AGD_OPE.1](AGD_OPE.1.md) — Operational user guidance | Boundary/artifact references; element mapping open |
| [AGD_PRE.1](AGD_PRE.1.md) — Preparative procedures | Boundary/artifact references; element mapping open |
| [ALC_CMC.1](ALC_CMC.1.md) — Labelling of the TOE | Boundary/artifact references; element mapping open |
| [ALC_CMC.2](ALC_CMC.2.md) — Use of the CM system | Boundary/artifact references; element mapping open |
| [ALC_CMC.3](ALC_CMC.3.md) — Authorization controls | Boundary/artifact references; element mapping open |
| [ALC_CMC.4](ALC_CMC.4.md) — Production support, acceptance procedures and automation | Boundary/artifact references; element mapping open |
| [ALC_CMC.5](ALC_CMC.5.md) — Advanced support | Boundary/artifact references; element mapping open |
| [ALC_CMS.1](ALC_CMS.1.md) — TOE CM coverage | Boundary/artifact references; element mapping open |
| [ALC_CMS.2](ALC_CMS.2.md) — Parts of the TOE CM coverage | Boundary/artifact references; element mapping open |
| [ALC_CMS.3](ALC_CMS.3.md) — Implementation representation CM coverage | Boundary/artifact references; element mapping open |
| [ALC_CMS.4](ALC_CMS.4.md) — Problem tracking CM coverage | Boundary/artifact references; element mapping open |
| [ALC_CMS.5](ALC_CMS.5.md) — Development tools CM coverage | Boundary/artifact references; element mapping open |
| [ALC_DEL.1](ALC_DEL.1.md) — Delivery procedures | Boundary/artifact references; element mapping open |
| [ALC_DVS.1](ALC_DVS.1.md) — Identification of security controls | Boundary/artifact references; element mapping open |
| [ALC_DVS.2](ALC_DVS.2.md) — Sufficiency of security controls | Boundary/artifact references; element mapping open |
| [ALC_FLR.1](ALC_FLR.1.md) — Basic flaw remediation | Boundary/artifact references; element mapping open |
| [ALC_FLR.2](ALC_FLR.2.md) — Flaw reporting procedures | Boundary/artifact references; element mapping open |
| [ALC_FLR.3](ALC_FLR.3.md) — Systematic flaw remediation | Boundary/artifact references; element mapping open |
| [ALC_LCD.1](ALC_LCD.1.md) — Developer defined life cycle processes | Boundary/artifact references; element mapping open |
| [ALC_LCD.2](ALC_LCD.2.md) — Measurable life cycle model | Boundary/artifact references; element mapping open |
| [ALC_TDA.1](ALC_TDA.1.md) — Uniquely identifying implementation representation | Boundary/artifact references; element mapping open |
| [ALC_TDA.2](ALC_TDA.2.md) — Matching CMS scope of implementation representation | Boundary/artifact references; element mapping open |
| [ALC_TDA.3](ALC_TDA.3.md) — Regenerate TOE with well-defined development tools | Boundary/artifact references; element mapping open |
| [ALC_TAT.1](ALC_TAT.1.md) — Well-defined development tools | Boundary/artifact references; element mapping open |
| [ALC_TAT.2](ALC_TAT.2.md) — Compliance with implementation standards | Boundary/artifact references; element mapping open |
| [ALC_TAT.3](ALC_TAT.3.md) — Compliance with implementation standards - all parts | Boundary/artifact references; element mapping open |
| [ATE_COV.1](ATE_COV.1.md) — Evidence of coverage | Boundary/artifact references; element mapping open |
| [ATE_COV.2](ATE_COV.2.md) — Analysis of coverage | Boundary/artifact references; element mapping open |
| [ATE_COV.3](ATE_COV.3.md) — Rigorous analysis of coverage | Boundary/artifact references; element mapping open |
| [ATE_DPT.1](ATE_DPT.1.md) — Testing: basic design | Boundary/artifact references; element mapping open |
| [ATE_DPT.2](ATE_DPT.2.md) — Testing: security enforcing modules | Boundary/artifact references; element mapping open |
| [ATE_DPT.3](ATE_DPT.3.md) — Testing: modular design | Boundary/artifact references; element mapping open |
| [ATE_DPT.4](ATE_DPT.4.md) — Testing: implementation representation | Boundary/artifact references; element mapping open |
| [ATE_FUN.1](ATE_FUN.1.md) — Functional testing | Boundary/artifact references; element mapping open |
| [ATE_FUN.2](ATE_FUN.2.md) — Ordered functional testing | Boundary/artifact references; element mapping open |
| [ATE_IND.1](ATE_IND.1.md) — Independent testing - conformance | Boundary/artifact references; element mapping open |
| [ATE_IND.2](ATE_IND.2.md) — Independent testing - sample | Boundary/artifact references; element mapping open |
| [ATE_IND.3](ATE_IND.3.md) — Independent testing - complete | Boundary/artifact references; element mapping open |
| [AVA_VAN.1](AVA_VAN.1.md) — Vulnerability survey | Boundary/artifact references; element mapping open |
| [AVA_VAN.2](AVA_VAN.2.md) — Vulnerability analysis | Boundary/artifact references; element mapping open |
| [AVA_VAN.3](AVA_VAN.3.md) — Focused vulnerability analysis | Boundary/artifact references; element mapping open |
| [AVA_VAN.4](AVA_VAN.4.md) — Methodical vulnerability analysis | Boundary/artifact references; element mapping open |
| [AVA_VAN.5](AVA_VAN.5.md) — Advanced methodical vulnerability analysis | Boundary/artifact references; element mapping open |

## Conditional or excluded components

No implementation page is asserted for these components. Reopen their scope if the deployment, features or evaluation approach changes.

| Component | Disposition and reason |
| --- | --- |
| FCO_NRO.1 — Selective proof of origin | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FCO_NRO.2 — Enforced proof of origin | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FCO_NRR.1 — Selective proof of receipt | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FCO_NRR.2 — Enforced proof of receipt | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FCS_RBG.6 — Random bit generation service | Random-bit service feature not selected. DHIS2 consumes platform randomness for application secrets; the scope does not offer an evaluated random-bit generation service. Secret entropy/provider behavior remains in scope. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FDP_DAU.1 — Basic Data Authentication | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FDP_DAU.2 — Data Authentication with Identity of Guarantor | Optional verifiable-evidence feature not selected. The current web scope does not claim third-party-verifiable origin, receipt or data-validity evidence. Authentication, transport integrity, input validation and attributable audit remain in scope. Revisit if signed records/receipts are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FMT_LIM.1 — Limited capabilities | Specialized lifecycle feature not selected. No manufacturing/test-mode capability or availability claim is selected for this web application. Production debug/test endpoints and administrative functions still require attack-surface review and access control. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FMT_LIM.2 — Limited availability | Specialized lifecycle feature not selected. No manufacturing/test-mode capability or availability claim is selected for this web application. Production debug/test endpoints and administrative functions still require attack-surface review and access control. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_ANO.1 — Anonymity | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_ANO.2 — Anonymity without soliciting information | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_PSE.1 — Pseudonymity | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_PSE.2 — Reversible pseudonymity | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_PSE.3 — Alias pseudonymity | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_UNL.1 — Unlinkability of operations | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_UNO.1 — Unobservability | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_UNO.2 — Allocation of information impacting unobservability | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_UNO.3 — Unobservability without soliciting information | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPR_UNO.4 — Authorized user observability | Specialized privacy guarantee not selected. The baseline scope is authenticated health-data access with attributable audit, not anonymous/pseudonymous/unlinkable/unobservable operations. This does not exclude patient confidentiality, minimization, de-identification needs or privacy-law obligations; those require separate business/privacy decisions and access/data-flow review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPT_EMS.1 — Emanation of TSF and User data | Excluded from application TOE; platform responsibility. Physical tamper/emanation resistance belongs to hosting/hardware outside the Java/browser TOE. Hosting controls remain an environment assumption; application timing and information leaks remain in web vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPT_PHP.1 — Passive detection of physical attack | Excluded from application TOE; platform responsibility. Physical tamper/emanation resistance belongs to hosting/hardware outside the Java/browser TOE. Hosting controls remain an environment assumption; application timing and information leaks remain in web vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPT_PHP.2 — Notification of physical attack | Excluded from application TOE; platform responsibility. Physical tamper/emanation resistance belongs to hosting/hardware outside the Java/browser TOE. Hosting controls remain an environment assumption; application timing and information leaks remain in web vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FPT_PHP.3 — Resistance to physical attack | Excluded from application TOE; platform responsibility. Physical tamper/emanation resistance belongs to hosting/hardware outside the Java/browser TOE. Hosting controls remain an environment assumption; application timing and information leaks remain in web vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FRU_PRS.1 — Limited priority of service | Specialized resource guarantee not selected. No subject-priority service or minimum reserved-resource guarantee is selected. Capacity limits, denial-of-service resistance, availability and maximum quotas remain in web/deployment review; revisit if clinical service priorities are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FRU_PRS.2 — Full priority of service | Specialized resource guarantee not selected. No subject-priority service or minimum reserved-resource guarantee is selected. Capacity limits, denial-of-service resistance, availability and maximum quotas remain in web/deployment review; revisit if clinical service priorities are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FRU_RSA.2 — Minimum and maximum quotas | Specialized resource guarantee not selected. No subject-priority service or minimum reserved-resource guarantee is selected. Capacity limits, denial-of-service resistance, availability and maximum quotas remain in web/deployment review; revisit if clinical service priorities are required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FTA_SSL.1 — TSF-initiated session locking | Optional UI-lock feature not selected. The baseline web profile uses inactivity termination and explicit logout, not a separate screen-lock/unlock protocol. Session denial after expiry/logout remains in scope; revisit if workstation/application locking is required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| FTA_SSL.2 — User-initiated locking | Optional UI-lock feature not selected. The baseline web profile uses inactivity termination and explicit logout, not a separate screen-lock/unlock protocol. Session denial after expiry/logout remains in scope; revisit if workstation/application locking is required. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_INT.1 — PP introduction | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_CCL.1 — Conformance claims | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_SPD.1 — Security problem definition | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_OBJ.1 — Security objectives for the operational environment | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_OBJ.2 — Security objectives | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_ECD.1 — Extended components definition | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_REQ.1 — Direct rationale security requirements | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| APE_REQ.2 — Derived security requirements | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_INT.1 — PP-Module introduction | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_CCL.1 — PP-Module conformance claims | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_SPD.1 — PP-Module security problem definition | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_OBJ.1 — PP-Module security objectives for the operational environment | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_OBJ.2 — PP-Module security objectives | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_ECD.1 — PP-Module extended components definition | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_REQ.1 — PP-Module direct rationale security requirements | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_REQ.2 — PP-Module derived security requirements | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_MCO.1 — PP-Module consistency | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACE_CCO.1 — PP-Configuration consistency | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ASE_COMP.1 — Consistency of Security Target (ST) | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ADV_COMP.1 — Design compliance with the base component-related user guidance, ETR for composite evaluation and report of the base component evaluation authority | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ALC_COMP.1 — Integration of the dependent component into the related base component and Consistency check for delivery and acceptance procedures | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ATE_COMP.1 — Composite product functional testing | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| AVA_COMP.1 — Composite product vulnerability assessment | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_COR.1 — Composition rationale | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_DEV.1 — Functional description | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_DEV.2 — Basic evidence of design | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_DEV.3 — Detailed evidence of design | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_REL.1 — Basic reliance information | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_REL.2 — Reliance information | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_CTT.1 — Interface testing | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_CTT.2 — Rigorous interface testing | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_VUL.1 — Composition vulnerability review | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_VUL.2 — Composition vulnerability analysis | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
| ACO_VUL.3 — Enhanced-Basic composition vulnerability analysis | Excluded from current web-application evaluation approach. This task evaluates the DHIS2 application, not a new PP/PP-Module/PP-Configuration or a formally composed/composite TOE using base evaluation evidence. Third-party libraries remain in dependency and vulnerability review. User direction on 2026-11-04: review DHIS2 as a web application and document exclusions. |
