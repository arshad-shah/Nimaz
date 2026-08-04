# Nimaz — Documentation Contract

> **Owns:** the rules for writing, updating and organising everything in `docs/` — the ownership
> matrix, the house style, the diagram standard, and the mechanical drift checks.
> **Update when:** you add or retire a doc, add or change a `check_docs.py` check, or change the
> house style.
> **Verified by:** `python3 scripts/check_docs.py --only DOC` (checks `DOC-01` … `DOC-03`).
> **Related:** [`README.md`](README.md) is the index of what exists; this file is the rules for
> keeping it true.

**The rule this whole document exists to state:**

> A change that alters behaviour, structure, or a contract is **not finished** until the doc that
> owns that area is updated in the same commit. Not a follow-up. Not an issue. The same commit.

Docs drift for one reason: updating them is optional at the moment of the change and expensive
afterwards. So the parts that can be checked by a machine are, and the parts that can't have an
explicit owner and an explicit trigger. This file is both lists.

---

## Contents

1. [Ownership matrix — what to update when](#1-ownership-matrix--what-to-update-when)
2. [House style — the anatomy of a doc](#2-house-style--the-anatomy-of-a-doc)
3. [Diagram standard](#3-diagram-standard)
4. [The drift checks](#4-the-drift-checks)
5. [Writing rules](#5-writing-rules)
6. [Adding, splitting and retiring docs](#6-adding-splitting-and-retiring-docs)
7. [Checklists — for humans and for agents](#7-checklists--for-humans-and-for-agents)

---

## 1. Ownership matrix — what to update when

Every doc owns a **disjoint** area. If two docs would say the same thing, one of them links to
the other instead. Find your change on the left; the middle column is mandatory.

| If your change… | Update | Enforced by |
|---|---|---|
| adds/renames/removes a `Route`, or wires a destination | [`NAVIGATION.md`](NAVIGATION.md) §3 + §2 diagram | `NAV-01` … `NAV-05` |
| adds/changes an announcement route key or its grammar | [`NAVIGATION.md`](NAVIGATION.md) §4 | `NAV-06` … `NAV-08` |
| adds/changes a help deep-link key | [`NAVIGATION.md`](NAVIGATION.md) §5 | `NAV-09`, `NAV-10` |
| adds/renames a Service, Worker, widget, notification channel, or DataStore file | [`SUBSYSTEMS.md`](SUBSYSTEMS.md) §0 inventory **and** the owning section | `SUB-02` … `SUB-06` |
| changes the DB schema / adds a migration | [`SUBSYSTEMS.md`](SUBSYSTEMS.md) §5 (incl. the schema-version line) | `SUB-01` |
| changes the FCM payload, announcement types, or celebration events | [`SUBSYSTEMS.md`](SUBSYSTEMS.md) §12 | `SUB-07` … `SUB-09` |
| changes audio, background work, notifications/alarms, preferences, content delivery, prayer-time calc, init/monitoring, sync, or sharing | [`SUBSYSTEMS.md`](SUBSYSTEMS.md) — the owning section | review |
| changes a layer pattern, DI convention, navigation pattern, or theming/design-system rule | [`ARCHITECTURE.md`](ARCHITECTURE.md) (and §9 if it resolves or introduces a deviation) | review |
| fixes or discovers a clean-architecture anti-pattern | [`CLEAN_ARCHITECTURE_CHECKLIST.md`](CLEAN_ARCHITECTURE_CHECKLIST.md) — tick it or add it | review |
| changes the Ask-with-Proof feature, the Worker, or the capability contract | [`ai-ask-with-proof.md`](ai-ask-with-proof.md) | review |
| adds/changes an instrumented test module or the CI test lane | [`TESTING.md`](TESTING.md) | review |
| retires shipped data, a seeder, or a migration path | [`DATA_RETIREMENT.md`](DATA_RETIREMENT.md) + [`retirement.yaml`](retirement.yaml) | review |
| changes how CI authenticates to the content repo | [`CONTENT_REPO_AUTH.md`](CONTENT_REPO_AUTH.md) | review |
| bundles, replaces or removes a font | [`FONT_LICENSES.md`](FONT_LICENSES.md) | review |
| adds a doc, or changes these rules | this file + [`README.md`](README.md) | `DOC-01`, `DOC-02` |

**Nothing owns "general knowledge".** If a fact has no owner in this table, it belongs in the
code as a comment, not in a doc — a fact with no owner is a fact nobody will update.

### 1.1 Precedence

When two sources disagree, this is the order of truth:

1. **The code.** Always. A doc that disagrees with the code is a bug in the doc.
2. **The doc that owns the area** per the matrix above.
3. **[`ARCHITECTURE.md`](ARCHITECTURE.md)** for anything architectural not otherwise owned.
4. **[`CLAUDE.md`](../CLAUDE.md)** — a summary and an entry point, never the detail. If CLAUDE.md
   and a `docs/` file disagree, the `docs/` file wins and CLAUDE.md needs fixing.

Historical documents (`docs/archive/`) and per-change design records (`docs/superpowers/`) are
**not** sources of truth at any level. They record what was intended at a moment in time.

---

## 2. House style — the anatomy of a doc

Every current doc in `docs/` opens with the same four-line block. `DOC-01` fails without it.

```markdown
# Nimaz — <Title>

> **Owns:** <the disjoint area this doc is the source of truth for>
> **Update when:** <the concrete triggers — "you add a Route", not "things change">
> **Verified by:** <the command, or "review only — no mechanical check">
> **Related:** <links to the neighbouring docs and what they own instead>
```

Then, in order:

1. A `---` rule.
2. A **Contents** list for anything over 150 lines, with working anchors — `DOC-04` requires the
   list, `DOC-03` requires the anchors to resolve.
3. Numbered `## N. Section` headings. Numbers are stable — they are cited from code comments and
   from other docs (`SUBSYSTEMS.md §12`), so **renumbering is a breaking change**. Add `§N.M`
   subsections rather than renumbering, and if a section truly must go, leave the number retired
   rather than shifting everything up.
4. Sections end with `---` between top-level sections in long docs.

**Naming.** Current docs are `SCREAMING_SNAKE.md` when they own a domain
(`ARCHITECTURE.md`, `SUBSYSTEMS.md`), `kebab-case.md` when they document one feature
(`ai-ask-with-proof.md`). Historical material lives under `docs/archive/`, design records under
`docs/superpowers/`.

---

## 3. Diagram standard

A diagram earns its place when it shows something a list cannot: a **flow**, an **ordering**, or
a **fan-out**. A diagram that restates a table is worse than the table — it is a second thing to
keep in sync. Prefer adding a row; reach for a picture when the shape is the point.

**Rules.**

- **Mermaid only**, in a ```` ```mermaid ```` fence, so it renders on GitHub with no toolchain.
- **Validate before committing** — this is checked in CI, not just advised:

  ```bash
  npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
  ```

  A broken diagram renders as a raw error block on GitHub — visible to everyone, noticed by
  nobody. Watch for the two that bite most often: a **semicolon** inside a `sequenceDiagram`
  message ends the statement, and an **unquoted `#` or `<br>`** in node text needs `"…"`.
- **Pick the type by what you're showing:**
  | Showing | Use |
  |---|---|
  | who depends on whom, what reaches what | `flowchart LR` / `TB` |
  | ordering across components over time | `sequenceDiagram` |
  | a lifecycle with named states | `stateDiagram-v2` |
  | table-shaped data | **a table** |
- **One idea per diagram.** If it needs more than ~15 nodes, split it — `NAVIGATION.md` §2 splits
  one unreadable graph into three readable ones.
- **Label the edges** that carry a decision (`-->|foreground|`, `-->|user taps|`). An unlabelled
  arrow between two boxes usually means the diagram hasn't decided what it's saying.
- **Node text may name code**, but keep it short: the class name, not the signature.
- **Say what the diagram omits.** Every graph in these docs that shows partial reachability says
  so directly underneath. A picture reads as complete unless it tells you it isn't.

---

## 4. The drift checks

```bash
python3 scripts/check_docs.py              # all checks; exit 1 on any failure
python3 scripts/check_docs.py --only NAV   # one family: NAV | SUB | DOC
python3 scripts/check_docs.py --list       # every check id and what it guards

npm install --no-save mermaid jsdom        # diagrams (needs Node; separate on purpose)
node scripts/check_mermaid.mjs
```

`check_docs.py` is pure Python with no dependencies and no Android toolchain — it runs in seconds
anywhere. `check_mermaid.mjs` needs Node, which is why it is a separate script and a separate CI
step: the claim checker must always be runnable, even when the diagram checker is not. Both run
on every PR via `.github/workflows/docs_check.yml`.

| Check | Guards |
|---|---|
| `NAV-01` | every `Route` in `Routes.kt` appears in the `NAVIGATION.md` route reference |
| `NAV-02` | every route named in `NAVIGATION.md` still exists in `Routes.kt` |
| `NAV-03` | the destination count claimed in `NAVIGATION.md` matches `NavGraph.kt` |
| `NAV-04` | every destination is wired with `taggedComposable` (never a bare `composable`) |
| `NAV-05` | every `Route` has a matching `ScreenTags` entry |
| `NAV-06` | every static announcement route key is documented |
| `NAV-07` | every announcement key documented still exists in `AnnouncementRoutes.kt` |
| `NAV-08` | every `Route` reachable from an announcement key is named in the grammar section |
| `NAV-09` | every help deep-link key is documented |
| `NAV-10` | every help deep-link key documented still exists in `HelpDeepLink.kt` |
| `SUB-01` | the schema version `SUBSYSTEMS.md` claims matches `NIMAZ_DATABASE_VERSION` |
| `SUB-02` | every `Worker` class is documented |
| `SUB-03` | every `Service` class is documented |
| `SUB-04` | every widget package is documented |
| `SUB-05` | every notification channel id is documented |
| `SUB-06` | every DataStore file name is documented |
| `SUB-07` | every FCM announcement payload key is documented |
| `SUB-08` | every `AnnouncementType` key is documented |
| `SUB-09` | every `CelebrationEvent` key is documented |
| `DOC-01` | every current doc carries the standard header block |
| `DOC-02` | every current doc is listed in the `README.md` index |
| `DOC-03` | every link resolves — cross-doc file **and** anchor, and same-file anchors |
| `DOC-04` | every doc over 150 lines opens with a contents list |
| *(mermaid)* | every ```` ```mermaid ```` fence parses — `scripts/check_mermaid.mjs` |

**Several checks run in both directions** (`NAV-01`/`NAV-02`, `NAV-06`/`NAV-07`,
`NAV-09`/`NAV-10`) — that is deliberate. A doc that lists a route deleted three releases ago is
as misleading as one missing a route added yesterday, and only the second kind gets noticed.

### 4.1 Adding a check

Extend `scripts/check_docs.py` whenever a doc makes a **countable, list-shaped claim** about the
code — an inventory, an allowlist, an enum, a version number. Those are exactly the claims that
rot invisibly.

1. Add an inventory function that reads the truth out of the code (never out of a doc).
2. Add the check to the right family (`check_nav` / `check_sub` / `check_doc`), and use
   `report.expect_covered(...)` for coverage-shaped checks so the failure message stays uniform.
3. Register its id and one-line description in the `CHECKS` dict.
4. Add the row to the table above.
5. Make the failure message say **which file to edit and what to add** — a checker that only says
   "failed" gets suppressed rather than fixed.

**Do not** add a check that greps a doc for prose. Check identifiers, not sentences: prose
changes for good reasons, and a brittle check is one the next person will delete.

---

## 5. Writing rules

- **Ground every claim in code you have read.** Cite the path in backticks
  (`` `core/navigation/NavGraph.kt` ``). If you did not open the file, do not describe it.
- **Prefer tables for inventories, prose for reasons.** A table says what exists; the paragraph
  under it says why it is like that. The "why" is the part that saves the next person an hour,
  and the part no checker can generate.
- **Document decisions, not just mechanics.** "Vibration is a channel property, so the preference
  is honoured by posting on a second channel" is worth ten lines describing the channel fields.
- **Write down the accepted gaps.** Known limitations, deliberate deviations and "this is
  intentional, don't fix it" notes belong in the doc — otherwise someone re-discovers them as a
  bug and re-litigates the decision.
- **No dated language.** Not "new", "recently", "currently", "as of today" — every doc is
  current by definition, and "the celebration type (new)" is stale the moment it ships. Cite an
  issue number (`#301`) or a schema version when the *when* actually matters.
- **One source of truth per fact.** If a second section needs the same table, link to the first —
  `SUBSYSTEMS.md` §2 says *"the widget roster lives in §0.4"* rather than repeating it.
  Copy-paste is how two true things become one true and one false.
- **Keep the numbers checkable.** When you write a count ("89 destinations"), make sure a check
  in `check_docs.py` owns it — or don't write the number.

---

## 6. Adding, splitting and retiring docs

**Adding.** Copy the header block from §2, add the row to the ownership matrix in §1, add the row
to [`README.md`](README.md). `DOC-01` and `DOC-02` fail until you do. State the disjoint area it
owns — if you cannot say what it owns that no other doc owns, it should be a section in an
existing doc instead.

**Splitting.** A section that has grown past roughly 300 lines and has its own vocabulary is a
candidate (`ai-ask-with-proof.md` was one). Replace the old section with a one-paragraph summary
and a link — never leave the full text in both places. Update §1 so the new doc's area is carved
*out* of the old owner's.

**Retiring.** Move it to `docs/archive/` and note in `docs/archive/README.md` what replaced it
and when. Do not delete: a historical doc explains why the code looks the way it does. Archived
docs are exempt from `DOC-01`/`DOC-02` and are never sources of truth.

**What lives where.**

| Directory | Contains | Source of truth? |
|---|---|---|
| `docs/*.md` | the current, owned, maintained documentation | ✅ |
| `docs/archive/` | superseded planning documents, kept for context | ❌ |
| `docs/superpowers/{plans,specs}/` | per-change design records, dated, never updated | ❌ |
| `docs/design/`, `docs/quran/` | design mockups and reference sheets for one feature | ❌ |

---

## 7. Checklists — for humans and for agents

### 7.1 Before opening a PR

- [ ] Found my change in the §1 ownership matrix and updated the doc it names — **in this commit**.
- [ ] Updated the inventory table *and* the prose section, when the change touches both.
- [ ] Added or updated a diagram if the change altered a flow, an ordering, or a fan-out — and
      `node scripts/check_mermaid.mjs` parses it.
- [ ] Removed anything the change made untrue, including in docs I did not otherwise touch.
- [ ] `python3 scripts/check_docs.py` passes.
- [ ] `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest` pass.

### 7.2 For agents

Everything in §7.1, plus:

- **Read the owning doc before you change the area.** The ownership matrix in §1 tells you which
  one. Reading it first is usually cheaper than reading the code it describes.
- **Update the doc in the same turn as the code**, not in a wrap-up step at the end — a
  compacted context loses the detail that made the doc worth writing.
- **Never invent a fact to satisfy a check.** If `SUB-05` wants a channel documented, open the
  file and read its importance; do not guess a plausible value. A green check over a fabricated
  row is worse than a red one.
- **When you find drift outside your task, fix it if it is a line or two**, and say so in the PR
  description. If it is larger, say what you found and where — do not silently leave it.
- **Do not copy an open item** from [`CLEAN_ARCHITECTURE_CHECKLIST.md`](CLEAN_ARCHITECTURE_CHECKLIST.md)
  or [`ARCHITECTURE.md` §9](ARCHITECTURE.md#9-known-deviations--tech-debt-registry) as a pattern
  to follow. Those lists exist to be shrunk.
- **Prefer deleting a stale paragraph to appending a corrected one.** Docs rot by accretion; two
  paragraphs that disagree are worse than one that is merely incomplete.
