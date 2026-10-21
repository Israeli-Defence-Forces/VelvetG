from .model import *


def analyze(trace, intervals, transitions, maximum_gap_ns=750_000_000, minimum_attempts=2):
    if maximum_gap_ns<=0 or minimum_attempts<2: raise ValueError('Invalid observational combo definition')
    chains=[]; chain=[]
    def finish(reason):
        if len(chain)>=minimum_attempts:
            gaps=[(b['ns']-a['ns'])/1e6 for a,b in zip(chain,chain[1:])]
            related=[t for t in transitions if same_stream(t,chain[0])
                     and chain[0]['sequence']<=t['sequence']<=chain[-1]['sequence']]
            chains.append(dict(quality='INFERRED',attacks=[ref(a) for a in chain],spacing_ms=gaps,
                               target_entity=chain[0]['entity'],sprint_transitions=[ref(t) for t in related],
                               termination=reason,reasons=['ATTEMPT_CHAIN_NOT_CONFIRMED_COMBO']))
        chain.clear()
    for e in attacks(trace):
        if chain:
            prev=chain[-1]
            between=[r for r in trace['events'] if r['epoch']==prev['epoch']
                     and prev['sequence']<r['sequence']<e['sequence']]
            correction=any(r['kind'] in (6,15) and r['data']==22 for r in between)
            boundary=not same_stream(e,prev) or e['entity']!=prev['entity'] or e['entity']<0
            if boundary or correction or e['ns']-prev['ns']>maximum_gap_ns:
                finish('CONTEXT_THREAD_OR_TARGET_CHANGED' if boundary else 'CORRECTION_OBSERVED' if correction else 'SPACING_GAP')
        chain.append(e)
    finish('TRACE_ENDED')
    return result(trace,[ref(a) for a in attacks(trace)],chains,
                  unavailable('ACCEPTED_HIT_CHAIN','TARGET_SEPARATION','OPTIMAL_RESET_STRATEGY'),
                  ['CONFIGURABLE_ATTEMPT_CHAIN_DEFINITION'])
