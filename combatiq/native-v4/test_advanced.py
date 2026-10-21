import tempfile
import unittest
from pathlib import Path
from combat_analyzer import COLUMNS, analyze, read_trace
from analytics import combo


def trace(rows, tail='# LOSS,0\n# END\n'):
    lines=['# CombatIQ native experimental schema 2; synthetic', ','.join(COLUMNS)]
    for i,(ns,kind,data,*extra) in enumerate(rows,1):
        row=dict(ns=ns,kind=kind,data=data,moveForward='NaN',packetClass='',tick=1,
                 epoch=1,thread=1,sequence=i,entity=-1,flags=7,x='NaN',y='NaN',z='NaN')
        if extra: row.update(extra[0])
        lines.append(','.join(str(row[k]) for k in COLUMNS))
    with tempfile.TemporaryDirectory() as d:
        p=Path(d)/'fixture.csv';p.write_text('\n'.join(lines)+'\n'+tail)
        return read_trace(p), analyze(p)


class AdvancedTests(unittest.TestCase):
    def test_wtap_evidence_is_independent(self):
        t,r=trace([(0,2,17),(10,1,17),(11,3,0,{'moveForward':0}),
                   (12,10,1),(13,11,0),(14,5,12),(20,2,17),
                   (21,10,0),(22,11,1),(23,5,13)])
        w=r['analyzers']['wtap']['observations'][0]
        self.assertTrue(w['candidate'])
        self.assertIn('LOCAL_SPRINT_STOP_OBSERVED',w['reasons'])
        self.assertIn('LOCAL_SPRINT_RESTART_OBSERVED',w['reasons'])
        self.assertIn('ATTACK_AFTER_TRANSITION',w['reasons'])
        self.assertEqual(w['movement_samples'],1)
        self.assertEqual(w['sample_delay_ms'],.000001)
        self.assertEqual(w['stops'][0]['sequence'],5)

    def test_navigation_release_abstains(self):
        t,r=trace([(0,2,17),(10,1,17),(11,3,0),(400_000_000,2,17)])
        self.assertFalse(r['analyzers']['wtap']['inferred'])

    def test_cross_thread_attack_not_tap_context(self):
        t,r=trace([(0,2,17),(1,1,17),(2,3,0),(3,2,17),(4,5,13,{'thread':2})])
        self.assertFalse(r['wtap']['intervals'][0]['candidate_for_tap_training'])
        self.assertFalse(r['analyzers']['wtap']['inferred'])

    def test_block_order_and_sparse_state(self):
        t,r=trace([(0,13,0),(1,9,1,{'flags':7}),(2,5,13),(3,14,0),
                   (4,5,17),(5,9,1,{'flags':39}),(6,5,16),(7,9,1,{'flags':7})])
        b=r['analyzers']['block_hit']
        self.assertEqual([e['action'] for e in b['observations']['attacks'][0]['packet_order']],[13,17,16])
        self.assertEqual([e['blocking'] for e in b['observations']['attacks'][0]['block_transitions']],[True,False])
        self.assertEqual(b['inferred'][0]['pattern'],'ATTACK_USE_PROXIMITY')
        self.assertEqual(b['unavailable']['RAW_LEFT_RIGHT_PRESS_OVERLAP']['quality'],'NOT_RECORDED')

    def test_attack_intent_without_packet_is_not_swing(self):
        t,r=trace([(0,13,0)])
        self.assertEqual(len(r['analyzers']['block_hit']['observations']['unmatched_attack_intents']),1)
        self.assertFalse(r['analyzers']['block_hit']['inferred'])

    def test_ambiguous_feedback_one_counter_output(self):
        t,r=trace([(0,9,1,{'entity':7}),(1,6,18,{'entity':7,'thread':2}),
                   (2,15,19,{'entity':7,'x':2}),(3,5,13,{'entity':9})])
        h=r['analyzers']['hit_select']
        self.assertEqual(len(h['inferred']),1)
        self.assertEqual(len(h['inferred'][0]['feedback_candidates']),2)
        self.assertIn('AMBIGUOUS_FEEDBACK_MATCH',h['inferred'][0]['reasons'])
        self.assertEqual(h['server_outcome']['quality'],'NOT_RECORDED')

    def test_nonlocal_velocity_not_counterattack(self):
        t,r=trace([(0,9,1,{'entity':7}),(1,6,18,{'entity':8}),(2,5,13)])
        self.assertFalse(r['analyzers']['hit_select']['inferred'])

    def test_cross_thread_time_is_not_causal_order(self):
        t,r=trace([(0,9,1,{'entity':7}),(1,6,18,{'entity':7,'thread':2}),(2,5,13)])
        self.assertEqual(r['analyzers']['hit_select']['inferred'][0]['order'],'CROSS_THREAD_TIME_PROXIMITY_ONLY')

    def test_combo_target_gap_and_correction(self):
        t,r=trace([(0,5,13,{'entity':7}),(100,5,13,{'entity':7}),
                   (200,5,13,{'entity':8}),(300,5,13,{'entity':8}),
                   (350,6,22),(400,5,13,{'entity':8}),(900_000_000,5,13,{'entity':8})])
        c=r['analyzers']['combo']['inferred']
        self.assertEqual([x['target_entity'] for x in c],[7,8])
        self.assertEqual([x['termination'] for x in c],['CONTEXT_THREAD_OR_TARGET_CHANGED','CORRECTION_OBSERVED'])
        self.assertEqual(len(combo.analyze(t,[],[],minimum_attempts=3)['inferred']),0)

    def test_invalid_trace_abstains_all_modules(self):
        t,r=trace([(0,13,0),(1,5,13),(2,14,0),(3,5,13)],'# LOSS,1\n# END\n')
        for a in r['analyzers'].values():
            self.assertEqual(a['quality'],'INVALID_TRACE');self.assertEqual(a['inferred'],[])

    def test_deterministic_replay(self):
        rows=[(0,5,13,{'entity':8}),(10,5,13,{'entity':8})]
        self.assertEqual(trace(rows)[1],trace(rows)[1])

    def test_entity_mismatch_does_not_pair_setter(self):
        t,r=trace([(0,10,1,{'entity':7}),(1,11,0,{'entity':8})])
        self.assertEqual(r['sprint']['transitions'][0]['reason'],'SETTER_HEAD_MISSING')

    def test_cross_thread_repress_abstains(self):
        t,r=trace([(0,2,17),(1,1,17),(2,3,0),(3,2,17,{'thread':2})])
        self.assertIn('INPUT_STREAM_CHANGED',r['wtap']['intervals'][0]['reasons'])

    def test_negative_timestamp_refused(self):
        with self.assertRaises(ValueError): trace([(-1,2,17)])

    def test_empty_trace_is_invalid(self):
        self.assertIn('NO_EVENTS',trace([])[0]['reasons'])

class DenseAndCounterTests(unittest.TestCase):
    def test_dense_window_abstains_and_marks_truncation(self):
        rows=[(i,9,1,{'flags':39}) for i in range(100)]+[(110,5,13),(111,14,0)]
        t,r=trace(rows)
        b=r['analyzers']['block_hit']
        self.assertIn('DENSE_WINDOWS_TRUNCATED_TO_64_PER_KIND_ABSTAIN',b['reasons'])
        self.assertEqual(b['inferred'],[])
        self.assertLessEqual(len(b['observations']['attacks'][0]['blocking_observations']),64)

    def test_filtered_is_separate_from_loss(self):
        t,r=trace([(0,2,17)],'# LOSS,0\n# FILTERED,17\n# END\n')
        self.assertTrue(t['complete'])
        self.assertEqual(r['recording']['packet_filtered'],17)
        self.assertEqual(r['recording']['event_loss'],0)
        for tail in ('# FILTERED,1\n# LOSS,0\n# END\n', '# LOSS,0\n# FILTERED,-1\n# END\n'):
            with self.assertRaises(ValueError):trace([(0,2,17)],tail)
