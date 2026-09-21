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

The four-page flow uses welcome, prayer, learning and progress art; Quran and permissions
art remain unused resources. The learning message accompanies Learn to Pray PR #643.
Permission setup is an optional shared bottom sheet after page four. Reminder/progress
overlays are native controls and explicitly labeled examples, not fabricated personal data.
All intro copy is translated into en/de/fr/id/ms/tr without exceptions.

## Review on device before merge

- Walk all four pages, Back, Skip and Let's Begin; grant/deny permissions in the optional
  setup sheet, dismiss it, and test Get Started and Not Now. Completion must fire once.
- Confirm artwork travels with each page, eased Next/Back motion, Back fading without
  a layout jump, stable primary-button width, and no double advance on rapid taps.
- Check 320dp width, compact landscape, tablet and 200% font scale; body content must scroll and
  navigation remain reachable. Text must not collide with artwork highlights.
- Check light/dark app preferences: the onboarding art intentionally retains the fixed dark
  branding, while typography, spacing and controls use the shared Nimaz design system.
- Deny and grant each permission; verify returning from Android settings updates the cards.
- Run `:feature:onboarding:testDebugUnitTest` and `:feature:onboarding:lintDebug`.

The local environment could not download Gradle 9.5.1. No emulator screenshot or passing Android
build is claimed; native rendering and CI remain required before merge.
