"""Prepare user-supplied local stove recordings; original art is never published.

Requires numpy and soundfile >= 0.13. The local asset folder owns exact original bytes,
prompt provenance, prepared WAVs, export hashes and a blended listening preview.
Only mono Vorbis exports are copied into the mod's runtime resources.
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
ASSET = ROOT / "art/assets/prototype_stove"
RUNTIME = ROOT / "civilization-mod/src/main/resources/assets/civilization/sounds"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rms(samples):
    return float(np.sqrt(np.mean(samples * samples)))


def prepare(path):
    samples, rate = sf.read(path, always_2d=True)
    samples = samples.mean(axis=1)
    original_rms = rms(samples)
    # Avoid decoder padding and generated introductions/end fades.
    samples = samples[int(.5 * rate):-int(.5 * rate)].copy()
    samples -= samples.mean()
    if len(samples) < rate * 3 or rms(samples) < 1e-6:
        raise ValueError(f"Recording is too short or silent: {path}")
    # Gentle circular one-second level compensation; retain transient character.
    frame = int(.1 * rate)
    count = len(samples) // frame
    samples = samples[:count * frame]
    levels = np.sqrt(np.mean(samples.reshape(count, frame) ** 2, axis=1))
    levels = sum(np.roll(levels, i) for i in range(-5, 6)) / 11
    gains = np.clip(np.median(levels) / np.maximum(levels, 1e-8), .67, 1.5)
    centers = (np.arange(count) + .5) * frame
    envelope = np.interp(np.arange(len(samples)),
                         np.r_[centers[-1] - len(samples), centers, centers[0] + len(samples)],
                         np.r_[gains[-1], gains, gains[0]])
    samples *= envelope
    # Overlap the tail and head, then rotate to an interior start. No silence seam.
    cross = int(.3 * rate)
    t = np.linspace(0, np.pi / 2, cross, endpoint=False)
    join = samples[-cross:] * np.cos(t) + samples[:cross] * np.sin(t)
    samples = np.r_[samples[cross:-cross], join]
    # Match RMS with a soft peak ceiling. Avoid a loud crackle transient forcing
    # its entire recording quiet relative to the hiss.
    low, high = 0., 10000.
    for _ in range(50):
        gain = (low + high) / 2
        leveled = .6 * np.tanh(samples * gain / .6)
        if rms(leveled) < .10:
            low = gain
        else:
            high = gain
    samples = .6 * np.tanh(samples * ((low + high) / 2) / .6)
    return samples, rate, original_rms


def check(path):
    samples, rate = sf.read(path, always_2d=True)
    assert samples.shape[1] == 1, "Positional sounds must be mono"
    samples = samples[:, 0]
    assert np.isfinite(samples).all() and np.max(np.abs(samples)) < .99
    assert .095 < rms(samples) < .105, "Export level drift"
    seam = abs(samples[0] - samples[-1])
    normal_step = float(np.quantile(np.abs(np.diff(samples)), .999))
    assert seam < normal_step, "Loop seam exceeds normal transient steps"
    windows = len(samples) // rate
    levels = [rms(samples[i * rate:(i + 1) * rate]) for i in range(windows)]
    assert min(levels) > .04, "Quiet gap in loop"
    return {"seconds": len(samples) / rate, "sample_rate": rate,
            "rms": rms(samples), "peak": float(np.max(np.abs(samples))),
            "seam_step": float(seam), "normal_step_99_9_percent": normal_step,
            "one_second_rms_range": [min(levels), max(levels)]}


def smooth(value):
    t = np.clip(value, 0, 1)
    return t * t * (3 - 2 * t)


def preview(loops, rate):
    # Mirrors StoveSoundMix, accelerating cooking solely for this audition.
    t = np.arange(rate * 26) / rate
    work = np.interp(t, [0, 4, 8, 14, 20, 26], [0, 0, 800, 1000, 1400, 1800])
    scorched = smooth((work - 1000) / 400)
    golden = .9 * smooth((work - 600) / 200) * (1 - scorched)
    charred = 1.25 * scorched
    norm = np.sqrt(1 + golden * golden + charred * charred)
    out = np.zeros(len(t))
    for samples, gain in zip(loops, [1 / norm, golden / norm, charred / norm]):
        out += samples[np.arange(len(t)) % len(samples)] * gain
    # Audition fade belongs only to the preview, never the published loops.
    edge = int(.05 * rate)
    out[:edge] *= np.linspace(0, 1, edge)
    out[-edge:] *= np.linspace(1, 0, edge)
    sf.write(ASSET / "audio/blend-preview.wav", out, rate, subtype="PCM_16")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    manifest = json.loads((ASSET / "audio/sources.json").read_text(encoding="utf-8"))
    if args.check:
        receipt = json.loads((ASSET / "audio-receipt.json").read_text())
        assert receipt["builder_sha256"] == digest(Path(__file__)), "Preparation recipe changed"
        assert receipt["manifest_sha256"] == digest(ASSET / "audio/sources.json"), "Source manifest changed"
        for name, entry in receipt["files"].items():
            for key in ("original", "source_wav", "source_ogg", "runtime_ogg"):
                record = entry[key]
                assert digest(ROOT / record["path"]) == record["sha256"], record["path"]
            print(name, json.dumps(check(RUNTIME / f"stove_{name}.ogg")))
        return
    receipt = {"method": "User-supplied ElevenLabs MP3s: mono downmix, 0.5-second end trims, bounded one-second level compensation, 0.3-second equal-power cyclic seam, RMS 0.10 with 0.6 soft peak ceiling, mono Vorbis export at compression level 0.1.",
               "builder_sha256": digest(Path(__file__)),
               "manifest_sha256": digest(ASSET / "audio/sources.json"),
               "settings": {"numpy": np.__version__, "soundfile": sf.__version__}, "files": {}}
    loops, rates = [], []
    for name, entry in manifest["files"].items():
        source = ASSET / "audio" / entry["original"]
        assert digest(source) == entry["sha256"], "Original recording changed"
        samples, rate, original_rms = prepare(source)
        wav, ogg = ASSET / f"stove_{name}.wav", ASSET / f"stove_{name}.ogg"
        sf.write(wav, samples, rate, subtype="PCM_16")
        sf.write(ogg, samples, rate, format="OGG", subtype="VORBIS", compression_level=.1)
        stats = check(ogg)
        record = {"original_rms": original_rms, "validation": stats}
        for key, path in [("original", source), ("source_wav", wav), ("source_ogg", ogg)]:
            record[key] = {"path": path.relative_to(ROOT).as_posix(), "sha256": digest(path)}
        receipt["files"][name] = record
        loops.append(samples)
        rates.append(rate)
        print(name, json.dumps(stats))
    assert len(set(rates)) == 1, "Preview requires matching sample rates"
    preview(loops, rates[0])
    # Publish only after every candidate passes decoding, level and seam checks.
    for name, record in receipt["files"].items():
        ogg = ASSET / f"stove_{name}.ogg"
        target = RUNTIME / ogg.name
        shutil.copyfile(ogg, target)
        record["runtime_ogg"] = {"path": target.relative_to(ROOT).as_posix(), "sha256": digest(target)}
    (ASSET / "audio-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")


if __name__ == "__main__":
    # Vorbis encoding in Windows' bundled libsndfile exceeds the default stack.
    threading.stack_size(8 * 1024 * 1024)
    with ThreadPoolExecutor(max_workers=1) as worker:
        worker.submit(main).result()
