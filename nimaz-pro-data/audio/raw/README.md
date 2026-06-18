# Drop-in human recordings

Place clean recordings here named by `audio_key` (e.g. `letter_alif.wav`,
`l4_ba.wav`) — accepted extensions: `.wav .flac .m4a .mp3 .ogg .opus`.
Any file present here **overrides** the synthesized (eSpeak NG) clip for that
key. See `../manifest.json` for the text/transliteration of every key, and
`../CREDITS.md` for the workflow. Re-run `scripts/process_qaida_audio.py`
after adding files.
