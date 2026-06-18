# Qaida Audio (tap-to-hear clips)

Covers sub-issue **B** of the
[Qaida Reader epic (#171)](https://github.com/arshad-shah/Nimaz/issues/171) —
[issue #173](https://github.com/arshad-shah/Nimaz/issues/173): produce one short
audio clip for **every** `audio_key` authored by sub-issue A, preprocessed for
instant tap-to-play. Schema/DB loading is sub-issue **C**; playback wiring is
sub-issue **F**.

## What's produced

| Artifact | Where | What |
|----------|-------|------|
| Audio pack | `app/src/main/assets/qaida/audio/<audio_key>.mp3` | 434 clips, **2.05 MB** total |
| Manifest | `audio/manifest.json` | `audio_key → filename, text, duration_ms, bytes, source, license` |
| Credits | `audio/CREDITS.md` | per-source provenance & license |
| Drop-in dir | `audio/raw/` | optional human recordings that override TTS |

One clip per `audio_key` across all token types — letters, harakat, tanween,
madd, leen, sukoon, shadda, muqatta'at, syllables and words. The pack is
**1:1** with the data: the build fails on any missing or orphan clip.

## Source: one consistent voice

No public dataset covers the syllable/word tokens, and the Qaida wants a single
child-friendly voice throughout. Each `audio_key` is sourced in this order:

1. **Human recording** — `audio/raw/<audio_key>.{wav,flac,m4a,mp3,ogg,opus}`
   if present (the drop-in path for a final release).
2. **Synthesized** — otherwise generated with **eSpeak NG** (Arabic voice) from
   the fully-diacritized text the content task already authored. Robotic but
   consistent, complete, reproducible, and free of licensing entanglements
   (synthesized speech output is not encumbered by the engine's GPL). This is
   the functional default that unblocks sub-issues C/F; swap in recordings any
   time by dropping files in `audio/raw/` and re-running — no code change.

`letter_*` keys are spoken as the letter **name** (e.g. `أَلِف`), not the bare
glyph, because tapping a letter in Lesson 1 should say its name.

## Preprocessing (ffmpeg)

Identical for recordings and TTS: **mono** → **trim** leading/trailing silence
(so taps feel instant) → **loudness-normalize** to EBU R128 (I=-16, TP=-1.5) →
encode **mp3 @ 56 kbps** (a few KB per clip).

## Delivery decision: **bundled (Option 1)**

The whole pack is **2.05 MB** — far under the ~8–10 MB bundle threshold — so it
ships in `app/src/main/assets/qaida/audio/`, consistent with how the app already
bundles the prepopulated DB and fonts. It works offline immediately with no
download step. (If the pack later grows past ~10 MB, the manifest doubles as a
download manifest for the on-demand `AdhanDownloadWorker`/WorkManager pattern —
Option 2.)

## Reproducing

```bash
cd nimaz-pro-data
python3 scripts/process_qaida_audio.py          # build the pack + manifest + credits
python3 scripts/process_qaida_audio.py --check   # verify pack is 1:1 with the data
```

Requires `ffmpeg`/`ffprobe` on PATH, plus `espeak-ng` for any key without a
human recording in `audio/raw/`.
