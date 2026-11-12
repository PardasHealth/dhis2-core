#!/usr/bin/env python3
"""Generate AFTA-only requirement/code/test/evidence traces; --check is read-only."""
import argparse
import csv
import hashlib
import io
import json
import re
from pathlib import Path

SECURITY = Path(__file__).resolve().parents[1]
ROOT = SECURITY.parents[1]
CHECKLIST = SECURITY / 'evidence/afta-selected-checklist-2026-11-07.json'
OUTPUT = SECURITY / 'common-criteria'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def local(path):
    resolved = (ROOT / path).resolve()
    require(resolved.is_relative_to(ROOT), f'Path outside checkout: {path}')
    require(resolved.is_file(), f'Missing artifact: {path}')
    return resolved


def evidence_index():
    records = {}
    for path in sorted((SECURITY / 'evidence').glob('afta-*.json')):
        data = json.loads(path.read_text())
        commit = data.get('implementation_commit')
        if not commit:
            continue
        require(re.fullmatch(r'[0-9a-f]{40}', commit), f'Invalid commit: {path}')
        require(commit not in records, f'Ambiguous evidence for {commit}')
        suites = data.get('suites', data.get('after_fix_suites', []))
        require(suites, f'No recorded test suites: {path}')
        tests = []
        drift = []
        unhashed = []
        hashes = data['source_sha256']
        for source, digest in hashes.items():
            current = local(source)
            require(re.fullmatch(r'[0-9a-f]{64}', digest), f'Invalid hash: {source}')
            if hashlib.sha256(current.read_bytes()).hexdigest() != digest:
                drift.append(source)
            if '/src/test/java/' in source and source.endswith('.java'):
                tests.append(source)
        # Bind recorded class names to the exact test sources, rather than
        # treating a similarly named file or the presence of JSON as a pass.
        for suite in suites:
            class_path = suite['class'].replace('.', '/') + '.java'
            if not any(t.endswith('/' + class_path) for t in tests):
                matches = [p for p in (ROOT / 'dhis-2').rglob(class_path.rsplit('/', 1)[-1])
                           if p.as_posix().endswith('/src/test/java/' + class_path)]
                require(len(matches) == 1, f'Missing/ambiguous test {suite["class"]}: {path}')
                source = matches[0].relative_to(ROOT).as_posix()
                tests.append(source)
                unhashed.append(source)
            require(suite['tests'] > suite['skipped'] and
                    suite['failures'] == suite['errors'] == 0,
                    f'Record does not report executed passing cases: {path}')
        records[commit] = {
            'record': path.relative_to(ROOT).as_posix(),
            'tests': tests,
            'drift': drift,
            'unhashed': unhashed,
        }
    return records


def build(data, records):
    require([r['row'] for r in data['rows']] == list(range(1, 83)),
            'Expected the original 82 AFTA rows in source order')
    result = []
    for row in data['rows']:
        code = []
        for key in row['references']:
            ref = data['references'][key]
            if ref['repository'] == 'core':
                path = local(ref['path'])
                require(hashlib.sha256(path.read_bytes()).hexdigest() == ref['source_sha256'],
                        f'Checklist source changed; review before refreshing: {key}')
                lines = path.read_text().splitlines()
                require(0 < ref['line'] <= len(lines) and
                        ref['symbol'] in lines[ref['line'] - 1], f'Stale source anchor: {key}')
                code.append(f'{ref["path"]}#L{ref["line"]}')
            else:
                require(ref['repository'] == 'platform', f'Unknown repository: {key}')
                commit = data['baseline_platform_commit']
                code.append(f'https://github.com/PardasHealth/dhis2-platform/blob/'
                            f'{commit}/{ref["path"]}#L{ref["line"]}')
        traces, tests, evidence, drift, unhashed = set(), set(), set(), set(), set()
        followups = list(row.get('implementation_followups', []))
        if row.get('implementation_followup'):
            followups.append(row['implementation_followup'])
        for followup in followups:
            require(followup['full_requirement_closed'] is False,
                    f'Closure requires separate acceptance review: row {row["row"]}')
            trace = (OUTPUT / followup['trace']).relative_to(ROOT).as_posix()
            local(trace)
            traces.add(trace)
            commit = followup['implementation_commit']
            require(commit in records, f'Missing dated evidence for {commit}')
            record = records[commit]
            tests.update(record['tests'])
            evidence.add(record['record'])
            drift.update(record['drift'])
            unhashed.update(record['unhashed'])
        if row['duplicate_of_row']:
            original = data['rows'][row['duplicate_of_row'] - 1]
            require(not row['counted'] and row['key'] == original['key'] and
                    row['status'] == original['status'], 'Inconsistent duplicate')
        require(row['gap_and_acceptance'].strip(), f'Missing acceptance: row {row["row"]}')
        result.append({
            'row': row['row'], 'requirement': row['key'],
            'optional': row['optional'], 'counted': row['counted'],
            'duplicate_of_row': row['duplicate_of_row'] or '',
            'status': row['status'], 'reference_role': row['reference_role'],
            'code_or_boundary': '; '.join(code),
            'implementation_traces': '; '.join(sorted(traces)),
            'recorded_test_sources': '; '.join(sorted(tests)),
            'dated_evidence': '; '.join(sorted(evidence)),
            'evidence_currency': ('Historical record; changed sources require rerun'
                                  if drift else 'Recorded hashes match; suite-only tests may lack hashes')
                                 if evidence else 'No item-specific execution record linked',
            'changed_since_evidence': '; '.join(sorted(drift)),
            'test_sources_without_recorded_hash': '; '.join(sorted(unhashed)),
            'remaining_acceptance': row['gap_and_acceptance'],
        })
    return result


def link(path):
    target = path if path.startswith('https://') else '../../../' + path
    label = path.rsplit('/', 1)[-1].split('#')[0]
    return f'[{label}]({target})'


def render(rows):
    stream = io.StringIO(newline='')
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]), lineterminator='\n')
    writer.writeheader()
    writer.writerows(rows)
    md = [
        '# Selected AFTA requirement traceability\n\n',
        'Scope: the original 82 checklist rows only, preserving the duplicate and three '
        'optional entries. Status and acceptance come from the selected checklist; '
        'this register does not change fit percentages or close requirements.\n\n',
        'Each row links code or boundary references, recorded implementation follow-ups, '
        'test sources and dated execution evidence where explicitly linked. '
        'An empty test/evidence field is an open mapping, not proof that no tests exist. '
        'Unresolved definitions retain boundary references only.\n\n',
        'Evidence is historical. Hash differences are listed in the '
        '[CSV](afta-requirement-traceability.csv) and require review and rerunning affected '
        'tests before asserting current behavior. The CSV also lists suite test sources '
        'whose hashes were not captured by the dated record. Matching hashes do not prove a new run, '
        'production acceptance or full clause coverage. Shared suites are not added '
        'together as independent passes.\n\n',
        'Regenerate: `python3 docs/security/tools/render_afta_traceability.py`. '
        'Validate without writing: add `--check`. The validator checks source hashes, '
        'anchors, test-class paths, dated record links and generated consistency.\n\n',
        '| Row / requirement | Status / reference role | Code or boundary | '
        'Follow-up / dated record | Recorded tests | Evidence currency | Remaining acceptance |\n',
        '| --- | --- | --- | --- | --- | --- | --- |\n',
    ]
    for row in rows:
        label = f'{row["row"]}. `{row["requirement"]}`'
        if row['optional']:
            label += ' (optional)'
        if row['duplicate_of_row']:
            label += f' (duplicate of {row["duplicate_of_row"]})'
        def links(field):
            return '<br>'.join(link(p) for p in row[field].split('; ') if p)
        fields = [label, row['status'] + '<br>' + row['reference_role'],
                  links('code_or_boundary'),
                  '<br>'.join(filter(None, [links('implementation_traces'), links('dated_evidence')])),
                  links('recorded_test_sources') or 'Not linked', row['evidence_currency'],
                  row['remaining_acceptance']]
        md.append('| ' + ' | '.join(f.replace('|', '\\|').replace('\n', ' ') for f in fields) + ' |\n')
    return {'afta-requirement-traceability.csv': stream.getvalue(),
            'afta-requirement-traceability.md': ''.join(md)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    rows = build(json.loads(CHECKLIST.read_text()), evidence_index())
    for name, content in render(rows).items():
        path = OUTPUT / name
        if args.check:
            require(path.is_file() and path.read_text() == content, f'Regenerate {path}')
        else:
            path.write_text(content)
    print(f'Validated {len(rows)} AFTA rows; '
          f'{sum(bool(r["dated_evidence"]) for r in rows)} have explicitly linked follow-up records. '
          'No full requirement closure asserted.')


if __name__ == '__main__':
    main()
