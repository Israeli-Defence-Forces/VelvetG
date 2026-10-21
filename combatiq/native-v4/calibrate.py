#!/usr/bin/env python3
"""Offline private-server trial calibration. Never schedules packets or controls input."""
import argparse
from collections import defaultdict
import json
import math
from pathlib import Path
import statistics

REQUIRED={'trial','session','split','context','candidate_ms','release_ms','tick_ms',
          'client_event','server_event','server_tick','server_sequence','accepted','authoritative'}


def validate(rows):
    seen=set()
    sessions={}
    server_ids=set()
    for r in rows:
        if set(r)!=REQUIRED: raise ValueError('Unexpected/missing trial fields')
        if any(not isinstance(r[k],str) or not r[k] for k in
               ('trial','session','context','client_event','server_event')):
            raise ValueError('Nonempty anonymous identifiers required')
        if r['trial'] in seen: raise ValueError('Duplicate trial')
        seen.add(r['trial'])
        identity=(r['session'],r['server_event'])
        if identity in server_ids: raise ValueError('Server event reused across trials')
        server_ids.add(identity)
        if r['split'] not in ('train','test'): raise ValueError('Require train/test split by session')
        if r['session'] in sessions and sessions[r['session']]!=r['split']:
            raise ValueError('Session leakage across splits')
        sessions[r['session']]=r['split']
        if type(r['accepted']) is not bool or type(r['authoritative']) is not bool:
            raise ValueError('Explicit boolean outcome/source required')
        for k in ('candidate_ms','release_ms','tick_ms'):
            if type(r[k]) not in (int,float) or not math.isfinite(r[k]) or r[k]<=0:
                raise ValueError('Positive finite timing required')
        for k in ('server_tick','server_sequence'):
            if type(r[k]) is not int or r[k]<0: raise ValueError('Invalid server identity')
    return rows


def summary(rows,alpha=.05):
    sessions=defaultdict(list)
    for r in rows: sessions[r['session']].append(int(r['accepted']))
    means=[statistics.mean(v) for _,v in sorted(sessions.items())]
    if not means: return dict(trials=0,sessions=0,mean_session_acceptance=None,bounds=None)
    mean=statistics.mean(means)
    radius=math.sqrt(math.log(2/alpha)/(2*len(means)))
    return dict(trials=len(rows),sessions=len(means),mean_session_acceptance=mean,
                bounds=[max(0,mean-radius),min(1,mean+radius)],
                release_ms_mean=statistics.mean(r['release_ms'] for r in rows),
                release_ms_sd=statistics.stdev([r['release_ms'] for r in rows]) if len(rows)>1 else None,
                release_in_client_ticks_mean=statistics.mean(r['release_ms']/r['tick_ms'] for r in rows),
                interval='Hoeffding bound on independent bounded session means; not per-hit independence')


def calibrate(rows,baseline_ms,candidates,min_sessions=10):
    validate(rows)
    if baseline_ms not in candidates or len(set(candidates))!=len(candidates):
        raise ValueError('Prespecify unique candidates including baseline')
    if min_sessions<2: raise ValueError('At least two independent sessions required')
    if any(r['candidate_ms'] not in candidates for r in rows): raise ValueError('Unregistered candidate')
    if any(not r['authoritative'] for r in rows):
        return dict(quality='UNKNOWN',recommendation=None,reasons=['NONAUTHORITATIVE_OUTCOMES'],contexts={})
    contexts={}
    for context in sorted({r['context'] for r in rows}):
        group=[r for r in rows if r['context']==context]
        train={c:summary([r for r in group if r['candidate_ms']==c and r['split']=='train']) for c in candidates}
        test={c:summary([r for r in group if r['candidate_ms']==c and r['split']=='test'],alpha=.025) for c in candidates}
        adequate=all(s['sessions']>=min_sessions for s in train.values())
        # Train-only selection. Held-out outcomes never choose the candidate.
        selected=min(candidates,key=lambda c:(-train[c]['mean_session_acceptance'],c)) if adequate else None
        reasons=[];recommendation=None
        if not adequate: reasons.append('INSUFFICIENT_TRAIN_SESSIONS')
        elif selected==baseline_ms: reasons.append('TRAIN_SELECTION_IS_BASELINE')
        elif min(test[selected]['sessions'],test[baseline_ms]['sessions'])<min_sessions:
            reasons.append('INSUFFICIENT_HELD_OUT_SESSIONS')
        elif test[selected]['bounds'][0]<=test[baseline_ms]['bounds'][1]:
            reasons.append('HELD_OUT_UNCERTAINTY_OVERLAPS')
        else:
            recommendation=selected;reasons.append('HELD_OUT_SESSION_BOUNDS_SEPARATE')
        contexts[context]=dict(train=train,test=test,selected_on_train=selected,
                               informational_candidate_ms=recommendation,reasons=reasons)
    return dict(quality='INFERRED' if rows else 'UNKNOWN',recommendation=None,contexts=contexts,
                reasons=['CONTEXT_SPECIFIC_INFORMATION_ONLY','NO_UNIVERSAL_OPTIMUM'],
                assumptions=['Independent session randomization units; outcome definition prespecified',
                             'No client/server clock alignment or one-way delay is inferred',
                             'Per-context bounds do not adjust multiplicity across exploratory contexts',
                             'Authoritative provenance is supplied by the experimenter, not authenticated by this file'])


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('trials',type=Path)
    p.add_argument('--baseline-ms',type=float,required=True)
    p.add_argument('--candidates-ms',type=float,nargs='+',required=True)
    a=p.parse_args()
    rows=[json.loads(line) for line in a.trials.read_text().splitlines() if line.strip()]
    print(json.dumps(calibrate(rows,a.baseline_ms,a.candidates_ms),indent=2,allow_nan=False))

if __name__=='__main__': main()
