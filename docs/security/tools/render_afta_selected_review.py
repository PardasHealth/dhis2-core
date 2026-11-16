#!/usr/bin/env python3
"""Render the user's bounded AFTA checklist review, CSV and management charts.

Requires matplotlib for rendering charts. --check validates data, source anchors
and the generated Markdown/CSV without third-party packages.
"""
import argparse
import csv
import hashlib
import io
import json
import os
from datetime import date
from collections import Counter
from pathlib import Path

SECURITY = Path(__file__).resolve().parents[1]
ROOT = SECURITY.parents[1]
INPUT = SECURITY / 'evidence/afta-selected-checklist-2026-11-07.json'
FOLDER = SECURITY / 'common-criteria'
P = 'Partial evidence'
G = 'Gap / evidence not established'
D = 'Definition or edition unresolved'
STATUSES = [P, G, D]


def percent(n, total):
    return f'{100*n/total:.1f}%'


def implementation_progress(data):
    """Count linked, distinct implementation commits without treating rows as closed."""
    commits = {}
    for row in data['rows']:
        followups = list(row.get('implementation_followups', []))
        if row.get('implementation_followup'):
            followups.append(row['implementation_followup'])
        for followup in followups:
            commit = followup['implementation_commit']
            entry = commits.setdefault(commit, {
                'date': followup['date'], 'rows': {}, 'traces': set()})
            assert entry['date'] == followup['date'], commit
            entry['rows'][row['row']] = row
            entry['traces'].add(followup['trace'])
    ordered = sorted(commits.items(), key=lambda item: (item[1]['date'], item[0]))
    primary = set()
    optional = set()
    timeline = []
    for day in sorted({entry['date'] for entry in commits.values()}):
        todays = [entry for entry in commits.values() if entry['date'] == day]
        for entry in todays:
            for row in entry['rows'].values():
                if row['counted']:
                    (optional if row['optional'] else primary).add(row['row'])
        timeline.append((day, sum(entry['date'] <= day for entry in commits.values()), len(primary)))
    return ordered, timeline, primary, optional


def render(data):
    rows = data['rows']
    refs = data['references']
    changes, timeline, touched, optional_touched = implementation_progress(data)
    updated = date.fromisoformat(timeline[-1][0]).strftime('%-d %B %Y')
    assert [r['row'] for r in rows] == list(range(1, 83))
    unique = [r for r in rows if r['counted']]
    assert len(unique) == len({r['key'] for r in rows}) == 81
    assert {r['row'] for r in rows if r['optional']} == {63, 64, 66}
    assert [(r['row'], r['duplicate_of_row']) for r in rows if not r['counted']] == [(81, 80)]
    for r in rows:
        assert r['status'] in STATUSES
        assert r['topic'] and r['observation'] and r['gap_and_acceptance']
        assert all(k in refs for k in r['references'])
        if not r['counted']:
            original = rows[r['duplicate_of_row'] - 1]
            assert r['key'] == original['key'] and r['status'] == original['status']
    for key, r in refs.items():
        assert r['repository'] in {'core', 'platform'}
        if r['repository'] == 'core':
            path = ROOT / r['path']
            assert path.is_file()
            assert hashlib.sha256(path.read_bytes()).hexdigest() == r['source_sha256'], (key, 'revalidate changed code')
            assert r['symbol'] in path.read_text().splitlines()[r['line'] - 1], key
    primary = [r for r in unique if not r['optional']]
    optional = [r for r in unique if r['optional']]
    assert len(primary) == 78 and len(optional) == 3
    counts = Counter(r['status'] for r in primary)
    assert [counts[s] for s in STATUSES] == [43, 10, 25]
    md = ['# AFTA selected-checklist review and gap report\n\n',
          '**Date:** 7 November 2026. **Source identified by user:** AFTA; exact checklist title/version and full clause definitions not supplied.\n\n',
          '**Scope:** only the supplied 82 numbered entries. This is a separate, narrower work list; it does not replace or declare completion of the broader Common Criteria review. '
          'An official evaluation may require dependencies or assurance work beyond this list. Those need explicit agreement rather than silent expansion here.\n\n',
          'Implementation follow-up: **16 November 2026**. See the [token failure-boundary fix](afta-token-failure-boundary.md), [OIDC event UID fix](afta-oidc-event-uid-binding.md), [login success context fix](afta-login-success-context.md), [MFA failure-event fix](afta-mfa-failure-events.md), [email login challenge UID fix](afta-login-email-challenge-uid.md), [email disable challenge target fix](afta-email-challenge-identity.md), [TOTP provisioning URI fix](afta-totp-provisioning-uri.md), [current-user and MFA UID-binding fix](afta-current-user-uid-binding.md), [atomic failure-counter fix](afta-atomic-failure-counters.md), [OIDC event account-association fix](afta-oidc-event-account-association.md), [global session invalidation fix](afta-global-session-invalidation.md), [authentication address-source fix](afta-authentication-address-source.md), [API-token failure audit fix](afta-token-failure-audit.md), [remediation progress record](afta-remediation-progress.md), [token IP constraints](afta-token-ip-constraints.md), [audit-review paging and filters](afta-audit-review-paging.md), [PAT owner binding](afta-token-owner-binding.md) and [external broker TLS](afta-external-broker-tls.md). Broker-path evidence moves row 47 from a gap to partial evidence; no complete item is closed.\n\n',
          '## Manager summary\n\n',
          '**Demonstrated full fit: 0 of 78 (0%). Partial implementation evidence: 43 of 78 (55.1%). '
          'All 78 still need some work, definition or proof (100%).** These figures describe this checklist review; they do not mean the application is 0% secure, '
          'that 55.1% is certified, or that all development must be redone.\n\n',
          '![AFTA selected checklist evidence and open work](charts/afta-selected-dashboard.png)\n\n',
          '| Result for primary work list | Count | Percentage | Meaning |\n| --- | ---: | ---: | --- |\n',
          '| Fully demonstrated fit | 0 | 0.0% | No item has complete agreed AFTA definition/operations and sufficient end-to-end evidence to close it. |\n']
    meanings = {P: 'Relevant implementation is identified, but full requirement coverage or acceptance is incomplete.',
                G: 'The selected guarantee is not established by the reviewed evidence. This is not an exhaustive proof that code is absent.',
                D: 'The exact extension, iteration or edition must be obtained before a valid fit decision.'}
    for s in STATUSES:
        md.append(f'| {s} | {counts[s]} | {percent(counts[s], 78)} | {meanings[s]} |\n')
    md += ['| Still require work or evidence | 78 | 100.0% | Includes every partial, gap and unresolved item above; not an additional category. |\n\n',
           'The primary denominator is **78 unique entries not explicitly marked optional**, not an approved declaration that all 78 are mandatory. '
           'Three explicitly optional entries are shown separately: one has partial evidence and two need definitions. '
           'Including optional items gives **81 unique entries: 44 partial (54.3%), 10 gaps (12.3%) and 27 unresolved (33.3%); 0 fully closed**. '
           'The source has 82 rows because rows 80 and 81 repeat the same identifier.\n\n',
           'Do not compare these percentages directly with the earlier 199-component chart: this report counts selected **element/iteration identifiers**, '
           'while the earlier report counts broader components. Neither measures effort, time, cost or certification probability. '
           'No arbitrary half-credit or estimated compliance percentage is assigned to partial evidence.\n\n',
           f'## Implementation progress — updated {updated}\n\n',
           '![AFTA recorded implementation progress](charts/afta-implementation-progress.png)\n\n',
           f'**{len(changes)} distinct implementation commits** are linked to **{len(touched)}/78 primary entries '
           f'({percent(len(touched), 78)})**, plus **{len(optional_touched)}/3 optional entries**. '
           f'The other **{78-len(touched)} primary entries** have no item-specific implementation follow-up linked in this assessment. '
           'A linked follow-up means a bounded code change and dated execution record exist; it is not full implementation, '
           'a completed requirement, an effort percentage, or proof that unlinked entries lack code. '
           '**Full closure remains 0/78**, so the requirement-status chart above stays unchanged.\n\n',
           'The timeline uses recorded follow-up dates and cumulative distinct commits/primary entries. '
           'Several changes can address one entry, and one change can address several entries. '
           'Test counts from overlapping executions are not added. See '
           '[implementation progress and open work](afta-remediation-progress.md) and '
           '[methods, regression tests and exact source hashes](afta-code-traceability.md).\n\n',
           '### Applied changes and selected source rows\n\n',
           'Source row numbers identify entries in the original numbered checklist. For example, '
           '**29 (`FIA_AFL.1.1`)** means source entry 29, authentication failure handling element 1; '
           '29 is not a completion count or standards chapter. Changes sharing a date are listed by commit ID.\n\n',
           '| Recorded date | Selected source rows / requirement IDs | Implementation commit | Detailed change trace |\n',
           '| --- | --- | --- | --- |\n',
           '## What needs management attention\n\n',
           '1. **Confirm the AFTA checklist version and exact requirements.** Resolve extensions, crypto iterations, edition changes and the duplicated certificate identifier. '
           'This unblocks 25 primary items from a valid fit decision.\n',
           '2. **Make the selected production release work securely.** Retain artifact-integrity checks after the user-confirmed successful startup on 9 November; fix and test the CSRF-aware production browser login integration. '
           'Local CSRF-off development mode is explicitly excluded from security acceptance.\n',
           '3. **Close the important behavior gaps.** Prioritize audit retention/full-storage policy, encryption and MFA rules, access control, integrity/tamper response, reliable time, '
           'session history/fault tolerance and protected connections according to the selected requirements.\n',
           '4. **Collect item-specific proof.** Complete the defined positive/negative tests in the frozen production deployment, then obtain any required evaluator acceptance. '
           'Assign named owners and dates after the missing definitions and acceptance parameters are agreed.\n\n',
           '## Checklist normalization and definition issues\n\n',
           '- **Row 22:** supplied `ITC.2.2`; provisionally normalized to `FDP_ITC.2.2` from adjacent entries. Confirm against the source.\n',
           '- **Rows 80/81:** both say `FIA_X509_EXT.1.1/Rev`. Both original rows are preserved below; row 81 is counted only once with row 80. '
           'No different element number is invented for row 81.\n',
           '- **Rows 63, 64 and 66:** explicitly marked optional in the supplied text. Row 65 is not marked optional and remains in the primary list pending clarification.\n',
           '- **Rows 9–12:** AFTA storage labels/numbering do not align with the supplied ISO/IEC 15408-2:2026 storage family. '
           'For example, 2026 `FAU_STG.1.1` concerns storage location; `FAU_STG.1.2` is absent; deletion/modification protection is under `FAU_STG.2`; '
           'threshold actions and full-storage behavior appear under `FAU_STG.4` and `.5`. This is a comparison note, not an approved remapping.\n',
           '- **Row 64:** `FCS_CKM.4.1` is absent from the supplied 2026 inventory; key destruction is treated under `FCS_CKM.6`. '
           'Do not substitute identifiers without the AFTA source edition.\n',
           '- **Four `FCS_COP.1.1` iterations:** each is counted separately, but its operation/algorithm/key size/standard is unknown. '
           'Hashing, encryption and TLS are not assigned to iteration numbers by assumption.\n',
           '- **Extended requirements:** `FIA_PMG_EXT`, `FPT_TUD_EXT`, `FCS_HTTPS_EXT`, `FCS_TLSC_EXT`, `FCS_TLSS_EXT` and `FIA_X509_EXT` '
           'require their exact AFTA definitions/selections; the supplied base standards do not define these extensions. '
           'The same extension identifier can be used differently by different profiles.\n\n',
           '## Row-by-row review\n\n',
           'Each observation is a bounded source/evidence assessment, not an AFTA pass. '
           'For unresolved/gap rows, linked code is **related boundary context only**, not implementation coverage. '
           'All rows retain open acceptance work. Original Persian labels and row numbers are preserved in the '
           '[assessment data](../evidence/afta-selected-checklist-2026-11-07.json).\n\n',
           '| Source row / requirement | Result | Existing evidence or boundary | Gap and acceptance work | Code references |\n',
           '| --- | --- | --- | --- | --- |\n']
    progress_table = []
    for commit, change in changes:
        affected = ', '.join(
            f'{row["row"]} (`{row["key"]}`)' + (' [optional]' if row['optional'] else '')
            for row in sorted(change['rows'].values(), key=lambda row: row['row']))
        traces = '<br>'.join(f'[Change and evidence]({trace})' for trace in sorted(change['traces']))
        progress_table.append(
            f'| {change["date"]} | {affected} | '
            f'[{commit[:10]}](https://github.com/PardasHealth/dhis2-core/commit/{commit}) | {traces} |\n')
    marker = md.index('## What needs management attention\n\n')
    md[marker:marker] = progress_table + ['\n']
    def link(key):
        r = refs[key]
        target = (f'../../../{r["path"]}#L{r["line"]}' if r['repository'] == 'core' else
                  f'https://github.com/PardasHealth/dhis2-platform/blob/{data["baseline_platform_commit"]}/{r["path"]}#L{r["line"]}')
        return f'[{Path(r["path"]).name}:{r["line"]}]({target})'
    def cell(s):
        return s.replace('|', '\\|').replace('\n', ' ')
    for r in rows:
        notes = (' **Optional.**' if r['optional'] else '') + (f' **Duplicate of row {r["duplicate_of_row"]}; not counted again.**' if not r['counted'] else '')
        md.append('| ' + ' | '.join(cell(x) for x in (
            f'**{r["row"]}. `{r["key"]}`** — {r["topic"]}{notes}', r['status'], r['observation'],
            r['gap_and_acceptance'], '<br>'.join(link(k) for k in r['references']))) + ' |\n')
    md += ['\n## Evidence boundary and maintaining this report\n\n',
           f'Reviewed code references use core baseline `{data["baseline_core_commit"]}` and platform baseline `{data["baseline_platform_commit"]}`. '
           'Core links are relative to the checkout and include the dated implementation follow-up identified by source hashes; platform links are pinned to that commit. Core source hashes and line assertions are checked by the renderer; platform references are pinned to the reviewed commit. '
           'source changes require revalidation.\n\n',
           'This review reuses the recorded source analysis and targeted tests; it is not a fresh full-suite, penetration or independent evaluation. '
           'The core evidence records 262 passing cases/four existing skips across dated runs, not 262 tests of this selected checklist. '
           'Platform mode/configuration tests do not establish production browser acceptance. '
           'A local artifact checksum and Git revision pin are not equivalent to trusted-supplier signature verification. '
           'HTTPS support is not a pass for unknown TLS or X.509 extensions.\n\n',
           'See [core verification](../common-criteria-verification.md), [gap solutions](web-gap-solutions.md), '
           '[MFA/encryption findings](crypto-and-two-factor-gaps.md) and '
           '[platform evidence](https://github.com/PardasHealth/dhis2-platform/blob/main/docs/validation.md).\n\n',
           'Downloads: [CSV](afta-selected-requirements-review.csv), [PNG chart](charts/afta-selected-dashboard.png), '
           '[SVG chart](charts/afta-selected-dashboard.svg), [printable chart PDF](charts/afta-selected-dashboard.pdf). '
           'Implementation progress: [PNG](charts/afta-implementation-progress.png), [SVG](charts/afta-implementation-progress.svg), '
           '[PDF](charts/afta-implementation-progress.pdf).\n\n',
           'Edit the item-specific assessment JSON and regenerate with '
           '`python3 docs/security/tools/render_afta_selected_review.py` (Matplotlib required). '
           'Use `--check` to validate the data and generated text without Matplotlib. '
           'When definitions arrive, review each affected row, add exact code/test/evaluator evidence and revise counts; do not change statuses merely to improve a percentage.\n']
    fields = ['row', 'source_key', 'key', 'optional', 'counted', 'duplicate_of_row', 'status', 'topic',
              'observation', 'gap_and_acceptance', 'reference_role', 'references', 'normalization_note', 'supplied_line']
    stream = io.StringIO(newline='')
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator='\n')
    writer.writeheader()
    for r in rows:
        writer.writerow({k: '; '.join(r[k]) if isinstance(r[k], list) else r[k] for k in fields})
    return {FOLDER / 'afta-selected-requirements-review.md': ''.join(md),
            FOLDER / 'afta-selected-requirements-review.csv': stream.getvalue()}, counts


def charts(counts, data):
    os.environ.setdefault('MPLCONFIGDIR', '/tmp/dhis2-afta-matplotlib')
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    plt.rcParams.update({'font.family': 'DejaVu Sans', 'svg.fonttype': 'none'})
    changes, timeline, touched, optional_touched = implementation_progress(data)
    updated = date.fromisoformat(timeline[-1][0]).strftime('%-d %B %Y')
    fig, axes = plt.subplots(1, 2, figsize=(14, 7), facecolor='#F7F9FC')
    fig.subplots_adjust(top=.71, bottom=.25, wspace=.4)
    fig.text(.06, .92, 'AFTA selected-checklist review', fontsize=25, weight='bold', color='#172B46')
    fig.text(.06, .86, 'Provisional evidence coverage and open work - not an approval score', fontsize=14, color='#43556C')
    fig.text(.06, .81, f'78 unique primary entries | 3 optional entries reported separately | duplicate counted once | updated {updated}',
             fontsize=10.5, color='#52637A')
    colors = ['#2679A6', '#C95747', '#B48528']
    axes[0].pie([counts[s] for s in STATUSES], colors=colors, startangle=90, counterclock=False,
                wedgeprops={'width': .28, 'edgecolor': '#F7F9FC', 'linewidth': 3})
    axes[0].set_title('Where is implementation evidence?', fontsize=14, weight='bold', pad=16, color='#172B46')
    axes[0].text(0, .08, percent(counts[P], 78), ha='center', fontsize=30, weight='bold', color=colors[0])
    axes[0].text(0, -.20, 'partial evidence', ha='center', fontsize=12, color='#43556C')
    labels = [f'Partial evidence: {counts[P]}/78 ({percent(counts[P], 78)})',
              f'Gaps / evidence not established: {counts[G]}/78 ({percent(counts[G], 78)})',
              f'Need AFTA definition / edition: {counts[D]}/78 ({percent(counts[D], 78)})']
    axes[0].legend(labels, loc='upper center', bbox_to_anchor=(.5, -.03), frameon=False, fontsize=10, labelspacing=.8)
    axes[1].pie([78], colors=['#D08327'], startangle=90,
                wedgeprops={'width': .28, 'edgecolor': '#F7F9FC', 'linewidth': 3})
    axes[1].set_title('How much still needs closure?', fontsize=14, weight='bold', pad=16, color='#172B46')
    axes[1].text(0, .08, '100%', ha='center', fontsize=32, weight='bold', color='#A65E12')
    axes[1].text(0, -.20, 'further work / proof', ha='center', fontsize=12, color='#43556C')
    axes[1].text(.5, -.08, '78/78 have open obligations\n0/78 fully demonstrated fit (0%)',
                 transform=axes[1].transAxes, ha='center', va='top', fontsize=11, color='#43556C', linespacing=1.7)
    fig.text(.06, .105, 'Partial evidence is not full compliance. Open obligations are not an estimate of development effort.',
             fontsize=11, weight='bold', color='#172B46')
    fig.text(.06, .035, 'Source: user-supplied AFTA identifier list and item-specific source assessment; exact AFTA document/version remains unprovided.\n'
             'These figures use a different scope/unit from the earlier 199-component dashboard and must not be treated as a progress comparison.',
             fontsize=10, color='#52637A', linespacing=1.5)
    folder = FOLDER / 'charts'
    for ext in ('png', 'svg', 'pdf'):
        fig.savefig(folder / f'afta-selected-dashboard.{ext}', dpi=180, facecolor=fig.get_facecolor())
        if ext == 'svg':
            path = folder / f'afta-selected-dashboard.{ext}'
            path.write_text('\n'.join(line.rstrip() for line in path.read_text().splitlines()) + '\n')
    plt.close(fig)

    fig, axes = plt.subplots(1, 2, figsize=(14, 6.8), facecolor='#F7F9FC')
    fig.subplots_adjust(top=.74, bottom=.25, left=.18, right=.95, wspace=.55)
    fig.text(.06, .92, 'AFTA recorded implementation progress', fontsize=24, weight='bold', color='#172B46')
    fig.text(.06, .85, f'Updated {updated} | {len(changes)} distinct linked implementation commits | 0/78 requirements closed',
             fontsize=12, color='#43556C')
    for ax in axes:
        ax.set_facecolor('#F7F9FC')
        ax.spines[['top', 'right']].set_visible(False)
        ax.tick_params(colors='#43556C')
    values = [len(touched), 78-len(touched)]
    axes[0].barh(['Linked code follow-ups', 'No linked follow-up'], values, color=['#2679A6', '#AAB6C5'])
    axes[0].set_xlim(0, 84)
    axes[0].invert_yaxis()
    axes[0].set_xlabel('Unique primary checklist entries (78 total)')
    axes[0].set_title('Entries with recorded implementation work', fontsize=13, weight='bold', pad=15)
    for i, value in enumerate(values):
        axes[0].text(value+1, i, f'{value}/78 ({percent(value, 78)})', va='center', fontsize=10)
    labels = [date.fromisoformat(day).strftime('%-d %b') for day, _, _ in timeline]
    axes[1].plot(range(len(labels)), [count for _, count, _ in timeline], color='#2679A6', marker='o', linewidth=2.5)
    axes[1].set_xticks(range(len(labels)), labels, rotation=25)
    axes[1].set_ylim(0, len(changes)+4)
    axes[1].set_ylabel('Cumulative distinct linked commits')
    axes[1].set_title('Recorded changes over time', fontsize=13, weight='bold', pad=15)
    axes[1].grid(axis='y', alpha=.2)
    for i, (_, count, _) in enumerate(timeline):
        axes[1].annotate(str(count), (i, count), xytext=(0, 9), textcoords='offset points', ha='center')
    fig.text(.06, .10, f'{len(optional_touched)}/3 optional entries also have linked follow-ups. Row counts overlap across commits.', fontsize=11, color='#43556C')
    fig.text(.06, .035, 'Implementation activity is not full requirement closure or an estimate of effort. Source: linked follow-ups in the selected-checklist assessment.\n'
             'Full acceptance, deployment policies and unresolved AFTA definitions remain open; test totals are not added across executions.',
             fontsize=10, color='#52637A', linespacing=1.5)
    for ext in ('png', 'svg', 'pdf'):
        path = folder / f'afta-implementation-progress.{ext}'
        fig.savefig(path, dpi=180, facecolor=fig.get_facecolor())
        if ext == 'svg':
            path.write_text('\n'.join(line.rstrip() for line in path.read_text().splitlines()) + '\n')
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    data = json.loads(INPUT.read_text())
    output, counts = render(data)
    for path, content in output.items():
        if args.check:
            assert path.read_text() == content, f'Out of date: {path}'
        else:
            path.write_text(content)
    if not args.check:
        charts(counts, data)
    print('Validated 82 source rows, 81 unique items, 78 primary and 3 optional; 43 partial, 10 gaps, 25 unresolved primary items.')


if __name__ == '__main__':
    main()
