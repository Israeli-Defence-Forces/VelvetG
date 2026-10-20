#!/usr/bin/env python3
"""Offline condition manifests and paired analysis; never alters networking or generates inputs."""
import argparse
import csv
import itertools
import json
import math
import random
import statistics
import sys
import unittest

def conditions():
    result = []
    for rtt, share, profile in itertools.product((40, 120, 180, 240, 300, 400),
                                                (0.2, 0.5, 0.8),
                                                ("stable", "mild", "variable")):
        up, down = rtt * share, rtt * (1 - share)
        std = (0, 0) if profile == "stable" else ((2, 2) if profile == "mild"
                                                   else (up / 4, down / 4))
        result.append(dict(condition="%d-u%d-%s" % (rtt, round(share * 100), profile),
                           nominal_added_rtt_ms=rtt, upload_share=share,
                           upload_delay_ms=round(up, 6), download_delay_ms=round(down, 6),
                           upload_std_ms=round(std[0], 6), download_std_ms=round(std[1], 6),
                           distribution="normal", emulator_loss_pct=0,
                           emulator_reorder=False,
                           note="Added delay, not measured RTT; measure achieved paths and TCP stalls."))
    return result

def paired(rows, condition):
    groups = {}
    for row in rows:
        if row["condition"] != condition:
            continue
        cluster, strategy = row["cluster"], row["strategy"]
        if not cluster or strategy not in ("baseline", "candidate"):
            raise ValueError("Require named clusters and baseline/candidate strategies")
        value = float(row["value"])
        if not math.isfinite(value):
            raise ValueError("Non-finite endpoint")
        group = groups.setdefault(cluster, {})
        if strategy in group:
            raise ValueError("Aggregate within the randomization unit first; duplicate cluster arm")
        group[strategy] = value
    if len(groups) < 2 or any(set(v) != {"baseline", "candidate"} for v in groups.values()):
        raise ValueError("Require at least two complete independent paired clusters")
    return [v["candidate"] - v["baseline"] for _, v in sorted(groups.items())]

def quantile(values, p):
    values = sorted(values)
    at = (len(values) - 1) * p
    lo = int(at)
    return values[lo] + (values[min(lo + 1, len(values) - 1)] - values[lo]) * (at - lo)

def analyze(deltas, seed=731, draws=10000):
    if len(deltas) < 2 or draws < 100:
        raise ValueError("Insufficient paired units or draws")
    rng = random.Random(seed)
    n = len(deltas)
    observed = statistics.mean(deltas)
    bootstrap = [statistics.mean(deltas[rng.randrange(n)] for _ in range(n))
                 for _ in range(draws)]
    if n <= 16:
        null = [sum(s * d for s, d in zip(signs, deltas)) / n
                for signs in itertools.product((-1, 1), repeat=n)]
        p = sum(abs(x) >= abs(observed) - 1e-12 for x in null) / len(null)
        method = "exact paired label swap"
    else:
        hits = sum(abs(sum(rng.choice((-1, 1)) * d for d in deltas) / n)
                   >= abs(observed) - 1e-12 for _ in range(draws))
        p = (hits + 1) / (draws + 1)
        method = "Monte Carlo paired label swap"
    return dict(paired_clusters=n, mean_candidate_minus_baseline=observed,
                bootstrap_percentile_95=[quantile(bootstrap, .025), quantile(bootstrap, .975)],
                two_sided_randomization_p=p, randomization_method=method, seed=seed,
                assumptions=["Independent paired randomization units; arm labels exchangeable under sharp null",
                             "Aggregate encounters within a unit before this analysis",
                             "One prespecified condition/endpoint; adjust multiplicity outside this tool"],
                warning="No causal interpretation for self-selected observational strategies; small-sample CIs unstable.")

class Tests(unittest.TestCase):
    def test_matrix(self):
        c = conditions()
        self.assertEqual(len(c), 54)
        self.assertEqual(len(set(x["condition"] for x in c)), 54)
        for x in c:
            self.assertAlmostEqual(x["upload_delay_ms"] + x["download_delay_ms"],
                                   x["nominal_added_rtt_ms"])
            self.assertGreaterEqual(x["upload_std_ms"], 0)
    def test_pairing(self):
        rows = [dict(condition="x", cluster=str(i), strategy=s, value=str(i + (s == "candidate")))
                for i in range(3) for s in ("baseline", "candidate")]
        self.assertEqual(paired(rows, "x"), [1, 1, 1])
        with self.assertRaises(ValueError):
            paired(rows + [rows[0]], "x")
        with self.assertRaises(ValueError):
            paired(rows[:-1], "x")
    def test_statistics(self):
        s = analyze([0, 0, 0], draws=100)
        self.assertEqual(s["two_sided_randomization_p"], 1)
        s = analyze([1, 1, 1, 1], draws=100)
        self.assertEqual(s["two_sided_randomization_p"], .125)
        self.assertEqual(s["bootstrap_percentile_95"], [1, 1])
        self.assertEqual(analyze([1, -2, 3], draws=100),
                         analyze([1, -2, 3], draws=100))
        self.assertEqual(quantile([0, 10], .25), 2.5)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--matrix", action="store_true")
    mode.add_argument("--analyze", metavar="PAIRED_CSV")
    mode.add_argument("--self-test", action="store_true")
    parser.add_argument("--condition")
    parser.add_argument("--seed", type=int, default=731)
    args = parser.parse_args()
    if args.self_test:
        result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Tests))
        sys.exit(0 if result.wasSuccessful() else 1)
    if args.matrix:
        print(json.dumps(dict(schema=1, conditions=conditions()), indent=2))
    else:
        if not args.condition:
            parser.error("--analyze requires --condition")
        with open(args.analyze, newline="", encoding="utf-8") as source:
            reader = csv.DictReader(source)
            if reader.fieldnames != ["condition", "cluster", "strategy", "value"]:
                raise ValueError("Expected CSV header condition,cluster,strategy,value")
            result = analyze(paired(reader, args.condition), args.seed)
        result["condition"] = args.condition
        print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main()
