package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.HelpItem
import com.arshadshah.nimaz.domain.model.HelpStep
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle

fun helpIcon(key: String?): ImageVector = when (key) {
    "schedule" -> Icons.Filled.Schedule
    "notifications_active" -> Icons.Filled.NotificationsActive
    "explore" -> Icons.Filled.Explore
    "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
    "task_alt" -> Icons.Filled.TaskAlt
    "build" -> Icons.Filled.Build
    "tune" -> Icons.Filled.Tune
    "more_time" -> Icons.Filled.MoreTime
    else -> Icons.AutoMirrored.Filled.HelpOutline
}

@Composable
fun helpColor(key: String): Color = when (key) {
    "indigo" -> Color(0xFF6366F1)
    "gold" -> Color(0xFFEAB308)
    "teal" -> Color(0xFF14B8A6)
    "green" -> Color(0xFF22C55E)
    "violet" -> Color(0xFF8B5CF6)
    "orange" -> Color(0xFFF97316)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun HelpIconBox(iconKey: String?, tint: Color, boxSize: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = helpIcon(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(boxSize * 0.55f)
        )
    }
}

@Composable
fun HelpTopicTile(topic: HelpTopic, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = helpColor(topic.colorKey)
    NimazCard(modifier = modifier, style = NimazCardStyle.OUTLINED, onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpIconBox(topic.iconKey, tint)
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (topic.subtitle.isNotBlank()) {
                Text(
                    text = topic.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HelpQuestionRow(question: HelpItem.HelpQuestion, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (expanded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = question.answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun HelpGuideRow(
    guide: HelpItem.HelpGuide,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NimazCard(modifier = modifier, style = NimazCardStyle.OUTLINED, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpIconBox(guide.iconKey, tint, boxSize = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                val meta = guide.estimatedMinutes?.let { "About $it min" } ?: "Step-by-step"
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HelpStepTimeline(
    steps: List<HelpStep>,
    modifier: Modifier = Modifier,
    onPathChipClick: (HelpStep) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Rail: numbered node + connecting line
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(accent.copy(alpha = 0.25f))
                        )
                    }
                }
                Spacer(Modifier.width(13.dp))
                // Step card
                NimazCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 0.dp else 14.dp),
                    style = NimazCardStyle.OUTLINED
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (step.body.isNotBlank()) {
                            Text(
                                text = step.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                        if (step.pathLabels.isNotEmpty()) {
                            HelpPathChip(
                                labels = step.pathLabels,
                                enabled = step.deeplinkRoute != null,
                                onClick = { onPathChipClick(step) },
                                modifier = Modifier.padding(top = 9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpPathChip(
    labels: List<String>,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (i != labels.lastIndex) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
