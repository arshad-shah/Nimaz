# Qaida screen previews

These are native Robolectric renders of the actual Compose screens at 411 × 900 dp,
using the Nimaz light/dark themes and sample progress. They are not image-generated mockups.
The recording-ready state is simulated for this preview; no recordings ship with this branch.

Updated to follow the supplied reference using existing Nimaz theme colours exclusively,
including the primary, secondary and tertiary chapter surfaces. Serif headings use the
bundled Amiri face; cards, buttons, progress and lesson medallions reuse existing components.

The focused preview uses three sample short-vowel cards in lesson 4 (its actual curriculum
position). The completion preview records three self-checks and no completed listening, so
only practice receives a checkmark. Smaller screens and longer content scroll naturally.

Validation: 168 Qaida-related unit/component tests and four native render checks passed.
These captures are at normal font size; device, TalkBack, RTL and large-text checks remain
release validation tasks.

## Journey

![Qaida journey](previews/journey.png)

## Focused learning

![Focused learning](previews/focus.png)

## Dark theme

![Focused learning in dark theme](previews/focus-dark.png)

## Completion

![Practice completion](previews/reward.png)
