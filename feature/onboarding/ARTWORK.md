# Illustrated onboarding assets

Six original illustrations generated with the built-in image-generation tool from the supplied
Nimaz onboarding reference, then encoded as WebP at quality 85 without resizing. They live in
`src/main/res/drawable-nodpi/`. No text or functioning UI is baked into them.

Shared prompt: portrait 2:3, layered painterly/vector-like Islamic illustration; Nimaz deep
teal `#061A1C` / `#0A2A2A`, muted greens and warm cream/gold dawn light; quiet dark title and
footer regions; full bleed; no text, UI, buttons, logos or phone frame.

| Asset | Subject prompt |
| --- | --- |
| `onboarding_welcome.webp` | Pointed arch, crescent, mountain mosque, prayer rug, lantern and plant. |
| `onboarding_prayer.webp` | Open sunrise landscape, layered mountains, mosque and minaret at right, foreground foliage. |
| `onboarding_learning.webp` | Adult man in cream thobe and cap standing in prayer on a mat, lower left, hands folded, mihrab behind. Decorative, not an instructional posture diagram. |
| `onboarding_quran.webp` | Open Quran respectfully resting on a wooden rehal, warm lantern and mosque interior. No legible scripture. |
| `onboarding_progress.webp` | Sunrise mountain view through an arch, mosque silhouette, leafy plants, dark lower region for native content. |
| `onboarding_permissions.webp` | Open mosque arch, sunrise and plants, lower dark region for three permission cards. |

The reference's visual direction is adapted to shipped functionality: Learning describes Qaida
and the content library. There is no claim of a step-by-step Salah tutor or fabricated user
progress. Existing localized strings are reused across all shipped locales.

## Review on device before merge

- Walk all six pages, Back, Skip and Get Started; confirm permission prompts remain on the last page.
- Check 320dp width, compact landscape, tablet and 200% font scale; body content must scroll and
  navigation remain reachable. Text must not collide with artwork highlights.
- Check light/dark app preferences: the onboarding art intentionally retains the fixed dark
  branding, while typography, spacing and controls use the shared Nimaz design system.
- Deny and grant each permission; verify returning from Android settings updates the cards.
- Run `:feature:onboarding:testDebugUnitTest` and `:feature:onboarding:lintDebug`.

The local environment could not download Gradle 9.5.1. No emulator screenshot or passing Android
build is claimed; native rendering and CI remain required before merge.
