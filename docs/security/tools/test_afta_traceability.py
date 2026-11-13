"""Negative checks for evidence links used to assess the selected AFTA list."""
import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import render_afta_traceability as trace


class AftaTraceabilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(trace.CHECKLIST.read_text())
        cls.records = trace.evidence_index()

    def test_original_rows_optional_and_duplicate_are_preserved(self):
        rows = trace.build(self.data, self.records)
        self.assertEqual(82, len(rows))
        self.assertEqual(81, sum(r['counted'] for r in rows))
        self.assertEqual([63, 64, 66], [r['row'] for r in rows if r['optional']])
        self.assertEqual(80, rows[80]['duplicate_of_row'])
        self.assertIn('afta-authentication-audit-records-2026-11-13.json', rows[0]['dated_evidence'])
        self.assertNotEqual('No item-specific execution record linked', rows[0]['evidence_currency'])

    def test_missing_execution_record_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Missing dated evidence'):
            trace.build(self.data, {})

    def test_closure_cannot_be_inferred_from_a_followup(self):
        data = copy.deepcopy(self.data)
        data['rows'][35]['implementation_followup']['full_requirement_closed'] = True
        with self.assertRaisesRegex(ValueError, 'Closure requires separate acceptance'):
            trace.build(data, self.records)

    def test_changed_source_anchor_is_rejected(self):
        data = copy.deepcopy(self.data)
        data['references']['auditgen']['line'] = 1
        with self.assertRaisesRegex(ValueError, 'Stale source anchor'):
            trace.build(data, self.records)

    def test_changed_current_source_requires_revalidation(self):
        data = copy.deepcopy(self.data)
        data['references']['auditgen']['source_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Checklist source changed'):
            trace.build(data, self.records)

    def test_old_records_are_preserved_but_drift_is_visible(self):
        records = copy.deepcopy(self.records)
        commit = self.data['rows'][35]['implementation_followup']['implementation_commit']
        records[commit]['drift'] = ['changed-source.java']
        row = trace.build(self.data, records)[35]
        self.assertIn('changed-source.java', row['changed_since_evidence'])
        self.assertIn('require rerun', row['evidence_currency'])
        self.assertIn('afta-remediation-2026-11-09.json', row['dated_evidence'])

    def test_sources_outside_checkout_are_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Path outside checkout'):
            trace.local('../outside.java')

    def test_failed_or_unexecuted_record_is_rejected(self):
        for counts in [dict(tests=1, skipped=1, failures=0, errors=0),
                       dict(tests=1, skipped=0, failures=1, errors=0)]:
            with self.subTest(counts=counts), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                evidence = root / 'evidence'
                evidence.mkdir()
                source = root / 'dhis-2/module/src/test/java/example/SecurityTest.java'
                source.parent.mkdir(parents=True)
                source.write_text('class SecurityTest {}')
                (evidence / 'afta-fixture.json').write_text(json.dumps({
                    'implementation_commit': 'a' * 40,
                    'source_sha256': {},
                    'suites': [{'class': 'example.SecurityTest', **counts}],
                }))
                with patch.object(trace, 'ROOT', root), patch.object(trace, 'SECURITY', root):
                    with self.assertRaisesRegex(ValueError, 'does not report executed passing'):
                        trace.evidence_index()


if __name__ == '__main__':
    unittest.main()
