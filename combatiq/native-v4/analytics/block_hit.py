from .model import *


def input_overlaps(events):
    state={};out=[]
    for e in events:
        key=(e['epoch'],e['thread']);held=state.setdefault(key,{})
        if e['kind']==19:
            button=e['data']//2;down=bool(e['data']%2)
            held[button]=down
            if held.get(0) and held.get(1):out.append(dict(ref(e),quality='OBSERVED',reason='LEFT_RIGHT_EVENT_PROXIES_BOTH_DOWN'))
    return out


def analyze(trace, intervals, transitions, window_ns=250_000_000):
    events=trace['events'];out=[];patterns=[];index=EventIndex(events);attack_events=attacks(trace)
    overlaps=input_overlaps(events)
    for attack in attack_events:
        before=index.truncated
        intent=[e for e in index.window(attack,[13],window_ns) if e['sequence']<=attack['sequence']]
        use=index.window(attack,[14],window_ns)
        packets=[e for e in index.window(attack,[5],window_ns) if e['data'] in (16,17)]
        states=index.window(attack,[9,10,11,22],window_ns)
        blocks=[e for e in states if e['flags']&32];changes=[]
        for a,b in zip(states,states[1:]):
            if bool(a['flags']&32)!=bool(b['flags']&32):changes.append(dict(row=b['row'],blocking=bool(b['flags']&32),quality='OBSERVED',reason='BLOCK_STATE_CHANGED_BETWEEN_OBSERVATIONS'))
        samples=index.window(attack,[3],window_ns)
        movement=[dict(ref(e),binding_down=e['data']==1,forward=e['moveForward'],strafe=e['x']) for e in samples]
        overlap=[e for e in overlaps if e['epoch']==attack['epoch'] and e['thread']==attack['thread'] and abs(e['ns']-attack['ns'])<=window_ns]
        ordered=sorted([attack]+packets,key=lambda e:e['sequence'])
        out.append(dict(attack=ref(attack),intent=ref(intent[-1]) if intent else None,use_intents=[ref(e) for e in use],
                        blocking_observations=[ref(e) for e in blocks],block_transitions=changes,
                        packet_order=[dict(ref(e),action=e['data']) for e in ordered],input_overlap=overlap,
                        movement_during_sequence=movement,truncated=index.truncated!=before,
                        reasons=['SEND_ATTEMPTS_ONLY','USE_PLACEMENT_DOES_NOT_PROVE_SWORD_BLOCK','MOVEMENT_SLOWDOWN_CAUSE_NOT_PROVEN']))
        if (use or blocks or packets) and index.truncated==before and context_scoped(trace,attack):
            patterns.append(dict(attack_row=attack['row'],quality='INFERRED',pattern='ATTACK_USE_PROXIMITY',reason='RETROSPECTIVE_SEQUENCE_NOT_ACCEPTED_BLOCK_HIT'))
    unmatched=[ref(e) for e in events if e['kind']==13 and not any(a['data']==13 and a['sequence']>=e['sequence'] for a in index.window(e,[5],window_ns))]
    swings=[dict(ref(e),reason='SWING_WITHOUT_OBSERVED_ATTACK_PACKET') for e in events if e['kind']==20 and not any(a['data']==13 for a in index.window(e,[5],window_ns))]
    absent=['IMMUNITY_WINDOW','CAUSAL_MOVEMENT_SLOWDOWN']
    if trace['schema']<3:absent+=['RAW_LEFT_RIGHT_PRESS_OVERLAP','SWING_CALLBACK']
    return result(trace,dict(attacks=out,unmatched_attack_intents=unmatched,unmatched_swings=swings),patterns,
                  unavailable(*absent),['OBSERVATIONAL_BLOCK_SEQUENCE']+(['DENSE_WINDOWS_TRUNCATED_TO_64_PER_KIND_ABSTAIN'] if index.truncated else []))
