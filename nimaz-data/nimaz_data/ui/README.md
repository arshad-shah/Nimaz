# UI — five tiers, strictly one-directional (§10)

A tier may only use tiers below it. Screens contain no raw colour, no raw
spacing, no one-off components.

| tier | lives in | rule |
|---|---|---|
| **L0 tokens** | `tokens.tcss` | semantic names only; a component naming a hex value is a bug |
| **L1 primitives** | `primitives/` | `Stack` `Row` `Box` `Text` `Rule` `Icon` `Spacer` — no product meaning |
| **L2 elements** | `elements/` | `Panel` `Button` `Chip` `StateDot` `KeyHint` `Table` `ListRow` `Callout` `CodeBlock` `Stat` `Field` |
| **L3 patterns** | `patterns/` | `Inspector` `PipelineTrack` `DriftRibbon` `ChangeRow` `RuleRow` `ManifestGrid` `SplitView` `DiffView` |
| **L4 screens** | `screens/` | Collections, Browse, Changes, Build, Rules, Agent |

Each L1–L3 widget is a `Widget` subclass with a sibling `.tcss` that references
only tokens. `textual-serve` renders the identical widget tree in a browser for
anyone who does not want a terminal.

## Status

L0 is real and complete — `tokens.tcss` is the file every other tier resolves
against, and the CLI already uses the same four state colours (`cli/output.py`),
so terminal output and the console agree today.

L1–L4 are not built yet. The CLI is the working front door; the console is the
next slice. Nothing above L0 should be written until the tiers below it exist,
because a screen written first is exactly how one-off components get into a
design system.
