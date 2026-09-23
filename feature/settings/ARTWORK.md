# Prayer settings illustrations

Four illustrations explain the three prayer-time choices that are hard to put into words. They
head the pickers in `PrayerSettingsScreen` and live in `src/main/res/drawable-nodpi/` as WebP
(quality 85, 1536×1024). No text, numbers or UI are baked into them; the sentence under each
picture says what it shows, and each image's `contentDescription` is `null` (decorative).

Generated with Codex's built-in image generation in the Learn to Pray / onboarding style —
flat painterly, subtle grain, deep teal `#10444A` / `#061A1C`, champagne gold `#D9B26A`, warm
ivory — then corrected by hand where the geometry had to be exact.

| Asset | Picker | What it shows, and how it was checked |
| --- | --- | --- |
| `prayer_settings_asr_standard.webp` | Asr (Standard) | One staff, one sun, one shadow, side-on. Two ivory pebbles sit at **exactly 1× and 2×** the staff's height (366 px → 827 px and 1193 px from the base, moved by hand after generation). The shadow is **1.16×**: just past the first pebble. |
| `prayer_settings_asr_hanafi.webp` | Asr (Hanafi) | The same staff, pebbles and ground **pixel for pixel** (composited from the Standard image). Later afternoon: the sun lower but well above the hills — not sunset — and the shadow **2.14×**, just past the second pebble. Crossfades with the Standard image as the option changes. |
| `prayer_settings_high_latitude.webp` | High-latitude rule | Midnight on a northern bay: the horizon still glows under a deep-teal sky. No drawn sun path — every generated path read as a V, and the preview arc above the picker already shows where Fajr and Isha land. |
| `prayer_settings_twilight_angle.webp` | Calculation method | Pre-dawn landscape over an astrolabe-style cut-away; three fine gold rays fall from the horizon to the hidden sun at **shallow, close angles (~15°–20°)**, the real range methods use. The first draft's steep rays were rejected. |

Revise an image by editing it, not regenerating: the Asr pair must stay pixel-identical except
for the sun and shadow, or the crossfade shows a jump.
