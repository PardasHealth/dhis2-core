#!/usr/bin/env python3
"""Render component trace/gap pages; does not assess or certify conformance."""
import hashlib
import json
import re
from pathlib import Path

SECURITY = Path(__file__).resolve().parents[1]
ROOT = SECURITY.parents[1]
FOLDER = SECURITY / 'common-criteria/component-traces'


def render_traces(rows):
    inventory = json.loads((SECURITY / 'evidence/component-trace-elements-2026-11-07.json').read_text())
    elements = inventory['elements']
    mappings = inventory['partial_element_mappings']
    assert set(elements) == {r['standard_key'] for r in rows}
    assert len({e for es in elements.values() for e in es}) == sum(map(len, elements.values()))
    for key, entry in mappings.items():
        assert key in elements[key.rsplit('.', 1)[0]]
        source = ROOT / entry['path']
        assert source.is_file()
        assert hashlib.sha256(source.read_bytes()).hexdigest() == entry["source_sha256"], (key, "Revalidate source mapping")
        assert 1 <= entry['line'] <= len(source.read_text().splitlines())
    output = {}
    retained = [r for r in rows if r['applicability'].startswith('Related')]
    excluded = [r for r in rows if r not in retained]
    index = ['# Component trace documents\n\n',
             f'{len(retained)} components retained for web-application review have individual pages. '
             f'{len(excluded)} conditional or excluded components are recorded below with their rationale. '
             'These are alternative candidates for an eventual Security Target, not a mandatory combined package.\n\n',
             '**Trace depth varies.** FMT_MSA.3 has a separately maintained element/code/test analysis. '
             'Other pages expose the existing code candidates, boundary references, test sources and gaps. '
             'A small set additionally contains source-reviewed partial element mappings. '
             'An unmapped element remains open; creating its page does not establish coverage. '
             'Part 3 requires developer, content and evaluator artifacts, not merely source code.\n\n',
             'Element identifiers come from the supplied 2026 standards. Requirement text is not reproduced. '
             'The element inventory does not select ST operations or close dependencies. '
             'Source line anchors refer to the reviewed checkout; revalidate after upstream updates.\n\n',
             'See the [component gap assessment](../../common-criteria-gap-assessment.md), '
             '[verification record](../../common-criteria-verification.md) and '
             '[scope](../web-application-scope.md). Regenerate with '
             '`python3 docs/security/tools/render_common_criteria_review.py`; add `--check` to validate.\n\n',
             '| Component | Trace depth |\n| --- | --- |\n']
    def source_links(paths):
        return '\n'.join(f'- [{Path(p).name}](../../../../{p})' for p in paths) or 'None established in this review.'
    for row in retained:
        key = row['standard_key']
        depth = ('Maintained element/code/test analysis' if key == 'FMT_MSA.3' else
                 'Partial element mappings; remaining elements open' if any(e in mappings for e in elements[key]) else
                 'Component code candidates; element mapping open' if row['code_files'] else
                 'Boundary/artifact references; element mapping open')
        index.append(f'| [{key}]({key}.md) — {row["component"]} | {depth} |\n')
        if key == 'FMT_MSA.3':
            assert (FOLDER / f'{key}.md').is_file()
            continue
        body = [f'# {key} — {row["component"]}\n\n',
                f'ISO/IEC 15408-{row["part"]}:2026, clause {row["clause"]}. '
                f'Trace update: {row.get("last_update_date", "2026-11-07")}; component disposition baseline: 2026-11-04.\n\n',
                f'**Status: {row["coverage_status"]}.** {depth}. No conformance claim.\n\n',
                '[All component pages](README.md) · '
                f'[Component disposition](../../common-criteria-gap-assessment.md#{key.lower().replace("_", "-").replace(".", "-")})\n\n',
                '## Web scope\n\n', row['applicability'] + '. ' + row['scope_reason'] + '\n\n',
                '## Code candidates\n\n', source_links(row['code_files']) + '\n\n']
        if row['code_symbols']:
            body += ['Symbols to inspect: ' + '; '.join(f'`{s}`' for s in row['code_symbols']) + '. '
                     'This component-level list is not a mapping to every element below.\n\n']
        body += ['## Element trace\n\n',
                 'Identifiers below inventory the obligations; consult the licensed standard for their exact text. '
                 'Partial mappings establish only the stated behavior and retain the stated limitations.\n\n']
        for element in elements[key]:
            body += [f'### {element}\n\n']
            if element in mappings:
                m = mappings[element]
                body += [f'**Partial source mapping.** [{Path(m["path"]).name}:{m["line"]}]'
                         f'(../../../../{m["path"]}#L{m["line"]}): {m["observation"]}\n\n',
                         '**Open:** ' + m['limit'] + '\n\n']
            else:
                kind = {'D':'Developer delivery', 'C':'Evidence content', 'E':'Evaluator activity'}.get(element[-1], 'Functional requirement')
                body += [f'**Unmapped — {kind}.** No individual implementation or artifact mapping has been established '
                         'for this element in this review. The component candidates above must not be treated as '
                         'proof for it. Complete the element-specific trace, applicable ST operations and verification.\n\n']
        body += ['## Evidence and limits\n\n', row['evidence_basis'] + '\n\n',
                 'Test sources (execution and scope are recorded separately in the '
                 '[verification record](../../common-criteria-verification.md)):\n\n',
                 source_links(row['test_files']) + '\n\n',
                 'Related boundary references, **not implementation coverage**:\n\n',
                 source_links(row['related_sources']) + '\n\n',
                 '## Work required before closure\n\n', row['gap_and_required_action'] + '\n\n',
                 'Proposed owner: ' + row['owner'] + '.\n\n',
                 'Solution plans: ' + ', '.join(f'[{g}](../web-gap-solutions.md#{g.lower()})' for g in row['gap_groups']) + '.\n\n',
                 'Select the component and its operations in the agreed ST, resolve dependencies, '
                 'map every selected element to exact code/configuration/process evidence, and verify positive '
                 'and negative cases in the frozen deployment. Assurance elements require the corresponding '
                 'developer and evaluator artifacts.\n']
        output[FOLDER / f'{key}.md'] = ''.join(body)
    index += ['\n## Conditional or excluded components\n\n',
              'No implementation page is asserted for these components. Reopen their scope if the deployment, '
              'features or evaluation approach changes.\n\n', '| Component | Disposition and reason |\n| --- | --- |\n']
    for r in excluded:
        reason = (r['applicability'] + '. ' + r['scope_reason']).replace('|', '\\|').replace('\n', ' ')
        index.append(f'| {r["standard_key"]} — {r["component"]} | {reason} |\n')
    output[FOLDER / 'README.md'] = ''.join(index)
    return output
