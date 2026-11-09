#!/usr/bin/env python3
"""Render fork review documents from manually assessed component dispositions.

This validates document consistency, not ISO/IEC 15408 conformance. No third-party
packages or licensed standard text are required to render the checked-in data.
"""

import argparse
import csv
import io
import json
import re
from collections import Counter
from pathlib import Path

import sys

sys.dont_write_bytecode = True
from render_component_traces import render_traces


SECURITY = Path(__file__).resolve().parents[1]
ROOT = SECURITY.parents[1]
INPUT = SECURITY / "evidence/component-gap-assessment-2026-11-04.json"


def anchor(key):
    return key.lower().replace("_", "-").replace(".", "-")


def cell(value):
    return str(value).replace("|", "\\|").replace("\n", " ")


def links(paths):
    return "<br>".join(f"[{Path(p).name}](../../{p})" for p in paths)


def render(data):
    rows = data["components"]
    solution_plan = (SECURITY / "common-criteria/web-gap-solutions.md").read_text()
    solution_ids = set(re.findall(r'id="(g-\d{2}|w-\d{2})"', solution_plan))
    assert {f"g-{n:02d}" for n in range(1, 16)} <= solution_ids
    assert {f"w-{n:02d}" for n in range(1, 11)} <= solution_ids
    assert len(rows) == 261
    assert Counter(r["part"] for r in rows) == {2: 155, 3: 106}
    assert len({r["standard_key"] for r in rows}) == len(rows)
    required = (
        "component", "clause", "review_depth", "coverage_status", "applicability", "scope_reason",
        "evidence_basis", "gap_and_required_action", "owner", "gap_groups",
    )
    for row in rows:
        key = row["standard_key"]
        assert re.fullmatch(r"[FA][A-Z]{2}_[A-Z]{3,8}\.\d+", key), key
        assert (key.startswith("F")) == (row["part"] == 2), key
        assert re.fullmatch(r"\d+\.\d+\.\d+", row["clause"]), key
        assert all(row.get(k) for k in required), key
        assert row["coverage_status"] in {
            "Partial implementation evidence", "No direct implementation trace established",
            "Assurance evidence incomplete", "Conditional scope; evidence missing",
        }, key
        for path in row["code_files"] + row["related_sources"] + row["test_files"]:
            assert (ROOT / path).is_file(), (key, path)
        assert all(re.fullmatch(r"G-\d{2}", g) for g in row["gap_groups"]), key
        assert all(g.lower() in solution_ids for g in row["gap_groups"]), key

    introduction = (
        "Reviewed baseline: DHIS2-Core `2.44-SNAPSHOT`, `07d4f3f7ea` plus the recorded "
        "security patches. Original disposition date: 2026-11-04. "
        f"Latest follow-up: {data.get('last_update_date', data['review_date'])}.\n\n"
        "**261 component IDs have a baseline gap disposition: 155 from Part 2 and 106 "
        "from Part 3. This is component-level gap triage, not a completed element-by-element "
        "evaluation or a conformance/certification claim.** Applicability, assignments, "
        "selections, refinements and dependency closure remain subject to an agreed Security "
        "Target (ST), Protection Profile and assurance package. Higher levels, alternative "
        "approaches, PP/Module work and composition requirements are not all simultaneously "
        "mandatory. No component is silently excluded.\n\n"
        "The supplied 2026 editions provide identifiers, short titles and clause references. "
        "Licensed requirement text is not reproduced. Part 1 supplies the evaluation model "
        "and ST framework; see the [Part 1 scope](common-criteria-traceability.md#part-1-evaluation-model-and-boundary).\n\n"
        "Scope follows the user's web-application direction: see the "
        "[scope and exclusion rationale](common-criteria/web-application-scope.md). "
        "Every row records a scope disposition and its reason. An exclusion does not mean "
        "the component is impossible for web applications; it records this evaluation boundary "
        "or a specialized feature not selected in the baseline profile.\n\n"
        "A **code candidate** is an identified implementation path with incomplete coverage. "
        "A **related boundary reference** is context to inspect, not a claim that it implements "
        "the component. **No direct implementation trace established** means this review has "
        "not established one; it does not assert the feature is absent everywhere. Test-source "
        "links do not prove execution. The [verification record](common-criteria-verification.md) "
        "contains dated test results, build and browser limits.\n\n"
        "The earlier inventory omitted the five `*_COMP.1` assurance components because its "
        "family-name parser accepted only three letters. They are included here. The former "
        "256-row inventory and its unreviewed placeholders are superseded by these explicit "
        "gap dispositions; full implementation/element coverage remains open.\n\n"
    )
    index = ["# Common Criteria 2026 — component ID to code index\n\n", introduction,
             "Search for an exact key such as `FMT_MSA.3`. See its "
             "[element-level code and test trace](common-criteria/component-traces/FMT_MSA.3.md), "
             "the [full gap assessment](common-criteria-gap-assessment.md), "
             "[filterable CSV](common-criteria-component-code-index.csv) and "
             "[machine-readable dispositions](evidence/component-gap-assessment-2026-11-04.json).\n\n"]
    report = ["# Common Criteria 2026 — component gap assessment\n\n", introduction,
              "The table records the reviewed evidence basis, remaining obligation and proposed "
              "owner for each component. Gap IDs refer to the "
              "[prioritized register](common-criteria-review.md#gap-register). "
              "Each gap ID links to its [solution and acceptance plan](common-criteria/web-gap-solutions.md). "
              "Owners are proposed roles, not assignments to named people. Each row requires "
              "the component-specific artifact/behavior at its selected level; code and "
              "related references are in the [code index](common-criteria-component-code-index.md).\n\n"
              "## Review limits and closure\n\n"
              "The review considered source/configuration boundaries and the existing recorded "
              "tests. It did not inspect every instruction, endpoint, dependency or separately "
              "maintained application source. Negative findings mean evidence was not established "
              "in this reviewed package. They are not exhaustive absence proofs.\n\n"
              "To close a selected row: complete the ST operations and dependency rationale, "
              "trace each normative element to the exact code/configuration/process artifact, "
              "run positive and negative tests in the frozen evaluated configuration, and "
              "obtain required evaluator evidence. An excluded row needs an explicit scope "
              "rationale. Merely changing its status does not close it.\n\n"]
    last = None
    for row in rows:
        group = (row["part"], row["standard_key"].split("_")[0])
        if group != last:
            title = f"## Part {group[0]} — {group[1]}\n\n"
            index += [title, "| Key / clause | Component | Code candidate / symbol | Related boundary references | Test/evidence | Disposition |\n",
                      "| --- | --- | --- | --- | --- | --- |\n"]
            report += [title, "| Key / component | Web scope and reason | Evidence basis | Gap and required action if selected | Proposed owner / register |\n",
                       "| --- | --- | --- | --- | --- |\n"]
            last = group
        key = row["standard_key"]
        code = links(row["code_files"]) or "No direct trace established."
        if row["code_symbols"]:
            code += "<br>" + "; ".join(f"`{s}`" for s in row["code_symbols"])
        ev = links(row["test_files"])
        if ev:
            ev += "<br>"
        ev += row["evidence"]
        disposition = row["coverage_status"] + ". " + row["applicability"] + f"; [scope/gap/action](common-criteria-gap-assessment.md#{anchor(key)})."
        index.append("| " + " | ".join(cell(x) for x in (
            (f"[**{key}**](common-criteria/component-traces/{key}.md) / {row['clause']}"
             if row["applicability"].startswith("Related") else f"**{key}** / {row['clause']}"), row["component"], code,
            links(row["related_sources"]) or "See artifact/evaluation scope.", ev, disposition,
        )) + " |\n")
        report.append("| " + " | ".join(cell(x) for x in (
            f'<a id="{anchor(key)}"></a>**{key}** — {row["component"]}',
            row["applicability"] + ". " + row["scope_reason"],
            row["evidence_basis"], row["gap_and_required_action"],
            row["owner"] + "<br>" + ", ".join(
                f"[{g}](common-criteria/web-gap-solutions.md#{g.lower()})"
                for g in row["gap_groups"]
            ),
        )) + " |\n")
    fields = list(dict.fromkeys(k for row in rows for k in row))
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
    writer.writeheader()
    for row in rows:
        writer.writerow({k: "; ".join(v) if isinstance(v, list) else v for k, v in row.items()})
    return {
        **render_traces(rows),
        SECURITY / "common-criteria-component-code-index.md": "".join(index),
        SECURITY / "common-criteria-component-code-index.csv": stream.getvalue(),
        SECURITY / "common-criteria-gap-assessment.md": "".join(report),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if generated documents differ")
    args = parser.parse_args()
    for path, text in render(json.loads(INPUT.read_text())).items():
        if args.check:
            if not path.exists() or path.read_text() != text:
                raise SystemExit(f"Out of date: {path.relative_to(ROOT)}")
        else:
            path.write_text(text)
    print("Validated 261 unique component dispositions and generated document consistency.")


if __name__ == "__main__":
    main()
