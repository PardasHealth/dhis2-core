# FMT_MSA.3 — Static attribute initialization

Standard: ISO/IEC 15408-2:2026, clause 13.5.8.
Code snapshot: DHIS2-Core `2.44-SNAPSHOT`, baseline `07d4f3f7ea` plus the recorded
security patch. **Coverage status: reviewed, partial; no conformance claim.**

## Requirement-to-code mapping

The requirement descriptions below are summaries. Consult the supplied standard
for the exact component operations. The proposed scope here is DHIS2 object
sharing: owner, public access, user access and user-group access. It does not
establish coverage of every security attribute in the whole product.

| Standard key | Requirement summary | Implementing code / exact symbol | What the code demonstrates | Remaining condition |
| --- | --- | --- | --- | --- |
| **FMT_MSA.3.1** | Initial security attributes follow the selected access/information-flow policy and its selected default property. | [HibernateIdentifiableObjectStore.save(T, UserDetails, boolean)](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/common/hibernate/HibernateIdentifiableObjectStore.java#L121) | The ordinary creation path clears public/user/group sharing when `clearSharing` is true, sets an owner when missing, chooses public/private access using ACL authority and schema defaults, checks allowed public access, then persists. | Identify the SFP, object types and attributes in the ST; review every creation/import path and special default metadata. |
| **FMT_MSA.3.1** | Defaults have a defined security property. | [AccessStringHelper.DEFAULT / READ_WRITE](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/AccessStringHelper.java#L67); [DefaultAclService.resetSharing](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java#L523) | The base public-access default is `--------`; reset assigns that value and clears explicit user/group grants. It can select public read/write when the creator may make the object public and the schema's default is public. | This is **not** a uniform restrictive-default policy: an ST selecting restrictive defaults everywhere would need further analysis/remediation. The standard allows the property to be selected. |
| **FMT_MSA.3.1** | Default policy is selected for relevant object types. | [DefaultAclService.defaultPrivate / defaultPublic](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java#L447); [DashboardSchemaDescriptor.getSchema](../../../../dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/DashboardSchemaDescriptor.java#L50) | `defaultPrivate` reads `Schema.isDefaultPrivate`; `defaultPublic` uses its inverse. Dashboard explicitly sets `defaultPrivate=true`. | Enumerate the schema settings for all evaluated object types; Dashboard is one concrete example, not proof for all types. |
| **FMT_MSA.3.2** | Identified authorized roles can supply alternative initial values at creation. | [DefaultMetadataImportService.preCreateBundleObject](../../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/DefaultMetadataImportService.java#L248); [HibernateIdentifiableObjectStore.save(..., clearSharing)](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/common/hibernate/HibernateIdentifiableObjectStore.java#L121) | Metadata preparation applies default sharing when incoming public access is empty. Explicit initial values can be retained; the store also supports retaining supplied sharing through the `clearSharing=false` path. Creation checks still apply to shareable classes. | Trace importer flags and all callers of the preserve-sharing path; supplying a value alone does not prove that every override is authorized. |
| **FMT_MSA.3.2** | Initial overrides are constrained by the assigned roles. | [SecurityCheck.check](../../../../dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/metadata/objectbundle/validation/SecurityCheck.java#L53); [DefaultAclService.verifySharing](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java#L557); [DefaultAclService.canMakeClassPublic / canMakeClassPrivate](../../../../dhis-2/dhis-services/dhis-service-acl/src/main/java/org/hisp/dhis/security/acl/DefaultAclService.java#L417); [HibernateIdentifiableObjectStore.checkPublicAccess](../../../../dhis-2/dhis-services/dhis-service-core/src/main/java/org/hisp/dhis/common/hibernate/HibernateIdentifiableObjectStore.java#L965) | Import validation checks create authority and sharing, unless the relevant bypass mode applies. ACL checks the schema's `CREATE_PUBLIC`/`CREATE_PRIVATE` authorities; the store rejects disallowed creation access. Dashboard defines `F_DASHBOARD_PUBLIC_ADD` and implicit private authority in its schema. | Complete the ST role assignment and verify privileged override/skip-sharing modes, API/import entry points and negative tests for each evaluated class. |

## Concrete code example

The default-selection logic in `DefaultAclService.resetSharing` includes:

```java
sharing.setPublicAccess(AccessStringHelper.DEFAULT);

if (canMakePublic(userDetails, object) && defaultPublic(object)) {
  sharing.setPublicAccess(AccessStringHelper.READ_WRITE);
}
sharing.resetUserAccesses();
sharing.resetUserGroupAccesses();
```

The related initial-import decision in
`DefaultMetadataImportService.preCreateBundleObject` is:

```java
if (StringUtils.isEmpty(object.getSharing().getPublicAccess())) {
  aclService.resetSharing(object, params.getUserDetails());
}
```

These are code trace points, not substitutes for the complete creation and
validation paths linked above. The named symbols are primary references; GitHub
line anchors describe this reviewed snapshot and can move after an upstream merge.

## Test trace

[Existing AclServiceTest](../../../../dhis-2/dhis-test-integration/src/test/java/org/hisp/dhis/security/acl/AclServiceTest.java)
contains these eight named cases, confirmed passed in the recorded Surefire XML:

| Test method | Supporting behavior |
| --- | --- |
| `testResetSharingPropsPrivate` | Restrictive public default and cleared user/group grants for a visualization without public-create authority. |
| `testResetSharingPropsPublic` | Public read/write default when the creator has the relevant authority. |
| `testResetSharingEventVisualizationPropsPrivate` | Private reset behavior for event visualizations. |
| `testResetSharingEventVisualizationPropsPublic` | Public reset behavior for authorized event-visualization creators. |
| `testBlockMakePublic` | Public sharing is refused without the relevant authority. |
| `testAllowMakePublic` | Public sharing is permitted for an authorized creator. |
| `testBlockDashboardPublic` | A dashboard cannot be made public by an unauthorized creator. |
| `testAllowDashboardPublic` | The corresponding authorized dashboard creation/sharing case. |

No new test execution is claimed for this documentation-only mapping. The
[verification record](../../common-criteria-verification.md) and
[machine-readable execution evidence](../../evidence/review-2026-11-03.json)
record the earlier `AclServiceTest` execution: 60 passed and one unrelated existing
null-owner case skipped. The eight named cases above were not skipped.

## Dependencies and assessment

The 2026 component depends on **FMT_MSA.1** (management of security attributes)
and **FMT_SMR.1** (security roles). Their code/evidence references appear in the
[component-ID index](../../common-criteria-component-code-index.md).

The code implements mechanisms relevant to both elements for object sharing.
Marking the component fully covered still requires the chosen SFP/default
property/authorized roles, dependency closure, and tests of all evaluated creation
paths. Do not interpret post-creation sharing management alone as proof of the
initial-value override required by FMT_MSA.3.2.
