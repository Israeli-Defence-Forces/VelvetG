"""Deterministic passive analyzers; evidence is never an accepted-hit claim."""
from . import wtap, block_hit, hit_select, combo


def analyze_all(trace, intervals, transitions):
    return {name: module.analyze(trace, intervals, transitions) for name, module in
            [('wtap', wtap), ('block_hit', block_hit), ('hit_select', hit_select), ('combo', combo)]}
