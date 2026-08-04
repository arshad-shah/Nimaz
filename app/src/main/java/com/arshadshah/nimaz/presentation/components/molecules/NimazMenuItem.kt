package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.asLanguageLabel

/**
 * A row inside a [NimazMenuGroup].
 *
 * @param selected marks this row as the group's current choice. A picker list is otherwise
 *   indistinguishable from a navigation list — the row you are already on looks exactly like
 *   the twelve you are not — so a selected row fills with the accent container, sets its title
 *   in the on-container colour, and swaps [trailingIcon] for a check. Selection is announced
 *   to accessibility services too, which a colour fill alone never is.
 * @param subtitleStyle overrides the subtitle's style. Needed where the subtitle is not Latin
 *   text: an Urdu endonym set in the body font falls back to a system Naskh face (see
 *   [com.arshadshah.nimaz.presentation.theme.asLanguageLabel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
    selected: Boolean = false,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(0.dp),
        colors = NimazCardDefaults.selectable(
            container = Color.Transparent,
            content = MaterialTheme.colorScheme.onSurface,
            activeContainer = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = 0.dp
    ) {
        val titleColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        val subtitleColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                NimazIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) titleColor else iconTint,
                    size = NimazIconSize.LARGE
                )

                Nima
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = subtitleStyle,
                        color = subtitleColor
                    )
                }
            }

            when {
                selected -> NimazIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = titleColor,
                    iconSize = 20.dp
                )

                trailingIcon != null -> NimazIcon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    variant = NimazIconVariant.MUTED,
                    iconSize = 18.dp
                )
            }
        }
    }
}

@Composable
fun NimazMenuGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazMenuItem")
@Composable
private fun NimazMenuItemPreview() {
    NimazTheme {
        NimazMenuItem(
            title = "Prayer Tracker",
            subtitle = "Track your daily prayers & qada",
            icon = Icons.Default.Schedule,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazMenuGroup")
@Composable
private fun NimazMenuGroupPreview() {
    NimazTheme {
        NimazMenuGroup(modifier = Modifier.padding(16.dp)) {
            NimazMenuItem(
                title = "Prayer Tracker",
                subtitle = "Track your daily prayers",
                icon = Icons.Default.Schedule,
                onClick = {}
            )
        }
    }
}

/** The picker shape: one row selected, its neighbours not — the contrast is the point. */
@Preview(showBackground = true, widthDp = 400, name = "NimazMenuItem — selected")
@Composable
private fun NimazMenuItemSelectedPreview() {
    NimazTheme {
        NimazMenuGroup(modifier = Modifier.padding(16.dp)) {
            NimazMenuItem(
                title = "Saheeh International",
                subtitle = "English",
                trailingIcon = null,
                onClick = {}
            )
            NimazMenuItem(
                title = "Abul A'ala Maududi",
                subtitle = "اردو",
                // The endonym is Arabic-script, so it needs the Nastaliq face and its
                // leading — the body font would fall back to a system Naskh.
                subtitleStyle = MaterialTheme.typography.bodySmall
                    .asLanguageLabel(TranslationLanguage.URDU),
                trailingIcon = null,
                selected = true,
                onClick = {}
            )
            NimazMenuItem(
                title = "Marmaduke Pickthall",
                subtitle = "English",
                trailingIcon = null,
                onClick = {}
            )
        }
    }
}
