package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Where you are, as somewhere you can go.
 *
 * A breadcrumb that is only a label is a label. Set in a top bar's subtitle it also truncates —
 * "Doctrine › God › The names of God › …" runs out of bar by the third level of a hierarchy
 * that goes five deep, which is exactly when a reader most needs to know where they are. Here
 * each crumb is a control that returns to that level, the row scrolls horizontally instead of
 * eliding, and the last crumb is marked as current rather than being one more thing to tap.
 *
 * [home] is the crumb for the top of the hierarchy — the whole tree, before any focus.
 */
@Composable
fun NimazBreadcrumbBar(
    home: String,
    crumbs: List<String>,
    onCrumbClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    homeIcon: ImageVector? = null,
) {
    val state = rememberLazyListState()
    // Follow the deepest crumb, which is the one that just changed and the one that says where
    // the list below now starts.
    LaunchedEffect(crumbs.size) {
        if (crumbs.isNotEmpty()) state.animateScrollToItem(crumbs.size)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        androidx.compose.foundation.layout.Column {
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "home") {
                    NimazChip(
                        text = home,
                        selected = crumbs.isEmpty(),
                        variant = NimazChipVariant.FILTER,
                        leadingIcon = homeIcon,
                        onClick = { onCrumbClick(HOME_INDEX) },
                    )
                }
                crumbs.forEachIndexed { index, crumb ->
                    item(key = "sep-$index") { CrumbSeparator() }
                    item(key = "crumb-$index") {
                        NimazChip(
                            text = crumb,
                            selected = index == crumbs.lastIndex,
                            variant = NimazChipVariant.FILTER,
                            onClick = { onCrumbClick(index) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** The index [NimazBreadcrumbBar] reports for the home crumb. */
const val HOME_INDEX = -1

@Composable
private fun CrumbSeparator() {
    NimazIcon(
        imageVector = NimazIcons.Forward,
        contentDescription = null,
        variant = NimazIconVariant.MUTED,
        size = NimazIconSize.SMALL,
    )
}

@Preview(showBackground = true, widthDp = 390, name = "NimazBreadcrumbBar — Light")
@Composable
private fun NimazBreadcrumbBarLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { BreadcrumbSample() }
}

@Preview(
    showBackground = true, widthDp = 390, name = "NimazBreadcrumbBar — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazBreadcrumbBarDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { BreadcrumbSample() }
}

@Composable
private fun BreadcrumbSample() {
    NimazBreadcrumbBar(
        home = "All subjects",
        crumbs = listOf("Doctrine", "God", "The names of God"),
        onCrumbClick = {},
    )
}
