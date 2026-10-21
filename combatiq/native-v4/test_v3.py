import tempfile
import unittest
from pathlib import Path
from analyze_native import analyze

H = '# CombatIQ native experimental schema 1; packet hooks are NOT server acceptance or actual socket writes\nns,kind,data,moveForward,packetClass\n'
class RecordingTests(unittest.TestCase):
    def a(self, events, tail='# LOSS,0\n# END\n'):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/'a.csv'; p.write_text(H+events+tail)
            return analyze(p)
    def test_missing_sampler_is_not_false_missed(self):
        x = self.a('0,2,17,NaN,\n10,1,17,NaN,\n20,2,17,NaN,\n')
        self.assertEqual(x['unsampled_forward_releases'], 0)
        self.assertEqual(x['releases_indeterminate_missing_sampling_coverage'], 1)
    def test_sampled_release(self):
        x = self.a('0,2,17,NaN,\n10,1,17,NaN,\n15,3,0,0.0,\n20,2,17,NaN,\n')
        self.assertEqual(x['sampled_forward_releases'], 1)
    def test_bracketed_release_no_sample(self):
        x = self.a('0,3,1,1.0,\n5,2,17,NaN,\n20,1,17,NaN,\n25,2,17,NaN,\n50,3,1,1.0,\n')
        self.assertEqual(x['unsampled_forward_releases'], 1)
    def test_gap_prevents_false_missed(self):
        x = self.a('0,3,1,1.0,\n10,2,17,NaN,\n200000000,1,17,NaN,\n210000000,2,17,NaN,\n300000000,3,1,1.0,\n')
        self.assertEqual(x['unsampled_forward_releases'], 0)
        self.assertEqual(x['releases_indeterminate_missing_sampling_coverage'], 1)
    def test_binding_disagrees(self):
        x = self.a('0,2,17,NaN,\n10,1,17,NaN,\n15,3,1,1.0,\n20,2,17,NaN,\n')
        self.assertEqual(x['releases_binding_state_disagreed'], 1)
    def test_unknown_initial(self):
        x = self.a('10,1,17,NaN,\n20,2,17,NaN,\n')
        self.assertEqual(x['unclassified_initial_release'], 1)
    def test_incomplete_retracts_classification(self):
        x = self.a('0,2,17,NaN,\n10,1,17,NaN,\n15,3,0,0.0,\n20,2,17,NaN,\n','')
        self.assertFalse(x['log_complete'])
        self.assertEqual(x['sampled_forward_releases'], 0)
        self.assertEqual(x['preliminary_sampled_forward_releases'], 1)
    def test_hook_probe_and_error(self):
        x = self.a('0,7,1,NaN,\n5,8,1,NaN,NoSuchFieldException\n')
        self.assertTrue(x['movement_hook_probe_seen'])
        self.assertEqual(x['movement_hook_errors'][0]['error'], 'NoSuchFieldException')
    def test_real_csv(self):
        p = Path('/mnt/data/combatiq-native-20261021-002713-496.csv')
        if not p.exists(): self.skipTest('Historical recording not attached to this test environment')
        if p.exists():
            x = analyze(p)
            self.assertTrue(x['log_complete'])
            self.assertEqual(x['releases_indeterminate_missing_sampling_coverage'], 27)
            self.assertEqual(x['event_loss'], 0)
if __name__=='__main__': unittest.main()
