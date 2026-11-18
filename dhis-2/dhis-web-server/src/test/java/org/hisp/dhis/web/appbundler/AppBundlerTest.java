/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.web.appbundler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the AppBundler class. */
class AppBundlerTest {

  @TempDir Path tempDir;

  private File appListFile;
  private String buildDir;
  private String downloadDir;
  private String artifactId;

  @BeforeEach
  void setUp() throws IOException {
    // Create a temporary app list file with some different examples of valid app URLs
    appListFile = tempDir.resolve("test-apps.json").toFile();
    new ObjectMapper()
        .writeValue(
            appListFile,
            Arrays.asList(
                "https://github.com/d2-ci/import-export-app",
                "https://github.com/d2-ci/data-quality-app#patch/2.42.0",
                "https://github.com/d2-ci/dashboard-app#v101.1.6",
                "https://github.com/d2-ci/aggregate-data-entry-app#v101.0.5"));

    // Set up build directory
    buildDir = tempDir.resolve("build").toString();
    downloadDir = tempDir.resolve("download").toString();
    artifactId = "dhis-web-apps";
  }

  @Test
  void testForkSourceZipRejectedBeforeDownload() throws IOException {
    new ObjectMapper()
        .writeValue(
            appListFile,
            Arrays.asList(
                "https://github.com/PardasHealth/capture-app#117c4b5e21dc5169c07250d25bc9e67e90eeb922"));

    AppBundler bundler =
        new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");

    IOException failure = assertThrows(IOException.class, bundler::execute);
    assertTrue(failure.getMessage().contains("source archive"));
    assertTrue(failure.getMessage().contains("manifest.webapp"));
  }

  @Test
  void testValidBuiltForkZipIsBundled() throws IOException {
    Path artifacts = Files.createDirectory(tempDir.resolve("built-apps"));
    createAppZip(artifacts.resolve("capture-app.zip"), true, true);
    new ObjectMapper().writeValue(
        appListFile, Arrays.asList("https://github.com/PardasHealth/capture-app#abc123"));
    String previous = System.getProperty("PARDAS_APP_BUNDLE_DIR");
    System.setProperty("PARDAS_APP_BUNDLE_DIR", artifacts.toString());
    try {
      AppBundler bundler =
          new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");
      bundler.execute();
      assertTrue(Files.isRegularFile(Path.of(buildDir, artifactId, "capture-app.zip")));
      assertTrue(Files.isRegularFile(Path.of(buildDir, artifactId, "apps-bundle.json")));
    } finally {
      restoreBundleDirectory(previous);
    }
  }

  @Test
  void testBuiltForkZipWithoutManifestIsRejected() throws IOException {
    Path artifacts = Files.createDirectory(tempDir.resolve("built-apps"));
    createAppZip(artifacts.resolve("capture-app.zip"), false, true);
    new ObjectMapper().writeValue(
        appListFile, Arrays.asList("https://github.com/PardasHealth/capture-app#abc123"));
    String previous = System.getProperty("PARDAS_APP_BUNDLE_DIR");
    System.setProperty("PARDAS_APP_BUNDLE_DIR", artifacts.toString());
    try {
      AppBundler bundler =
          new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");
      IOException failure = assertThrows(IOException.class, bundler::execute);
      assertTrue(failure.getMessage().contains("manifest.webapp"));
    } finally {
      restoreBundleDirectory(previous);
    }
  }

  @Test
  void testBuiltForkZipWithoutAssetsIsRejected() throws IOException {
    Path artifacts = Files.createDirectory(tempDir.resolve("built-apps"));
    createAppZip(artifacts.resolve("capture-app.zip"), true, false);
    new ObjectMapper().writeValue(
        appListFile, Arrays.asList("https://github.com/PardasHealth/capture-app#abc123"));
    String previous = System.getProperty("PARDAS_APP_BUNDLE_DIR");
    System.setProperty("PARDAS_APP_BUNDLE_DIR", artifacts.toString());
    try {
      AppBundler bundler =
          new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");
      IOException failure = assertThrows(IOException.class, bundler::execute);
      assertTrue(failure.getMessage().contains("no JavaScript or HTML assets"));
    } finally {
      restoreBundleDirectory(previous);
    }
  }

  private static void createAppZip(Path path, boolean manifest, boolean assets) throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
      if (manifest) {
        zip.putNextEntry(new ZipEntry("manifest.webapp"));
        zip.write("{\"name\":\"Capture\",\"version\":\"1.0.0\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      if (assets) {
        zip.putNextEntry(new ZipEntry("index.html"));
        zip.write("<html></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
  }

  private static void restoreBundleDirectory(String previous) {
    if (previous == null) {
      System.clearProperty("PARDAS_APP_BUNDLE_DIR");
    } else {
      System.setProperty("PARDAS_APP_BUNDLE_DIR", previous);
    }
  }

  @Test
  void testInvalidRequiredAppUrlFailsBeforeDownload() throws IOException {
    new ObjectMapper()
        .writeValue(appListFile, Arrays.asList("https://example.com/not-a-github-app"));

    AppBundler bundler =
        new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");

    IOException failure = assertThrows(IOException.class, bundler::execute);
    assertTrue(failure.getMessage().contains("Invalid required app URL"));
  }

  @Test
  void testEmptyAppListFailsBeforeDownload() throws IOException {
    new ObjectMapper().writeValue(appListFile, Arrays.asList());

    AppBundler bundler =
        new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");

    IOException failure = assertThrows(IOException.class, bundler::execute);
    assertTrue(failure.getMessage().contains("No application URLs configured"));
  }

  @Test
  void testAppBundlerCreatesCorrectStructure() throws IOException {
    // Execute the app bundler so we install the apps to our temp directory
    AppBundler bundler =
        new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");
    bundler.execute();

    Path buildDirPath = Path.of(buildDir).resolve(artifactId);
    assertTrue(Files.exists(buildDirPath), "Build directory should exist");

    Path downloadArtifactDir = Path.of(downloadDir).resolve(artifactId);
    assertTrue(Files.exists(downloadArtifactDir), "Artifact download directory should exist");

    Path etagsDir = downloadArtifactDir.resolve("etags");
    assertTrue(Files.exists(etagsDir), "Etags directory should exist");

    Path importExportAppZip = downloadArtifactDir.resolve("import-export-app.zip");
    assertTrue(Files.exists(importExportAppZip), "Import export app ZIP should exist");

    Path settingsAppZip = downloadArtifactDir.resolve("data-quality-app.zip");
    assertTrue(Files.exists(settingsAppZip), "Settings app ZIP should exist");

    Path dashboardAppZip = downloadArtifactDir.resolve("aggregate-data-entry-app.zip");
    assertTrue(Files.exists(dashboardAppZip), "Dashboard app ZIP should exist");

    Path dashboardAppZipInBuild = buildDirPath.resolve("dashboard-app.zip");
    assertTrue(Files.exists(dashboardAppZipInBuild), "Copied dashboard app ZIP should exist");

    Path bundleInfoFile = buildDirPath.resolve("apps-bundle.json");
    assertTrue(Files.exists(bundleInfoFile), "Bundle info file should exist");
  }
}
