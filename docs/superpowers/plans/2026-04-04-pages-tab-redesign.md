# Pages Tab Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Visually refresh the pages tab grid — refined card styling, lighter Juz headers, and per-surah pill badge indicators.

**Architecture:** Modify `QuranPageGrid.kt` in three passes: update page card styling (border + background), update Juz header styling, then replace surah start text with pill badges. Single file, no new files.

**Tech Stack:** Jetpack Compose, Material 3

---

## File Structure

- **Modify:** `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt` — the only file that changes

---

### Task 1: Update page card styling

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt:166-211`

- [ ] **Step 1: Update the Card shape, border, and colors**

In the page card `Card(...)` block (currently lines 166-211), make these changes:

1. Change the card `shape` from `RoundedCornerShape(12.dp)` to `RoundedCornerShape(10.dp)`
2. Add a default border to ALL cards (not just selected ones). The `Modifier` chain becomes:

```kotlin
Card(
    onClick = { onNavigateToPage(pageNumber) },
    modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)
        .border(
            width = if (isSelected) 2.dp else 1.5.dp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(10.dp)
        ),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
        containerColor = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            isComplete -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        }
    )
)
```

Note: the old code used a `.then(if (isSelected) Modifier.border(...) else Modifier)` pattern. Replace that entire `.then(...)` block with the single `.border(...)` call above that handles both selected and default states.

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt
git commit -m "feat: update page card styling with borders and refined background"
```

---

### Task 2: Update Juz header styling

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt:120-134`

- [ ] **Step 1: Change the Juz header Surface color and text color**

Replace the Juz header block (lines 120-134) with:

```kotlin
item(key = "page_juz_header_$juz") {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.quran_home_juz_pages_format, juz, startPage, endPage),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
```

Changes from current:
- `color` parameter: `MaterialTheme.colorScheme.primaryContainer` → `MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)`
- Text `color`: `MaterialTheme.colorScheme.onPrimaryContainer` → `MaterialTheme.colorScheme.primary`

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt
git commit -m "feat: lighten Juz header styling in pages tab"
```

---

### Task 3: Replace surah start indicators with pill badges

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt:140-154`

- [ ] **Step 1: Add FlowRow import**

Add this import at the top of the file:

```kotlin
import androidx.compose.foundation.layout.FlowRow
```

Also add if not already present:

```kotlin
import androidx.compose.foundation.layout.ExperimentalLayoutApi
```

And add `@OptIn(ExperimentalLayoutApi::class)` to the `pageGridItems` function annotation (alongside the existing `@OptIn(ExperimentalMaterial3Api::class)`).

- [ ] **Step 2: Replace the surah start indicator block**

Replace the current surah start indicator block (lines 140-154):

```kotlin
// OLD:
val surahStarts = row.flatMap { pageNumber ->
    surahStartPageMap[pageNumber] ?: emptyList()
}
if (surahStarts.isNotEmpty()) {
    item(key = "surah_start_${row.first()}") {
        val label = surahStarts.joinToString(", ") { "\u25B8 $it" }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
        )
    }
}
```

With:

```kotlin
// NEW:
val surahStarts = row.flatMap { pageNumber ->
    surahStartPageMap[pageNumber] ?: emptyList()
}
if (surahStarts.isNotEmpty()) {
    item(key = "surah_start_${row.first()}") {
        FlowRow(
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            surahStarts.forEach { surahName ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "\u25B8 $surahName",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/QuranPageGrid.kt
git commit -m "feat: replace surah start text with pill badge indicators"
```

---

### Task 4: End-to-end verification

- [ ] **Step 1: Build and install on device/emulator**

Run: `./gradlew :app:installDebug`

- [ ] **Step 2: Manual verification checklist**

1. Open app → Quran tab → Pages tab
2. Verify:
   - Page cards have subtle 1.5dp borders with refined background
   - Juz headers are lighter (not solid primaryContainer)
   - Surah start indicators appear as individual pill badges
   - Multiple surah starts in same row show as separate pills
   - Selected page card has stronger 2dp primary border
   - Completed pages still show primaryContainer solid background
   - Progress rings still display on partially-read pages
   - Jump-to-page input still works
3. Scroll through all 30 Juz sections for consistency

- [ ] **Step 3: Final commit if any tweaks needed**

```bash
git add -u
git commit -m "fix: polish pages tab after manual testing"
```
