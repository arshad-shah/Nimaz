package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The shared "action pill" used across readers (Quran ayah, Dua) — a rounded,
 * outlined [Surface] that hosts a row of [NimazPillActionButton]s. Centralised
 * here so every reader's action row looks and animates identically.
 */
@Composable
fun NimazActionPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * A single circular action inside a [NimazActionPill]. The icon tint animates
 * between an inactive neutral state and an [active] state tinted with
 * [activeColor] (e.g. red for favourite, gold for bookmark).
 */
@Composable
fun NimazPillActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val tint by animateColorAsState(
        targetValue = if (active) activeColor
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "pill_action_tint"
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}


@Preview
@Composable
private fun NimazActionPillPreview() {
    NimazActionPill {
        NimazPillActionButton(
            icon = Icons.Default.Bookmark,
            contentDescription = "Bookmark",
            onClick = {},
            active = true
        )

    }}

@Preview
@Composable
private fun NimazPillActionButtonPreview() {
    NimazPillActionButton(
        icon = Icons.Default.Bookmark,
        contentDescription = "Bookmark",
        onClick = {},
        active = true
    )
}