"""Summarize a successful physics.csv and optionally plot its measured temperature curves.

Usage: python tools/analyze_thermal_lab.py path/to/physics.csv [--plot]
Matplotlib is optional; outputs remain beside the ignored measurement file.
"""
import argparse
import csv
import json
import re
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("measurements", type=Path)
    parser.add_argument("--plot", action="store_true")
    args = parser.parse_args()
    with args.measurements.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    failed = [r["experiment"] for r in rows if r["passed"] != "true"]
    if failed:
        raise SystemExit("Physical checks failed: " + ", ".join(failed))
    values = {r["experiment"]: float(r["value"]) for r in rows}
    ledger_errors = [abs(float(r["value"]) - float(r["reference"]))
                     for r in rows if "ledger" in r["experiment"] or "conservation" in r["experiment"]]
    summary = {
        "checks": len(rows), "failures": failed,
        "maximum_recorded_ledger_error_game_units": max(ledger_errors, default=0),
        "room_rises_c": {k: v for k, v in values.items() if "room_rise" in k},
        "foundry_rises_f": {k: v for k, v in values.items() if re.fullmatch(r"foundry_(closed|vented)_rise_\d+s", k)},
        "limits": "Controlled game-unit lattice experiments; not SI calibration or CFD.",
    }
    destination = args.measurements.with_name("physics-summary.json")
    destination.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(f"{len(rows)} checks passed; maximum recorded ledger error {max(ledger_errors, default=0):.3g} game units")
    if args.plot:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt

        fig, axes = plt.subplots(1, 2, figsize=(13, 5), layout="constrained")
        names = {
            "wood": "Wood", "brick": "Brick", "glass": "Glass",
            "brick_thick": "Thicker brick", "brick_door": "Open door",
            "brick_roof": "Roof vent", "brick_shaft": "Roof + shaft",
        }
        for key, label in names.items():
            points = sorted((int(match[1]), value) for name, value in values.items()
                            if (match := re.fullmatch(re.escape(key) + r"_room_rise_(\d+)s", name)))
            axes[0].plot([0] + [p[0] for p in points], [0] + [p[1] for p in points], label=label)
        axes[0].axvspan(0, 60, color="#e8a33e", alpha=.12)
        axes[0].axvline(60, color="#777777", linestyle="--", linewidth=1)
        axes[0].set(title="Same heat input, different buildings", xlabel="Simulated seconds (heater off at 60)", ylabel="Mean room temperature rise (°C)")
        axes[0].legend(fontsize=8)
        for kind, label in [("closed", "Sealed workshop"), ("vented", "Workshop with chimney")]:
            seconds = [0, 60, 120, 180]
            rise = [0] + [values[f"foundry_{kind}_rise_{t}s"] / 1.8 for t in seconds[1:]]
            axes[1].plot(seconds, rise, marker="o", label=label)
        axes[1].set(title="Equal industrial waste-heat budgets", xlabel="Simulated seconds (continuous heating)", ylabel="Occupied-air temperature rise (°C)")
        axes[1].legend(fontsize=9)
        for axis in axes:
            axis.grid(alpha=.2)
            axis.set_ylim(bottom=0)
        fig.suptitle("Civilization thermal laboratory — measured lattice response", fontsize=14)
        fig.savefig(args.measurements.with_name("thermal-curves.png"), dpi=160)
        plt.close(fig)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
