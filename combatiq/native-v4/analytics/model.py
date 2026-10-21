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
    def __init__(self, events):
        from collections import defaultdict
        self.groups=defaultdict(list)
        for e in events:
            self.groups[(e['epoch'],e['thread'],e['kind'])].append(e)
        self.times={}
        for key,group in self.groups.items():
            group.sort(key=lambda e:(e['ns'],e['sequence']))
            self.times[key]=[e['ns'] for e in group]
        self.truncated=0

    def window(self,event,kinds,width,limit=64):
        from bisect import bisect_left,bisect_right
        output=[]
        for kind in kinds:
            key=(event['epoch'],event['thread'],kind)
            times=self.times.get(key,[]);group=self.groups.get(key,[])
            lo=bisect_left(times,event['ns']-width);hi=bisect_right(times,event['ns']+width)
            if hi-lo>limit:
                self.truncated+=1
                center=bisect_left(times,event['ns'])
                lo=max(lo,min(center-limit//2,hi-limit));hi=lo+limit
            output.extend(group[lo:hi])
        return sorted(output,key=lambda e:e['sequence'])
