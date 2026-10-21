#!/usr/bin/env python3
"""CombatIQ native schema 1 diagnostics (including safe preliminary live-log counts).

Never treats packet attempts as accepted hits or an incomplete capture as conclusive.
Usage: python analyze_native.py --latest  OR  python analyze_native.py PATH.csv
"""
import argparse
import json
import statistics
from bisect import bisect_left, bisect_right
from collections import Counter
from pathlib import Path


def analyze(path):
    path = Path(path)
    lines = path.read_text(encoding='utf-8').splitlines()
    if len(lines) < 2 or not lines[0].startswith('# CombatIQ native experimental schema 1'):
        raise ValueError('Unsupported native telemetry log')
    if lines[1] != 'ns,kind,data,moveForward,packetClass':
        raise ValueError('Unrecognized columns')

    footer_present = lines[-1] == '# END'
    losses = [line for line in lines[2:] if line.startswith('# LOSS,')]
    try:
        lost = int(losses[0].split(',')[1]) if len(losses) == 1 else None
    except ValueError:
        lost = None
    capped = any(line.startswith('# CAPPED') for line in lines)
    complete = footer_present and lost == 0 and not capped
    reason = ('complete' if complete else
              'recording still open or interrupted; # END missing' if not footer_present else
              'missing/invalid loss footer' if lost is None else
              'recorder hit its row cap' if capped else
              'recorder reported lost events')

    data = []
    event_counts = Counter()
    skipped_trailing_partial_line = False
    event_names = {1:'forward_key_up', 2:'forward_key_down',
                   3:'movement_sample', 4:'client_tick',
                   5:'send_attempt', 6:'receive_before_filter',
                   7:'movement_hook_seen', 8:'movement_hook_failure'}
    for line_i, line in enumerate(lines[2:], start=2):
        if line.startswith('#') or not line.strip():
            continue
        p = line.split(',', 4)
        if len(p) != 5:
            if line_i == len(lines)-1 and not footer_present:
                skipped_trailing_partial_line = True
                break
            raise ValueError('Malformed event record at line {}'.format(line_i+1))
        try:
            ns, kind, value, move = int(p[0]), int(p[1]), int(p[2]), float(p[3])
        except ValueError:
            if line_i == len(lines)-1 and not footer_present:
                skipped_trailing_partial_line = True
                break
            raise ValueError('Malformed numeric event at line {}'.format(line_i+1))
        data.append((ns, kind, value, move, p[4]))
        event_counts[event_names.get(kind, 'unknown_kind_{}'.format(kind))] += 1

    # A completely missing sampling stream is a failed instrument, NOT 27 missed taps.
    data.sort(key=lambda x: x[0])
    samples = [(ns, value, move) for ns, kind, value, move, _ in data if kind == 3]
    sample_times = [ns for ns, _, _ in samples]
    event_errors = [
        {'stage': 'initialization' if value == 1 else 'observation', 'error': cls}
        for ns, kind, value, move, cls in data if kind == 8
    ]
    released = None
    had_down = False
    sampled = missed = initial_unknown = indeterminate = pending = binding_mismatch = 0
    tick_intervals = []
    prev_tick = None
    # Client 1.8.9 movement samples are normally ~50ms apart. Evidence must
    # bracket the entire release interval without a long unobserved gap.
    maximum_bracket_ns = 150_000_000
    for ns, kind, value, move, _ in data:
        if kind == 2:
            if released is not None:
                if not had_down:
                    initial_unknown += 1
                elif not sample_times:
                    indeterminate += 1
                else:
                    lo = bisect_left(sample_times, released)
                    hi = bisect_right(sample_times, ns)
                    seen = samples[lo:hi]
                    if any(v == 0 for _, v, _ in seen):
                        sampled += 1
                    elif seen:
                        # Raw key-up but the sampled binding stayed down.
                        # This is a different result from a missed sample.
                        binding_mismatch += 1
                    else:
                        before = lo - 1
                        after = hi
                        if (not event_errors and before >= 0 and after < len(samples)
                                and released - sample_times[before] <= maximum_bracket_ns
                                and sample_times[after] - ns <= maximum_bracket_ns
                                and sample_times[after] - sample_times[before] <= maximum_bracket_ns):
                            missed += 1
                        else:
                            indeterminate += 1
                released = None
            had_down = True
        elif kind == 1:
            if released is None:
                released = ns
        elif kind == 4:
            if prev_tick is not None:
                tick_intervals.append((ns - prev_tick) / 1e6)
            prev_tick = ns
    if released is not None:
        pending = 1

    unverified = indeterminate + binding_mismatch + pending
    censored = unverified if complete else unverified + sampled + missed
    coverage = ('no_movement_samples' if not samples else
                'hook_failure_reported' if event_errors else 'movement_samples_recorded')

    return {
        'file': str(path),
        'log_complete': complete,
        'completion_reason': reason,
        'footer_present': footer_present,
        'capped': capped,
        'event_loss': lost,
        'skipped_trailing_partial_line': skipped_trailing_partial_line,
        'sampled_forward_releases': sampled if complete else 0,
        'unsampled_forward_releases': missed if complete else 0,
        'unclassified_initial_release': initial_unknown,
        'censored_releases': censored,
        'preliminary_only': not complete,
        'preliminary_sampled_forward_releases': sampled,
        'preliminary_unsampled_forward_releases': missed,
        'pending_release_without_repress': pending,
        'releases_indeterminate_missing_sampling_coverage': indeterminate,
        'releases_binding_state_disagreed': binding_mismatch,
        'movement_sample_coverage': coverage,
        'movement_hook_probe_seen': bool(event_counts['movement_hook_seen']),
        'movement_hook_errors': event_errors,
        'event_counts': dict(event_counts),
        'observed_client_tick_intervals_ms': {
            'count': len(tick_intervals),
            'mean': round(statistics.mean(tick_intervals),2) if tick_intervals else None,
            'maximum': round(max(tick_intervals),2) if tick_intervals else None,
        },
        'packet_observations': {
            'send_attempt': event_counts['send_attempt'],
            'receive_before_filter': event_counts['receive_before_filter'],
        },
        'notes': [
            'Preliminary counts in unfinished or lossy logs are NOT conclusive W-tap outcomes.',
            'Zero movement samples means W-tap results are indeterminate; never label them missed.',
            'W samples are binding observations, NOT confirmed sprint resets or hits.',
            'Movement sampling availability: event_counts.movement_sample reveals hook coverage.',
            'Only default LWJGL W key code 17 is instrumented.',
            'Packet send observations are attempts, not confirmed socket writes.',
            'Large tick intervals can include GUI/world transitions, paused/unloaded periods or stalls.',
        ],
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('recording', type=Path, nargs='?', help='Path to the CSV recording')
    p.add_argument('--latest', action='store_true', help='Analyze most recently modified file under your user profile')
    a = p.parse_args()
    if a.latest:
        if a.recording:
            p.error('choose a recording OR --latest')
        folder = Path.home() / 'VelvetG-CombatIQ'
        files = list(folder.glob('combatiq-native-*.csv'))
        if not files:
            p.error('No CSV files found in {}'.format(folder))
        filename = max(files, key=lambda f: f.stat().st_mtime)
    elif a.recording:
        filename = a.recording
    else:
        p.error('specify RECORDING.csv or --latest')
    print(json.dumps(analyze(filename), indent=2))


if __name__ == '__main__':
    main()
