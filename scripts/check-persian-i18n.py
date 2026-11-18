#!/usr/bin/env python3
"""Validate the Persian DHIS2 core resource bundle against its English source.

Run from any directory: python3 scripts/check-persian-i18n.py
Uses only the Python standard library.
"""
from __future__ import annotations

from collections import Counter
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
BUNDLE = ROOT / "dhis-2/dhis-services/dhis-service-core/src/main/resources"
SOURCE = BUNDLE / "i18n_global.properties"
TARGET = BUNDLE / "i18n_global_fa.properties"
PLACEHOLDER = re.compile(r"\\{\\d+\\}|%(?:\\d+\\$)?[sd]")
UNICODE_ESCAPE = re.compile(r"(?<!\\\\)\\\\u(?![0-9a-fA-F]{4})")


def read_entries(path: Path) -> tuple[dict[str, str], list[str]]:
    entries: dict[str, str] = {}
    errors: list[str] = []
    for number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")):
            continue
        if "=" not in line:
            errors.append(f"{path.name}:{number}: missing '=' separator")
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if not key:
            errors.append(f"{path.name}:{number}: empty key")
        if key in entries:
            errors.append(f"{path.name}:{number}: duplicate key {key}")
        if UNICODE_ESCAPE.search(value):
            errors.append(f"{path.name}:{number}: malformed Unicode escape")
        entries[key] = value
    return entries, errors


def main() -> int:
    source, errors = read_entries(SOURCE)
    target, target_errors = read_entries(TARGET)
    errors.extend(target_errors)
    for key in sorted(source.keys() - target.keys()):
        errors.append(f"missing Persian key: {key}")
    for key in sorted(target.keys() - source.keys()):
        errors.append(f"unexpected Persian key: {key}")
    for key in sorted(source.keys() & target.keys()):
        if Counter(PLACEHOLDER.findall(source[key])) != Counter(PLACEHOLDER.findall(target[key])):
            errors.append(f"placeholder mismatch: {key}")
        if key.startswith("format.") and source[key] != target[key]:
            errors.append(f"date format pattern changed: {key}")
    print(f"English keys: {len(source)}; Persian keys: {len(target)}; errors: {len(errors)}")
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
