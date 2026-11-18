#!/usr/bin/env python3
"""Offline regression tests for the PardasHealth ZIP preflight."""
import importlib.util
import json
import pathlib
import tempfile
import unittest
import zipfile

SCRIPT = pathlib.Path(__file__).with_name("verify-pardas-app-bundles.py")
SPEC = importlib.util.spec_from_file_location("pardas_bundle_preflight", SCRIPT)
preflight = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(preflight)


class BundlePreflightTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = pathlib.Path(self.temp.name)
        self.archive = self.directory / "capture-app.zip"

    def write_zip(self, manifest=None, asset=True):
        with zipfile.ZipFile(self.archive, "w") as output:
            if manifest is not None:
                output.writestr("manifest.webapp", json.dumps(manifest))
            if asset:
                output.writestr("static/main.js", "console.log('ready')")

    def test_valid_archive_reports_digest_and_manifest(self):
        self.write_zip({"name": "Capture", "version": "1.0.0"})
        result = preflight.inspect(self.archive)
        self.assertEqual(result["manifest_name"], "Capture")
        self.assertEqual(result["manifest_version"], "1.0.0")
        self.assertEqual(len(result["sha256"]), 64)

    def test_missing_archive_fails(self):
        with self.assertRaisesRegex(ValueError, "Missing required"):
            preflight.inspect(self.archive)

    def test_source_archive_without_manifest_fails(self):
        self.write_zip()
        with self.assertRaisesRegex(ValueError, "manifest.webapp"):
            preflight.inspect(self.archive)

    def test_empty_manifest_name_fails(self):
        self.write_zip({"name": "", "version": "1.0.0"})
        with self.assertRaisesRegex(ValueError, "nonempty strings"):
            preflight.inspect(self.archive)

    def test_missing_compiled_assets_fails(self):
        self.write_zip({"name": "Capture", "version": "1.0.0"}, asset=False)
        with self.assertRaisesRegex(ValueError, "built JS or HTML"):
            preflight.inspect(self.archive)

    def test_invalid_zip_fails(self):
        self.archive.write_text("not a ZIP", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "invalid app ZIP"):
            preflight.inspect(self.archive)


if __name__ == "__main__":
    unittest.main()
