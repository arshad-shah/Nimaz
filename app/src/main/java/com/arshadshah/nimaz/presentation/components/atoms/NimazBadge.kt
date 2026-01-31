package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Badge size presets
 */
enum class NimazBadgeSize(val height: Dp, val horizontalPadding: Dp) {
    SMALL(18.dp, 6.dp),
    MEDIUM(22.dp, 8.dp),
    LARGE(26.dp, 10.dp)
}

/**
 * Status badge types for different contexts
 */
sealed class BadgeType(val label: String, val color: Color) {
    // Hadith grades
    object Sahih : BadgeType("Sahih", Color(0xFF4CAF50))
    object Hasan : BadgeType("Hasan", Color(0xFF8BC34A))
    object Daif : BadgeType("Da'if", Color(0xFFFF9800))
    object Mawdu : BadgeType("Mawdu'", Color(0xFFF44336))

    // Quran revelation types
    object Meccan : BadgeType("Meccan", Color(0xFF795548))
    object Medinan : BadgeType("Medinan", Color(0xFF00796B))

    // Prayer status
    object Prayed : BadgeType("Prayed", NimazColors.StatusColors.Prayed)
    object Missed : BadgeType("Missed", NimazColors.StatusColors.Missed)
    object Pending : BadgeType("Pending", NimazColors.StatusColors.Pending)
    object Qada : BadgeType("Qada", NimazColors.StatusColors.Qada)
    object Jamaah : BadgeType("Jama'ah", NimazColors.StatusColors.Jamaah)

    // Fasting status
    object Fasted : BadgeType("Fasted", NimazColors.FastingColors.Fasted)
    object NotFasted : BadgeType("Not Fasted", NimazColors.FastingColors.NotFasted)
    object Makeup : BadgeType("Makeup", NimazColors.FastingColors.Makeup)
    object Exempted : BadgeType("Exempted", NimazColors.FastingColors.Exempted)

    // Custom
    data class Custom(
        private val customLabel: String,
        private val customColor: Color
    ) : BadgeType(customLabel, customColor)
}

/**
 * Primary badge component for status indicators.
 */
@Composable
fun NimazBadge(
    text: String,
    modifier: Modifier = Modifier,
    size: NimazBadgeSize = NimazBadgeSize.MEDIUM,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = RoundedCornerShape(4.dp),
    outlined: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (outlined) {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, backgroundColor, shape)
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .padding(horizontal = size.horizontalPadding, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (outlined) backgroundColor else textColor
        )
    }
}

/**
 * Typed badge component using predefined badge types.
 */
@Composable
fun StatusBadge(
    type: BadgeType,
    modifier: Modifier = Modifier,
    size: NimazBadgeSize = NimazBadgeSize.MEDIUM,
    outlined: Boolean = false
) {
    NimazBadge(
        text = type.label,
        modifier = modifier,
        size = size,
        backgroundColor = type.color,
        textColor = Color.White,
        outlined = outlined
    )
}

/**
 * Hadith grade badge.
 */
@Composable
fun HadithGradeBadge(
    grade: String,
    modifier: Modifier = Modifier,
    size: NimazBadgeSize = NimazBadgeSize.SMALL
) {
    val badgeType = when (grade.lowercase()) {
        "sahih" -> BadgeType.Sahih
        "hasan" -> BadgeType.Hasan
        "daif", "da'if" -> BadgeType.Daif
        "mawdu", "mawdu'" -> BadgeType.Mawdu
        else -> BadgeType.Custom(grade, Color.Gray)
    }

    StatusBadge(
        type = badgeType,
        modifier = modifier,
        size = size
    )
}

/**
 * Get hadith grade badge colors as a Pair (background, text).
 */
fun getHadithGradeBadgeColors(grade: String): Pair<Color, Color> {
    return when (grade.lowercase()) {
        "sahih" -> Pair(BadgeType.Sahih.color, Color.White)
        "hasan" -> Pair(BadgeType.Hasan.color, Color.White)
        "daif", "da'if" -> Pair(BadgeType.Daif.color, Color.White)
        "mawdu", "mawdu'" -> Pair(BadgeType.Mawdu.color, Color.White)
        else -> Pair(Color.Gray, Color.White)
    }
}

/**
 * Numeric badge (like notification count).
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    backgroundColor: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.onError
) {
    val displayText = if (count > maxCount) "$maxCount+" else count.toString()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Dot badge indicator.
 */
@Composable
fun DotBadge(
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.error
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Badge with icon content using Material3 BadgedBox.
 */
@Composable
fun IconWithBadge(
    icon: ImageVector,
    badgeCount: Int?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showBadge: Boolean = true,
    iconSize: Dp = 24.dp
) {
    BadgedBox(
        badge = {
            if (showBadge) {
                if (badgeCount != null && badgeCount > 0) {
                    Badge { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
                } else if (badgeCount == null) {
                    Badge()
                }
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Surah number badge for Quran.
 */
@Composable
fun SurahNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Verse number indicator.
 */
@Composable
fun VerseNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "NimazBadge")
@Composable
private fun NimazBadgePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazBadge(text = "Default Badge")
            NimazBadge(text = "Outlined", outlined = true)
            NimazBadge(text = "Small", size = NimazBadgeSize.SMALL)
            NimazBadge(text = "Large", size = NimazBadgeSize.LARGE)
        }
    }
}

@Preview(showBackground = true, name = "Status Badges")
@Composable
private fun StatusBadgePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(type = BadgeType.Sahih)
            StatusBadge(type = BadgeType.Hasan)
            StatusBadge(type = BadgeType.Daif)
            StatusBadge(type = BadgeType.Meccan)
            StatusBadge(type = BadgeType.Medinan)
        }
    }
}

@Preview(showBackground = true, name = "Prayer Status Badges")
@Composable
private fun PrayerStatusBadgePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(type = BadgeType.Prayed)
            StatusBadge(type = BadgeType.Missed)
            StatusBadge(type = BadgeType.Pending)
            StatusBadge(type = BadgeType.Qada)
            StatusBadge(type = BadgeType.Jamaah)
        }
    }
}

@Preview(showBackground = true, name = "Hadith Grade Badge")
@Composable
private fun HadithGradeBadgePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HadithGradeBadge(grade = "Sahih")
            HadithGradeBadge(grade = "Hasan")
            HadithGradeBadge(grade = "Daif")
            HadithGradeBadge(grade = "Unknown")
        }
    }
}

@Preview(showBackground = true, name = "Count Badge")
@Composable
private fun CountBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CountBadge(count = 5)
            CountBadge(count = 42)
            CountBadge(count = 100)
        }
    }
}

@Preview(showBackground = true, name = "Dot Badge")
@Composable
private fun DotBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DotBadge()
            DotBadge(size = 12.dp, color = Color.Green)
            DotBadge(size = 16.dp, color = Color.Blue)
        }
    }
}

@Preview(showBackground = true, name = "Icon With Badge")
@Composable
private fun IconWithBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IconWithBadge(
                icon = Icons.Default.Notifications,
                badgeCount = 5,
                contentDescription = "Notifications"
            )
            IconWithBadge(
                icon = Icons.Default.Notifications,
                badgeCount = null,
                contentDescription = "Has notifications"
            )
            IconWithBadge(
                icon = Icons.Default.Notifications,
                badgeCount = 0,
                contentDescription = "No notifications"
            )
        }
    }
}

@Preview(showBackground = true, name = "Surah Number Badge")
@Composable
private fun SurahNumberBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SurahNumberBadge(number = 1)
            SurahNumberBadge(number = 36)
            SurahNumberBadge(number = 114)
        }
    }
}

@Preview(showBackground = true, name = "Verse Number Badge")
@Composable
private fun VerseNumberBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VerseNumberBadge(number = 1)
            VerseNumberBadge(number = 7)
            VerseNumberBadge(number = 255)
        }
    }
}
