# Bundled frontend fork sources

The Maven `dhis-web-server` module reads `apps-to-bundle.json` during
`prepare-package` (unless `skip.bundle.apps` is enabled). Its
`AppBundler` downloads GitHub **source archives** using
`https://codeload.github.com/<owner>/<repo>/zip/<ref>` and copies them
to `target/classes/static/dhis-web-apps/` for inclusion in the WAR.

Capture, Dashboard and Settings are redirected to the PardasHealth forks
at explicitly pinned commit SHAs (Capture `117c4b5e21dc5169c07250d25bc9e67e90eeb922`, Dashboard `2a865ad185dc1e1ad6376e0b48e9881207c49b24`, Settings `413654993aaa005f44635b88d5ba078508341000`). All other apps remain on their existing sources.

**Important:** This source-archive bundling path is not the same as
installing the deployable ZIP produced by `dhis2-platform`'s
`./dhis2 apps build` command. A successful source download or WAR
packaging step does not prove that the bundled frontend is executable,
that its UI/App Runtime dependencies come from the PardasHealth forks,
or that browser login and navigation work.

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
