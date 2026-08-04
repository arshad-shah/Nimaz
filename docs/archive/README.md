# Archived documentation

**Nothing in this directory is a source of truth.** These are superseded planning documents,
kept because they explain *why* parts of the codebase look the way they do — not how anything
works today. They are never updated, and they are exempt from the documentation contract
(`DOC-01` / `DOC-02` in [`../DOCUMENTATION.md`](../DOCUMENTATION.md)).

If one of these files contradicts a doc in `docs/`, the doc in `docs/` is right. If it
contradicts the code, the code is right.

| File | What it was | Read instead |
|---|---|---|
| `nimaz-pro-technical-foundation.md` | the original technical design for the rewrite | [`../ARCHITECTURE.md`](../ARCHITECTURE.md), [`../SUBSYSTEMS.md`](../SUBSYSTEMS.md) |
| `nimaz-pro-data-guide.md` | the planned content/data model | [`../SUBSYSTEMS.md`](../SUBSYSTEMS.md) §5–§7, and the `arshad-shah/nimaz-data` repository |
| `nimaz-pro-ui-tasks.md` | the UI build-out task list | [`../ARCHITECTURE.md`](../ARCHITECTURE.md) §8 (design system), [`../NAVIGATION.md`](../NAVIGATION.md) |
| `nimaz-pro-claude-code-tasks.md` | the agent task breakdown for the same build-out | [`../DOCUMENTATION.md`](../DOCUMENTATION.md), [`../../CLAUDE.md`](../../CLAUDE.md) |

**Naming.** These files predate the current name and package: they say "Nimaz Pro" and
`com.nimazpro.app`. The app is **Nimaz** and the package is **`com.arshadshah.nimaz`**.

**Retiring a doc into here:** move it, add its row above with what replaced it, and remove its
row from [`../README.md`](../README.md). See
[`../DOCUMENTATION.md` §6](../DOCUMENTATION.md#6-adding-splitting-and-retiring-docs).
