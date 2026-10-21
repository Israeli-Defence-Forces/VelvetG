"""Shared schema-neutral evidence helpers. All times are client observations."""
from enum import Enum


class Quality(str, Enum):
    OBSERVED = 'OBSERVED'
    INFERRED = 'INFERRED'
    UNKNOWN = 'UNKNOWN'
    NOT_RECORDED = 'NOT_RECORDED'
    CONTRADICTORY = 'CONTRADICTORY'
    INVALID_TRACE = 'INVALID_TRACE'


def evidence(value=None, quality=Quality.UNKNOWN, *reasons):
    return dict(value=value, quality=quality.value, reasons=list(reasons))


def result(trace, observations, inferred, unavailable, reasons=()):
    return dict(quality=('OBSERVED' if trace['complete'] else 'INVALID_TRACE'),
                reasons=list(reasons) + trace['reasons'], observations=observations,
                inferred=inferred if trace['complete'] else [], unavailable=unavailable,
                server_outcome=evidence(None, Quality.NOT_RECORDED, 'NO_SERVER_INSTRUMENTATION'))


def same_stream(a, b):
    return a['epoch'] == b['epoch'] and a['thread'] == b['thread']


def near(a, b, window):
    return same_stream(a, b) and abs(a['ns'] - b['ns']) <= window


def attacks(trace):
    return [e for e in trace['events'] if trace['schema'] >= 2 and e['kind'] == 5 and e['data'] == 13]


def ref(e):
    return {k: e[k] for k in ('row', 'ns', 'epoch', 'thread', 'sequence', 'entity')}


def unavailable(*reasons):
    return {r: evidence(None, Quality.NOT_RECORDED, r) for r in reasons}


class EventIndex:
    """Bounded proximity queries. Dense ambiguous windows are explicitly truncated."""
    def __init__(self, events, by_thread=True):
        from collections import defaultdict
        self.groups=defaultdict(list);self.by_thread=by_thread
        for e in events:
            self.groups[(e['epoch'],e['thread'] if by_thread else None,e['kind'])].append(e)
        self.times={}
        for key,group in self.groups.items():
            group.sort(key=lambda e:(e['ns'],e['sequence']))
            self.times[key]=[e['ns'] for e in group]
        self.truncated=0

    def window(self,event,kinds,width,limit=64):
        from bisect import bisect_left,bisect_right
        output=[]
        for kind in kinds:
            key=(event['epoch'],event['thread'] if self.by_thread else None,kind)
            times=self.times.get(key,[]);group=self.groups.get(key,[])
            lo=bisect_left(times,event['ns']-width);hi=bisect_right(times,event['ns']+width)
            if hi-lo>limit:
                self.truncated+=1
                center=bisect_left(times,event['ns'])
                lo=max(lo,min(center-limit//2,hi-limit));hi=lo+limit
            output.extend(group[lo:hi])
        return sorted(output,key=lambda e:e['sequence'])


def context_scoped(trace,event):
    return trace['schema']<3 or event.get('connection',0)>0


def movement_context(events,event,window=500_000_000,index=None):
    index=index or EventIndex(events,by_thread=False);before=index.truncated
    selected=index.window(event,[17,18,21,22],window)
    local=[e for e in selected if e['kind'] in (17,18,22)]
    return dict(truncated=index.truncated!=before,local_velocity=[dict(ref(e),velocity=[e['x'],e['y'],e['z']]) for e in local if e['kind']==17],
                ground_observations=[dict(ref(e),grounded=bool(e['flags']&8)) for e in local],
                target_positions=[dict(ref(e),position=[e['x'],e['y'],e['z']],reason='OBSERVED_TARGET_NOT_PROVEN_ATTACKER') for e in selected if e['kind']==21])
