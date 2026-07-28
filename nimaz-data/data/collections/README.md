# Collections

One directory per collection: `collection.yaml` + `records.ndjson`.

**This directory is empty until the §11 bootstrap runs against the real corpus.**
`nz init` writes it from the sealed vault — see [`../../README.md`](../../README.md).
Committing sources before `nz build --against-vault` proves the export lossless
would mean trusting a re-encoding nobody has checked.

A collection is the unit of versioning, validation, shipping and rollback. Adding
one is a file here, not a change to the tool; see the README for the shape of
`collection.yaml` and what `kind:` buys you.
