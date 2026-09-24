# Letter articulation previews

The explorer uses **24 distinct raster teaching schematics for the 29 letters**, with native,
localized captions. Gold rings identify the articulation area; teal arrows indicate airflow.
Tongue profiles, lip contact, glottal closure and nasal routing change with the selected letter.
Emphatic consonants have additional tongue-root highlighting. Dad uses a superior schematic
of lateral tongue contact with the upper molars; waw includes a frontal lip-rounding inset.
Shared visible positions may share an asset; voicing and dynamic timing are explained by the
existing letter guidance and audio, not by a static image.

These are simplified position diagrams, not scans, motion simulations or certified teaching
material. Qualified tajweed/anatomy review remains required before release, particularly for
pharyngeal/epiglottal distinctions, lateral dad, and emphatic secondary articulation. The view
for dad does not depict its secondary pharyngeal constriction. Consonant waw/ya and the
nasal-resonance explanations remain present. Arrows describe a route, not timing or airflow rate.

The assets are drawn directly to WebP with Matplotlib Agg; no SVG or generative-image anatomy
is used in the current explorer. Their editable geometry is in `scripts/render_qaida_articulation.py`;
the glyph-to-asset inventory is [ARTICULATION_ASSETS.json](ARTICULATION_ASSETS.json).

Reference checks (not endorsements or copied artwork):
- [LMU Arabic place-of-articulation teaching notes](https://www.phonetik.uni-muenchen.de/~hoole/kurse/artikul/arabic.pdf): velar/uvular and pharyngeal/glottal distinctions, including limitations of broad pharyngeal labels.
- [LMU secondary articulation notes](https://www.phonetik.uni-muenchen.de/~hoole/kurse/artikul/secondary_articulations_n.pdf): Arabic emphatic secondary articulation.
- The app’s existing `nimaz-pro-data/json/qaida_letters.json`: letter-specific makhraj descriptions.

## Roborazzi captures

Source commit: `73c6732776e1dd05ce159db3f0be5bb5f0d78473`.

| Letter | Light | Dark |
| --- | --- | --- |
| ا | ![Light](previews/articulation-01.png) | ![Dark](previews/articulation-01-dark.png) |
| ب | ![Light](previews/articulation-02.png) | ![Dark](previews/articulation-02-dark.png) |
| ت | ![Light](previews/articulation-03.png) | ![Dark](previews/articulation-03-dark.png) |
| ث | ![Light](previews/articulation-04.png) | ![Dark](previews/articulation-04-dark.png) |
| ج | ![Light](previews/articulation-05.png) | ![Dark](previews/articulation-05-dark.png) |
| ح | ![Light](previews/articulation-06.png) | ![Dark](previews/articulation-06-dark.png) |
| خ | ![Light](previews/articulation-07.png) | ![Dark](previews/articulation-07-dark.png) |
| د | ![Light](previews/articulation-08.png) | ![Dark](previews/articulation-08-dark.png) |
| ذ | ![Light](previews/articulation-09.png) | ![Dark](previews/articulation-09-dark.png) |
| ر | ![Light](previews/articulation-10.png) | ![Dark](previews/articulation-10-dark.png) |
| ز | ![Light](previews/articulation-11.png) | ![Dark](previews/articulation-11-dark.png) |
| س | ![Light](previews/articulation-12.png) | ![Dark](previews/articulation-12-dark.png) |
| ش | ![Light](previews/articulation-13.png) | ![Dark](previews/articulation-13-dark.png) |
| ص | ![Light](previews/articulation-14.png) | ![Dark](previews/articulation-14-dark.png) |
| ض | ![Light](previews/articulation-15.png) | ![Dark](previews/articulation-15-dark.png) |
| ط | ![Light](previews/articulation-16.png) | ![Dark](previews/articulation-16-dark.png) |
| ظ | ![Light](previews/articulation-17.png) | ![Dark](previews/articulation-17-dark.png) |
| ع | ![Light](previews/articulation-18.png) | ![Dark](previews/articulation-18-dark.png) |
| غ | ![Light](previews/articulation-19.png) | ![Dark](previews/articulation-19-dark.png) |
| ف | ![Light](previews/articulation-20.png) | ![Dark](previews/articulation-20-dark.png) |
| ق | ![Light](previews/articulation-21.png) | ![Dark](previews/articulation-21-dark.png) |
| ك | ![Light](previews/articulation-22.png) | ![Dark](previews/articulation-22-dark.png) |
| ل | ![Light](previews/articulation-23.png) | ![Dark](previews/articulation-23-dark.png) |
| م | ![Light](previews/articulation-24.png) | ![Dark](previews/articulation-24-dark.png) |
| ن | ![Light](previews/articulation-25.png) | ![Dark](previews/articulation-25-dark.png) |
| ه | ![Light](previews/articulation-26.png) | ![Dark](previews/articulation-26-dark.png) |
| و | ![Light](previews/articulation-27.png) | ![Dark](previews/articulation-27-dark.png) |
| ي | ![Light](previews/articulation-28.png) | ![Dark](previews/articulation-28-dark.png) |
| ء | ![Light](previews/articulation-29.png) | ![Dark](previews/articulation-29-dark.png) |
