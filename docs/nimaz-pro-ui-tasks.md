# Nimaz Pro - UI Component & Screen Building Tasks

> **Prerequisites**: Complete all 35 foundation tasks first. This document assumes you have working ViewModels and data layer.

---

## Document Overview

This document provides step-by-step tasks to build the complete UI from ground up:

| Phase | Tasks | Focus |
|-------|-------|-------|
| **Phase 1** | 1-15 | Atoms (Basic Building Blocks) |
| **Phase 2** | 16-35 | Molecules (Composed Components) |
| **Phase 3** | 36-50 | Organisms (Complex Sections) |
| **Phase 4** | 51-79 | Complete Screens |

**Total: 79 Tasks**

---

# PHASE 1: ATOMS (Tasks 1-15)

Atoms are the smallest, indivisible UI components. They are pure composables with no business logic.

---

## Task 1: NimazButton

**File**: `presentation/components/atoms/NimazButton.kt`

Create buttons with 4 variants matching the design system.

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

enum class NimazButtonVariant {
    PRIMARY,      // Teal filled
    SECONDARY,    // Gold filled
    OUTLINED,     // Teal border, transparent fill
    TEXT          // Text only, no background
}

enum class NimazButtonSize {
    SMALL,        // Height 36dp, text 14sp
    MEDIUM,       // Height 44dp, text 15sp
    LARGE         // Height 52dp, text 16sp
}

@Composable
fun NimazButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazButtonVariant = NimazButtonVariant.PRIMARY,
    size: NimazButtonSize = NimazButtonSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    iconPosition: IconPosition = IconPosition.START
) {
    val height = when (size) {
        NimazButtonSize.SMALL -> 36.dp
        NimazButtonSize.MEDIUM -> 44.dp
        NimazButtonSize.LARGE -> 52.dp
    }
    
    val colors = when (variant) {
        NimazButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = NimazColors.Primary500,
            contentColor = Color.White,
            disabledContainerColor = NimazColors.Primary500.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
        NimazButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = NimazColors.Gold500,
            contentColor = Color.Black,
            disabledContainerColor = NimazColors.Gold500.copy(alpha = 0.5f),
            disabledContentColor = Color.Black.copy(alpha = 0.7f)
        )
        NimazButtonVariant.OUTLINED -> ButtonDefaults.outlinedButtonColors(
            contentColor = NimazColors.Primary500,
            disabledContentColor = NimazColors.Primary500.copy(alpha = 0.5f)
        )
        NimazButtonVariant.TEXT -> ButtonDefaults.textButtonColors(
            contentColor = NimazColors.Primary500,
            disabledContentColor = NimazColors.Primary500.copy(alpha = 0.5f)
        )
    }
    
    when (variant) {
        NimazButtonVariant.PRIMARY, NimazButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors,
                shape = MaterialTheme.shapes.medium
            ) {
                ButtonContent(text, icon, iconPosition, loading)
            }
        }
        NimazButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors,
                shape = MaterialTheme.shapes.medium
            ) {
                ButtonContent(text, icon, iconPosition, loading)
            }
        }
        NimazButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors
            ) {
                ButtonContent(text, icon, iconPosition, loading)
            }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    icon: ImageVector?,
    iconPosition: IconPosition,
    loading: Boolean
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = LocalContentColor.current,
            strokeWidth = 2.dp
        )
    } else {
        if (icon != null && iconPosition == IconPosition.START) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
        if (icon != null && iconPosition == IconPosition.END) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

enum class IconPosition { START, END }
```

**Verification**: Create a preview showing all 4 variants in both enabled and disabled states.

---

## Task 2: NimazIconButton

**File**: `presentation/components/atoms/NimazIconButton.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

enum class NimazIconButtonStyle {
    FILLED,       // Solid background
    TONAL,        // Lighter tinted background
    OUTLINED,     // Border only
    STANDARD      // No background
}

@Composable
fun NimazIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    style: NimazIconButtonStyle = NimazIconButtonStyle.TONAL,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color = NimazColors.Neutral300,
    backgroundColor: Color = NimazColors.Neutral900,
    enabled: Boolean = true
) {
    val bgColor = when (style) {
        NimazIconButtonStyle.FILLED -> backgroundColor
        NimazIconButtonStyle.TONAL -> backgroundColor.copy(alpha = 0.5f)
        NimazIconButtonStyle.OUTLINED -> Color.Transparent
        NimazIconButtonStyle.STANDARD -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (style == NimazIconButtonStyle.OUTLINED) {
                    Modifier.border(1.dp, NimazColors.Neutral700, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) tint else tint.copy(alpha = 0.5f)
        )
    }
}
```

---

## Task 3: NimazCard

**File**: `presentation/components/atoms/NimazCard.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

enum class NimazCardStyle {
    FILLED,       // Solid neutral-900 background
    ELEVATED,     // Slight elevation effect
    OUTLINED,     // Border with transparent bg
    GRADIENT      // Gradient background
}

@Composable
fun NimazCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.FILLED,
    backgroundColor: Color = NimazColors.Neutral900,
    borderColor: Color = NimazColors.Neutral800,
    gradientColors: List<Color>? = null,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    val backgroundModifier = when (style) {
        NimazCardStyle.FILLED -> Modifier.background(backgroundColor)
        NimazCardStyle.ELEVATED -> Modifier.background(backgroundColor)
        NimazCardStyle.OUTLINED -> Modifier
            .background(Color.Transparent)
            .border(1.dp, borderColor, shape)
        NimazCardStyle.GRADIENT -> Modifier.background(
            brush = Brush.linearGradient(
                colors = gradientColors ?: listOf(NimazColors.Primary800, NimazColors.Primary900)
            )
        )
    }
    
    Column(
        modifier = modifier
            .clip(shape)
            .then(backgroundModifier)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

// Specialized gradient cards
@Composable
fun NimazGradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(NimazColors.Primary800, NimazColors.Primary900),
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.GRADIENT,
        gradientColors = gradientColors,
        cornerRadius = cornerRadius,
        onClick = onClick,
        content = content
    )
}
```

---

## Task 4: ArabicText

**File**: `presentation/components/atoms/ArabicText.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nimazpro.app.presentation.theme.AmiriFontFamily
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = NimazColors.Neutral0,
    textAlign: TextAlign = TextAlign.End,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontFamily = AmiriFontFamily,
        fontSize = fontSize,
        color = color,
        textAlign = textAlign,
        lineHeight = if (lineHeight == TextUnit.Unspecified) fontSize * 1.8f else lineHeight,
        maxLines = maxLines,
        style = style.copy(
            textDirection = TextDirection.Rtl
        )
    )
}

// Quran verse text with special styling
@Composable
fun QuranVerseText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = NimazColors.Neutral0,
    verseNumber: Int? = null
) {
    val displayText = if (verseNumber != null) {
        "$text ﴿${verseNumber.toArabicNumerals()}﴾"
    } else text
    
    ArabicText(
        text = displayText,
        modifier = modifier,
        fontSize = fontSize,
        color = color,
        lineHeight = fontSize * 2f
    )
}

// Extension to convert numbers to Arabic numerals
fun Int.toArabicNumerals(): String {
    val arabicNumerals = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return this.toString().map { arabicNumerals[it.digitToInt()] }.joinToString("")
}
```

---

## Task 5: NimazTextField

**File**: `presentation/components/atoms/NimazTextField.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun NimazTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) NimazColors.Error else NimazColors.Neutral400,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = NimazColors.Neutral0
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(NimazColors.Primary500),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = NimazColors.Neutral900,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = when {
                                isError -> NimazColors.Error
                                isFocused -> NimazColors.Primary500
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = NimazColors.Neutral500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NimazColors.Neutral600
                            )
                        }
                        innerTextField()
                    }
                    
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { onTrailingIconClick?.invoke() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = null,
                                tint = NimazColors.Neutral500
                            )
                        }
                    }
                }
            }
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = NimazColors.Error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
```

---

## Task 6: NimazSwitch

**File**: `presentation/components/atoms/NimazSwitch.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun NimazSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = NimazColors.Primary500,
    inactiveColor: Color = NimazColors.Neutral700
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        label = "trackColor"
    )
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "thumbOffset"
    )
    
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
```

---

## Task 7: NimazChip

**File**: `presentation/components/atoms/NimazChip.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

enum class NimazChipStyle {
    FILLED,
    OUTLINED
}

@Composable
fun NimazChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    style: NimazChipStyle = NimazChipStyle.FILLED,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    selectedColor: Color = NimazColors.Primary500,
    unselectedColor: Color = NimazColors.Neutral800
) {
    val backgroundColor = when {
        selected && style == NimazChipStyle.FILLED -> selectedColor
        style == NimazChipStyle.FILLED -> unselectedColor
        else -> Color.Transparent
    }
    
    val contentColor = when {
        selected -> Color.White
        style == NimazChipStyle.OUTLINED -> NimazColors.Neutral300
        else -> NimazColors.Neutral300
    }
    
    val shape = RoundedCornerShape(20.dp)
    
    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (style == NimazChipStyle.OUTLINED && !selected) {
                    Modifier.border(1.dp, NimazColors.Neutral700, shape)
                } else if (style == NimazChipStyle.OUTLINED && selected) {
                    Modifier.border(1.dp, selectedColor, shape)
                } else Modifier
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(6.dp))
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
        
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
        }
    }
}

// Filter chip group
@Composable
fun NimazFilterChipGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            NimazChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelected(option) }
            )
        }
    }
}
```

---

## Task 8: NimazBadge

**File**: `presentation/components/atoms/NimazBadge.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimazpro.app.presentation.theme.NimazColors

enum class NimazBadgeType {
    DEFAULT,      // Neutral gray
    PRIMARY,      // Teal
    SECONDARY,    // Gold
    SUCCESS,      // Green
    WARNING,      // Orange
    ERROR,        // Red
    SAHIH,        // Green for authentic hadith
    HASAN,        // Yellow for good hadith
    DAIF,         // Orange for weak hadith
    MECCAN,       // Purple for Meccan surahs
    MEDINAN       // Blue for Medinan surahs
}

@Composable
fun NimazBadge(
    text: String,
    modifier: Modifier = Modifier,
    type: NimazBadgeType = NimazBadgeType.DEFAULT,
    size: BadgeSize = BadgeSize.MEDIUM
) {
    val (backgroundColor, textColor) = when (type) {
        NimazBadgeType.DEFAULT -> Pair(NimazColors.Neutral800, NimazColors.Neutral300)
        NimazBadgeType.PRIMARY -> Pair(NimazColors.Primary500.copy(alpha = 0.2f), NimazColors.Primary400)
        NimazBadgeType.SECONDARY -> Pair(NimazColors.Gold500.copy(alpha = 0.2f), NimazColors.Gold500)
        NimazBadgeType.SUCCESS -> Pair(Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E))
        NimazBadgeType.WARNING -> Pair(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFF59E0B))
        NimazBadgeType.ERROR -> Pair(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444))
        NimazBadgeType.SAHIH -> Pair(Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E))
        NimazBadgeType.HASAN -> Pair(Color(0xFFEAB308).copy(alpha = 0.2f), Color(0xFFEAB308))
        NimazBadgeType.DAIF -> Pair(Color(0xFFF97316).copy(alpha = 0.2f), Color(0xFFF97316))
        NimazBadgeType.MECCAN -> Pair(Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFF8B5CF6))
        NimazBadgeType.MEDINAN -> Pair(Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF3B82F6))
    }
    
    val (paddingH, paddingV, fontSize) = when (size) {
        BadgeSize.SMALL -> Triple(6.dp, 2.dp, 10.sp)
        BadgeSize.MEDIUM -> Triple(8.dp, 4.dp, 11.sp)
        BadgeSize.LARGE -> Triple(12.dp, 6.dp, 12.sp)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = paddingH, vertical = paddingV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

enum class BadgeSize { SMALL, MEDIUM, LARGE }

// Count badge (for notifications, unread counts)
@Composable
fun NimazCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99
) {
    val displayText = if (count > maxCount) "$maxCount+" else count.toString()
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NimazColors.Error)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
```

---

## Task 9: NimazDivider

**File**: `presentation/components/atoms/NimazDivider.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun NimazDivider(
    modifier: Modifier = Modifier,
    color: Color = NimazColors.Neutral800,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

@Composable
fun NimazVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = NimazColors.Neutral800,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .width(thickness)
            .fillMaxHeight()
            .background(color)
    )
}
```

---

## Task 10: NimazProgressIndicators

**File**: `presentation/components/atoms/NimazProgressIndicators.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

// Linear progress bar
@Composable
fun NimazProgressBar(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    color: Color = NimazColors.Primary500,
    trackColor: Color = NimazColors.Neutral800,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

// Circular progress with percentage
@Composable
fun NimazCircularProgress(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = NimazColors.Primary500,
    trackColor: Color = NimazColors.Neutral800
) {
    Canvas(modifier = modifier.size(size)) {
        val sweepAngle = 360 * progress.coerceIn(0f, 1f)
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        
        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )
        
        // Progress
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = stroke
        )
    }
}

// Loading spinner
@Composable
fun NimazLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = NimazColors.Primary500,
    size: Dp = 40.dp,
    strokeWidth: Dp = 3.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth
    )
}
```

---

## Task 11: PrayerColorIndicator

**File**: `presentation/components/atoms/PrayerColorIndicator.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimazpro.app.domain.model.PrayerType
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun PrayerColorIndicator(
    prayerType: PrayerType,
    modifier: Modifier = Modifier,
    width: Dp = 4.dp,
    height: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(width / 2))
            .background(prayerType.color)
    )
}

// Extension property to get color for prayer type
val PrayerType.color: Color
    get() = when (this) {
        PrayerType.FAJR -> NimazColors.Fajr
        PrayerType.SUNRISE -> Color(0xFFF59E0B)
        PrayerType.DHUHR -> NimazColors.Dhuhr
        PrayerType.ASR -> NimazColors.Asr
        PrayerType.MAGHRIB -> NimazColors.Maghrib
        PrayerType.ISHA -> NimazColors.Isha
    }

// Horizontal color bar for stats
@Composable
fun PrayerColorBar(
    prayerType: PrayerType,
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(prayerType.color.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(prayerType.color)
        )
    }
}
```

---

## Task 12: NimazIcon

**File**: `presentation/components/atoms/NimazIcon.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimazpro.app.presentation.theme.NimazColors

// Icon with colored background container
@Composable
fun NimazIconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    backgroundColor: Color = NimazColors.Primary500.copy(alpha = 0.2f),
    iconTint: Color = NimazColors.Primary500,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}

// Emoji icon (for categories, quick actions)
@Composable
fun NimazEmojiIcon(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    backgroundColor: Color = NimazColors.Neutral800,
    fontSize: Int = 20,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = fontSize.sp
        )
    }
}
```

---

## Task 13: NimazSlider

**File**: `presentation/components/atoms/NimazSlider.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun NimazSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    activeColor: Color = NimazColors.Primary500,
    inactiveColor: Color = NimazColors.Neutral700
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = activeColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor,
            disabledThumbColor = activeColor.copy(alpha = 0.5f),
            disabledActiveTrackColor = activeColor.copy(alpha = 0.5f),
            disabledInactiveTrackColor = inactiveColor.copy(alpha = 0.5f)
        )
    )
}
```

---

## Task 14: NimazCheckbox & NimazRadio

**File**: `presentation/components/atoms/NimazSelectionControls.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun NimazCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = NimazColors.Primary500
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) activeColor else Color.Transparent,
        label = "checkboxBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (checked) activeColor else NimazColors.Neutral600,
        label = "checkboxBorder"
    )
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun NimazRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = NimazColors.Primary500
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeColor else NimazColors.Neutral600,
        label = "radioBorder"
    )
    
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}
```

---

## Task 15: NimazBottomSheet & Dialog Base

**File**: `presentation/components/atoms/NimazContainers.kt`

```kotlin
package com.nimazpro.app.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nimazpro.app.presentation.theme.NimazColors

// Bottom sheet handle
@Composable
fun BottomSheetHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(NimazColors.Neutral600)
    )
}

// Dialog container
@Composable
fun NimazDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NimazColors.Neutral900)
                .padding(24.dp),
            content = content
        )
    }
}

// Confirmation dialog
@Composable
fun NimazConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    NimazDialog(onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = NimazColors.Neutral0
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = NimazColors.Neutral400
        )
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            NimazButton(
                text = dismissText,
                onClick = onDismiss,
                variant = NimazButtonVariant.TEXT
            )
            
            Spacer(Modifier.width(12.dp))
            
            NimazButton(
                text = confirmText,
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                variant = if (isDestructive) NimazButtonVariant.PRIMARY else NimazButtonVariant.PRIMARY
            )
        }
    }
}
```

---

# PHASE 2: MOLECULES (Tasks 16-35)

Molecules are combinations of atoms that form functional UI components.

---

## Task 16: PrayerTimeCard

**File**: `presentation/components/molecules/PrayerTimeCard.kt`

```kotlin
package com.nimazpro.app.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nimazpro.app.domain.model.PrayerType
import com.nimazpro.app.presentation.components.atoms.*
import com.nimazpro.app.presentation.theme.NimazColors

data class PrayerTimeData(
    val type: PrayerType,
    val nameEnglish: String,
    val nameArabic: String,
    val time: String, // Formatted time like "5:23 AM"
    val isPrayed: Boolean = false,
    val isActive: Boolean = false, // Is this the current/next prayer
    val isEnabled: Boolean = true // Sunrise is typically disabled for tracking
)

@Composable
fun PrayerTimeCard(
    prayer: PrayerTimeData,
    onTogglePrayed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        prayer.isActive -> Brush.linearGradient(
            colors = listOf(NimazColors.Primary800, NimazColors.Primary900)
        )
        else -> Brush.linearGradient(
            colors = listOf(NimazColors.Neutral900, NimazColors.Neutral900)
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = prayer.isEnabled) { onTogglePrayed() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prayer icon with color
        NimazIconBox(
            icon = prayer.type.icon,
            size = 44.dp,
            backgroundColor = prayer.type.color.copy(alpha = 0.2f),
            iconTint = prayer.type.color
        )
        
        Spacer(Modifier.width(15.dp))
        
        // Prayer names
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prayer.nameEnglish,
                style = MaterialTheme.typography.titleMedium,
                color = NimazColors.Neutral0
            )
            Text(
                text = prayer.nameArabic,
                style = MaterialTheme.typography.bodySmall,
                color = NimazColors.Neutral400
            )
        }
        
        // Time
        Text(
            text = prayer.time,
            style = MaterialTheme.typography.titleLarge,
            color = NimazColors.Neutral0
        )
        
        if (prayer.isEnabled) {
            Spacer(Modifier.width(15.dp))
            
            // Prayer status checkbox
            PrayerStatusIndicator(
                isPrayed = prayer.isPrayed,
                onClick = onTogglePrayed
            )
        }
    }
}

@Composable
private fun PrayerStatusIndicator(
    isPrayed: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isPrayed) NimazColors.Primary600 else Color.Transparent)
            .then(
                if (!isPrayed) Modifier.border(2.dp, NimazColors.Neutral700, RoundedCornerShape(50))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isPrayed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Prayed",
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
        }
    }
}

// Helper extension for prayer icons
val PrayerType.icon: ImageVector
    get() = when (this) {
        PrayerType.FAJR -> Icons.Default.WbTwilight // Use appropriate icons
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.LightMode
        PrayerType.ASR -> Icons.Default.WbCloudy
        PrayerType.MAGHRIB -> Icons.Default.WbTwilight
        PrayerType.ISHA -> Icons.Default.NightsStay
    }
```

---

## Task 17: CountdownTimer

**File**: `presentation/components/molecules/CountdownTimer.kt`

```kotlin
package com.nimazpro.app.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimazpro.app.presentation.theme.NimazColors
import com.nimazpro.app.presentation.theme.OutfitFontFamily

data class CountdownTime(
    val hours: Int,
    val minutes: Int,
    val seconds: Int
)

@Composable
fun CountdownTimer(
    time: CountdownTime,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CountdownUnit(value = time.hours, label = "Hours")
        CountdownSeparator()
        CountdownUnit(value = time.minutes, label = "Min")
        CountdownSeparator()
        CountdownUnit(value = time.seconds, label = "Sec")
    }
}

@Composable
private fun CountdownUnit(
    value: Int,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NimazColors.Neutral0.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                fontFamily = OutfitFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NimazColors.Neutral0
            )
        }
        
        Spacer(Modifier.height(5.dp))
        
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NimazColors.Neutral500
        )
    }
}

@Composable
private fun CountdownSeparator() {
    Text(
        text = ":",
        fontFamily = OutfitFontFamily,
        fontSize = 28.sp,
        color = NimazColors.Neutral600,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
    )
}

// Compact countdown for widgets/small spaces
@Composable
fun CompactCountdown(
    time: CountdownTime,
    modifier: Modifier = Modifier
) {
    Text(
        text = "${time.hours.toString().padStart(2, '0')}:${time.minutes.toString().padStart(2, '0')}:${time.seconds.toString().padStart(2, '0')}",
        fontFamily = OutfitFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = NimazColors.Primary400,
        modifier = modifier
    )
}
```

---

## Task 18: SurahListItem

**File**: `presentation/components/molecules/SurahListItem.kt`

```kotlin
package com.nimazpro.app.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimazpro.app.domain.model.RevelationType
import com.nimazpro.app.domain.model.Surah
import com.nimazpro.app.presentation.components.atoms.ArabicText
import com.nimazpro.app.presentation.components.atoms.NimazBadge
import com.nimazpro.app.presentation.components.atoms.NimazBadgeType
import com.nimazpro.app.presentation.theme.NimazColors
import com.nimazpro.app.presentation.theme.OutfitFontFamily

@Composable
fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Surah number badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NimazColors.Neutral800),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.number.toString(),
                fontFamily = OutfitFontFamily,
                fontSize = 16.sp,
                color = NimazColors.Primary400
            )
        }
        
        Spacer(Modifier.width(15.dp))
        
        // Surah info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.nameEnglish,
                style = MaterialTheme.typography.titleMedium,
                color = NimazColors.Neutral0
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = surah.nameTransliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral500
                )
                
                if (showBadge) {
                    Text(
                        text = " • ",
                        color = NimazColors.Neutral600
                    )
                    Text(
                        text = "${surah.versesCount} verses",
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Neutral500
                    )
                }
            }
        }
        
        // Arabic name
        Column(horizontalAlignment = Alignment.End) {
            ArabicText(
                text = surah.nameArabic,
                fontSize = 20.sp,
                color = NimazColors.Primary400,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )
            
            if (showBadge) {
                Spacer(Modifier.height(4.dp))
                NimazBadge(
                    text = if (surah.revelationType == RevelationType.MECCAN) "Meccan" else "Medinan",
                    type = if (surah.revelationType == RevelationType.MECCAN) 
                        NimazBadgeType.MECCAN else NimazBadgeType.MEDINAN,
                    size = BadgeSize.SMALL
                )
            }
        }
    }
}
```

---

## Task 19: AyahCard

**File**: `presentation/components/molecules/AyahCard.kt`

```kotlin
package com.nimazpro.app.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimazpro.app.domain.model.Ayah
import com.nimazpro.app.presentation.components.atoms.*
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun AyahCard(
    ayah: Ayah,
    onPlayAudio: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    fontSize: Int = 28
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Arabic text with gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NimazColors.Primary900.copy(alpha = 0.5f),
                            NimazColors.Neutral900
                        )
                    )
                )
                .padding(20.dp)
        ) {
            QuranVerseText(
                text = ayah.textUthmani,
                fontSize = fontSize.sp,
                verseNumber = ayah.numberInSurah
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Transliteration (if enabled)
        if (showTransliteration && ayah.transliteration != null) {
            Text(
                text = ayah.transliteration,
                style = MaterialTheme.typography.bodyMedium,
                color = NimazColors.Primary400,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        
        // Translation
        if (showTranslation && ayah.translation != null) {
            Text(
                text = ayah.translation,
                style = MaterialTheme.typography.bodyLarge,
                color = NimazColors.Neutral300,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazIconButton(
                icon = Icons.Outlined.PlayArrow,
                onClick = onPlayAudio,
                style = NimazIconButtonStyle.TONAL,
                size = 36.dp
            )
            
            NimazIconButton(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                onClick = onBookmark,
                style = NimazIconButtonStyle.TONAL,
                size = 36.dp,
                tint = if (isBookmarked) NimazColors.Gold500 else NimazColors.Neutral400
            )
            
            NimazIconButton(
                icon = Icons.Outlined.Share,
                onClick = onShare,
                style = NimazIconButtonStyle.TONAL,
                size = 36.dp
            )
            
            Spacer(Modifier.weight(1f))
            
            // Verse reference
            Text(
                text = "${ayah.surahId}:${ayah.numberInSurah}",
                style = MaterialTheme.typography.labelMedium,
                color = NimazColors.Neutral500
            )
        }
    }
}
```

---

## Task 20-35: Continue with remaining molecules

Create the following molecule components following the same patterns:

| Task | Component | Description |
|------|-----------|-------------|
| 20 | `HadithListItem` | Hadith in collection list |
| 21 | `HadithCard` | Full hadith display with chain |
| 22 | `DuaListItem` | Dua in category list |
| 23 | `DuaCard` | Full dua with counter |
| 24 | `TasbihCounter` | Animated circular counter |
| 25 | `TasbihPresetCard` | Dhikr preset selection |
| 26 | `BookmarkItem` | Unified bookmark display |
| 27 | `SearchResultItem` | Search result with highlighting |
| 28 | `CalendarDay` | Single day in calendar |
| 29 | `IslamicEventCard` | Upcoming event display |
| 30 | `StatCard` | Statistics card with icon |
| 31 | `SettingsItem` | Settings row |
| 32 | `SettingsSection` | Grouped settings |
| 33 | `LocationItem` | Location in list |
| 34 | `ReciterItem` | Audio reciter selection |
| 35 | `QuickActionCard` | Home screen quick action |

---

# PHASE 3: ORGANISMS (Tasks 36-50)

Organisms are complex components combining multiple molecules.

| Task | Component | Description |
|------|-----------|-------------|
| 36 | `PrayerTimesSection` | Complete prayer times with countdown |
| 37 | `QuranReader` | Scrollable ayah list with audio |
| 38 | `HadithReader` | Full hadith with navigation |
| 39 | `DuaReader` | Dua with audio and counter |
| 40 | `CalendarGrid` | Monthly calendar view |
| 41 | `PrayerStatsChart` | Prayer completion charts |
| 42 | `FastingCalendar` | Ramadan/fasting tracker |
| 43 | `QiblaCompass` | Animated compass |
| 44 | `ZakatForm` | Asset input form |
| 45 | `BottomNavBar` | Main navigation |
| 46 | `TopAppBar` | Screen header |
| 47 | `SearchBar` | Expandable search |
| 48 | `AudioPlayer` | Mini audio player |
| 49 | `OnboardingSlide` | Single onboarding page |
| 50 | `FilterTabs` | Horizontal filter tabs |

---

# PHASE 4: SCREENS (Tasks 51-79)

Build complete screens using organisms and molecules.

| Task | Screen | Route |
|------|--------|-------|
| 51 | `HomeScreen` | home |
| 52 | `QuranHomeScreen` | quran |
| 53 | `QuranReaderScreen` | quran/{surahId} |
| 54 | `SurahInfoScreen` | quran/{surahId}/info |
| 55 | `HadithCollectionScreen` | hadith |
| 56 | `HadithReaderScreen` | hadith/{bookId}/{hadithId} |
| 57 | `DuasCollectionScreen` | duas |
| 58 | `DuaReaderScreen` | duas/{categoryId}/{duaId} |
| 59 | `TasbihScreen` | tasbih |
| 60 | `PrayerTrackerScreen` | prayer-tracker |
| 61 | `PrayerStatsScreen` | prayer-stats |
| 62 | `FastTrackerScreen` | fasting |
| 63 | `MakeupFastsScreen` | fasting/makeup |
| 64 | `ZakatCalculatorScreen` | zakat |
| 65 | `QiblaScreen` | qibla |
| 66 | `IslamicCalendarScreen` | calendar |
| 67 | `BookmarksScreen` | bookmarks |
| 68 | `SearchScreen` | search |
| 69 | `MoreMenuScreen` | more |
| 70 | `SettingsScreen` | settings |
| 71 | `PrayerSettingsScreen` | settings/prayer |
| 72 | `NotificationSettingsScreen` | settings/notifications |
| 73 | `QuranSettingsScreen` | settings/quran |
| 74 | `AppearanceSettingsScreen` | settings/appearance |
| 75 | `LocationScreen` | settings/location |
| 76 | `LanguageScreen` | settings/language |
| 77 | `WidgetsScreen` | settings/widgets |
| 78 | `AboutScreen` | about |
| 79 | `OnboardingScreen` | onboarding |

---

# Screen Implementation Example

## Task 51: HomeScreen

**File**: `presentation/screens/home/HomeScreen.kt`

```kotlin
package com.nimazpro.app.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nimazpro.app.presentation.components.atoms.*
import com.nimazpro.app.presentation.components.molecules.*
import com.nimazpro.app.presentation.components.organisms.*
import com.nimazpro.app.presentation.theme.NimazColors

@Composable
fun HomeScreen(
    onNavigateToQuran: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToMore: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "quran" -> onNavigateToQuran()
                        "hadith" -> onNavigateToHadith()
                        "duas" -> onNavigateToDuas()
                        "more" -> onNavigateToMore()
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with prayer countdown
            item {
                HomeHeader(
                    location = uiState.currentLocation?.name ?: "Loading...",
                    hijriDate = uiState.hijriDate,
                    gregorianDate = uiState.gregorianDate,
                    nextPrayer = uiState.nextPrayer,
                    countdown = uiState.countdown,
                    onLocationClick = { /* Navigate to location */ },
                    onNotificationClick = { /* Navigate to notifications */ },
                    onSettingsClick = { /* Navigate to settings */ }
                )
            }
            
            // Prayer times section
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(
                    title = "Prayer Times",
                    actionText = "View All",
                    onActionClick = { /* Navigate to full prayer times */ }
                )
            }
            
            items(uiState.todayPrayers) { prayer ->
                PrayerTimeCard(
                    prayer = prayer,
                    onTogglePrayed = { 
                        viewModel.onEvent(HomeEvent.LogPrayer(prayer.type)) 
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            
            // Quick actions
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Quick Actions")
                
                QuickActionsGrid(
                    onQuranClick = onNavigateToQuran,
                    onTasbihClick = onNavigateToTasbih,
                    onQiblaClick = { /* Navigate to qibla */ },
                    onDuasClick = onNavigateToDuas,
                    onCalendarClick = { /* Navigate to calendar */ },
                    onZakatClick = { /* Navigate to zakat */ },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            
            // Continue reading
            if (uiState.readingProgress != null) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionHeader(title = "Continue Reading")
                    
                    ContinueReadingCard(
                        surahName = uiState.readingProgress?.surahName ?: "",
                        ayahNumber = uiState.readingProgress?.ayahNumber ?: 0,
                        progress = uiState.readingProgress?.progress ?: 0f,
                        onClick = { /* Navigate to reader */ },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            
            // Bottom spacing
            item {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HomeHeader(
    location: String,
    hijriDate: String?,
    gregorianDate: String?,
    nextPrayer: PrayerTimeData?,
    countdown: CountdownTime?,
    onLocationClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NimazColors.Primary900, NimazColors.Neutral950)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top row with location and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLocationClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NimazColors.Primary500
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NimazColors.Neutral300
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NimazIconButton(
                        icon = Icons.Outlined.Notifications,
                        onClick = onNotificationClick,
                        style = NimazIconButtonStyle.TONAL
                    )
                    NimazIconButton(
                        icon = Icons.Outlined.Settings,
                        onClick = onSettingsClick,
                        style = NimazIconButtonStyle.TONAL
                    )
                }
            }
            
            Spacer(Modifier.height(30.dp))
            
            // Dates
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hijriDate != null) {
                    ArabicText(
                        text = hijriDate,
                        fontSize = 18.sp,
                        color = NimazColors.Primary400,
                        textAlign = TextAlign.Center
                    )
                }
                
                if (gregorianDate != null) {
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Neutral400
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Next prayer
                if (nextPrayer != null) {
                    Text(
                        text = "NEXT PRAYER",
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.Neutral400,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = nextPrayer.nameEnglish,
                        style = MaterialTheme.typography.displayMedium,
                        color = NimazColors.Neutral0
                    )
                    
                    ArabicText(
                        text = nextPrayer.nameArabic,
                        fontSize = 24.sp,
                        color = NimazColors.Primary400,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Countdown
                if (countdown != null) {
                    CountdownTimer(time = countdown)
                    
                    Spacer(Modifier.height(15.dp))
                    
                    if (nextPrayer != null) {
                        Text(
                            text = nextPrayer.time,
                            style = MaterialTheme.typography.titleMedium,
                            color = NimazColors.Gold500
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NimazColors.Neutral0
        )
        
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = NimazColors.Primary500
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onQuranClick: () -> Unit,
    onTasbihClick: () -> Unit,
    onQiblaClick: () -> Unit,
    onDuasClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onZakatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("📖", "Quran", onQuranClick),
        QuickAction("📿", "Tasbih", onTasbihClick),
        QuickAction("🧭", "Qibla", onQiblaClick),
        QuickAction("🤲", "Duas", onDuasClick),
        QuickAction("📅", "Calendar", onCalendarClick),
        QuickAction("💰", "Zakat", onZakatClick),
    )
    
    // 2 rows of 3
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.take(3).forEach { action ->
                QuickActionCard(
                    emoji = action.emoji,
                    label = action.label,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.drop(3).forEach { action ->
                QuickActionCard(
                    emoji = action.emoji,
                    label = action.label,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class QuickAction(
    val emoji: String,
    val label: String,
    val onClick: () -> Unit
)
```

---

# Final Notes

## Component Organization Checklist

After completing all tasks, verify:

- [ ] All 15 Atom components created
- [ ] All 20 Molecule components created  
- [ ] All 15 Organism components created
- [ ] All 29 Screens created
- [ ] All screens connected to ViewModels
- [ ] Navigation working between all screens
- [ ] Dark/Light theme working
- [ ] Arabic text rendering correctly (RTL)
- [ ] Animations smooth and performant

## Design Token Consistency

Ensure all components use:
- `NimazColors` for colors
- `MaterialTheme.typography` for text styles
- `MaterialTheme.shapes` for corner radii
- Standard spacing: 4, 8, 12, 16, 20, 24, 32dp

## Testing Components

For each component, create:
1. `@Preview` composables showing all states
2. Unit tests for any logic
3. Screenshot tests for visual regression

---

**Document Version**: 1.0  
**Total Tasks**: 79  
**Estimated Time**: 40-60 hours
