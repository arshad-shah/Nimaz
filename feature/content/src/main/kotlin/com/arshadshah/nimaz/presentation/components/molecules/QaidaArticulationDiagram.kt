package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import com.arshadshah.nimaz.feature.content.R

/** Locations, not a motion simulation. Shared makharij deliberately share a location. */
internal enum class ArticulationSite {
    CAVITY, DEEP_THROAT, MID_THROAT, UPPER_THROAT, BACK_TONGUE, FORWARD_BACK_TONGUE,
    MID_TONGUE, TONGUE_SIDE, TONGUE_EDGE, TONGUE_TIP, TONGUE_TIP_BACK,
    UPPER_GUM, LOWER_TEETH, UPPER_TEETH_EDGE, LOWER_LIP, CLOSED_LIPS, ROUNDED_LIPS
}

internal fun articulationSite(glyph: String): ArticulationSite? = when (glyph.filterNot { it in '\u064B'..'\u065F' || it == '\u0670' || it == '\u0640' }.trim()) {
    "ا", "ى" -> ArticulationSite.CAVITY
    "ء", "أ", "إ", "ؤ", "ئ", "ه" -> ArticulationSite.DEEP_THROAT
    "ع", "ح" -> ArticulationSite.MID_THROAT
    "غ", "خ" -> ArticulationSite.UPPER_THROAT
    "ق" -> ArticulationSite.BACK_TONGUE
    "ك" -> ArticulationSite.FORWARD_BACK_TONGUE
    "ج", "ش", "ي" -> ArticulationSite.MID_TONGUE
    "ض" -> ArticulationSite.TONGUE_SIDE
    "ل" -> ArticulationSite.TONGUE_EDGE
    "ن" -> ArticulationSite.TONGUE_TIP
    "ر" -> ArticulationSite.TONGUE_TIP_BACK
    "ط", "د", "ت" -> ArticulationSite.UPPER_GUM
    "ص", "ز", "س" -> ArticulationSite.LOWER_TEETH
    "ظ", "ذ", "ث" -> ArticulationSite.UPPER_TEETH_EDGE
    "ف" -> ArticulationSite.LOWER_LIP
    "ب", "م" -> ArticulationSite.CLOSED_LIPS
    "و" -> ArticulationSite.ROUNDED_LIPS
    else -> null
}

/** Original scalable anatomy schematics, with theme-derived tissue, cavity and contact colours. */
@Composable
fun QaidaArticulationDiagram(glyph: String, detail: String, modifier: Modifier = Modifier) {
    val site = articulationSite(glyph) ?: return
    val c = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = c.onSurfaceVariant)
    val upperLip = stringResource(R.string.qaida_anatomy_upper_lip)
    val lowerLip = stringResource(R.string.qaida_anatomy_lower_lip)
    val tongueLabel = stringResource(R.string.qaida_anatomy_tongue)
    val teethLabel = stringResource(R.string.qaida_anatomy_teeth)
    val throatLabel = stringResource(R.string.qaida_anatomy_throat)
    val nasalLabel = stringResource(R.string.qaida_anatomy_nose)
    val molarsLabel = stringResource(R.string.qaida_anatomy_molars)
    val front = site in listOf(ArticulationSite.LOWER_LIP, ArticulationSite.CLOSED_LIPS, ArticulationSite.ROUNDED_LIPS)
    val top = site == ArticulationSite.TONGUE_SIDE
    val view = stringResource(if (front) R.string.qaida_anatomy_front else if (top) R.string.qaida_anatomy_top else R.string.qaida_anatomy_side)
    val description = stringResource(R.string.qaida_anatomy_description, glyph, view, detail)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(view, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(210.dp).semantics { contentDescription = description }) {
            val scale = minOf(size.width / 360f, size.height / 230f)
            withTransform({ translate((size.width - 360f * scale) / 2, (size.height - 230f * scale) / 2); scale(scale, scale, Offset.Zero) }) {
                fun shape(path: Path, fill: androidx.compose.ui.graphics.Color) {
                    drawPath(path, fill)
                    drawPath(path, c.outline, style = Stroke(1.5f))
                }
                fun mark(x: Float, y: Float) {
                    drawCircle(c.surface, 10f, Offset(x, y))
                    drawCircle(c.primary, 8f, Offset(x, y), style = Stroke(2.5f))
                    drawCircle(c.primary, 3f, Offset(x, y))
                }
                fun label(text: String, x: Float, y: Float, from: Offset, to: Offset) {
                    drawLine(c.outline, from, to, 1f)
                    drawText(textMeasurer, text, topLeft = Offset(x, y), style = labelStyle)
                }
                when {
                    front -> {
                        val upper = Path().apply {
                            moveTo(64f, 112f); cubicTo(106f, 79f, 139f, 82f, 180f, 97f)
                            cubicTo(222f, 82f, 253f, 79f, 296f, 112f)
                            cubicTo(247f, 127f, 214f, 129f, 180f, 122f)
                            cubicTo(143f, 129f, 103f, 125f, 64f, 112f); close()
                        }
                        val lower = Path().apply {
                            moveTo(64f, 112f); cubicTo(108f, 127f, 142f, 130f, 180f, 122f)
                            cubicTo(220f, 130f, 254f, 125f, 296f, 112f)
                            cubicTo(249f, 180f, 111f, 180f, 64f, 112f); close()
                        }
                        shape(upper, c.secondaryContainer); shape(lower, c.tertiaryContainer)
                        if (site == ArticulationSite.ROUNDED_LIPS) {
                            drawOval(c.surface, Offset(146f, 98f), Size(68f, 57f))
                            drawOval(c.primary, Offset(146f, 98f), Size(68f, 57f), style = Stroke(5f))
                        } else if (site == ArticulationSite.LOWER_LIP) {
                            drawRoundRect(c.surface, Offset(135f, 101f), Size(90f, 31f), androidx.compose.ui.geometry.CornerRadius(6f))
                            drawLine(c.outline, Offset(180f, 103f), Offset(180f, 130f), 1.5f)
                            drawLine(c.primary, Offset(145f, 133f), Offset(215f, 133f), 5f, StrokeCap.Round)
                            mark(180f, 133f)
                        } else {
                            drawLine(c.primary, Offset(100f, 119f), Offset(260f, 119f), 4f, StrokeCap.Round)
                            mark(180f, 119f)
                        }
                        label(upperLip, 145f, 52f, Offset(180f, 80f), Offset(180f, 98f))
                        label(lowerLip, 145f, 190f, Offset(180f, 179f), Offset(180f, 161f))
                    }
                    top -> {
                        val palate = Path().apply {
                            moveTo(105f, 210f); cubicTo(64f, 108f, 98f, 28f, 180f, 24f)
                            cubicTo(266f, 28f, 297f, 111f, 255f, 210f)
                            lineTo(228f, 200f); cubicTo(262f, 113f, 247f, 57f, 180f, 53f)
                            cubicTo(116f, 57f, 96f, 112f, 132f, 200f); close()
                        }
                        shape(palate, c.secondaryContainer)
                        for (side in listOf(-1, 1)) for (i in 0..4) {
                            drawRoundRect(c.surface, Offset(180f + side * (76f - i * 5) - 10f, 87f + i * 22f), Size(19f, 18f), androidx.compose.ui.geometry.CornerRadius(5f))
                        }
                        val tongue = Path().apply {
                            moveTo(127f, 218f); cubicTo(101f, 166f, 117f, 91f, 180f, 82f)
                            cubicTo(241f, 91f, 253f, 166f, 231f, 218f); close()
                        }
                        shape(tongue, c.tertiaryContainer)
                        drawLine(c.primary, Offset(123f, 136f), Offset(124f, 187f), 7f, StrokeCap.Round)
                        mark(123f, 158f)
                        label(molarsLabel, 0f, 55f, Offset(60f, 80f), Offset(101f, 109f))
                        label(tongueLabel, 268f, 174f, Offset(256f, 184f), Offset(210f, 170f))
                    }
                    else -> {
                        val profile = Path().apply {
                            moveTo(120f, 12f); cubicTo(177f, -1f, 231f, 9f, 243f, 40f)
                            lineTo(238f, 57f); lineTo(273f, 79f); quadraticTo(281f, 90f, 251f, 97f)
                            lineTo(248f, 107f); quadraticTo(269f, 112f, 254f, 120f)
                            quadraticTo(266f, 131f, 249f, 139f); cubicTo(254f, 172f, 220f, 188f, 193f, 190f)
                            lineTo(185f, 223f); lineTo(96f, 223f); lineTo(99f, 170f)
                            cubicTo(69f, 139f, 78f, 84f, 93f, 56f); quadraticTo(100f, 23f, 120f, 12f); close()
                        }
                        shape(profile, c.surfaceContainerHighest)
                        val nasal = Path().apply {
                            moveTo(129f, 47f); cubicTo(176f, 34f, 222f, 47f, 236f, 77f)
                            lineTo(258f, 84f); cubicTo(224f, 98f, 174f, 75f, 126f, 86f); close()
                        }
                        shape(nasal, c.secondaryContainer)
                        val mouth = Path().apply {
                            moveTo(120f, 96f); cubicTo(155f, 77f, 206f, 81f, 235f, 100f)
                            lineTo(250f, 119f); lineTo(237f, 142f)
                            cubicTo(211f, 167f, 165f, 155f, 146f, 155f)
                            lineTo(145f, 223f); lineTo(114f, 223f); cubicTo(108f, 162f, 101f, 122f, 120f, 96f); close()
                        }
                        shape(mouth, c.surface)
                        // Front upper and lower incisors; the tongue is drawn inside the oral cavity.
                        drawRoundRect(c.secondaryContainer, Offset(231f, 99f), Size(10f, 23f), androidx.compose.ui.geometry.CornerRadius(2f))
                        drawRoundRect(c.secondaryContainer, Offset(229f, 136f), Size(10f, 18f), androidx.compose.ui.geometry.CornerRadius(2f))
                        val contact = when (site) {
                            ArticulationSite.BACK_TONGUE -> Offset(137f, 94f)
                            ArticulationSite.FORWARD_BACK_TONGUE -> Offset(154f, 90f)
                            ArticulationSite.MID_TONGUE -> Offset(188f, 89f)
                            ArticulationSite.TONGUE_EDGE -> Offset(216f, 94f)
                            ArticulationSite.TONGUE_TIP -> Offset(222f, 97f)
                            ArticulationSite.TONGUE_TIP_BACK -> Offset(214f, 99f)
                            ArticulationSite.UPPER_GUM -> Offset(230f, 101f)
                            ArticulationSite.LOWER_TEETH -> Offset(226f, 138f)
                            ArticulationSite.UPPER_TEETH_EDGE -> Offset(240f, 123f)
                            else -> null
                        }
                        val tongue = Path().apply {
                            moveTo(146f, 220f); cubicTo(153f, 195f, 139f, 164f, 146f, 143f)
                            if (contact != null) {
                                cubicTo(150f, 118f, contact.x - 17f, contact.y + 4f, contact.x, contact.y + 3f)
                                cubicTo(contact.x + 8f, contact.y + 10f, 211f, 161f, 169f, 170f)
                            } else {
                                cubicTo(164f, 119f, 203f, 131f, 226f, 140f)
                                cubicTo(234f, 151f, 210f, 164f, 169f, 170f)
                            }
                            cubicTo(164f, 190f, 163f, 207f, 163f, 220f); close()
                        }
                        shape(tongue, c.tertiaryContainer)
                        label(nasalLabel, 278f, 39f, Offset(272f, 55f), Offset(214f, 66f))
                        label(teethLabel, 283f, 102f, Offset(278f, 116f), Offset(242f, 113f))
                        label(tongueLabel, 273f, 164f, Offset(265f, 171f), Offset(196f, 153f))
                        label(throatLabel, 22f, 194f, Offset(72f, 205f), Offset(112f, 188f))
                        when (site) {
                            ArticulationSite.CAVITY -> {
                                val flow = Path().apply { moveTo(122f, 209f); cubicTo(113f, 164f, 121f, 117f, 155f, 111f); quadraticTo(192f, 108f, 225f, 125f) }
                                drawPath(flow, c.primary, style = Stroke(4f, cap = StrokeCap.Round))
                                drawLine(c.primary, Offset(225f, 125f), Offset(211f, 127f), 3f)
                                drawLine(c.primary, Offset(225f, 125f), Offset(218f, 113f), 3f)
                            }
                            ArticulationSite.DEEP_THROAT -> mark(124f, 205f)
                            ArticulationSite.MID_THROAT -> mark(119f, 166f)
                            ArticulationSite.UPPER_THROAT -> mark(119f, 122f)
                            else -> contact?.let { mark(it.x, it.y) }
                        }
                    }
                }
            }
        }
        Text(stringResource(R.string.qaida_anatomy_key), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        if (glyph == "و" || glyph == "ي") Text(stringResource(R.string.qaida_anatomy_consonant), style = MaterialTheme.typography.bodySmall)
        if (glyph == "م" || glyph == "ن") Text(stringResource(R.string.qaida_anatomy_nasal), style = MaterialTheme.typography.bodySmall)
    }
}
