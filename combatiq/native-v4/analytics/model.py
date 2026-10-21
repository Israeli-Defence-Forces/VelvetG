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
