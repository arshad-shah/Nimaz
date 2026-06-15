# Surah List Item Redesign

## Problem

The current `SurahListItem` in `QuranSurahListItem.kt` places the English name + metadata column, Arabic name, and info button all in the same horizontal `Row`. The Arabic `ArabicText` has no width constraint, so longer Arabic names eat into the weighted column's space, crushing the metadata row (revelation type, verse count, page range, juz).

## Design: Two-Row Stacked Layout

### Layout Structure

```
Card (RoundedCornerShape 14dp, fillMaxWidth)
  └── Column
      ├── Row (top row: number + names + info)
      │   ├── Box (surah number indicator, 40dp)
      │   │   ├── CheckCircle icon (if khatam complete)
      │   │   └── Rotated square + number text (if incomplete)
      │   ├── Spacer (12dp)
      │   ├── Text (English name, weight(1f), maxLines=1, ellipsis)
      │   ├── ArabicText (nameArabic, MEDIUM, flexShrink=0 equivalent)
      │   └── IconButton (info, 36dp)
      │
      ├── Row (metadata badges, full-width, below names)
      │   │   starts at left-margin = 52dp (aligns with English name)
      │   ├── Badge ("Makkah"/"Madinah", weight(1f))
      │   ├── Badge ("X ayahs", weight(1f))
      │   ├── Badge ("P. X-Y", weight(1f))  [conditional: startPage > 0]
      │   └── Badge ("Juz X", weight(1f))   [conditional: startPage > 0]
      │
      └── LinearProgressIndicator (khatam, 3dp, conditional)
```

### Key Changes from Current

1. **Metadata moves to its own row** — no longer competing for horizontal space with names
2. **Badges use `weight(1f)`** — equal distribution across the full width
3. **Each badge**: `Surface` or `Box` with `MaterialTheme.colorScheme.primary.copy(alpha=0.12)` background, `RoundedCornerShape(8.dp)`, centered `labelSmall` text
4. **English name gets `weight(1f)`** in the top row — takes remaining space after number, Arabic, and info button
5. **Arabic name uses no weight** — measures at intrinsic size, never truncated (flex-shrink equivalent: just don't apply weight)
6. **Badge text**: `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, `textAlign = TextAlign.Center`
7. **Badge row left margin**: `padding(start = 52.dp)` to align with the English name (40dp box + 12dp spacer)
8. **Page range format**: Shortened to `P. X-Y` to save space on small screens

### Badge Styling

- Background: `MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)`
- Text color: `MaterialTheme.colorScheme.onSurfaceVariant`
- Typography: `labelSmall`
- Shape: `RoundedCornerShape(8.dp)`
- Padding: `vertical = 4.dp, horizontal = 4.dp`
- Each badge: `Modifier.weight(1f)` for equal distribution
- Gap between badges: `Arrangement.spacedBy(6.dp)`

### Conditional Badges

When `startPage <= 0`, only show 2 badges (revelation type + verse count). They still use `weight(1f)` so they stretch to fill the row.

### Selected State

No change — keeps the existing `border` + `primaryContainer` background behavior.

### Khatam Progress Bar

No change — stays at the bottom of the Column as-is.

## Files to Modify

- `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt` — the only file that needs changes

## Verification

1. Build the app and navigate to QuranHomeScreen
2. Scroll through the Surah list — verify no metadata crushing on long names (Al-Mujadilah, Al-Mumtahanah, etc.)
3. Verify short names (Al-Nas, Qaf) look proportional
4. Verify badges distribute evenly across the row
5. Verify khatam progress bar and completed checkmark still work
6. Test with different font sizes (accessibility settings)
