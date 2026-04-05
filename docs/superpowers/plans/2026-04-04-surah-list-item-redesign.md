# Surah List Item Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the `SurahListItem` composable so metadata badges get their own row and never get crushed by long Surah names.

**Architecture:** Move from a single-row layout (number + names + metadata + info crammed horizontally) to a two-row layout: top row for number/names/info, second row for full-width metadata badges. The badges use `weight(1f)` to distribute evenly.

**Tech Stack:** Jetpack Compose, Material 3

---

## File Structure

- **Modify:** `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt` — the only file that changes

---

### Task 1: Extract a MetadataBadge helper composable

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt`

- [ ] **Step 1: Add the MetadataBadge composable**

Add this private composable at the bottom of the file (before the `@Preview` function, after the closing brace of `SurahListItem`). This is a small reusable piece since we render 2-4 badges with identical styling.

```kotlin
@Composable
private fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
        )
    }
}
```

- [ ] **Step 2: Add required imports**

Add these imports at the top of the file (if not already present):

```kotlin
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
```

- [ ] **Step 3: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt
git commit -m "feat: add MetadataBadge composable for Surah list item redesign"
```

---

### Task 2: Restructure SurahListItem to two-row layout

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt:89-201`

- [ ] **Step 1: Replace the content inside the Card's Column**

Replace the entire `Column { ... }` block inside the Card (lines 89-217) with this new layout. The key change: the old single `Row` is split into a top `Row` (names) and a bottom `Row` (badges).

```kotlin
        Column {
            // Top row: number + English name + Arabic name + info button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah number indicator
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.quran_home_completed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(45f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text = surah.number.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // English name — takes remaining space, truncates if needed
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Arabic name — intrinsic width, never truncated
                ArabicText(
                    text = surah.nameArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Info button
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.quran_home_surah_info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Metadata badges row — aligned with English name (40dp box + 12dp spacer = 52dp start)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isMeccan = surah.revelationType == RevelationType.MECCAN
                MetadataBadge(
                    text = if (isMeccan) stringResource(R.string.quran_home_makkah) else stringResource(R.string.quran_home_madinah),
                    modifier = Modifier.weight(1f)
                )
                MetadataBadge(
                    text = stringResource(R.string.quran_home_verses_count, surah.ayahCount),
                    modifier = Modifier.weight(1f)
                )
                if (startPage > 0) {
                    MetadataBadge(
                        text = stringResource(R.string.quran_home_page_range_format, startPage, endPage),
                        modifier = Modifier.weight(1f)
                    )
                    MetadataBadge(
                        text = stringResource(R.string.quran_home_juz_indicator, getJuzForPage(startPage)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Khatam progress bar (unchanged)
            if (isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount > 0) {
                LinearProgressIndicator(
                    progress = { khatamReadCount.toFloat() / khatamTotalAyahs },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 14.dp),
                    color = if (isComplete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
```

- [ ] **Step 2: Remove unused imports**

Remove these imports that are no longer needed (the metadata row no longer uses `Arrangement.spacedBy` for inline text or `Spacer` for height between name/metadata):

```kotlin
// Keep all existing imports — the new layout still uses them all.
// Just verify no yellow "unused import" warnings in the IDE.
```

Actually, all existing imports are still used. No removals needed.

- [ ] **Step 3: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt
git commit -m "feat: restructure SurahListItem to two-row layout with full-width badges"
```

---

### Task 3: Update preview and verify visually

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt` (preview section)

- [ ] **Step 1: Add a long-name preview to stress-test the layout**

Add a second preview below the existing one to verify long names don't crush anything:

```kotlin
@Preview(showBackground = true)
@Composable
private fun SurahListItemLongNamePreview() {
    NimazTheme {
        SurahListItem(
            surah = Surah(
                number = 58,
                nameArabic = "\u0627\u0644\u0645\u062C\u0627\u062F\u0644\u0629",
                nameEnglish = "Al-Mujadilah",
                nameTransliteration = "The Pleading Woman",
                revelationType = RevelationType.MEDINAN,
                ayahCount = 22,
                juzStart = 28,
                orderInMushaf = 105,
                startPage = 542
            ),
            onClick = {},
            onInfoClick = {},
            startPage = 542,
            endPage = 545
        )
    }
}
```

- [ ] **Step 2: Verify both previews render correctly**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

Then check the Compose preview in Android Studio — both the short-name (Al-Fatihah) and long-name (Al-Mujadilah) previews should show badges evenly distributed without crushing.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/QuranSurahListItem.kt
git commit -m "feat: add long-name preview for SurahListItem layout verification"
```

---

### Task 4: End-to-end verification

- [ ] **Step 1: Build and install on device/emulator**

Run: `./gradlew :app:installDebug`

- [ ] **Step 2: Manual verification checklist**

1. Open the app → navigate to Quran tab → Surah list
2. Scroll through and verify:
   - Short names (Al-Nas, Qaf) — badges fill the row evenly
   - Long names (Al-Mujadilah, Al-Mumtahanah) — no crushing, English name truncates if needed
   - Arabic names are fully visible (never truncated)
   - Badges are aligned with the English name (left edge matches)
   - Selected state border + background still works
   - Khatam progress bar shows correctly
   - Info button is tappable
3. Test with system font size set to "Largest" in Android accessibility settings

- [ ] **Step 3: Final commit if any tweaks needed**

```bash
git add -u
git commit -m "fix: polish SurahListItem layout after manual testing"
```
