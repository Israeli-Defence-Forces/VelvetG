from .model import *


def analyze(trace, intervals, transitions, window_ns=500_000_000):
    events=trace['events']; local={e['epoch']:e['entity'] for e in events
                                if e['kind'] in (9,10,11) and e['entity']>=0}
    feedback=[e for e in events if e['kind'] in (6,15) and e['entity']==local.get(e['epoch'],-999)
              and (e['data']==18 or e['data']==19 and e['x']==2)]
    observations=[dict(ref(e),kind='VELOCITY' if e['data']==18 else 'HURT_STATUS',
                       stage='HANDLER_RETURN' if e['kind']==15 else 'INGRESS_BEFORE_FILTER',
                       source='UNKNOWN',quality='OBSERVED') for e in feedback]
    pairs=[]
    # One output per attack. Preserve all ambiguous matches instead of duplicating confidence.
    for attack in attacks(trace):
        candidates=[e for e in feedback if e['epoch']==attack['epoch'] and 0<=attack['ns']-e['ns']<=window_ns]
        if not candidates: continue
        same=[e for e in candidates if same_stream(e,attack) and e['sequence']<attack['sequence']]
        pairs.append(dict(attack=ref(attack),feedback_candidates=[ref(e) for e in candidates],
                          quality='INFERRED',
                          order='SAME_THREAD_FEEDBACK_BEFORE_ATTACK' if same else 'CROSS_THREAD_TIME_PROXIMITY_ONLY',
                          reasons=['AMBIGUOUS_FEEDBACK_MATCH' if len(candidates)>1 else 'ATTACK_NEAR_FEEDBACK',
                                   'ATTACKER_IDENTITY_UNKNOWN','VELOCITY_SOURCE_UNKNOWN'],
                          observed_delay_ms=(attack['ns']-candidates[-1]['ns'])/1e6))
    return result(trace,observations,pairs,
                  unavailable('ATTACKER_IDENTITY','AUTHORITATIVE_DAMAGE','LOCAL_VELOCITY_RESPONSE','RELATIVE_TARGET_MOTION'))
