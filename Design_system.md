# Nimaz App Design System

## Overview

This document defines the design language and component patterns used throughout the Nimaz Android app. It serves as a reference for maintaining visual consistency and helping developers (and AI agents) understand the established patterns.

---

## Core Principles

1. **Consistency** - All components follow the same structural patterns
2. **Hierarchy** - Clear visual hierarchy through surfaces and elevation
3. **Accessibility** - Proper contrast ratios and touch targets
4. **Material 3** - Built on Material Design 3 foundations
5. **Sectioned Layout** - Content organized in distinct surface sections within cards

---

## Card Structure

### Primary Card Container

All major UI sections use `ElevatedCard` as the outer container:

```kotlin
ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,  // ~28dp corners
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),  // Outer padding inside card
        verticalArrangement = Arrangement.spacedBy(12.dp)  // Space between sections
    ) {
        // Header Section
        // Content Sections
        // Action Section
    }
}
```

**Key Properties:**
- Shape: `MaterialTheme.shapes.extraLarge` (consistent rounded corners)
- Elevation: `4.dp` default
- Container: `surface` color
- Inner padding: `8.dp`
- Section spacing: `12.dp`

---

## Section Patterns

### 1. Header Section

Headers use `primaryContainer` color and contain title + optional badges/actions:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    shape = RoundedCornerShape(16.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Icon + Title
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in container (optional)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SomeIcon,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title + Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Section Title",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Subtitle or description",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Right: Badges + Expand icon
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Status badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Badge",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Expand/collapse icon (if expandable)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    modifier = Modifier.padding(6.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
```

**Header Variants by State:**
| State | Container Color | Icon/Text Color |
|-------|----------------|-----------------|
| Default/Active | `primaryContainer` | `onPrimaryContainer` |
| Paused/Inactive | `surfaceVariant` | `onSurfaceVariant` |
| Success/Complete | `tertiaryContainer` | `onTertiaryContainer` |
| Warning | `secondaryContainer` | `onSecondaryContainer` |

---

### 2. Content Section

Content sections use semi-transparent `surfaceVariant`:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),  // Inner content padding
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Content here
    }
}
```

**Key Properties:**
- Color: `surfaceVariant.copy(alpha = 0.5f)` - semi-transparent for layering
- Shape: `RoundedCornerShape(16.dp)`
- Padding: `16.dp` (or `12.dp` for denser content)
- Spacing: `12.dp` between elements

---

### 3. Action Section

Action buttons grouped in their own surface:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Primary action (filled button)
        Button(
            onClick = { },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Action, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Primary Action", style = MaterialTheme.typography.labelLarge)
        }
        
        // Secondary actions (tonal icon buttons)
        FilledTonalIconButton(
            onClick = { },
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(Icons.Default.Edit, modifier = Modifier.size(20.dp))
        }
        
        // Destructive action
        FilledTonalIconButton(
            onClick = { },
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            )
        ) {
            Icon(
                Icons.Default.Delete,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
```

---

## Component Specifications

### Buttons

| Type | Height | Shape | Usage |
|------|--------|-------|-------|
| Primary `Button` | 44-52dp | `RoundedCornerShape(12.dp)` | Main actions |
| `FilledIconButton` | 36-44dp | `RoundedCornerShape(12.dp)` | Prominent icon actions |
| `FilledTonalIconButton` | 36-44dp | `RoundedCornerShape(12.dp)` | Secondary icon actions |
| Compact icon button | 32-36dp | Default | Inline actions |

**Button Colors:**
```kotlin
// Primary action
ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary
)

// Secondary/Tonal action
IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer
)

// Neutral action
IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
)

// Destructive action
IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
)
// With tint:
tint = MaterialTheme.colorScheme.error
```

---

### Badges / Chips

Status badges use small rounded surfaces:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    shape = RoundedCornerShape(8.dp)  // Smaller for badges
) {
    Text(
        text = "Badge Text",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
```

**Badge Color Mapping:**
| Semantic | Container | Text |
|----------|-----------|------|
| Active/Primary | `primaryContainer` | `onPrimaryContainer` |
| Success/Complete | `tertiaryContainer` | `onTertiaryContainer` |
| Warning/Paused | `secondaryContainer` | `onSecondaryContainer` |
| Neutral | `surfaceVariant` | `onSurfaceVariant` |
| Error | `errorContainer` | `onErrorContainer` |
| Percentage/Number | `primary` on light bg | `onPrimary` |

---

### Icon Containers

Icons wrapped in decorative containers:

```kotlin
// Standard icon container (40dp)
Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.size(40.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.SomeIcon,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// Medium icon container (44dp)
Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.size(44.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.SomeIcon,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// Tinted/subtle container (in headers)
Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    modifier = Modifier.size(40.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.SomeIcon,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

**Icon Container Sizes:**
| Size | Modifier | Icon Size | Corner Radius |
|------|----------|-----------|---------------|
| Small | `40.dp` | `20.dp` | `10.dp` |
| Medium | `44.dp` | `24.dp` | `10.dp` |
| Large | `48.dp` | `24-28.dp` | `12.dp` |

---

### Progress Indicators

#### Circular Progress (with icon inside)

```kotlin
Box(contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier.size(48.dp),
        strokeWidth = 4.dp,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}
```

#### Linear Progress

```kotlin
LinearProgressIndicator(
    progress = { progressFraction },
    modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp)),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.surface  // or surfaceVariant
)
```

**Progress Heights:**
- Compact/inline: `6.dp`
- Standard: `8.dp`
- Emphasis: `10.dp`

---

### Selection Cards (Toggle Pattern)

For mutually exclusive options (like Auto/Manual mode selectors):

```kotlin
// Wrapper surface containing all options
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Option cards (equal weight)
        SelectionCard(isSelected = true, ...)
        SelectionCard(isSelected = false, ...)
    }
}

// Individual selection card
@Composable
private fun RowScope.SelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Text Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Optional: Selection indicator (checkmark)
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
```

**Selection Card States:**
| State | Container | Icon Container | Text Color |
|-------|-----------|----------------|------------|
| Selected | `primaryContainer` | `primary @ 0.2` | `onPrimaryContainer` |
| Unselected | `surface` | `surfaceVariant` | `onSurface` / `onSurfaceVariant` |

**Key Properties:**
- Wrapper: `surfaceVariant @ 0.5`, `16.dp` corners, `8.dp` padding
- Cards: `12.dp` corners, `12.dp` padding
- Icon container: `40.dp`, `10.dp` corners
- Spacing between cards: `8.dp`
- Checkmark indicator: `24.dp`, `6.dp` corners

---

### Search Input Pattern

For search fields with action button:

```kotlin
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row with title and badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Location",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Optional: Context badge
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Manual",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Search Input Container
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Enter city or address",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
                )

                FilledIconButton(
                    onClick = { onSearch(query) },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Helper Text
        Text(
            text = "Enter a city name, postal code, or full address",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
```

**Search Input Key Properties:**
- Outer container: `surfaceVariant @ 0.5`, `16.dp` corners
- Input container: `surface`, `12.dp` corners
- Search button: `FilledIconButton`, `44.dp` size
- Text field has transparent borders (borderless look)
- Helper text: `bodySmall`, `onSurfaceVariant @ 0.7`

---

### Text Fields

```kotlin
OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = {
        Text(
            "Placeholder text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    },
    leadingIcon = {
        Icon(
            Icons.Default.Search,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    },
    trailingIcon = {
        // Clear button or action
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),  // or 14.dp
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
)
```

---

### Info Display Row

For displaying read-only information (like current location):

```kotlin
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Location",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dublin, Ireland",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional: Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}
```

---

## Spacing System

### Standard Spacing Values

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | `2.dp` | Inline text spacing, title/subtitle gap |
| `sm` | `4.dp` | Tight element spacing, loading indicator margin |
| `md` | `8.dp` | Card inner padding, compact spacing, selection card gaps |
| `lg` | `12.dp` | Section spacing, standard gaps, content padding |
| `xl` | `16.dp` | Content padding, generous spacing |
| `xxl` | `20.dp` | Form field spacing |
| `xxxl` | `24.dp` | Major section breaks |

### Component-Specific Spacing

```kotlin
// Card structure
ElevatedCard {
    Column(
        modifier = Modifier.padding(8.dp),           // Card inner padding
        verticalArrangement = Arrangement.spacedBy(12.dp)  // Section spacing
    )
}

// Section content
Surface {
    Column(
        modifier = Modifier.padding(16.dp),          // Section inner padding (standard)
        // or Modifier.padding(12.dp)                // Section inner padding (compact)
        verticalArrangement = Arrangement.spacedBy(12.dp)  // Element spacing
    )
}

// Row elements
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),   // Standard
    // or spacedBy(10.dp) for icon + text
    // or spacedBy(12.dp) for more breathing room
)

// Selection cards in wrapper
Row(
    modifier = Modifier.padding(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
)

// Button rows
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
)
```

---

## Typography Usage

| Style | Usage |
|-------|-------|
| `titleMedium` + `SemiBold` | Card/section titles |
| `titleSmall` + `SemiBold` | Subsection titles, item names, info labels |
| `bodyMedium` | Primary content text, info values |
| `bodySmall` | Secondary text, descriptions, metadata, helper text |
| `labelLarge` + `SemiBold` | Button text, selection card titles |
| `labelMedium` | Badges, chips, emphasized labels |
| `labelSmall` + `Bold` | Small badges, captions |

---

## Color Semantics

### Surface Hierarchy

```
┌─────────────────────────────────────────┐
│ ElevatedCard (surface)                  │
│  ┌───────────────────────────────────┐  │
│  │ Header (primaryContainer)         │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ Selection Area (surfaceVariant)   │  │
│  │  ┌─────────┐  ┌─────────┐         │  │
│  │  │Selected │  │Unselect │         │  │
│  │  │primary  │  │surface  │         │  │
│  │  │Container│  │         │         │  │
│  │  └─────────┘  └─────────┘         │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ Content (surfaceVariant @ 0.5α)   │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │ Nested (surface)            │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ Actions (surfaceVariant @ 0.5α)   │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Semantic Color Usage

| Semantic | Primary Color | Container | On-Container |
|----------|--------------|-----------|--------------|
| Primary/Active | `primary` | `primaryContainer` | `onPrimaryContainer` |
| Secondary | `secondary` | `secondaryContainer` | `onSecondaryContainer` |
| Success/Complete | `tertiary` | `tertiaryContainer` | `onTertiaryContainer` |
| Error/Delete | `error` | `errorContainer` | `onErrorContainer` |
| Neutral/Disabled | `outline` | `surfaceVariant` | `onSurfaceVariant` |

### State-Based Coloring

```kotlin
// Active/Selected state
color = MaterialTheme.colorScheme.primary
containerColor = MaterialTheme.colorScheme.primaryContainer

// Inactive/Unselected state
color = MaterialTheme.colorScheme.onSurfaceVariant
containerColor = MaterialTheme.colorScheme.surface  // or surfaceVariant

// Paused/Inactive state
color = MaterialTheme.colorScheme.outline
containerColor = MaterialTheme.colorScheme.surfaceVariant

// Nearly complete / Success
color = MaterialTheme.colorScheme.tertiary
containerColor = MaterialTheme.colorScheme.tertiaryContainer

// Error / Destructive
color = MaterialTheme.colorScheme.error
containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
```

---

## Shape System

| Token | Value | Usage |
|-------|-------|-------|
| `extraLarge` | ~28dp | Cards, dialogs |
| `large` | 16dp | Inner sections, large surfaces |
| `medium` | 12dp | Buttons, inputs, selection cards |
| `small` | 10dp | Icon containers |
| `extraSmall` | 8dp | Badges, chips |
| `tiny` | 6dp | Small badges, checkmarks |
| `micro` | 4dp | Progress bar corners |

```kotlin
// Explicit shapes used
RoundedCornerShape(16.dp)  // Sections, content surfaces
RoundedCornerShape(12.dp)  // Buttons, inputs, selection cards
RoundedCornerShape(10.dp)  // Icon containers
RoundedCornerShape(8.dp)   // Badges, chips
RoundedCornerShape(6.dp)   // Small badges, checkmark indicators
RoundedCornerShape(4.dp)   // Progress bars
RoundedCornerShape(2.dp)   // Inline progress indicators
```

---

## Animation Patterns

### Expand/Collapse

```kotlin
AnimatedVisibility(
    visible = isExpanded,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
) {
    // Content
}
```

### Button Appear/Disappear

```kotlin
AnimatedVisibility(
    visible = showButton,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut()
) {
    // Button
}
```

### Content Transitions

```kotlin
AnimatedContent(
    targetState = state,
    transitionSpec = {
        fadeIn() + slideInVertically { 20 } togetherWith fadeOut()
    }
) { currentState ->
    // Content based on state
}
```

---

## Compact Row Components

For list items or inline displays (like `ReadingProgressCard`, `KhatamListItem`):

```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainerLow,  // or surface
    shape = RoundedCornerShape(16.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading: Icon/Badge (40-48dp)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            // Number or Icon
        }
        
        // Content: Title + subtitle (weight 1f)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Title + badge
            Row {
                Text(title, modifier = Modifier.weight(1f, fill = false))
                Badge()
            }
            // Row 2: Subtitle + inline progress
            Row {
                Text(subtitle)
                LinearProgressIndicator(modifier = Modifier.weight(1f))
            }
        }
        
        // Trailing: Action buttons (36-40dp each)
        FilledIconButton(modifier = Modifier.size(40.dp)) { }
        FilledTonalIconButton(modifier = Modifier.size(40.dp)) { }
    }
}
```

---

## Floating/Overlay Components

For components that float above other content (like `KhatamProgressBar` above bottom nav):

```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shadowElevation = 8.dp  // Higher elevation for floating
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact content
    }
}
```

---

## Empty States

```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon in container
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,  // or primaryContainer.copy(alpha = 0.5f)
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SomeIcon,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Title
        Text(
            text = "Empty State Title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Subtitle
        Text(
            text = "Description or call to action",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
```

---

## Checklist for New Components

When creating a new component, verify:

- [ ] Uses `ElevatedCard` with `extraLarge` shape for main containers
- [ ] Has `4.dp` elevation for cards
- [ ] Inner card padding is `8.dp`
- [ ] Section spacing is `12.dp`
- [ ] Header uses `primaryContainer` (or appropriate variant)
- [ ] Content sections use `surfaceVariant.copy(alpha = 0.5f)`
- [ ] Inner surfaces use `RoundedCornerShape(16.dp)`
- [ ] Selection cards use `RoundedCornerShape(12.dp)`
- [ ] Buttons use `RoundedCornerShape(12.dp)`
- [ ] Icon containers use `RoundedCornerShape(10.dp)` with `40-44.dp` size
- [ ] Action buttons are `44.dp` height/size
- [ ] Compact icon buttons are `36-40.dp`
- [ ] Badges use `RoundedCornerShape(6-8.dp)`
- [ ] Progress bars have `RoundedCornerShape(4.dp)` corners
- [ ] Typography follows the style guide
- [ ] Colors use semantic mapping (not hardcoded)
- [ ] Selection states use `primaryContainer` vs `surface`
- [ ] Includes preview composables
- [ ] Supports dark mode via Material theme colors

---

## File Organization

Components should be organized as:

```
ui/
├── components/
│   ├── common/           # Shared components
│   ├── settings/         # Settings components
│   │   ├── LocationSettings.kt
│   │   └── ...
│   ├── quran/            # Quran-specific components
│   │   ├── KhatamComponents.kt
│   │   ├── ReadingProgressCard.kt
│   │   └── ...
│   └── ...
├── screens/
│   ├── settings/
│   │   ├── AboutScreen.kt
│   │   └── ...
│   ├── quran/
│   │   ├── QuranSearchScreen.kt
│   │   ├── StartKhatamScreen.kt
│   │   └── ...
│   └── ...
└── theme/
    ├── Color.kt
    ├── Theme.kt
    ├── Type.kt
    └── Shape.kt
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-03 | Initial design system documentation |
| 1.1 | 2025-01 | Added selection cards, search input, info display patterns |

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────┐
│ NIMAZ DESIGN QUICK REFERENCE                            │
├─────────────────────────────────────────────────────────┤
│ Card:        ElevatedCard, extraLarge, 4dp elevation    │
│ Sections:    surfaceVariant @ 0.5 alpha, 16dp corners   │
│ Headers:     primaryContainer, 16dp corners             │
│ Buttons:     44dp, 12dp corners                         │
│ Icon btns:   36-44dp, filledTonal                       │
│ Icon cont:   40-44dp, 10dp corners                      │
│ Badges:      6-8dp corners, labelSmall + Bold           │
│ Selection:   12dp corners, primaryContainer when active │
│ Spacing:     8dp (card), 12dp (sections), 16dp (content)│
│ Progress:    8dp height, 4dp corners                    │
│ Inputs:      12dp corners, surface container            │
│ Checkmarks:  24dp, 6dp corners, primary color           │
└─────────────────────────────────────────────────────────┘
```