# Learn to Pray — illustration review record

These generated review assets are not verified anatomical diagrams or depictions of the Prophet ﷺ.
Instructions and sources remain native text. The religious, posture and device review gates in
ARCHITECTURE.md §9 apply before release.

## Runtime assets

Directory: `feature/content/src/main/res/drawable-nodpi/`.
Male filenames: `learn_pray_<pose>.webp`. Female: `learn_pray_female_<pose>.webp`.
Poses in both sets: takbir, standing, bowing, rise, prostration, sitting, tashahhud, right, left.
Repeated movements reuse assets. Salam means the worshipper's own right/left: never mirror for RTL.

Generated with the built-in image-generation tool. Male WebP quality is 82. Eight female
assets are cropped without labels from the owner-approved revision-3 sheet (444×295,
WebP quality 90), preserving the approved drawings rather than regenerating them. The
separately approved final sujud edit is encoded at 1200×800, quality 88.
No stock images, embedded UI text or unreviewed recordings are bundled.

## Prompt set

Male base:

> Use case: illustration-story. Production art for Nimaz Learn to Pray Android feature. One single respectful anonymous contemporary adult Muslim learner, cream thobe, white kufi, short dark beard, barefoot on a dark teal prayer mat. Refined flat painterly editorial illustration, subtle paper grain, restrained deep teal and warm ivory palette, plain deep teal backdrop, no architecture, no text, no UI, no labels, entire figure and mat visible with generous padding, landscape 3:2. This is a generic learner, NOT a prophet. 

Pose variants: raised palms at shoulders; right hand over left forearm; horizontal bow with palms
on kneecaps; upright with arms lowered; forehead/nose, palms, knees and toes supported with
forearms raised; sitting upright with hands on thighs; right index finger extended;
head alone turned to the worshipper's right and then left.

The final male bow edit explicitly changes the shirt/trousers draft into the same loose cream
long-sleeved ankle-length thobe while preserving the pose. Thobe/kufi are wardrobe choices,
not required uniforms for prayer.

Female base:

> Use case: illustration-story. Production Nimaz Learn to Pray decorative illustration. One generic contemporary adult Muslim woman, medium olive skin, simple natural face, no makeup or jewellery. CONSISTENT COSTUME: plain warm ivory opaque extra-long khimar covering every strand of hair, ears, neck, shoulders and chest and draping to hips; plain muted sage-green very loose opaque full-length prayer dress, full sleeves to wrists, no waist definition; ivory opaque socks covering feet whenever visible. Only face and hands uncovered. Never show hair, neck, forearms, ankles or bare feet. No niqab. Refined flat painterly editorial illustration, subtle grain, deep teal plain backdrop, same dark teal prayer mat with restrained gold trim. Full figure and mat visible with generous margin. Landscape 3:2. No text, no UI, no labels. 

All female variants use the female standing reference for character/clothing consistency.
Additional instruction: keep ivory khimar, sage prayer dress and opaque socks in every pose.

- **takbir:** Stand upright front view with both hands raised beside shoulders, palms forward. Sleeves stay at wrists; no forearms exposed.
- **bowing:** User-selected review variant: shallow bow, hands on lower thighs above knees, fingers together, arms close. Preserve khimar coverage and opaque loose dress. This hand placement is not established by the checked source and needs a supporting reference before release.
- **rise:** Stand upright front view after bowing, arms relaxed down at sides.
- **prostration:** Compact side-view variant: lower hips, abdomen near thighs, limbs close, forehead/nose and palms supported by mat. Khimar and loose dress cover hair, neck, chest, arms, legs and ankles.
- **sitting:** Front view, upright on mat, both lower legs folded to her right (image left), hands flat on thighs, fingers together.
- **tashahhud:** Same side-sitting base, left hand flat, right index finger extended. Right hand is on image left; never mirror the artwork for RTL.
- **right:** Same side-sitting base, ONLY head turns to HER RIGHT (image left). Hands on thighs.
- **left:** Same side-sitting base, ONLY head turns to HER LEFT (image right). Hands on thighs.
- **standing:** Stand quietly three-quarter view with right hand over left forearm in front of torso. Same as reference but feet wear smooth opaque ivory fabric socks with NO individual toe outlines.

Refinements preserve each female edit target as the costume/identity reference; the male pose
is no longer the female pose template. Takbir sleeve cuffs extend to the hands. Feet are requested
as smooth opaque ivory fabric socks. Some toe outlines remain ambiguous and require visual review.
All nine female assets were replaced after explicit owner approval. Final revision prompts:
takbir hands closer to head; qiyam right palm over left high on chest without X wrists;
deeper dal-like ruku with palms on thighs; natural uncompressed sujud with toes tucked,
heels raised, palms closer beside face and elbows just above mat; smooth opaque ivory
sock fabric; sitting mat's long axis aligned with forward prayer direction. Other poses
were preserved from the approved review sheet. No new generation happened at extraction.

Reference checks (not a universal ruling):
- [SeekersGuidance](https://seekersguidance.org/answers/hanafi-fiqh/what-is-the-proper-method-for-women-to-bow-and-prostrate-in-the-prayer/): compact prostration and shallower bow, but **hands on knees**, not thighs.
- [Askimam guidance](https://islamqa.org/hanafi/askimam/126663/womans-sitting-posture-in-salah/): seated feet to the woman's right.
- The hands-on-thighs override comes from the product owner's explicit request. Do not attribute
  it to either checked source or call it a verified prophetic instruction.

## Required visual review

- [ ] Qualified review of Arabic, transliteration, meaning and cited evidence.
- [ ] Ruku: inspect each selected variant's hand placement and head/back alignment; obtain a source for the female override.
- [ ] Sujud: inspect forehead/nose, palm, knee and toe contact and the selected variant's arm placement.
- [ ] Sitting/tashahhud: inspect foot arrangement and the right-hand gesture.
- [ ] Wardrobe: loose opaque cream thobe/white kufi throughout the male set; loose opaque sage
      dress/long ivory khimar and covered feet throughout the female set.
- [ ] Confirm no exposed female hair, ears, neck, forearms, legs or ankles in any movement.
- [ ] Review sock rendering, garment drape and wrist coverage at actual phone size.
- [ ] Verify salam direction from the worshipper's perspective.
- [ ] Render on device: dark/light, 200% font scale, TalkBack, rotation and tablet.

Both sets share the lesson sequence/recitations; posture text and sources follow the selected
figure. Female ruku explicitly remains an unverified editorial variant. The clothing notes cite Abu Dawud 641 and distinguish the disputed prophetic attribution
of Abu Dawud 640. Covered feet in art are not evidence of an undisputed ruling.
