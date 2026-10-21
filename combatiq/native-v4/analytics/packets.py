"""Schema-3 packet object correspondence. Raw records are always retained separately."""
from collections import defaultdict
from .model import ref

STAGES={5:'SEND_ATTEMPT',6:'INGRESS_BEFORE_FILTER',15:'HANDLER_RETURN',
        23:'DISPATCH_ENTRY',24:'CHANNEL_WRITE_INVOCATION',26:'SEND_API_ENTRY'}


def normalize(trace):
    if trace['schema']<3:return trace['events']
    owners=defaultdict(set)
    for e in trace['events']:
        if e['kind'] in STAGES and e['packetId'] and e['connection']:
            owners[e['epoch'],e['packetId']].add(e['connection'])
    groups={};output=[]
    for original in trace['events']:
        e=dict(original)
        if e['kind'] not in STAGES or not e['packetId']:
            output.append(e);continue
        candidates=owners[e['epoch'],e['packetId']]
        if not e['connection'] and len(candidates)==1:
            e['connection']=next(iter(candidates));e['connection_evidence']='SAME_PACKET_OWNER_BOUND_OBSERVATION'
        key=(e['epoch'],e['packetId'],e['kind'],e['data'])
        if key in groups:
            kept=groups[key];kept.setdefault('duplicate_rows',[]).append(e['row'])
            continue
        e['duplicate_rows']=[];groups[key]=e;output.append(e)
    return output


def correlate(events):
    groups=defaultdict(list)
    for e in events:
        if e['kind'] in STAGES and e.get('packetId',0):groups[e['epoch'],e['packetId']].append(e)
    out=[]
    for (epoch,token),rows in sorted(groups.items()):
        out.append(dict(epoch=epoch,packet_id=token,action=rows[0]['data'],
                        stages=[dict(ref(e),stage=STAGES[e['kind']],connection=e['connection'],
                                     connection_evidence=e.get('connection_evidence','DIRECT' if e['connection'] else 'UNKNOWN'),
                                     duplicate_rows=e.get('duplicate_rows',[])) for e in rows],
                        socket_write='NOT_RECORDED',server_receipt='NOT_RECORDED',server_acceptance='NOT_RECORDED'))
    return out
