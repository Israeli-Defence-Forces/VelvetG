from collections import defaultdict
from .model import *


def analyze(trace, intervals, transitions, window_ns=500_000_000):
    events=trace['events'];local={e['epoch']:e['entity'] for e in events if e['kind'] in (9,10,11,17,18,22) and e['entity']>=0}
    grouped={};legacy=[]
    for e in events:
        if e['kind'] not in (6,15) or e['entity']!=local.get(e['epoch'],-999) or not(e['data']==18 or e['data']==19 and e['x']==2):continue
        if e.get('packetId',0):
            key=(e['epoch'],e['packetId']);old=grouped.get(key)
            if old is None or e['kind']==15:grouped[key]=e
        else:legacy.append(e)
    feedback=sorted(legacy+list(grouped.values()),key=lambda e:e['sequence'])
    observations=[dict(ref(e),kind='VELOCITY' if e['data']==18 else 'HURT_STATUS',
                       stage='HANDLER_RETURN' if e['kind']==15 else 'INGRESS_BEFORE_FILTER',source='UNKNOWN',quality='OBSERVED') for e in feedback]
    pairs=[];feedback_index=EventIndex(feedback,by_thread=False);intent_index=EventIndex(events);motion_index=EventIndex(events,by_thread=False)
    for attack in attacks(trace):
        before=feedback_index.truncated
        candidates=[e for e in feedback_index.window(attack,[6,15],window_ns) if 0<=attack['ns']-e['ns']<=window_ns]
        if not candidates:continue
        same=[e for e in candidates if same_stream(e,attack) and e['sequence']<attack['sequence']]
        intents=[e for e in intent_index.window(attack,[13],window_ns) if e['ns']<=attack['ns'] and e['sequence']<=attack['sequence']]
        if context_scoped(trace,attack) and feedback_index.truncated==before:
            pairs.append(dict(attack=ref(attack),attack_intent=ref(intents[-1]) if intents else None,
                              feedback_candidates=[ref(e) for e in candidates],quality='INFERRED',
                              order='SAME_THREAD_FEEDBACK_BEFORE_ATTACK' if same else 'CROSS_THREAD_TIME_PROXIMITY_ONLY',
                              reasons=['AMBIGUOUS_FEEDBACK_MATCH' if len(candidates)>1 else 'ATTACK_NEAR_FEEDBACK','ATTACKER_IDENTITY_UNKNOWN','VELOCITY_SOURCE_UNKNOWN'],
                              observed_delay_ms=(attack['ns']-candidates[-1]['ns'])/1e6,
                              movement=movement_context(events,candidates[-1],window_ns,motion_index)))
    absent=['ATTACKER_IDENTITY','AUTHORITATIVE_DAMAGE']
    if trace['schema']<3:absent+=['LOCAL_VELOCITY_RESPONSE','RELATIVE_TARGET_MOTION']
    return result(trace,observations,pairs,unavailable(*absent),['DENSE_FEEDBACK_WINDOWS_TRUNCATED_ABSTAIN'] if feedback_index.truncated else [])
