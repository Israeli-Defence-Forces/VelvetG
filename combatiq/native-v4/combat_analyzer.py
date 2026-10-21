#!/usr/bin/env python3
"""Strict retrospective native schemas 1/2 replay. No server outcome inference."""
import argparse
import csv
import json
import math
from collections import Counter, defaultdict
from pathlib import Path

LEGACY = ['ns','kind','data','moveForward','packetClass']
COLUMNS = LEGACY + ['tick','epoch','thread','sequence','entity','flags','x','y','z']
OBSERVED = 'DIRECTLY_OBSERVED'
LIMITED = 'INFERRED_WITH_LIMITATIONS'
UNKNOWN = 'INDETERMINATE'
ABSENT = 'NOT_INSTRUMENTED'
INVALID = 'INVALID_RECORDING'
PACKETS = {11:'START_SPRINTING',12:'STOP_SPRINTING',13:'ATTACK',14:'INTERACT',15:'INTERACT_AT',
           16:'RELEASE_USE_ITEM',17:'USE_OR_PLACEMENT',18:'VELOCITY',19:'ENTITY_STATUS',
           20:'RELATIVE_ENTITY',21:'TELEPORT_ENTITY',22:'PLAYER_CORRECTION',
           23:'OTHER_ENTITY_ACTION',24:'OTHER_DIGGING'}


def distribution(values):
    v = sorted(values)
    def q(p):
        pos = (len(v)-1)*p
        lo = int(pos)
        return v[lo] + (v[min(lo+1,len(v)-1)]-v[lo])*(pos-lo)
    return dict(n=len(v), median=q(.5), p90=q(.9), p95=q(.95), p99=q(.99), maximum=v[-1]) if v else dict(n=0)


def read_trace(path):
    text = Path(path).read_text(encoding='utf-8').splitlines()
    if len(text) < 2:
        raise ValueError('Missing schema/header')
    if text[0].startswith('# CombatIQ native experimental schema 1;'):
        schema, header = 1, LEGACY
    elif text[0].startswith('# CombatIQ native experimental schema 2;'):
        schema, header = 2, COLUMNS
    else:
        raise ValueError('Unsupported native schema')
    if next(csv.reader([text[1]])) != header:
        raise ValueError('Unrecognized columns')
    events, loss, ended, capped, trailer = [], None, False, False, False
    reasons = []
    for i,line in enumerate(text[2:], 3):
        if ended:
            raise ValueError('Data after END')
        if line.startswith('# LOSS,'):
            if loss is not None:
                raise ValueError('Duplicate LOSS')
            loss = int(line.split(',')[1])
            if loss < 0 or len(line.split(',')) != 2:
                raise ValueError('Invalid LOSS')
            trailer = True
            continue
        if line.startswith('# CAPPED'):
            if not trailer or capped:
                raise ValueError('Unexpected CAPPED')
            capped = True
            continue
        if line == '# END':
            ended = True
            continue
        if line.startswith('#') or trailer or not line:
            raise ValueError('Unexpected control/data row at %d' % i)
        parts = next(csv.reader([line]))
        if len(parts) != len(header):
            if i == len(text) and not text[-1].startswith('#'):
                reasons.append('TRAILING_PARTIAL_ROW')
                break
            raise ValueError('Wrong column count at %d' % i)
        row = dict(zip(header, parts))
        try:
            for k in ('ns','kind','data') + (('tick','epoch','thread','sequence','entity','flags') if schema == 2 else ()):
                row[k] = int(row[k])
            for k in ('moveForward',) + (('x','y','z') if schema == 2 else ()):
                row[k] = float(row[k])
                if math.isinf(row[k]):
                    raise ValueError('Infinite value')
        except ValueError as ex:
            raise ValueError('Invalid numeric row at %d' % i) from ex
        if row['kind'] not in range(1,17 if schema == 2 else 9):
            raise ValueError('Unknown event kind')
        if schema == 1:
            row.update(tick=-1,epoch=0,thread=0,sequence=len(events)+1,entity=-1,flags=0,x=math.nan,y=math.nan,z=math.nan)
        else:
            if row['sequence'] != (events[-1]['sequence']+1 if events else 1):
                reasons.append('SEQUENCE_GAP')
            if events and row['ns'] < events[-1]['ns']:
                reasons.append('ENQUEUE_CLOCK_REVERSED')
            if row['kind'] in (5,6,15) and row['data'] not in PACKETS:
                reasons.append('UNKNOWN_PACKET_CODE')
        row['row'] = i
        events.append(row)
    if not ended: reasons.append('END_MISSING')
    if loss is None: reasons.append('LOSS_MISSING')
    elif loss: reasons.append('EVENT_LOSS')
    if capped: reasons.append('CAPPED')
    return dict(schema=schema,complete=not reasons,reasons=sorted(set(reasons)),loss=loss,capped=capped,events=events)


def sprint_transitions(events):
    """Actual setter pair changes are distinct from changes between sparse samples."""
    stacks = defaultdict(list)
    previous = {}
    transitions = []
    for e in events:
        key = (e['epoch'],e['thread'])
        if e['kind'] == 10:
            stacks[key].append(e)
        elif e['kind'] == 11:
            start = stacks[key].pop() if stacks[key] else None
            if start and start['data'] != e['data']:
                transitions.append(dict(e,previous=start['data'],evidence=OBSERVED,reason='SETTER_HEAD_RETURN_CHANGED'))
            elif not start:
                transitions.append(dict(e,previous=None,evidence=UNKNOWN,reason='SETTER_HEAD_MISSING'))
            previous[key] = e
        elif e['kind'] == 9:
            old = previous.get(key)
            if old and old['data'] != e['data']:
                transitions.append(dict(e,previous=old['data'],evidence=LIMITED,reason='BETWEEN_OBSERVATION_POINTS'))
            previous[key] = e
    return transitions


def releases(trace, transitions):
    output = []
    samples_by_epoch = defaultdict(list)
    for e in trace['events']:
        if e['kind'] == 3: samples_by_epoch[e['epoch']].append(e)
    pending, had_down, last_epoch = None, False, None
    repeated_down = 0
    errors = any(e['kind'] == 8 or (e['kind']==16 and e['data']==3) for e in trace['events'])
    def finish(end, reason=None):
        nonlocal pending
        if pending is None: return
        start = pending
        seen = [e for e in samples_by_epoch[start['epoch']]
                if start['sequence'] < e['sequence'] < (end['sequence'] if end else 2**63)
                and e['thread'] == start['thread']]
        flags = []
        if reason: state, flags = UNKNOWN, [reason]
        elif not had_down: state, flags = UNKNOWN, ['INITIAL_BINDING_UNKNOWN']
        elif not trace['complete']: state, flags = INVALID, trace['reasons'][:]
        elif errors: state, flags = UNKNOWN, ['MOVEMENT_HOOK_ERROR']
        elif any(e['data'] == 0 for e in seen): state, flags = OBSERVED, ['RELEASE_SAMPLED']
        elif seen: state, flags = UNKNOWN, ['BINDING_REMAINED_DOWN']
        else:
            all_samples = samples_by_epoch[start['epoch']]
            before = [e for e in all_samples if e['sequence'] < start['sequence'] and e['thread'] == start['thread']]
            after = [e for e in all_samples if e['sequence'] > end['sequence'] and e['thread'] == start['thread']]
            if before and after and 0 <= after[0]['ns']-before[-1]['ns'] <= 150_000_000:
                state, flags = OBSERVED, ['RELEASE_NOT_SAMPLED']
            else: state, flags = UNKNOWN, ['MISSING_SAMPLING_COVERAGE']
        cutoff = end['ns']+250_000_000 if end else start['ns']
        nearby = [t for t in transitions if t['epoch']==start['epoch'] and start['ns']<=t['ns']<=cutoff]
        direct = [t for t in nearby if t['evidence']==OBSERVED and t['thread']==start['thread']]
        if any(t['data']==0 for t in direct): flags.append('LOCAL_SPRINT_STOP_OBSERVED')
        if any(t['data']==1 for t in direct): flags.append('LOCAL_SPRINT_RESTART_OBSERVED')
        attempts = [e for e in trace['events'] if trace['schema']==2 and e['kind']==5 and e['epoch']==start['epoch']
                    and start['ns']-100_000_000<=e['ns']<=cutoff]
        packet_order = [dict(row=e['row'],thread=e['thread'],code=PACKETS.get(e['data'],'LEGACY_UNDECODED')) for e in attempts]
        if any(e['data'] in (11,12) for e in attempts) and trace['schema']==2:
            flags.append('SPRINT_ACTION_ATTEMPT_OBSERVED')
        sample_gaps = [b['ns']-a['ns'] for a,b in zip(seen,seen[1:])]
        continuity_unknown = trace['schema']==1 and (not seen or any(x>150_000_000 for x in sample_gaps))
        output.append(dict(duration_evidence=UNKNOWN if continuity_unknown else state,
                           candidate_for_tap_training=(trace['schema']==2 and end is not None and state==OBSERVED
                               and 0 < end['ns']-start['ns'] <= 250_000_000
                               and any(a['data']==13 for a in attempts)),
                           candidate_rule='Experimental filter: <=250ms release plus ATTACK attempt nearby; not an optimum',
                           release_ns=start['ns'],repress_ns=end['ns'] if end else None,
                           duration_ms=(end['ns']-start['ns'])/1e6 if end else None,
                           epoch=start['epoch'],thread=start['thread'],evidence=state,reasons=flags,
                           samples=len(seen),first_sample_delay_ms=(seen[0]['ns']-start['ns'])/1e6 if seen else None,
                           sampled_forward=[e['moveForward'] for e in seen],packet_attempts=packet_order,
                           interpretation='Candidate forward release, not proof of intent, reset success or accepted hit'))
        pending = None
    for e in trace['events']:
        if last_epoch is not None and e['epoch']!=last_epoch and e['kind'] in (1,2,3,4,9,10,11,12,13,14):
            finish(None,'CONTEXT_EPOCH_CHANGED'); had_down=False
        if e['kind'] in (1,2,3,4,9,10,11,12,13,14): last_epoch=e['epoch']
        if e['kind']==12 and e['data']==0:
            finish(None,'FOCUS_GUI_OR_WORLD_INTERRUPTION'); had_down=False
        elif e['kind']==1:
            if pending is None: pending=e
        elif e['kind']==2:
            if pending is not None: finish(e)
            elif had_down: repeated_down += 1
            had_down=True
    finish(None,'RECORDING_ENDED_BEFORE_REPRESS')
    return output,repeated_down


def analyze(path):
    trace = read_trace(path)
    events = trace['events']
    counts = Counter(e['kind'] for e in events)
    transitions = sprint_transitions(events)
    intervals,repeats = releases(trace,transitions)
    valid_intervals = [r for r in intervals if r['evidence']==OBSERVED]
    packets = [e for e in events if e['kind']==5 and e['data']==13] if trace['schema']==2 else []
    attack_relations=[]
    for a in packets:
        next_t=[t for t in transitions if t['epoch']==a['epoch'] and t['thread']==a['thread'] and t['sequence']>a['sequence'] and 0<=t['ns']-a['ns']<=100_000_000]
        prev_t=[t for t in transitions if t['epoch']==a['epoch'] and t['thread']==a['thread'] and t['sequence']<a['sequence'] and 0<=a['ns']-t['ns']<=100_000_000]
        reason = 'ATTACK_BEFORE_SPRINT_TRANSITION' if next_t else 'ATTACK_AFTER_SPRINT_TRANSITION' if prev_t else 'NO_NEARBY_SAME_THREAD_TRANSITION'
        attack_relations.append(dict(row=a['row'],reason=reason,thread=a['thread'],epoch=a['epoch']))
    diagnostics=[dict(kind=e['kind'],code=e['data'],row=e['row']) for e in events if e['kind'] in (8,16)]
    local_ids={e['epoch']:e['entity'] for e in events if e['kind'] in (9,10,11) and e['entity']>=0}
    feedback=[e for e in events if e['kind']==6 and e['entity']==local_ids.get(e['epoch'],-999)
              and ((e['data']==19 and e['x']==2) or e['data']==18)]
    counters=[]
    for h in feedback:
        following=[a for a in packets if a['epoch']==h['epoch'] and 0<=a['ns']-h['ns']<=500_000_000]
        if following: counters.append(dict(feedback_row=h['row'],attack_row=following[0]['row'],
                                            delay_ms=(following[0]['ns']-h['ns'])/1e6,
                                            reason='ATTACK_AFTER_LOCAL_FEEDBACK_NOT_PROVEN_HIT_SELECT'))
    ticks=[e for e in events if e['kind']==4]
    dt=[(b['ns']-a['ns'])/1e6 for a,b in zip(ticks,ticks[1:]) if b['epoch']==a['epoch'] and b['ns']>=a['ns']]
    short=[x for x in dt if x<=150]
    attack_dt=[(b['ns']-a['ns'])/1e6 for a,b in zip(packets,packets[1:]) if b['epoch']==a['epoch'] and b['ns']>=a['ns']]
    evidence=INVALID if not trace['complete'] else OBSERVED
    packets_by_stage={str(k):dict(Counter(PACKETS.get(e['data'],'UNDECODED') for e in events if e['kind']==k)) for k in (5,6,15)}
    return dict(schema=trace['schema'],recording=dict(evidence=evidence,complete=trace['complete'],
                reasons=trace['reasons'],event_loss=trace['loss'],events=len(events),counts=dict(counts)),
                instrumentation_diagnostics=diagnostics,
                coverage=dict(actual_transport_write=ABSENT,server_acceptance=ABSENT,
                              local_sprint=UNKNOWN if any(d['kind']==16 and d['code']==4 for d in diagnostics) else OBSERVED if counts[9]+counts[10]+counts[11] else UNKNOWN if trace['schema']==2 else ABSENT,
                              attack_intent=OBSERVED if counts[13] else UNKNOWN if trace['schema']==2 else ABSENT,
                              packet_actions=UNKNOWN if any(d['kind']==16 and d['code'] in (5,6) for d in diagnostics) else OBSERVED if trace['schema']==2 and counts[5]+counts[6]+counts[15] else UNKNOWN if trace['schema']==2 else ABSENT,
                              connection_identity=ABSENT,
                              focus_boundaries=OBSERVED if counts[12] else UNKNOWN if trace['schema']==2 else ABSENT),
                wtap=dict(evidence=evidence,intervals=intervals,repeated_down_observations=repeats,
                          sampled=sum('RELEASE_SAMPLED' in r['reasons'] for r in valid_intervals),
                          unsampled=sum('RELEASE_NOT_SAMPLED' in r['reasons'] for r in valid_intervals),
                          duration_ms=distribution([r['duration_ms'] for r in valid_intervals if r['duration_ms'] is not None]),
                          note='Schema 1 cannot censor unrecorded focus/world changes; durations are queue-observation intervals.'),
                sprint=dict(transitions=transitions,attack_order=attack_relations),
                block_hit=dict(use_intents=counts[14],packet_attempts=packets_by_stage['5'],
                               evidence=LIMITED if trace['schema']==2 else ABSENT,
                               reason='Use/placement is not proof of sword blocking; no raw attack press-consumption hook'),
                hit_select=dict(feedback_counterattack_pairs=counters,evidence=LIMITED if feedback else ABSENT,
                                reason='Ingress is not attacker identity or server acceptance; overlapping feedback may share an attack'),
                combo=dict(attack_interval_ms=distribution(attack_dt),evidence=LIMITED if packets else ABSENT,
                           reason='Attack attempts, not accepted hits; no target motion reconstruction or optimal reset rule'),
                packets=packets_by_stage,
                timing=dict(short_tick_subset_ms=distribution(short),excluded_over_150ms=len(dt)-len(short),
                            gaps_over_1000ms=sum(x>1000 for x in dt),
                            note='Threshold-selected tick intervals; exclusions can include stalls as well as inactivity. Not FPS.'),
                calibration=dict(evidence=UNKNOWN,recommendation=None,reason='NO_CONTROLLED_SERVER_OUTCOME_LABELS'))


def finite_json(value):
    if isinstance(value,float) and not math.isfinite(value): return None
    if isinstance(value,dict): return {k:finite_json(v) for k,v in value.items()}
    if isinstance(value,list): return [finite_json(x) for x in value]
    return value


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('recording',type=Path)
    p.add_argument('--out',type=Path)
    args=p.parse_args()
    text=json.dumps(finite_json(analyze(args.recording)),indent=2,allow_nan=False,default=str)
    if args.out:
        with args.out.open('x',encoding='utf-8') as f: f.write(text+'\n')
    else: print(text)

if __name__=='__main__': main()
