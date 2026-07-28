# Changes — the single write funnel

Every write lands here as a directory: `change.yaml`, `up.sql`, `down.sql`.
Console edits, hand-written SQL and agent proposals produce the same artifact, so
the pipeline cannot tell them apart and treats them identically. `origin:` is
recorded for audit and is never branched on.

Ordering is by id (timestamp prefix). Builds are full rebuilds — sources plus
every unfolded change, applied in order, from zero — so there is no "applied"
state that can drift from reality.

`applied/` holds folded changes, kept for audit. A fold is only committed if the
artifact hash is identical before and after; one that changes the output is a bug.
