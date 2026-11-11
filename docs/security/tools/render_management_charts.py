#!/usr/bin/env python3
"""Plot documented review progress, never an invented conformance percentage.

Requires matplotlib. Counts come from the existing component disposition record.
"""
import json
import os
from collections import Counter
from pathlib import Path

os.environ.setdefault('MPLCONFIGDIR', '/tmp/dhis2-management-matplotlib')
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

SECURITY = Path(__file__).resolve().parents[1]
source = SECURITY / 'evidence/component-gap-assessment-2026-11-04.json'
rows = [r for r in json.loads(source.read_text())['components']
        if r['applicability'].startswith('Related')]
counts = Counter(r['coverage_status'] for r in rows)
# All recognized dispositions explicitly retain unresolved obligations. Abort if
# new statuses appear: a reviewer must assess closure semantics before plotting.
statuses = ['Partial implementation evidence', 'No direct implementation trace established',
            'Assurance evidence incomplete']
assert set(counts) == set(statuses), 'Review changed disposition/closure semantics'
values = [counts[s] for s in statuses]
total = len(rows)
assert total == 199 and values == [45, 82, 72], 'Update report narrative alongside charts'
colors = ['#2679A6', '#E7A43A', '#7967A8']
plt.rcParams.update({'font.family': 'DejaVu Sans', 'font.size': 12,
                     'svg.fonttype': 'none'})
fig, axes = plt.subplots(1, 2, figsize=(14, 7), facecolor='#F7F9FC')
fig.subplots_adjust(top=.72, bottom=.25, wspace=.38)
fig.text(.06, .92, 'DHIS2 Common Criteria review', fontsize=25, weight='bold', color='#172B46')
fig.text(.06, .86, 'Evidence and remaining work - not a certification score', fontsize=15, color='#43556C')
fig.text(.06, .81, '199 provisional review candidates | 62 excluded/conditional components omitted | 11 November 2026',
         fontsize=11, color='#52637A')
for ax in axes:
    ax.set_facecolor('#F7F9FC')
axes[0].pie(values, colors=colors, startangle=90, counterclock=False,
            wedgeprops={'width': .28, 'edgecolor': '#F7F9FC', 'linewidth': 3})
axes[0].text(0, .08, f'{values[0]/total:.1%}', ha='center', fontsize=29, weight='bold', color=colors[0])
axes[0].text(0, -.20, 'partial code evidence', ha='center', fontsize=11, color='#43556C')
axes[0].set_title('What evidence is documented?', fontsize=15, weight='bold', pad=14, color='#172B46')
labels = [f'Partial implementation evidence: {values[0]} ({values[0]/total:.1%})',
          f'No direct implementation trace: {values[1]} ({values[1]/total:.1%})',
          f'Incomplete assurance evidence: {values[2]} ({values[2]/total:.1%})']
axes[0].legend(labels, loc='upper center', bbox_to_anchor=(.5, -.03), frameon=False,
               fontsize=10, handlelength=1, labelspacing=.7)
axes[1].pie([total], colors=['#D08327'], startangle=90,
            wedgeprops={'width': .28, 'edgecolor': '#F7F9FC', 'linewidth': 3})
axes[1].text(0, .08, '100%', ha='center', fontsize=32, weight='bold', color='#A65E12')
axes[1].text(0, -.20, 'further work required', ha='center', fontsize=11, color='#43556C')
axes[1].set_title('What still needs closure?', fontsize=15, weight='bold', pad=14, color='#172B46')
axes[1].text(.5, -.08, '199 of 199 candidates still have open obligations\n0 of 199 documented fully closed (0%)',
             transform=axes[1].transAxes, ha='center', va='top', fontsize=11, color='#43556C', linespacing=1.7)
fig.text(.06, .11, 'These percentages measure documentation status, not product security, effort, time remaining or formal compliance.',
         fontsize=11, color='#172B46', weight='bold')
fig.text(.06, .035, 'Partial evidence is not a completed requirement. Final scope/assurance selection and independent evaluation remain open.\n'
         'Source: 4 November component assessment plus 11 November scoped external-broker TLS evidence.',
         fontsize=10, color='#52637A', linespacing=1.5)
folder = SECURITY / 'common-criteria/charts'
folder.mkdir(exist_ok=True)
for ext in ('png', 'svg', 'pdf'):
    fig.savefig(folder / f'management-gap-dashboard.{ext}', dpi=180, facecolor=fig.get_facecolor())
    if ext == 'svg':
        path = folder / f'management-gap-dashboard.{ext}'
        path.write_text('\n'.join(line.rstrip() for line in path.read_text().splitlines()) + '\n')
plt.close(fig)
print(f'Charts rendered: {total} open candidates, {counts[statuses[0]]} with partial implementation evidence.')
