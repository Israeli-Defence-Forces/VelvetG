from .model import *


def analyze(trace, intervals, transitions, window_ns=250_000_000):
    events=trace['events']; out=[]; patterns=[]
    index=EventIndex(events)
    attack_events=attacks(trace)
    for attack in attack_events:
        truncated_before=index.truncated
        intent=[e for e in index.window(attack,[13],window_ns) if e['kind']==13
                and e['sequence']<=attack['sequence']]
        use=index.window(attack,[14],window_ns)
        packets=[e for e in index.window(attack,[5],window_ns) if e['data'] in (16,17)]
        states=index.window(attack,[9,10,11],window_ns)
        blocks=[e for e in states if e['flags'] & 32]
        changes=[]
        for a,b in zip(states,states[1:]):
            if bool(a['flags']&32)!=bool(b['flags']&32):
                changes.append(dict(row=b['row'],blocking=bool(b['flags']&32),
                                    quality='OBSERVED',reason='BLOCK_STATE_CHANGED_BETWEEN_SPARSE_POINTS'))
        ordered=sorted([attack]+packets,key=lambda e:e['sequence'])
        out.append(dict(attack=ref(attack), intent=ref(intent[-1]) if intent else None,
                        use_intents=[ref(e) for e in use], blocking_observations=[ref(e) for e in blocks],
                        block_transitions=changes, packet_order=[dict(ref(e),action=e['data']) for e in ordered],
                        reasons=['SEND_ATTEMPTS_ONLY', 'USE_PLACEMENT_DOES_NOT_PROVE_SWORD_BLOCK']))
        if (use or blocks or packets) and index.truncated==truncated_before:
            patterns.append(dict(attack_row=attack['row'],quality='INFERRED',
                                 pattern='ATTACK_USE_PROXIMITY',reason='RETROSPECTIVE_SEQUENCE_NOT_ACCEPTED_BLOCK_HIT'))
    unmatched=[ref(e) for e in events if e['kind']==13 and not any(near(e,a,window_ns)
               and a['sequence']>=e['sequence'] for a in index.window(e,[5],window_ns) if a['data']==13)]
    return result(trace, dict(attacks=out,unmatched_attack_intents=unmatched), patterns,
                  unavailable('RAW_LEFT_RIGHT_PRESS_OVERLAP','SWING_CALLBACK','IMMUNITY_WINDOW','USE_MOVEMENT_SLOWDOWN'),
                  ['SPARSE_BLOCK_STATE_ONLY']+(['DENSE_WINDOWS_TRUNCATED_TO_64_PER_KIND_ABSTAIN'] if index.truncated else []))
