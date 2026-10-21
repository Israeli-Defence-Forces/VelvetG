import json
from pathlib import Path
import tempfile
import unittest
from combat_analyzer import analyze,read_trace,COLUMNS3,human_summary,finite_json


def fixture(rows):
    lines=['# CombatIQ native experimental schema 3; fixture',','.join(COLUMNS3)]
    for i,(ns,kind,data,*extra) in enumerate(rows,1):
        e=dict(ns=ns,kind=kind,data=data,moveForward='NaN',packetClass='',tick=1,epoch=1,thread=1,
               sequence=i,entity=7,flags=7,x='NaN',y='NaN',z='NaN',connection=1,phase=0,packetId=0)
        if extra:e.update(extra[0])
        lines.append(','.join(str(e[k]) for k in COLUMNS3))
    with tempfile.TemporaryDirectory() as d:
        p=Path(d)/'x.csv';p.write_text('\n'.join(lines)+'\n# LOSS,0\n# FILTERED,0\n# END\n')
        return read_trace(p),analyze(p)


class Schema3Tests(unittest.TestCase):
    def test_packet_identity_links_owner_and_preserves_raw(self):
        t,r=fixture([(0,5,13,{'packetId':4,'connection':0}),(1,26,13,{'packetId':4}),
                     (2,23,13,{'packetId':4}),(3,24,13,{'packetId':4})])
        self.assertEqual(t['events'][0]['connection'],0)
        stages=r['packet_correspondence'][0]['stages']
        self.assertEqual([s['stage'] for s in stages],['SEND_ATTEMPT','SEND_API_ENTRY','DISPATCH_ENTRY','CHANNEL_WRITE_INVOCATION'])
        self.assertEqual(stages[0]['connection_evidence'],'SAME_PACKET_OWNER_BOUND_OBSERVATION')
        self.assertEqual(r['packet_correspondence'][0]['socket_write'],'NOT_RECORDED')

    def test_duplicate_packet_stage_is_not_a_second_feedback(self):
        t,r=fixture([(0,9,1),(1,6,19,{'packetId':2,'x':2}),(2,6,19,{'packetId':2,'x':2}),
                     (3,15,19,{'packetId':2,'x':2}),(4,5,13)])
        self.assertEqual(r['normalized_event_count'],4)
        h=r['analyzers']['hit_select']
        self.assertEqual(len(h['observations']),1)
        self.assertEqual(len(h['inferred'][0]['feedback_candidates']),1)

    def test_mouse_overlap_swing_and_movement_are_actual_fields(self):
        t,r=fixture([(0,19,1),(1,19,3),(2,20,0),(3,13,0),(4,5,13),
                     (5,14,0),(6,22,3,{'flags':39}),(7,3,1,{'moveForward':.2,'x':.1})])
        b=r['analyzers']['block_hit']['observations']['attacks'][0]
        self.assertEqual(len(b['input_overlap']),1)
        self.assertEqual(b['movement_during_sequence'][0]['forward'],.2)
        self.assertEqual(b['movement_during_sequence'][0]['strafe'],.1)
        self.assertTrue(b['blocking_observations'])

    def test_swing_without_packet_abstains_from_hit(self):
        t,r=fixture([(0,20,0)])
        self.assertEqual(len(r['analyzers']['block_hit']['observations']['unmatched_swings']),1)
        self.assertEqual(r['analyzers']['block_hit']['server_outcome']['quality'],'NOT_RECORDED')

    def test_feedback_reports_movement_without_attacker_attribution(self):
        t,r=fixture([(0,9,1),(1,6,18,{'packetId':5}),(2,17,0,{'y':.4,'x':.1,'z':0,'flags':7}),
                     (3,21,0,{'entity':8,'x':3,'y':4,'z':5}),(4,13,0),(5,5,13,{'entity':8})])
        h=r['analyzers']['hit_select']['inferred'][0]
        self.assertEqual(h['attack_intent']['row'],7)
        self.assertEqual(h['movement']['local_velocity'][0]['velocity'],[.1,.4,0])
        self.assertIn('ATTACKER_IDENTITY_UNKNOWN',h['reasons'])
        self.assertEqual(h['movement']['target_positions'][0]['reason'],'OBSERVED_TARGET_NOT_PROVEN_ATTACKER')

    def test_unknown_owner_does_not_generate_tap_candidate(self):
        t,r=fixture([(0,2,17),(1,1,17),(2,3,0),(3,2,17),(4,5,13,{'connection':0})])
        self.assertFalse(r['analyzers']['wtap']['inferred'])

    def test_human_and_json_output_abstain(self):
        t,r=fixture([(0,9,1)])
        self.assertIn('Server acceptance: NOT RECORDED',human_summary(r))
        json.dumps(finite_json(r),allow_nan=False)
        with self.assertRaises(ValueError):fixture([(0,5,13,{'packetId':-1})])

    def test_dense_feedback_abstains_and_context_is_bounded(self):
        rows=[(0,9,1)]
        rows.extend((i+1,6,18,{'packetId':i+1}) for i in range(300))
        rows.extend((i+301,17,0,{'x':0,'y':.1,'z':0}) for i in range(300))
        rows.append((1000,5,13))
        t,r=fixture(rows)
        h=r['analyzers']['hit_select']
        self.assertFalse(h['inferred'])
        self.assertIn('DENSE_FEEDBACK_WINDOWS_TRUNCATED_ABSTAIN',h['reasons'])
        from analytics.model import movement_context
        context=movement_context(t['events'],t['events'][-1])
        self.assertTrue(context['truncated'])
        self.assertLessEqual(len(context['local_velocity']),64)
