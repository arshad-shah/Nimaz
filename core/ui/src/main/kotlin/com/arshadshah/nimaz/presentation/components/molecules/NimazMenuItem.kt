package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fastfood
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
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
 * @param trailing optional content rendered before [trailingIcon] — a status [NimazBadge], for
 *   instance, on a row whose state the reader needs at a glance. The row is the tap target, so
 *   anything interactive placed here handles its own taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    trailingIcon: ImageVector? = NimazIcons.Forward,
    trailing: (@Composable RowScope.() -> Unit)? = null,
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

                NimazIconWell(
                    icon,
                    contentDescription = null,
                    color = iconTint,
                )
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

            if (trailing != null) {
                trailing()
                Spacer(modifier = Modifier.width(8.dp))
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

/** The measurements every [NimazMenuGroup] and its rows share. */
object NimazMenuDefaults {
    /**
     * How far a [NimazMenuDivider] is pushed in on a row that carries a leading icon: the row's
     * own 16dp of padding plus the 40dp icon well, so the line begins where the icon ends.
     */
    val RowDividerInset: Dp = 56.dp

    /**
     * The symmetric inset for a divider between blocks that are *not* icon rows — a slider, a
     * dropdown, a free-form section. Matches the block's own content padding.
     */
    val SectionDividerInset: Dp = 16.dp
}

/**
 * The hairline between two rows of a [NimazMenuGroup].
 *
 * This exists because the same line was being written out by hand at every call site, and it
 * drifted: `padding(start = 56.dp), alpha = 0.5f` in More and Settings, `padding(horizontal =
 * 16.dp)` at full strength in the Qur'an settings, nothing at all on the Qur'an home — four rows
 * that behave identically, separated four different ways. There is one line now, at one weight,
 * and a group that wants no separation simply omits it.
 *
 * @param inset `true` (the default) starts the line past the leading icon well, which is what a
 *   group of [NimazMenuItem]/`NimazSettingsItem` rows wants. Pass `false` between blocks that
 *   have no icon column — a slider, a dropdown, a section of prose — where a start-inset line
 *   would hang off the content it is meant to divide.
 */
@Composable
fun NimazMenuDivider(
    modifier: Modifier = Modifier,
    inset: Boolean = true,
) {
    NimazDivider(
        modifier = modifier.padding(
            start = if (inset) {
                NimazMenuDefaults.RowDividerInset
            } else {
                NimazMenuDefaults.SectionDividerInset
            },
            end = if (inset) 0.dp else NimazMenuDefaults.SectionDividerInset,
        )
    )
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
            NimazMenuDivider()
            NimazMenuItem(
                title = "Fasting",
                subtitle = "2 make-up fasts",
                icon = Icons.Default.Fastfood,
                onClick = {}
            )
            NimazMenuDivider()
            NimazMenuItem(
                title = "Khatam",
                subtitle = "Juz 14 of 30",
                icon = Icons.AutoMirrored.Filled.MenuBook,
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
            NimazMenuDivider(inset = false)
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
            NimazMenuDivider(inset = false)
            NimazMenuItem(
                title = "Marmaduke Pickthall",
                subtitle = "English",
                trailingIcon = null,
                onClick = {}
            )
        }
    }
}
