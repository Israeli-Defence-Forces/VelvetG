import tempfile
import unittest
from pathlib import Path
from combat_analyzer import *

class ReplayTests(unittest.TestCase):
    def trace(self, rows, tail='# LOSS,0\n# END\n'):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'x.csv'
            lines=['# CombatIQ native experimental schema 2; test',','.join(COLUMNS)]
            for i,r in enumerate(rows,1):
                ns,kind,data,*rest=r
                o=dict(ns=ns,kind=kind,data=data,moveForward='NaN',packetClass='',tick=1,
                       epoch=1,thread=1,sequence=i,entity=-1,flags=7,x='NaN',y='NaN',z='NaN')
                if rest:o.update(rest[0])
                lines.append(','.join(str(o[k]) for k in COLUMNS))
            p.write_text('\n'.join(lines)+'\n'+tail)
            return analyze(p)
    def test_setter_and_attempts(self):
        r=self.trace([(0,12,1),(1,2,17),(10,1,17),(11,3,0,{'moveForward':0}),
                      (12,10,1),(13,11,0),(14,5,12),(20,2,17),(21,10,0),(22,11,1),(23,5,11),(24,5,13)])
        w=r['wtap']['intervals'][0]
        self.assertIn('RELEASE_SAMPLED',w['reasons'])
        self.assertIn('LOCAL_SPRINT_STOP_OBSERVED',w['reasons'])
        self.assertIn('LOCAL_SPRINT_RESTART_OBSERVED',w['reasons'])
        self.assertEqual([t['data'] for t in r['sprint']['transitions']],[0,1])
        self.assertEqual(r['coverage']['server_acceptance'],ABSENT)
    def test_unsampled(self):
        r=self.trace([(0,3,1),(1,2,17),(2,1,17),(3,2,17),(4,3,1)])
        self.assertEqual(r['wtap']['unsampled'],1)
    def test_gap(self):
        r=self.trace([(0,3,1),(1,2,17),(2,1,17),(3,2,17),(200000000,3,1)])
        self.assertEqual(r['wtap']['unsampled'],0)
    def test_duplicate_down(self):
        r=self.trace([(0,2,17),(1,2,17),(2,1,17),(3,3,0),(4,2,17)])
        self.assertEqual(r['wtap']['sampled'],1)
        self.assertEqual(r['wtap']['repeated_down_observations'],1)
    def test_focus(self):
        r=self.trace([(0,2,17),(1,1,17),(2,12,0),(3,2,17)])
        self.assertEqual(r['wtap']['sampled'],0)
        self.assertEqual(r['wtap']['intervals'][0]['evidence'],UNKNOWN)
    def test_unknown_initial(self):
        r=self.trace([(0,1,17),(1,3,0),(2,2,17)])
        self.assertIn('INITIAL_BINDING_UNKNOWN',r['wtap']['intervals'][0]['reasons'])
    def test_loss(self):
        r=self.trace([(0,2,17),(1,1,17),(2,3,0),(3,2,17)],'# LOSS,1\n# END\n')
        self.assertEqual(r['wtap']['sampled'],0)
        self.assertEqual(r['recording']['evidence'],INVALID)
    def test_sequence_gap(self):
        r=self.trace([(0,2,17),(1,1,17,{'sequence':8}),(2,3,0),(3,2,17)])
        self.assertFalse(r['recording']['complete'])
    def test_no_false_cross_thread_order(self):
        r=self.trace([(0,10,1,{'thread':2}),(1,11,0,{'thread':2}),(2,5,13,{'thread':1})])
        self.assertEqual(r['sprint']['attack_order'][0]['reason'],'NO_NEARBY_SAME_THREAD_TRANSITION')
    def test_counterattack_not_acceptance(self):
        r=self.trace([(0,9,1,{'entity':77}),(1,6,19,{'entity':77,'x':2}),(2,5,13)])
        self.assertEqual(len(r['hit_select']['feedback_counterattack_pairs']),1)
        self.assertIsNone(r['calibration']['recommendation'])
    def test_legacy_context_and_actions_absent(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'x.csv';p.write_text('# CombatIQ native experimental schema 1; test\n'+','.join(LEGACY)+'\n0,5,0,NaN,in\n# LOSS,0\n# END\n')
            r=analyze(p)
            self.assertEqual(r['coverage']['packet_actions'],ABSENT)
            self.assertEqual(r['combo']['attack_interval_ms']['n'],0)
    def test_unfinished_and_caps(self):
        r=self.trace([(0,2,17),(1,1,17)],'')
        self.assertFalse(r['recording']['complete'])
        self.assertIn('RECORDING_ENDED_BEFORE_REPRESS',r['wtap']['intervals'][0]['reasons'])
        r=self.trace([],'# LOSS,0\n# CAPPED recorder limit\n# END\n')
        self.assertFalse(r['recording']['complete'])
    def test_schema_controls(self):
        for tail in ['# LOSS,0\n# LOSS,0\n# END\n','# LOSS,0\n1,2,3\n# END\n','# END\ntrailing\n']:
            with self.assertRaises(ValueError):self.trace([],tail)
    def test_interact_not_attack(self):
        r=self.trace([(0,5,14),(1,5,15)])
        self.assertFalse(r['sprint']['attack_order'])
    def test_attack_before_after(self):
        r=self.trace([(0,5,13),(1,10,1),(2,11,0),(3,5,13)])
        self.assertEqual([a['reason'] for a in r['sprint']['attack_order']],
                         ['ATTACK_BEFORE_SPRINT_TRANSITION','ATTACK_AFTER_SPRINT_TRANSITION'])
    def test_binding_transform_not_false_release(self):
        r=self.trace([(0,2,17),(1,1,17),(2,3,1,{'moveForward':0}),(3,2,17)])
        self.assertEqual(r['wtap']['sampled'],0)
        self.assertIn('BINDING_REMAINED_DOWN',r['wtap']['intervals'][0]['reasons'])
    def test_context_epoch(self):
        r=self.trace([(0,2,17),(1,1,17),(2,12,1,{'epoch':2}),(3,2,17,{'epoch':2})])
        self.assertEqual(r['wtap']['sampled'],0)
    def test_no_callbacks_is_unknown_not_positive_coverage(self):
        r=self.trace([])
        self.assertEqual(r['coverage']['packet_actions'],UNKNOWN)
        self.assertEqual(r['coverage']['local_sprint'],UNKNOWN)
    def test_json_finite(self):
        r=self.trace([(0,10,1),(1,11,0)])
        json.dumps(finite_json(r),allow_nan=False)

if __name__=='__main__':unittest.main()
