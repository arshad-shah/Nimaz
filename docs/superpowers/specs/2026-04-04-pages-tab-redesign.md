# Pages Tab Visual Refresh

## Problem

The pages tab in QuranHomeScreen has dated styling — flat cards with no borders, solid-color Juz headers, and inaccurate Surah start indicators that group by row instead of showing per-page. Needs a visual refresh to match the modernized Surah list items and improve Surah subsection accuracy.

## Design: Refined Grid with Surah Chip Banners

### Page Card Styling

- **Background**: `MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)` (was `surfaceVariant.copy(alpha = 0.5f)`)
- **Border**: `1.5.dp` border, `MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)`, `RoundedCornerShape(10.dp)` (was 12dp, no border)
- **Shape**: `RoundedCornerShape(10.dp)` (was 12dp)
- **Selected state**: Keep existing `2.dp` primary border + `primaryContainer.copy(alpha = 0.6f)` background
- **Complete state**: Keep existing `primaryContainer` solid background
- **Text color**: Keep existing (primary for normal, onPrimaryContainer for selected/complete)
- **Progress ring**: Unchanged

### Juz Header Styling

- **Background**: `MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)` (was solid `primaryContainer`)
- **Text color**: `MaterialTheme.colorScheme.primary` (was `onPrimaryContainer`)
- **Shape**: Keep `RoundedCornerShape(10.dp)`
- **Padding**: Keep `horizontal = 14.dp, vertical = 10.dp`

### Surah Start Indicators

**Current behavior (buggy)**: Groups all surah starts from ANY page in the current row of 5, then shows them as a single comma-separated label above the row. This means if pages 2 and 5 both start surahs, they appear grouped together even though they're on different pages.

**New behavior**: Show surah start indicators as pill-shaped badges above the row, one badge per surah start. Each badge shows `"▸ SurahName starts"` or just `"▸ SurahName"` for the first page.

- **Badge background**: `MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)`
- **Badge text color**: `MaterialTheme.colorScheme.primary`
- **Badge typography**: `labelSmall`, `FontWeight.Medium`
- **Badge shape**: `RoundedCornerShape(12.dp)`
- **Badge padding**: `vertical = 3.dp, horizontal = 10.dp`
- **Row arrangement**: `Arrangement.spacedBy(6.dp)`, wrap with `FlowRow` if multiple surahs start in the same row
- **Row padding**: `start = 4.dp, top = 6.dp, bottom = 2.dp` (keep existing)

### Jump-to-Page Input

No changes — keep as-is.

## Files to Modify

- `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt` — the only file that changes

## Verification

1. Build the app and navigate to QuranHomeScreen → Pages tab
2. Verify page cards have subtle borders and refined background
3. Verify Juz headers have lighter styling
4. Verify Surah start indicators appear as individual pill badges
5. Verify selected/complete/progress states still work
6. Verify jump-to-page still works
7. Scroll through all 30 Juz sections to check consistency
