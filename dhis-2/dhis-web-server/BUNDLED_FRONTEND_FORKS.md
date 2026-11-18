# Bundled frontend fork sources

The Maven `dhis-web-server` module reads `apps-to-bundle.json` during
`prepare-package` (unless `skip.bundle.apps` is enabled). For non-PardasHealth entries, `AppBundler` downloads GitHub **source archives**
using `https://codeload.github.com/<owner>/<repo>/zip/<ref>`.
For PardasHealth entries, it instead requires locally built, validated ZIPs
from `PARDAS_APP_BUNDLE_DIR`. All selected archives are copied into
`target/classes/static/dhis-web-apps/` for inclusion in the WAR.

Capture, Dashboard and Settings are redirected to the PardasHealth forks
at explicitly pinned commit SHAs (Capture `117c4b5e21dc5169c07250d25bc9e67e90eeb922`, Dashboard `2a865ad185dc1e1ad6376e0b48e9881207c49b24`, Settings `413654993aaa005f44635b88d5ba078508341000`). All other apps remain on their existing sources.

**Important:** The legacy source-archive path for other apps is not the same as
installing a deployable ZIP produced by `dhis2-platform`'s
`./dhis2 apps build` command. Even successful ZIP validation and WAR
packaging do not prove that the bundled frontend is executable,
that its UI/App Runtime dependencies come from the PardasHealth forks,
or that browser login and navigation work.

## Bundling built PardasHealth apps

Build Capture, Dashboard, and Settings separately from their pinned source
commits, using the intended UI and App Runtime dependencies. Place their
**deployable** archives in one directory with these exact filenames:

- `capture-app.zip`
- `dashboard-app.zip`
- `settings-app.zip`

Each ZIP must contain a root-level `manifest.webapp` with `name` and
`version`, plus compiled JavaScript or HTML assets. A GitHub source
archive is not acceptable.

The `dhis-web-server/pom.xml` configuration forwards the Maven property
`pardas.app.bundle.dir` into the AppBundler Java system property
`PARDAS_APP_BUNDLE_DIR`. From the `dhis-2` Maven reactor, run:

```bash
mvn -pl dhis-web-server -am package \
  -Dpardas.app.bundle.dir=/absolute/path/to/built-app-zips
```

For a standalone AppBundler invocation, pass
`-DPARDAS_APP_BUNDLE_DIR=/absolute/path/to/built-app-zips`.
Do not set `skip.bundle.apps=true` when verifying the bundled WAR.

Before running Maven, perform an offline artifact preflight from the
`dhis-2/dhis-web-server` directory:

```bash
python3 scripts/verify-pardas-app-bundles.py \
  /absolute/path/to/built-app-zips \
  --output /absolute/path/to/app-artifact-inventory.json
```

This checks all three ZIPs and records their SHA-256 checksums and manifest
identities. Retain the inventory as a build artifact. It does not verify
that the archives were built from the pinned Git commits or from approved
forked dependencies; that requires separate CI provenance checks.

The local files are validated and copied into the existing bundling
cache; other configured upstream apps continue to use their existing
download mechanism. The bundler does not currently attest that a ZIP
was built from the pinned commit or verify dependency provenance:
record SHA-256 checksums and source commit identities in CI, and
inspect the resulting WAR before deployment.

## Frontend dependency audit

The fork repositories currently declare these version ranges in their
`package.json` files (inspect the pinned commits and lockfiles before
treating these as release evidence):

| App | `@dhis2/app-runtime` | `@dhis2/ui` |
| --- | --- | --- |
| Capture | `^3.17.4` | `^10.17.0` |
| Dashboard | `^3.14.4` | `^10.7.4` |
| Settings | `^3.13.1` | `^10.1.11` |

These are semver ranges, not verified resolved versions or evidence that
the applications use PardasHealth forked dependencies. For each pinned
application commit, inspect its lockfile and build logs, record the
resolved dependency versions and package origins, and retain the built
ZIP's SHA-256 checksum. Reject artifacts that do not match the approved
dependency provenance. The bundler's current manifest and asset checks
do **not** enforce this provenance.

Before promoting the updated WAR:

1. Verify each fork source ref exists and review/pin its commit.
2. Build the embedded WAR with app bundling enabled; inspect the
   `static/dhis-web-apps` contents and `apps-bundle.json`.
3. Confirm the runtime recognizes and loads Capture, Dashboard and Settings.
4. Rebuild the applications with explicitly pinned forked UI and
   App Runtime dependencies; verify resolved dependency provenance.
5. Test browser login, app navigation, localization, RTL/Jalali behavior
   and rollback on a non-production instance.

The list change does not deploy apps or modify a running DHIS2 server.
