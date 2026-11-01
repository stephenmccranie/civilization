"""Compare equal-work thermal benchmark CSVs; write a summary beside the candidate."""
import argparse
import csv
import json
import statistics
from pathlib import Path


def read(path):
    with path.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    result = {}
    for row in rows:
        if float(row["max_reference_error"]) > 1e-8:
            raise ValueError("Candidate differs from its saved physical reference")
        result.setdefault(row["scene"], []).append(row)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    args = parser.parse_args()
    before, after = read(args.baseline), read(args.candidate)
    if before.keys() != after.keys():
        raise ValueError("Benchmark scenes differ")
    summary = {}
    for scene in before:
        a, b = before[scene], after[scene]
        for key in ("initial_cells", "final_cells", "quarter_steps"):
            if {r[key] for r in a} != {r[key] for r in b}:
                raise ValueError(f"{scene}: unequal work ({key})")
        metrics = {}
        for key in ("wall_ms", "cpu_ms", "allocated_bytes"):
            old = statistics.median(float(r[key]) for r in a)
            new = statistics.median(float(r[key]) for r in b)
            metrics[key] = {"before": old, "after": new,
                            "reduction_percent": 100 * (1 - new / old) if old else None}
        metrics["max_reference_error"] = max(float(r["max_reference_error"]) for r in b)
        summary[scene] = metrics
        print(f'{scene}: elapsed {metrics["wall_ms"]["reduction_percent"]:.1f}% lower; '
              f'allocations {metrics["allocated_bytes"]["reduction_percent"]:.1f}% lower; '
              f'reference error {metrics["max_reference_error"]:.3g}')
    args.candidate.with_name("benchmark-summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
