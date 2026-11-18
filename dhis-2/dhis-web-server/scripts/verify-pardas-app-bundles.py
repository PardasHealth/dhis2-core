#!/usr/bin/env python3
"""Preflight locally built PardasHealth DHIS2 app ZIPs before Maven packaging."""
import argparse
import hashlib
import json
import pathlib
import sys
import zipfile

APPS = ("capture-app", "dashboard-app", "settings-app")


def inspect(path: pathlib.Path) -> dict:
    if not path.is_file():
        raise ValueError(f"Missing required app artifact: {path}")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    try:
        with zipfile.ZipFile(path) as archive:
            bad = archive.testzip()
            if bad is not None:
                raise ValueError(f"{path}: corrupt ZIP entry {bad}")
            entries = archive.namelist()
            if len(entries) != len(set(entries)):
                raise ValueError(f"{path}: duplicate ZIP entry names")
            if any(
                name.startswith("/") or "\\\\" in name or
                ".." in pathlib.PurePosixPath(name).parts
                for name in entries
            ):
                raise ValueError(f"{path}: unsafe ZIP entry path")
            if "manifest.webapp" not in entries:
                raise ValueError(f"{path}: missing root-level manifest.webapp")
            manifest = json.loads(archive.read("manifest.webapp"))
            if not isinstance(manifest, dict) or not all(
                isinstance(manifest.get(key), str) and manifest[key].strip()
                for key in ("name", "version")
            ):
                raise ValueError(f"{path}: manifest name/version must be nonempty strings")
            if not any(
                not name.endswith("/") and name.lower().endswith((".js", ".html"))
                for name in entries
            ):
                raise ValueError(f"{path}: missing built JS or HTML assets")
    except (zipfile.BadZipFile, json.JSONDecodeError, UnicodeDecodeError) as exc:
        raise ValueError(f"{path}: invalid app ZIP or manifest: {exc}") from exc
    return {
        "filename": path.name,
        "sha256": digest.hexdigest(),
        "manifest_name": manifest["name"],
        "manifest_version": manifest["version"],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle_dir", type=pathlib.Path)
    parser.add_argument("--output", type=pathlib.Path, help="Write checksum inventory JSON")
    args = parser.parse_args()
    try:
        inventory = [inspect(args.bundle_dir / f"{app}.zip") for app in APPS]
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    payload = json.dumps({"artifacts": inventory}, indent=2) + "\n"
    if args.output:
        args.output.write_text(payload, encoding="utf-8")
    print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
