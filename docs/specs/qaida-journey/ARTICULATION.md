# Letter articulation

The letter explorer's detail sheet teaches where each of the 29 letters is made **in words
only**. Under *Where it's made* it shows the broad articulation area (`MakhrajArea`, with its
cue) and the letter-specific makhraj description from the content artifact
(`QaidaMakhrajHelper`). A letter with no recorded description shows the area alone.

There are no articulation diagrams. The raster pose schematics (and every other Qaida
illustration) were removed: a static picture could not show voicing, timing or secondary
articulation, and the anatomy needed qualified review that it never received. The textual
guidance, the recordings and the learner's teacher are the teaching surface;
`scripts/check_qaida_resources.py` fails if a `qaida_*` drawable is added back.

The letter-detail flow is captured with every other Qaida state in [PREVIEWS.md](PREVIEWS.md).

Reference checks for the written descriptions (not endorsements):
- [LMU Arabic place-of-articulation teaching notes](https://www.phonetik.uni-muenchen.de/~hoole/kurse/artikul/arabic.pdf): velar/uvular and pharyngeal/glottal distinctions, including limitations of broad pharyngeal labels.
- [LMU secondary articulation notes](https://www.phonetik.uni-muenchen.de/~hoole/kurse/artikul/secondary_articulations_n.pdf): Arabic emphatic secondary articulation.
- The app’s existing `nimaz-pro-data/json/qaida_letters.json`: letter-specific makhraj descriptions.
