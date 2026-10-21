import copy
import unittest
from calibrate import calibrate, validate


def trials(n=100):
    return [dict(trial=f'{split}-{session}-{c}',session=f'{split}-{session}',split=split,context='controlled-ground',
                 candidate_ms=c,release_ms=c,tick_ms=50,client_event=f'client-{session}-{c}',
                 server_event=f'server-{session}-{c}',server_tick=session,server_sequence=c,
                 accepted=c==60,authoritative=True)
            for split in ('train','test') for session in range(n) for c in (40,60)]


class CalibrationTests(unittest.TestCase):
    def test_held_out_selection_and_context_only(self):
        r=calibrate(trials(),40,[40,60]);c=r['contexts']['controlled-ground']
        self.assertEqual(c['selected_on_train'],60)
        self.assertEqual(c['informational_candidate_ms'],60)
        self.assertIsNone(r['recommendation'])
        self.assertEqual(c['test'][60]['release_in_client_ticks_mean'],1.2)

    def test_small_sessions_abstain(self):
        c=calibrate(trials(2),40,[40,60])['contexts']['controlled-ground']
        self.assertIsNone(c['informational_candidate_ms'])
        self.assertIn('INSUFFICIENT_TRAIN_SESSIONS',c['reasons'])

    def test_non_authoritative_abstains(self):
        rows=trials();rows[0]['authoritative']=False
        self.assertEqual(calibrate(rows,40,[40,60])['quality'],'UNKNOWN')

    def test_leakage_duplicate_and_event_reuse_refused(self):
        rows=trials(2)
        for bad in [rows+[copy.deepcopy(rows[0])],rows+[dict(rows[0],trial='new',split='test')],
                    rows+[dict(rows[0],trial='new')]]:
            with self.assertRaises(ValueError): validate(bad)

    def test_no_holdout_selection_leak(self):
        rows=trials()
        for r in rows:
            if r['split']=='test':r['accepted']=r['candidate_ms']==40
        c=calibrate(rows,40,[40,60])['contexts']['controlled-ground']
        self.assertEqual(c['selected_on_train'],60)
        self.assertIsNone(c['informational_candidate_ms'])
        self.assertIn('HELD_OUT_UNCERTAINTY_OVERLAPS',c['reasons'])

    def test_nonfinite_and_unregistered_refused(self):
        rows=trials(2);rows[0]['tick_ms']=float('nan')
        with self.assertRaises(ValueError):validate(rows)
        with self.assertRaises(ValueError):calibrate(trials(2),40,[40,50])

    def test_empty_and_replay(self):
        self.assertEqual(calibrate([],40,[40,60])['quality'],'UNKNOWN')
        rows=trials(20)
        self.assertEqual(calibrate(rows,40,[40,60]),calibrate(list(reversed(rows)),40,[40,60]))
