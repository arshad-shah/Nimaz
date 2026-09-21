# Learn to Pray — illustration review record

These generated review assets are not verified anatomical diagrams or depictions of the Prophet ﷺ.
Instructions and sources remain native text. The religious, posture and device review gates in
ARCHITECTURE.md §9 apply before release.

## Runtime assets

Directory: `feature/content/src/main/res/drawable-nodpi/`.
Male filenames: `learn_pray_<pose>.webp`. Female: `learn_pray_female_<pose>.webp`.
Poses in both sets: takbir, standing, bowing, rise, prostration, sitting, tashahhud, right, left.
Repeated movements reuse assets. Salam means the worshipper's own right/left: never mirror for RTL.

Generated with the built-in image-generation tool, then encoded as WebP at quality 82.
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
- **bowing:** Bow in ruku side view, torso horizontal, hands resting ON knee joints not shins, head aligned with back. Khimar stays covering neck and chest and hangs naturally downward without exposing torso. Dress opaque and loose.
- **rise:** Stand upright front view after bowing, arms relaxed down at sides.
- **prostration:** Side view prostrating: forehead and nose against mat, palms beside head, elbows clear of mat, knees on mat, feet behind with sock-covered toes flexed on mat. Khimar remains in place and robe covers every leg and ankle, no bare toes.
- **sitting:** Side three-quarter view sitting upright on folded legs, hands resting flat on thighs. Full hair neck chest arms legs and feet covered, only face and hands visible.
- **tashahhud:** Three-quarter view sitting upright on folded legs. Left hand flat on left thigh, right hand on right thigh with right index finger extended forward and thumb/middle finger a ring. Show right hand in foreground.
- **right:** Front view seated on folded legs, torso faces camera, ONLY head turned to HER RIGHT (toward LEFT edge of image), nose points toward image LEFT. Hands on thighs.
- **left:** Front view seated on folded legs, torso faces camera, ONLY head turned to HER LEFT (toward RIGHT edge of image), nose points toward image RIGHT. Hands on thighs.
- **standing:** Stand quietly three-quarter view with right hand over left forearm in front of torso. Same as reference but feet wear smooth opaque ivory fabric socks with NO individual toe outlines.

Ruku/sujud refinement prompts use the male posture as the pose reference and the female standing
asset as the costume/identity reference: preserve hand-knee position and level back for ruku;
forehead/nose and palms contact the mat with forearms raised for sujud. Fully cover hair, ears,
neck, chest, arms, legs and feet, leaving face and hands visible.

## Required visual review

- [ ] Qualified review of Arabic, transliteration, meaning and cited evidence.
- [ ] Ruku: inspect hand/knee placement and back/head alignment through the loose garment.
- [ ] Sujud: inspect forehead/nose, palm, knee and toe contact; forearms must not rest on the mat.
- [ ] Sitting/tashahhud: inspect foot arrangement and the right-hand gesture.
- [ ] Wardrobe: loose opaque cream thobe/white kufi throughout the male set; loose opaque sage
      dress/long ivory khimar and covered feet throughout the female set.
- [ ] Confirm no exposed female hair, ears, neck, forearms, legs or ankles in any movement.
- [ ] Review sock rendering, garment drape and wrist coverage at actual phone size.
- [ ] Verify salam direction from the worshipper's perspective.
- [ ] Render on device: dark/light, 200% font scale, TalkBack, rotation and tablet.

Both sets share the lesson sequence/words. The app does not invent gender-specific movement
rules. The clothing notes cite Abu Dawud 641 and distinguish the disputed prophetic attribution
of Abu Dawud 640. Covered feet in art are not evidence of an undisputed ruling.
