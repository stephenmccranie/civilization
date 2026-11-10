"""Prepare an asset's user-supplied recordings for the mod; the originals are never published.

`python tools/audio/prepare.py <asset>` reads art/assets/<asset>/audio/sources.json. Each recording there names its
original, the original's sha256, the runtime sound it becomes and its kind:
- loop: a steady bed made seamless (trimmed ends, bounded level compensation, an equal-power cyclic seam);
- shot: one sound, from just before its onset until it falls silent.
Every export is a mono Vorbis file. The asset folder keeps the prepared WAV and OGG and a receipt of every hash; only
the OGGs are copied into the mod's runtime sounds. `--check` verifies the receipt and the published files.

Requires numpy and soundfile >= 0.13.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import threading
from concurrent.futures import ThreadPoolExecutor

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[2]
RUNTIME = ROOT / "civilization-mod/src/main/resources/assets/civilization/sounds"
LOOP_RMS = .10  # every loop's level; the game sets each sound's loudness
SHOT_PEAK = .8  # every shot's peak


def art_root():
    """Asset folders live in the main checkout's local art/, whichever checkout runs this."""
    for root in (ROOT, *ROOT.parents):
        if (root / "art/assets").is_dir():
            return root
    raise FileNotFoundError("No art/assets folder above this checkout")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rms(samples):
    return float(np.sqrt(np.mean(samples * samples)))


def mono(path):
    samples, rate = sf.read(path, always_2d=True)
    samples = samples.mean(axis=1)
    return samples - samples.mean(), rate


def envelope(samples, rate, seconds=.005):
    width = max(1, int(seconds * rate))
    return np.sqrt(np.convolve(samples * samples, np.ones(width) / width, "same"))


def loop(samples, rate):
    # Avoid decoder padding and generated introductions/end fades.
    samples = samples[int(.5 * rate):-int(.5 * rate)].copy()
    if len(samples) < rate * 3 or rms(samples) < 1e-6:
        raise ValueError("Loop recording is too short or silent")
    # Gentle circular one-second level compensation; retain transient character.
    frame = int(.1 * rate)
    count = len(samples) // frame
    samples = samples[:count * frame]
    levels = np.sqrt(np.mean(samples.reshape(count, frame) ** 2, axis=1))
    levels = sum(np.roll(levels, i) for i in range(-5, 6)) / 11
    gains = np.clip(np.median(levels) / np.maximum(levels, 1e-8), .67, 1.5)
    centers = (np.arange(count) + .5) * frame
    samples *= np.interp(np.arange(len(samples)),
                         np.r_[centers[-1] - len(samples), centers, centers[0] + len(samples)],
                         np.r_[gains[-1], gains, gains[0]])
    # Overlap the tail and head, then rotate to an interior start. No silence seam.
    cross = int(.3 * rate)
    t = np.linspace(0, np.pi / 2, cross, endpoint=False)
    join = samples[-cross:] * np.cos(t) + samples[:cross] * np.sin(t)
    samples = np.r_[samples[cross:-cross], join]
    # Match RMS under a soft peak ceiling, so one loud transient cannot force a whole bed quiet.
    low, high = 0., 10000.
    for _ in range(50):
        gain = (low + high) / 2
        if rms(.6 * np.tanh(samples * gain / .6)) < LOOP_RMS:
            low = gain
        else:
            high = gain
    return .6 * np.tanh(samples * ((low + high) / 2) / .6)


def shot(samples, rate):
    """From 5 ms before the onset to 20 ms past the last sound above -40 dB of the peak, faded at both ends."""
    level = envelope(samples, rate)
    loud = np.flatnonzero(level > .1 * level.max())
    quiet = np.flatnonzero(level > .01 * level.max())
    start, end = max(0, loud[0] - int(.005 * rate)), min(len(samples), quiet[-1] + int(.02 * rate))
    samples = samples[start:end].copy()
    fade_in, fade_out = int(.002 * rate), int(.02 * rate)
    samples[:fade_in] *= np.linspace(0, 1, fade_in)
    samples[-fade_out:] *= np.cos(np.linspace(0, np.pi / 2, fade_out))
    return samples * (SHOT_PEAK / np.max(np.abs(samples)))


def check_loop(samples, rate):
    assert .095 < rms(samples) < .105, "Export level drift"
    seam = abs(samples[0] - samples[-1])
    normal_step = float(np.quantile(np.abs(np.diff(samples)), .999))
    assert seam < normal_step, "Loop seam exceeds normal transient steps"
    levels = [rms(samples[i * rate:(i + 1) * rate]) for i in range(len(samples) // rate)]
    assert min(levels) > .04, "Quiet gap in loop"
    return {"seam_step": float(seam), "normal_step_99_9_percent": normal_step, "one_second_rms_range": [min(levels), max(levels)]}


def check_shot(samples, rate):
    assert abs(np.max(np.abs(samples)) - SHOT_PEAK) < .05, "Export level drift"
    assert np.argmax(np.abs(samples)) < .15 * rate, "Shot does not start at its onset"
    assert abs(samples[-1]) < .01, "Shot does not end silent"
    return {}


def check(path, kind):
    samples, rate = sf.read(path, always_2d=True)
    assert samples.shape[1] == 1, "Positional sounds must be mono"
    samples = samples[:, 0]
    assert np.isfinite(samples).all() and np.max(np.abs(samples)) < .99
    stats = check_loop(samples, rate) if kind == "loop" else check_shot(samples, rate)
    return {"seconds": len(samples) / rate, "sample_rate": rate, "rms": rms(samples), "peak": float(np.max(np.abs(samples))), **stats}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("asset")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    asset = art_root() / "art/assets" / args.asset
    manifest_path = asset / "audio/sources.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if args.check:
        receipt = json.loads((asset / "audio-receipt.json").read_text(encoding="utf-8"))
        assert receipt["builder_sha256"] == digest(Path(__file__)), "Preparation recipe changed"
        assert receipt["manifest_sha256"] == digest(manifest_path), "Source manifest changed"
        for name, entry in receipt["files"].items():
            assert digest(asset / "audio" / entry["original"]) == entry["original_sha256"], entry["original"]
            for key in ("source_wav", "source_ogg"):
                assert digest(asset / entry[key]["path"]) == entry[key]["sha256"], entry[key]["path"]
            runtime = RUNTIME / f"{name}.ogg"
            assert digest(runtime) == entry["runtime_ogg_sha256"], runtime
            print(name, json.dumps(check(runtime, entry["kind"])))
        return
    receipt = {"method": "Mono downmix. Loops: 0.5-second end trims, bounded one-second level compensation, 0.3-second "
                         f"equal-power cyclic seam, RMS {LOOP_RMS} under a 0.6 soft peak ceiling. Shots: 5 ms before onset "
                         f"to 20 ms past -40 dB, 2 ms and 20 ms fades, peak {SHOT_PEAK}. Mono Vorbis export at compression level 0.1.",
               "builder_sha256": digest(Path(__file__)), "manifest_sha256": digest(manifest_path),
               "settings": {"numpy": np.__version__, "soundfile": sf.__version__}, "files": {}}
    for entry in manifest["files"].values():
        source = asset / "audio" / entry["original"]
        assert digest(source) == entry["sha256"], f"Original recording changed: {source}"
        samples, rate = mono(source)
        name, kind = entry["runtime"], entry["kind"]
        prepared = {"loop": loop, "shot": shot}[kind](samples, rate)
        wav, ogg = asset / f"{name}.wav", asset / f"{name}.ogg"
        sf.write(wav, prepared, rate, subtype="PCM_16")
        sf.write(ogg, prepared, rate, format="OGG", subtype="VORBIS", compression_level=.1)
        stats = check(ogg, kind)
        receipt["files"][name] = {"kind": kind, "original": entry["original"], "original_sha256": entry["sha256"],
                                  "validation": stats, "source_wav": {"path": wav.name, "sha256": digest(wav)},
                                  "source_ogg": {"path": ogg.name, "sha256": digest(ogg)}}
        print(name, json.dumps(stats))
    # Publish only after every candidate passes decoding, level and seam checks. Vorbis streams carry a random serial,
    # so a re-export that decodes to the published audio leaves the published file as it is.
    for name, record in receipt["files"].items():
        target, export = RUNTIME / f"{name}.ogg", asset / f"{name}.ogg"
        if not (target.is_file() and np.array_equal(sf.read(target)[0], sf.read(export)[0])):
            shutil.copyfile(export, target)
        record["runtime_ogg_sha256"] = digest(target)
    (asset / "audio-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    # Vorbis encoding in Windows' bundled libsndfile exceeds the default stack.
    threading.stack_size(8 * 1024 * 1024)
    with ThreadPoolExecutor(max_workers=1) as worker:
        worker.submit(main).result()
