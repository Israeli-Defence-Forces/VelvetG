from .model import *


def analyze(trace, intervals, transitions):
    out = []
    for interval in intervals:
        start, end = interval['release_ns'], interval['repress_ns']
        own = [e for e in trace['events'] if e['epoch'] == interval['epoch']
               and e['thread'] == interval['thread']]
        within = [e for e in own if end is not None and start <= e['ns'] <= end]
        adjacent = [e for e in attacks(trace) if e['epoch']==interval['epoch'] and e['thread']==interval['thread'] and end is not None
                    and start-100_000_000 <= e['ns'] <= end+250_000_000]
        stopped = [e for e in transitions if e['epoch'] == interval['epoch']
                   and e['thread'] == interval['thread'] and e['data'] == 0
                   and e['previous'] == 1 and e['evidence']=='DIRECTLY_OBSERVED' and end is not None and start <= e['ns'] <= end+250_000_000]
        restarted = [e for e in transitions if e['epoch'] == interval['epoch']
                     and e['thread'] == interval['thread'] and e['data'] == 1
                     and e['previous'] == 0 and e['evidence']=='DIRECTLY_OBSERVED' and stopped and e['sequence'] > stopped[0]['sequence']
                     and start <= e['ns'] <= end+250_000_000]
        flags = ['RELEASE_OBSERVED', 'SERVER_OUTCOME_UNKNOWN']
        if 'RELEASE_SAMPLED' in interval['reasons']: flags.append('RELEASE_MOVEMENT_SAMPLED')
        if stopped: flags.append('LOCAL_SPRINT_STOP_OBSERVED')
        if restarted: flags.append('LOCAL_SPRINT_RESTART_OBSERVED')
        if adjacent: flags.append('ATTACK_ATTEMPT_OBSERVED')
        if any(e['kind']==5 and e['data'] in (11,12) for e in within): flags.append('SPRINT_ACTION_ATTEMPT_OBSERVED')
        if any(a['sequence'] < t['sequence'] for a in adjacent for t in stopped): flags.append('ATTACK_BEFORE_TRANSITION')
        if any(a['sequence'] > t['sequence'] for a in adjacent for t in stopped): flags.append('ATTACK_AFTER_TRANSITION')
        ticks = [e for e in within if e['kind']==4]
        tick_gaps = [b['ns']-a['ns'] for a,b in zip(ticks,ticks[1:])]
        sufficient = trace['complete'] and interval['evidence']=='DIRECTLY_OBSERVED' and end is not None
        if not sufficient: flags.append('INSUFFICIENT_EVIDENCE')
        candidate = sufficient and 0 < end-start <= 250_000_000 and bool(adjacent) and any(context_scoped(trace,a) for a in adjacent)
        out.append(dict(release_ns=start, repress_ns=end, epoch=interval['epoch'], thread=interval['thread'],
                        quality='INFERRED' if candidate else 'UNKNOWN', candidate=candidate,
                        reasons=flags+interval['reasons'], duration_ms=interval['duration_ms'],
                        update_opportunities=len(ticks), movement_samples=interval['samples'],
                        sample_delay_ms=interval['first_sample_delay_ms'],
                        unstable_tick_observed=any(g>75_000_000 for g in tick_gaps),
                        attacks=[ref(a) for a in adjacent], stops=[ref(t) for t in stopped],
                        restarts=[ref(t) for t in restarted],
                        transformed_samples=sum(e['kind']==3 and e['data']==1 and e['moveForward']<=0 for e in within)))
    inferred = [dict(release_ns=e['release_ns'], pattern='COMBAT_PROXIMATE_FORWARD_RELEASE',
                     quality='INFERRED', reasons=['SHORT_RELEASE_WITH_NEARBY_ATTACK_NOT_PROOF_OF_INTENT'])
                for e in out if e['candidate']]
    return result(trace, out, inferred, unavailable('PHYSICAL_KEYBOARD_LATENCY', 'SERVER_RESET_OUTCOME'))
