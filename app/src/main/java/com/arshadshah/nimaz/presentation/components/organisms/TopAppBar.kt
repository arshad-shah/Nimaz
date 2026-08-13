package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The app's top bar — a row of **floating frosted pills** rather than a solid band.
 *
 * A conventional [androidx.compose.material3.TopAppBar] paints an opaque surface
 * across the top, which covers the app-wide ornament drawn by
 * `NimazPatternBackground`. This renders the navigation icon, title, and actions as
 * separate translucent pills over a transparent bar, so the pattern shows through
 * the gaps — the same glass-pill language used on the Home screen. Every screen
 * that uses this (and [NimazBackTopAppBar]) inherits the look from one place.
 *
 * @param navigationIcon optional leading content, shown inside a circular pill.
 * @param actions optional trailing content; when null (the default) no actions pill
 *   is drawn, so a plain title screen shows just back + title with no empty nub.
 * @param scrollBehavior accepted for source compatibility with the callers that pass
 *   a `pinnedScrollBehavior()`. The pill bar does not collapse or recolour on scroll
 *   (the pills carry their own surface), so it is intentionally not consumed here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        navigationIcon?.let { nav ->
            FrostedPill(modifier = Modifier.size(PillHeight), shape = CircleShape) {
                nav()
            }
        }

        // The title pill sits at the START of the flexible area, right after the
        // back pill; the ornament shows in the space to its right, and the actions
        // pill (if any) is pushed to the far edge. A weighted Box does the spacing —
        // NOT weight(fill = false) on the pill itself, which mis-places it. A long
        // title truncates inside the Box rather than shoving the actions off-screen.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            FrostedPill(modifier = Modifier.heightIn(min = PillHeight), shape = PillShape) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // The screen's own heading, and the first stop for TalkBack's heading
                        // navigation. Material3 does not mark its title as one.
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        actions?.let { acts ->
            FrostedPill(modifier = Modifier.heightIn(min = PillHeight), shape = PillShape) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = acts,
                )
            }
        }
    }
}

/**
 * [NimazTopAppBar] with a back-navigation pill in the leading slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazBackTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    NimazTopAppBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                NimazIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

/** Height of the circular/rounded pills, and the shared pill radius. */
private val PillHeight = 48.dp
private val PillShape = RoundedCornerShape(24.dp)

/**
 * A single translucent, hairline-bordered pill with a soft shadow. Deliberately
 * *not* fully opaque so the ornament reads faintly through it, while the tint keeps
 * the title legible over any pattern.
 */
@Composable
private fun FrostedPill(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NimazTopAppBarWithSubtitlePreview() {
    NimazTheme {
        NimazTopAppBar(
            title = "Nimaz",
            subtitle = "Next: Dhuhr at 12:30 PM",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NimazTopAppBarTitleOnlyPreview() {
    NimazTheme {
        NimazTopAppBar(title = "Nimaz")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NimazBackTopAppBarPreview() {
    NimazTheme {
        NimazBackTopAppBar(
            title = "Settings",
            onBackClick = {},
            subtitle = "Customize your experience",
            actions = {
                IconButton(onClick = {}) {
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            }
        )
    }
}
