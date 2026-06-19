#!/usr/bin/env python3
"""
Nimaz Pro — Noorani Qaida audio pipeline (Issue #173, sub-issue B of #171).

Produces one short, tap-to-play audio clip for **every** ``audio_key`` defined
by sub-issue A (``json/qaida_cells.json`` + ``json/qaida_letters.json``),
preprocessed so taps feel instant and every clip is the same loudness.

Why a dedicated pipeline
------------------------
The app today only streams **ayah-level** recitation (``QuranAudioManager``).
A Qaida needs **granular** clips — a child taps a single letter / syllable /
word and must hear *just that*. No public dataset covers the syllable/word
tokens, so we need one consistent voice across the whole set.

Source strategy (consistent single voice, reproducible from raw inputs)
-----------------------------------------------------------------------
For each ``audio_key`` the pipeline picks a source in this order:

1. **Human recording** — if ``audio/raw/<audio_key>.{wav,flac,m4a,mp3,ogg}``
   exists it is used verbatim. This is the drop-in path: record a clean child-
   friendly set, name each file by ``audio_key``, drop them in ``audio/raw/``
   and re-run — no code change. Recommended for a final release.
2. **Synthesized voice** — otherwise the clip is generated with **eSpeak NG**
   (Arabic voice) from the fully-diacritized text the content task already
   authored. This yields a *complete, consistent, reproducible* pack covering
   all 434 tokens (letters, harakat, tanween, madd, leen, sukoon, shadda,
   muqatta'at, syllables and words) with one voice and zero licensing
   entanglement — synthesized speech output is not encumbered by the engine's
   GPL. It is robotic, so it is intended as the functional default that
   unblocks the schema/playback sub-issues (C/F) and is swappable per (1).

Preprocessing (ffmpeg), identical for both sources:
    1. downmix to **mono**
    2. **trim leading/trailing silence** (so taps feel instant)
    3. **loudness-normalize** to EBU R128 (``loudnorm`` I=-16 / TP=-1.5)
    4. encode small: **mp3 ~56 kbps** (each clip a few KB)

Outputs
-------
* ``app/src/main/assets/qaida/audio/<audio_key>.mp3`` — the bundled pack
  (delivery Option 1; see the size report the script prints and CREDITS.md).
* ``audio/manifest.json`` — audio_key → filename, text, duration_ms, bytes,
  source, license (verification + future download manifest).
* ``audio/CREDITS.md`` — provenance & license per source.

The run **fails** if any ``audio_key`` is missing a clip or any clip is an
orphan (no matching key), so the pack stays 1:1 with the data.

Usage
-----
    cd nimaz-pro-data
    python3 scripts/process_qaida_audio.py            # build everything
    python3 scripts/process_qaida_audio.py --check    # verify only, no encode

Requires ``ffmpeg``/``ffprobe`` on PATH, and ``espeak-ng`` for any key that has
no human recording in ``audio/raw/``.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

# ── Paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
DATA_DIR = SCRIPT_DIR.parent                       # nimaz-pro-data/
JSON_DIR = DATA_DIR / "json"
AUDIO_DIR = DATA_DIR / "audio"                     # manifest + credits + raw/
RAW_DIR = AUDIO_DIR / "raw"                        # drop-in human recordings
REPO_ROOT = DATA_DIR.parent
OUT_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "qaida" / "audio"

RAW_EXTS = (".wav", ".flac", ".m4a", ".mp3", ".ogg", ".opus")

# ── eSpeak NG voice (one consistent voice for the whole set) ───────────
ESPEAK_VOICE = "ar"      # Arabic; reads the diacritics our text already carries
ESPEAK_SPEED = "130"     # words-per-minute — slow & clear for learners
ESPEAK_PITCH = "45"      # 0-99; gentle, slightly raised

# ── ffmpeg encode settings ────────────────────────────────────────────
MP3_BITRATE = "56k"
# Trim silence at both ends; -50 dB gate, keep a tiny head/tail so it isn't clipped.
SILENCE_FILTER = (
    "silenceremove="
    "start_periods=1:start_threshold=-50dB:start_silence=0.03:"
    "stop_periods=-1:stop_threshold=-50dB:stop_silence=0.08:detection=peak"
)
LOUDNORM_FILTER = "loudnorm=I=-16:TP=-1.5:LRA=11"
AFILTER = f"{SILENCE_FILTER},{LOUDNORM_FILTER}"

LICENSE_TTS = "Synthesized with eSpeak NG (Arabic voice); output free to use/redistribute."
LICENSE_RECORDING = "Custom recording — see CREDITS.md for per-file provenance."


# ── Data loading ──────────────────────────────────────────────────────
def load_rows(name: str) -> list[dict]:
    data = json.loads((JSON_DIR / name).read_text(encoding="utf-8"))
    if isinstance(data, dict):                      # tolerate {"rows": [...]} wrappers
        for v in data.values():
            if isinstance(v, list):
                return v
        raise SystemExit(f"{name}: could not find a list of rows")
    return data


def build_targets() -> dict[str, dict]:
    """Map every audio_key → what to say + metadata, from cells + letters.

    Letter-name keys (``letter_*``) are spoken as the letter's *name*
    (e.g. ``أَلِف``) rather than the bare glyph, because tapping a letter in
    Lesson 1 should say its name. All other keys are spoken as the cell text.
    """
    cells = load_rows("qaida_cells.json")
    letters = load_rows("qaida_letters.json")

    targets: dict[str, dict] = {}
    for c in cells:
        key = (c.get("audio_key") or "").strip()
        if not key:
            raise SystemExit(f"cell id={c.get('id')} has no audio_key")
        targets.setdefault(key, {
            "audio_key": key,
            "text": (c.get("text_arabic") or "").strip(),
            "transliteration": c.get("transliteration"),
            "token_type": c.get("token_type"),
        })

    # Override letter-name keys with the canonical letter name for clearer audio.
    for l in letters:
        key = (l.get("audio_key") or "").strip()
        if not key:
            continue
        targets.setdefault(key, {
            "audio_key": key,
            "text": "",
            "transliteration": l.get("name_transliteration"),
            "token_type": "LETTER_NAME",
        })
        targets[key]["text"] = (l.get("name_arabic") or "").strip()
        targets[key]["transliteration"] = l.get("name_transliteration")
        if targets[key]["token_type"] in (None, "LETTER"):
            targets[key]["token_type"] = "LETTER_NAME"

    for key, t in targets.items():
        if not t["text"]:
            raise SystemExit(f"audio_key '{key}' has empty text to synthesize")
    return dict(sorted(targets.items()))


def find_raw(key: str) -> Path | None:
    for ext in RAW_EXTS:
        p = RAW_DIR / f"{key}{ext}"
        if p.exists():
            return p
    return None


# ── ffmpeg / espeak helpers ───────────────────────────────────────────
def run(cmd: list[str]) -> None:
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"{cmd[0]} failed: {res.stderr.strip().splitlines()[-1:]}")


def synth_wav(text: str, dst: Path) -> None:
    run(["espeak-ng", "-v", ESPEAK_VOICE, "-s", ESPEAK_SPEED,
         "-p", ESPEAK_PITCH, "-w", str(dst), text])


def encode(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", str(src),
         "-ac", "1", "-af", AFILTER, "-c:a", "libmp3lame", "-b:a", MP3_BITRATE,
         str(dst)])


def probe_duration_ms(path: Path) -> int:
    res = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", str(path)],
        capture_output=True, text=True)
    try:
        return round(float(res.stdout.strip()) * 1000)
    except ValueError:
        return 0


def build_one(t: dict, tmp: Path) -> dict:
    key = t["audio_key"]
    raw = find_raw(key)
    if raw is not None:
        source, license_ = "recording", LICENSE_RECORDING
        src = raw
    else:
        source, license_ = "tts:espeak-ng", LICENSE_TTS
        src = tmp / f"{key}.wav"
        synth_wav(t["text"], src)

    out = OUT_DIR / f"{key}.mp3"
    encode(src, out)
    return {
        "audio_key": key,
        "filename": out.name,
        "text_arabic": t["text"],
        "transliteration": t["transliteration"],
        "token_type": t["token_type"],
        "duration_ms": probe_duration_ms(out),
        "bytes": out.stat().st_size,
        "source": source,
        "license": license_,
    }


# ── Verification ──────────────────────────────────────────────────────
def cross_check(targets: dict[str, dict]) -> list[str]:
    problems = []
    on_disk = {p.stem for p in OUT_DIR.glob("*.mp3")} if OUT_DIR.exists() else set()
    missing = sorted(set(targets) - on_disk)
    orphan = sorted(on_disk - set(targets))
    if missing:
        problems.append(f"{len(missing)} missing clip(s): {missing[:8]}{' …' if len(missing) > 8 else ''}")
    if orphan:
        problems.append(f"{len(orphan)} orphan clip(s): {orphan[:8]}{' …' if len(orphan) > 8 else ''}")
    return problems


# ── CREDITS.md ────────────────────────────────────────────────────────
def write_credits(entries: list[dict]) -> None:
    n_rec = sum(1 for e in entries if e["source"] == "recording")
    n_tts = len(entries) - n_rec
    lines = [
        "# Qaida audio — credits & provenance",
        "",
        "Generated by `scripts/process_qaida_audio.py` (sub-issue B of #171, issue #173).",
        "Every clip is mono, silence-trimmed and EBU R128 loudness-normalized "
        f"(I=-16, TP=-1.5), encoded mp3 @ {MP3_BITRATE}.",
        "",
        "## Sources",
        "",
        f"- **Synthesized — eSpeak NG (Arabic voice `{ESPEAK_VOICE}`)**: {n_tts} clip(s). "
        "Produced from the fully-diacritized text in `json/qaida_cells.json` / "
        "`json/qaida_letters.json`. eSpeak NG is GPL, but its *synthesized speech "
        "output* carries no redistribution restriction (treated like compiler "
        "output). These are a consistent, fully-reproducible default voice.",
        f"- **Custom recordings (`audio/raw/`)**: {n_rec} clip(s). "
        "Drop a clean recording named `<audio_key>.wav` (or .flac/.m4a/.mp3/.ogg) "
        "into `audio/raw/` and re-run; it overrides the synthesized clip. "
        "Document the reciter/source and its license here when you add them.",
        "",
        "## Replacing the synthesized voice with human recordings",
        "",
        "1. Record each token (see `audio/manifest.json` for the text of every "
        "`audio_key`), one clip per key, any common format.",
        "2. Name each file by its `audio_key` and place it in `audio/raw/`.",
        "3. Re-run `python3 scripts/process_qaida_audio.py` — recordings are "
        "preferred over TTS automatically; the manifest records which is which.",
        "",
        "## Per-key source",
        "",
        "See `audio/manifest.json` (`source` + `license` fields) for the exact "
        "provenance of each of the "
        f"{len(entries)} clips.",
        "",
    ]
    (AUDIO_DIR / "CREDITS.md").write_text("\n".join(lines), encoding="utf-8")


# ── Main ──────────────────────────────────────────────────────────────
def main() -> int:
    ap = argparse.ArgumentParser(description="Build the Qaida audio pack.")
    ap.add_argument("--check", action="store_true",
                    help="verify the existing pack/manifest only; do not encode.")
    ap.add_argument("--jobs", type=int, default=8, help="parallel encode workers.")
    ap.add_argument("--clean", action="store_true",
                    help="delete the output dir before building (drops orphans).")
    args = ap.parse_args()

    for tool in ("ffmpeg", "ffprobe"):
        if shutil.which(tool) is None:
            print(f"error: '{tool}' not found on PATH", file=sys.stderr)
            return 2

    targets = build_targets()
    print(f"audio_keys to produce: {len(targets)}")

    if args.check:
        problems = cross_check(targets)
        man = AUDIO_DIR / "manifest.json"
        if not man.exists():
            problems.append("manifest.json missing")
        for p in problems:
            print("  ✗", p)
        print("OK" if not problems else "FAILED")
        return 1 if problems else 0

    if shutil.which("espeak-ng") is None and any(find_raw(k) is None for k in targets):
        print("error: 'espeak-ng' not found and some keys have no raw recording",
              file=sys.stderr)
        return 2

    if args.clean and OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    RAW_DIR.mkdir(parents=True, exist_ok=True)

    entries: list[dict] = []
    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td)
        with ThreadPoolExecutor(max_workers=args.jobs) as ex:
            futs = {ex.submit(build_one, t, tmp): t["audio_key"] for t in targets.values()}
            done = 0
            for fut in as_completed(futs):
                entries.append(fut.result())
                done += 1
                if done % 50 == 0 or done == len(futs):
                    print(f"  encoded {done}/{len(futs)}")

    entries.sort(key=lambda e: e["audio_key"])
    total_bytes = sum(e["bytes"] for e in entries)
    n_rec = sum(1 for e in entries if e["source"] == "recording")
    n_tts = len(entries) - n_rec

    manifest = {
        "schema": "qaida-audio-manifest/1",
        "generated_by": "scripts/process_qaida_audio.py",
        "format": "mp3",
        "bitrate": MP3_BITRATE,
        "channels": 1,
        "loudness": "EBU R128 I=-16 TP=-1.5 LRA=11",
        "delivery": "bundled:app/src/main/assets/qaida/audio/",
        "count": len(entries),
        "total_bytes": total_bytes,
        "sources": {"recording": n_rec, "tts:espeak-ng": n_tts},
        "clips": entries,
    }
    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    (AUDIO_DIR / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_credits(entries)

    problems = cross_check(targets)
    mb = total_bytes / (1024 * 1024)
    print(f"\nproduced {len(entries)} clips — {mb:.2f} MB total "
          f"({n_tts} synthesized, {n_rec} recorded)")
    print(f"delivery: {'BUNDLED (Option 1)' if mb <= 10 else 'consider DOWNLOAD pack (Option 2)'} "
          f"— pack is {mb:.2f} MB vs ~8-10 MB bundle threshold")
    print(f"output:   {OUT_DIR.relative_to(REPO_ROOT)}/")
    print(f"manifest: {(AUDIO_DIR / 'manifest.json').relative_to(REPO_ROOT)}")

    if problems:
        for p in problems:
            print("  ✗", p)
        print("FAILED — pack is not 1:1 with the data")
        return 1
    print("cross-check: zero missing, zero orphan ✓")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
