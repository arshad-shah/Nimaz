package com.arshadshah.nimaz.ui.screens.settings

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(navController: NavHostController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Debug Menu") },
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                },
                actions = {
                    // Search Icon
                    IconButton(onClick = {
                        searchQuery = if (searchQuery.isEmpty()) " " else ""
                    }) {
                        Icon(
                            imageVector = if (searchQuery.isEmpty()) Icons.Rounded.Search else Icons.Rounded.Close,
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Add Icon
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus_icon),
                            contentDescription = "Add",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search Bar
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                NimazTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    type = NimazTextFieldType.SEARCH,
                    placeholder = "Search preferences...",
                    leadingIconVector = Icons.Rounded.Search,
                    onSearchClick = { },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Preferences") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Statistics") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Actions") }
                )
            }

            when (selectedTab) {
                0 -> SharedPreferencesTab(searchQuery, snackbarHostState)
                1 -> StatisticsTab()
                2 -> ActionsTab(
                    snackbarHostState,
                    prefs = PrivateSharedPreferences(context)
                )
            }
        }

        // Add Preference Dialog
        if (showAddDialog) {
            AddPreferenceDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { key, value ->
                    scope.launch {
                        val prefs = PrivateSharedPreferences(context)
                        prefs.saveData(key, value)
                        snackbarHostState.showSnackbar("Preference added: $key")
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

data class PreferenceItem(
    val key: String,
    val value: Any,
    val type: PreferenceType,
    val category: PreferenceCategory,
    val displayName: String = key,
    val description: String? = null,
    val tags: Set<String> = emptySet(),
    val lastModified: Long = System.currentTimeMillis()
)

enum class PreferenceType {
    STRING, BOOLEAN, INT, LONG, FLOAT, DOUBLE, INT_SET
}

enum class PreferenceCategory {
    PRAYER_TIMES,
    LOCATION,
    APPEARANCE,
    SYSTEM,
    PERMISSIONS,
    DEBUG;

    fun getIcon(): ImageVector = when (this) {
        PRAYER_TIMES -> Icons.Rounded.Schedule
        LOCATION -> Icons.Rounded.LocationOn
        APPEARANCE -> Icons.Rounded.Palette
        SYSTEM -> Icons.Rounded.Settings
        PERMISSIONS -> Icons.Rounded.Security
        DEBUG -> Icons.Rounded.BugReport
    }
}

enum class PreferenceSortOption {
    KEY_ASC,
    KEY_DESC,
    TYPE,
    CATEGORY,
    LAST_MODIFIED
}

data class PreferenceFilter(
    val searchQuery: String = "",
    val categories: Set<PreferenceCategory> = emptySet(),
    val types: Set<PreferenceType> = emptySet(),
    val tags: Set<String> = emptySet(),
    val sortBy: PreferenceSortOption = PreferenceSortOption.KEY_ASC
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPreferencesTab(searchQuery: String, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val sharedPreferences = remember { PrivateSharedPreferences(context) }
    val scope = rememberCoroutineScope()

    // Sync searchQuery from parent with local filter
    var filter by remember { mutableStateOf(PreferenceFilter()) }

    // Update filter when searchQuery changes from parent
    LaunchedEffect(searchQuery) {
        filter = filter.copy(searchQuery = searchQuery.trim())
    }

    var showFilterSheet by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf<PreferenceItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        PreferenceFilterBar(
            filter = filter,
            onFilterClick = { showFilterSheet = true },
            onFilterChange = { filter = it },
            onSortOptionSelected = { sortOption ->
                filter = filter.copy(sortBy = sortOption)
            }
        )

        val allData = remember(filter, sharedPreferences) {
            sharedPreferences.getAllData()
                .map { (key, value) ->
                    PreferenceItem(
                        key = key,
                        value = value ?: "",
                        type = determinePreferenceType(value),
                        category = determineCategory(key),
                        displayName = getDisplayName(key),
                        description = getDescription(key),
                        tags = getTags(key)
                    )
                }
                .filter { item ->
                    filterPreference(item, filter)
                }
                .sortedWith(getSortComparator(filter.sortBy))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allData, key = { it.key }) { item ->
                var expanded by remember { mutableStateOf(false) }
                SharedTabCard(
                    item = item,
                    expanded = expanded,
                    onExpandClick = { expanded = !expanded },
                    onDeleteClick = {
                        scope.launch {
                            sharedPreferences.removeData(item.key)
                            snackbarHostState.showSnackbar("Preference deleted: ${item.displayName}")
                        }
                    },
                    onEditClick = { showEditDialog = item }
                )
            }
        }
    }

    if (showFilterSheet) {
        PreferenceFilterSheet(
            currentFilter = filter,
            onFilterChange = { filter = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    showEditDialog?.let { item ->
        EditPreferenceDialog(
            item = item,
            onDismiss = { showEditDialog = null },
            onSave = { key, value ->
                scope.launch {
                    when (item.type) {
                        PreferenceType.STRING -> sharedPreferences.saveData(key, value)
                        PreferenceType.BOOLEAN -> sharedPreferences.saveDataBoolean(
                            key,
                            value.toBoolean()
                        )

                        PreferenceType.INT -> sharedPreferences.saveDataInt(key, value.toInt())
                        PreferenceType.LONG -> sharedPreferences.saveDataLong(key, value.toLong())
                        PreferenceType.FLOAT -> sharedPreferences.saveDataFloat(
                            key,
                            value.toFloat()
                        )

                        PreferenceType.DOUBLE -> sharedPreferences.saveDataDouble(
                            key,
                            value.toDouble()
                        )

                        PreferenceType.INT_SET -> {
                            try {
                                val set = value.trim('[', ']').split(",").map { it.trim().toInt() }
                                    .toSet()
                                sharedPreferences.saveIntSet(key, set)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Invalid format for Int Set")
                                return@launch
                            }
                        }
                    }
                    snackbarHostState.showSnackbar("Preference updated: $key")
                    showEditDialog = null
                }
            }
        )
    }
}

@Composable
private fun PreferenceFilterBar(
    filter: PreferenceFilter,
    onFilterClick: () -> Unit,
    onSortOptionSelected: (PreferenceSortOption) -> Unit,
    onFilterChange: (PreferenceFilter) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(0.6f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(PreferenceCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = category in filter.categories,
                        onClick = {
                            onFilterChange(
                                filter.copy(
                                    categories = if (filter.categories.contains(category)) {
                                        filter.categories - category
                                    } else {
                                        filter.categories + category
                                    }
                                )
                            )
                        },
                        label = {
                            Text(
                                category.name.split("_")
                                    .joinToString(" ")
                                    .lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = category.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                }
            }
            Row(
                modifier = Modifier.weight(0.4f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter"
                    )
                }

                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showSortMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort"
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        PreferenceSortOption.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (sortOption) {
                                            PreferenceSortOption.KEY_ASC -> "Key (A to Z)"
                                            PreferenceSortOption.KEY_DESC -> "Key (Z to A)"
                                            PreferenceSortOption.TYPE -> "Type"
                                            PreferenceSortOption.CATEGORY -> "Category"
                                            PreferenceSortOption.LAST_MODIFIED -> "Last Modified"
                                        }
                                    )
                                },
                                onClick = {
                                    onSortOptionSelected(sortOption)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceFilterSheet(
    currentFilter: PreferenceFilter,
    onFilterChange: (PreferenceFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialogNimaz(
        icon = painterResource(id = R.drawable.settings_icon),
        title = "Filter Preferences",
        contentDescription = "Filter and sort preferences",
        contentHeight = 450.dp,
        cardContent = false,
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmButtonText = "Apply",
        showDismissButton = true,
        onDismiss = {
            // Reset filters
            onFilterChange(PreferenceFilter())
            onDismiss()
        },
        dismissButtonText = "Reset",
        action = {
            // Active filters count badge
            val activeFilters = currentFilter.categories.size + currentFilter.types.size
            if (activeFilters > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$activeFilters active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Categories Section
                FilterSection(title = "Categories") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreferenceCategory.entries.forEach { category ->
                            FilterChip(
                                selected = category in currentFilter.categories,
                                onClick = {
                                    val newCategories = if (category in currentFilter.categories) {
                                        currentFilter.categories - category
                                    } else {
                                        currentFilter.categories + category
                                    }
                                    onFilterChange(currentFilter.copy(categories = newCategories))
                                },
                                label = {
                                    Text(
                                        category.name.split("_").joinToString(" ").lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.getIcon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Types Section
                FilterSection(title = "Types") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreferenceType.entries.forEach { type ->
                            FilterChip(
                                selected = type in currentFilter.types,
                                onClick = {
                                    val newTypes = if (type in currentFilter.types) {
                                        currentFilter.types - type
                                    } else {
                                        currentFilter.types + type
                                    }
                                    onFilterChange(currentFilter.copy(types = newTypes))
                                },
                                label = {
                                    Text(
                                        type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }

                // Sort Options Section
                FilterSection(title = "Sort By") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PreferenceSortOption.entries.forEach { sortOption ->
                            SortOptionItem(
                                label = when (sortOption) {
                                    PreferenceSortOption.KEY_ASC -> "Key (A to Z)"
                                    PreferenceSortOption.KEY_DESC -> "Key (Z to A)"
                                    PreferenceSortOption.TYPE -> "Type"
                                    PreferenceSortOption.CATEGORY -> "Category"
                                    PreferenceSortOption.LAST_MODIFIED -> "Last Modified"
                                },
                                isSelected = sortOption == currentFilter.sortBy,
                                onClick = { onFilterChange(currentFilter.copy(sortBy = sortOption)) }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun determineCategory(key: String): PreferenceCategory {
    return when {
        // Prayer times related
        key.contains("_adjustment") ||
                key.contains("_angle") ||
                key.contains("calculation_method") ||
                key.contains("madhab") ||
                key.contains("high_latitude_rule") -> PreferenceCategory.PRAYER_TIMES

        // Location related
        key.contains("latitude") ||
                key.contains("longitude") ||
                key.contains("location") -> PreferenceCategory.LOCATION

        // Appearance/UI
        key.contains("FontSize") -> PreferenceCategory.APPEARANCE

        // Permissions
        key.contains("permission") ||
                key.contains("notification") ||
                key.contains("batteryOptimization") -> PreferenceCategory.PERMISSIONS

        // Debug
        key.contains("debug") ||
                key.contains("test") -> PreferenceCategory.DEBUG

        // System
        else -> PreferenceCategory.SYSTEM
    }
}

private fun getDisplayName(key: String): String {
    return when (key) {
        "calculation_method" -> "Calculation Method"
        "madhab" -> "Madhab"
        "high_latitude_rule" -> "High Latitude Rule"
        "location_type" -> "Location Method"
        "ArabicFontSize" -> "Arabic Font Size"
        "TranslationFontSize" -> "Translation Font Size"
        "batteryOptimization" -> "Battery Optimization"
        "notification_permission" -> "Notification Permission"
        "location_permission" -> "Location Permission"
        else -> when {
            key.endsWith("_adjustment") -> "${
                key.removeSuffix("_adjustment")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } Adjustment"

            key.endsWith("_angle") -> "${
                key.removeSuffix("_angle")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } Angle"

            else -> key.split("_").joinToString(" ") {
                it.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }
        }
    }
}

private fun filterPreference(item: PreferenceItem, filter: PreferenceFilter): Boolean {
    return (filter.searchQuery.isEmpty() ||
            item.displayName.contains(filter.searchQuery, ignoreCase = true) ||
            item.key.contains(filter.searchQuery, ignoreCase = true) ||
            item.value.toString().contains(filter.searchQuery, ignoreCase = true)) &&
            (filter.categories.isEmpty() || item.category in filter.categories) &&
            (filter.types.isEmpty() || item.type in filter.types) &&
            (filter.tags.isEmpty() || item.tags.any { it in filter.tags })
}

private fun getSortComparator(sortOption: PreferenceSortOption): Comparator<PreferenceItem> {
    return when (sortOption) {
        PreferenceSortOption.KEY_ASC -> compareBy { it.key }
        PreferenceSortOption.KEY_DESC -> compareByDescending { it.key }
        PreferenceSortOption.TYPE -> compareBy<PreferenceItem> { it.type }.thenBy { it.key }
        PreferenceSortOption.CATEGORY -> compareBy<PreferenceItem> { it.category }.thenBy { it.key }
        PreferenceSortOption.LAST_MODIFIED -> compareByDescending { it.lastModified }
    }
}

private fun determinePreferenceType(value: Any?): PreferenceType = when (value) {
    is String -> {
        // Try to parse as double if it's a string
        if (value.toDoubleOrNull() != null) {
            PreferenceType.DOUBLE
        } else {
            PreferenceType.STRING
        }
    }

    is Boolean -> PreferenceType.BOOLEAN
    is Int -> PreferenceType.INT
    is Long -> PreferenceType.LONG
    is Float -> PreferenceType.FLOAT
    is Set<*> -> PreferenceType.INT_SET
    else -> PreferenceType.STRING
}

private fun getDescription(key: String): String? {
    return when (key) {
        "calculation_method" -> "Method used to calculate prayer times (ISNA, MWL, etc.)"
        "madhab" -> "School of thought for Asr prayer calculation"
        "high_latitude_rule" -> "Rule for calculating prayer times in high latitude regions"
        "location_type" -> "Method of location determination (GPS or manual)"
        "ArabicFontSize" -> "Font size for Arabic text display"
        "TranslationFontSize" -> "Font size for translation text"
        "batteryOptimization" -> "Allow app to run in background for notifications"
        "notification_permission" -> "Permission to show prayer time notifications"
        "location_permission" -> "Permission to access device location"
        else -> when {
            key.endsWith("_adjustment") -> "Time adjustment (in minutes) for ${key.removeSuffix("_adjustment")} prayer"
            key.endsWith("_angle") -> "Angle calculation for ${key.removeSuffix("_angle")} prayer"
            else -> null
        }
    }
}

private fun getTags(key: String): Set<String> {
    val tags = mutableSetOf<String>()

    when {
        key.endsWith("_adjustment") -> {
            tags.add("prayer")
            tags.add("adjustment")
            tags.add(key.removeSuffix("_adjustment"))
        }

        key.endsWith("_angle") -> {
            tags.add("prayer")
            tags.add("angle")
            tags.add(key.removeSuffix("_angle"))
        }

        key.contains("FontSize") -> {
            tags.add("appearance")
            tags.add("font")
        }

        key.contains("location") -> {
            tags.add("location")
        }

        key.contains("permission") -> {
            tags.add("permission")
        }
    }

    return tags
}

@Composable
private fun SharedTabCard(
    item: PreferenceItem,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onExpandClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (item.type) {
                                        PreferenceType.STRING -> Icons.Rounded.TextFields
                                        PreferenceType.BOOLEAN -> Icons.Rounded.ToggleOn
                                        PreferenceType.INT, PreferenceType.LONG -> Icons.Rounded.Numbers
                                        PreferenceType.FLOAT, PreferenceType.DOUBLE -> Icons.AutoMirrored.Rounded.ShowChart
                                        PreferenceType.INT_SET -> Icons.AutoMirrored.Rounded.List
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.key,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type badge
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.type.name.lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Expand icon
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expanded) "Show less" else "Show more",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(6.dp).size(18.dp)
                            )
                        }
                    }
                }
            }

            // Content Section (when expanded)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Value Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Current Value",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = item.value.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Actions Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Delete")
                            }
                            Button(
                                onClick = onEditClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsTab(
) {
    val context = LocalContext.current
    var expandedCards by remember { mutableStateOf(setOf<String>()) }

    val toggleCard = { cardId: String ->
        expandedCards = if (expandedCards.contains(cardId)) {
            expandedCards - cardId
        } else {
            expandedCards + cardId
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Shared Preferences Card
        item {
            StatisticsCard(
                title = "Shared Preferences",
                icon = Icons.Default.Settings,
                isExpanded = expandedCards.contains("prefs"),
                onExpandClick = { toggleCard("prefs") }
            ) {
                val prefs = PrivateSharedPreferences(context)
                val allData = prefs.getAllData()

                StatRow("Total Preferences", allData.size.toString())
                StatRow("Boolean Values", allData.count { it.value is Boolean }.toString())
                StatRow("String Values", allData.count { it.value is String }.toString())
                StatRow(
                    "Numeric Values",
                    allData.count { it.value is Int || it.value is Long || it.value is Float || it.value is Double }
                        .toString()
                )
                StatRow("Set Values", allData.count { it.value is Set<*> }.toString())
            }
        }
        // System Information Card
        item {
            StatisticsCard(
                title = "System Information",
                icon = Icons.Default.Info,
                isExpanded = expandedCards.contains("system"),
                onExpandClick = { toggleCard("system") }
            ) {
                val activityManager =
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo =
                    ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
                val internalStorage = StatFs(context.filesDir.path)
                val blockSize = internalStorage.blockSizeLong
                val totalSize = internalStorage.blockCountLong * blockSize
                val availableSize = internalStorage.availableBlocksLong * blockSize

                StatRow("Android Version", Build.VERSION.RELEASE)
                StatRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                StatRow("Available Memory", "${memoryInfo.availMem / (1024 * 1024)} MB")
                StatRow("Total Memory", "${memoryInfo.totalMem / (1024 * 1024)} MB")
                StatRow("Available Storage", "${availableSize / (1024 * 1024)} MB")
                StatRow("Total Storage", "${totalSize / (1024 * 1024)} MB")
                StatRow("Low Memory", if (memoryInfo.lowMemory) "Yes" else "No")
            }
        }

        // App Information Card
        item {
            StatisticsCard(
                title = "App Information",
                icon = Icons.Default.Apps,
                isExpanded = expandedCards.contains("app"),
                onExpandClick = { toggleCard("app") }
            ) {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val appDir = context.filesDir

                packageInfo.versionName?.let { StatRow("Version Name", it) }
                StatRow("Version Code", Build.VERSION.SDK_INT.toString())
                StatRow("Package Name", context.packageName)
                StatRow("Data Directory Size", "${calculateDirSize(appDir) / 1024} KB")
                StatRow(
                    "First Install", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(packageInfo.firstInstallTime))
                )
                StatRow(
                    "Last Update", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(packageInfo.lastUpdateTime))
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onExpandClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Show less" else "Show more",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                }
            }

            // Content Section (when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun calculateDirSize(dir: File): Long {
    var size: Long = 0
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) {
            calculateDirSize(file)
        } else {
            file.length()
        }
    }
    return size
}

@Composable
fun ActionsTab(
    snackbarHostState: SnackbarHostState,
    prefs: PrivateSharedPreferences
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // File picker launcher for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isImporting = true
                try {
                    importPreferences(context, uri, prefs)
                    snackbarHostState.showSnackbar("Preferences imported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to import preferences: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        }
    }

    // File creation launcher for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    exportPreferences(context, uri, prefs)
                    snackbarHostState.showSnackbar("Preferences exported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to export preferences: ${e.message}")
                } finally {
                    isExporting = false
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Data Management Section
        item {
            SectionHeader(
                title = "Data Management",
                icon = Icons.Rounded.Delete
            )
        }

        // Clear All Data
        item {
            ActionCard(
                title = "Clear All Preferences",
                description = "This will delete all saved preferences. This action cannot be undone.",
                icon = Icons.Rounded.Delete,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { showConfirmDialog = true }
            )
        }

        // Import/Export Section
        item {
            SectionHeader(
                title = "Import / Export",
                icon = Icons.Default.Upload
            )
        }

        // Export
        item {
            ActionCard(
                title = "Export Preferences",
                description = "Save all preferences to a JSON file",
                icon = Icons.Default.Upload,
                isLoading = isExporting,
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(Date())
                    exportLauncher.launch("nimaz_prefs_$timestamp.json")
                }
            )
        }

        // Import
        item {
            ActionCard(
                title = "Import Preferences",
                description = "Load preferences from a JSON file",
                icon = Icons.Default.Download,
                isLoading = isImporting,
                onClick = { importLauncher.launch("application/json") }
            )
        }
    }

    // Confirm Dialog for clearing data
    if (showConfirmDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.settings_icon),
            title = "Clear All Preferences?",
            contentDescription = "Confirm clearing all preferences",
            description = "This action will delete all saved preferences and cannot be undone.",
            contentHeight = 150.dp,
            cardContent = false,
            onDismissRequest = { showConfirmDialog = false },
            onConfirm = {
                scope.launch {
                    prefs.clearData()
                    snackbarHostState.showSnackbar("All preferences cleared")
                    showConfirmDialog = false
                }
            },
            confirmButtonText = "Clear All",
            onDismiss = { showConfirmDialog = false },
            dismissButtonText = "Cancel",
            contentToShow = {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Warning: This cannot be undone!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }
}


@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    rightContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        enabled = isEnabled && !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (iconTint == MaterialTheme.colorScheme.error)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Content Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right content or chevron
            if (rightContent != null) {
                rightContent()
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

private fun exportPreferences(context: Context, uri: Uri, prefs: PrivateSharedPreferences) {
    val allPrefs = prefs.getAllData()
    val jsonObject = JSONObject()

    allPrefs.forEach { (key, value) ->
        jsonObject.put(
            key, when (value) {
                is Set<*> -> JSONObject().apply {
                    put("type", "Set")
//                put("value", JSONObject(value.map { it.toString() }))
                }

                else -> JSONObject().apply {
                    put("type", value?.javaClass?.simpleName ?: "null")
                    put("value", value)
                }
            })
    }

    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(jsonObject.toString(2).toByteArray())
    }
}

private fun importPreferences(context: Context, uri: Uri, prefs: PrivateSharedPreferences) {
    val jsonString =
        context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            ?: throw Exception("Could not read file")

    val jsonObject = JSONObject(jsonString)
    val keys = jsonObject.keys()

    while (keys.hasNext()) {
        val key = keys.next()
        val valueObj = jsonObject.getJSONObject(key)
        val type = valueObj.getString("type")
        val value = valueObj.get("value")

        when (type) {
            "String" -> prefs.saveData(key, value.toString())
            "Boolean" -> prefs.saveDataBoolean(key, value as Boolean)
            "Integer" -> prefs.saveDataInt(key, value as Int)
            "Long" -> prefs.saveDataLong(key, value as Long)
            "Float" -> prefs.saveDataFloat(key, value as Float)
            "Double" -> prefs.saveDataDouble(key, value as Double)
            "Set" -> {
                val setObj = value as JSONObject
                val setValues = setObj.keys().asSequence().toSet()
                prefs.saveData(key, setValues.toString())
            }
        }
    }
}

@Composable
fun AddPreferenceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PreferenceType.STRING) }

    AlertDialogNimaz(
        icon = painterResource(id = R.drawable.plus_icon),
        title = "Add New Preference",
        contentDescription = "Add a new shared preference",
        contentHeight = 350.dp,
        cardContent = false,
        onDismissRequest = onDismiss,
        onConfirm = {
            onAdd(key, value)
            onDismiss()
        },
        confirmButtonText = "Add",
        showConfirmButton = key.isNotBlank() && value.isNotBlank(),
        onDismiss = onDismiss,
        dismissButtonText = "Cancel",
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NimazTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = "Key",
                    placeholder = "Enter preference key"
                )

                // Type selector
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(PreferenceType.STRING, PreferenceType.BOOLEAN, PreferenceType.INT, PreferenceType.DOUBLE).forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = {
                                        selectedType = type
                                        if (type == PreferenceType.BOOLEAN && value !in listOf("true", "false")) {
                                            value = "false"
                                        }
                                    },
                                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }

                // Value input based on type
                when (selectedType) {
                    PreferenceType.BOOLEAN -> {
                        BooleanSelector(
                            value = value == "true",
                            onValueChange = { value = it.toString() }
                        )
                    }
                    PreferenceType.INT, PreferenceType.LONG -> {
                        NimazTextField(
                            value = value,
                            onValueChange = { value = it },
                            type = NimazTextFieldType.NUMBER,
                            label = "Value",
                            placeholder = "Enter integer value"
                        )
                    }
                    PreferenceType.FLOAT, PreferenceType.DOUBLE -> {
                        NimazTextField(
                            value = value,
                            onValueChange = { value = it },
                            type = NimazTextFieldType.NUMBER,
                            label = "Value",
                            placeholder = "Enter decimal value"
                        )
                    }
                    else -> {
                        NimazTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = "Value",
                            placeholder = "Enter string value"
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun BooleanSelector(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Value",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // True option
                Surface(
                    onClick = { onValueChange(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (value) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value,
                            onClick = { onValueChange(true) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "True",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (value) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (value) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // False option
                Surface(
                    onClick = { onValueChange(false) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (!value) MaterialTheme.colorScheme.errorContainer
                           else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (!value) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !value,
                            onClick = { onValueChange(false) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "False",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!value) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!value) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditPreferenceDialog(
    item: PreferenceItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var value by remember { mutableStateOf(item.value.toString()) }
    var hasError by remember { mutableStateOf(false) }

    // Calculate content height based on type
    val contentHeight = when (item.type) {
        PreferenceType.BOOLEAN -> 180.dp
        PreferenceType.INT_SET -> 250.dp
        else -> 200.dp
    }

    AlertDialogNimaz(
        icon = painterResource(id = R.drawable.settings_icon),
        title = "Edit Preference",
        contentDescription = "Edit shared preference value",
        description = "Key: ${item.key}",
        contentHeight = contentHeight,
        cardContent = false,
        onDismissRequest = onDismiss,
        onConfirm = {
            onSave(item.key, value)
            onDismiss()
        },
        confirmButtonText = "Save",
        showConfirmButton = !hasError && value.isNotBlank(),
        onDismiss = onDismiss,
        dismissButtonText = "Cancel",
        action = {
            // Type badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = item.type.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (item.type) {
                    PreferenceType.BOOLEAN -> {
                        BooleanSelector(
                            value = value.lowercase() == "true",
                            onValueChange = { value = it.toString() }
                        )
                    }
                    PreferenceType.INT, PreferenceType.LONG -> {
                        NimazTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                hasError = try {
                                    if (item.type == PreferenceType.INT) it.toInt()
                                    else it.toLong()
                                    false
                                } catch (e: Exception) {
                                    it.isNotBlank()
                                }
                            },
                            type = NimazTextFieldType.NUMBER,
                            label = "Value",
                            placeholder = "Enter ${item.type.name.lowercase()} value",
                            isError = hasError,
                            errorMessage = if (hasError) "Invalid ${item.type.name.lowercase()} format" else null
                        )
                    }
                    PreferenceType.FLOAT, PreferenceType.DOUBLE -> {
                        NimazTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                hasError = try {
                                    if (item.type == PreferenceType.FLOAT) it.toFloat()
                                    else it.toDouble()
                                    false
                                } catch (e: Exception) {
                                    it.isNotBlank()
                                }
                            },
                            type = NimazTextFieldType.NUMBER,
                            label = "Value",
                            placeholder = "Enter decimal value",
                            isError = hasError,
                            errorMessage = if (hasError) "Invalid decimal format" else null
                        )
                    }
                    PreferenceType.INT_SET -> {
                        NimazTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                hasError = try {
                                    it.trim('[', ']').split(",").map { num -> num.trim().toInt() }
                                    false
                                } catch (e: Exception) {
                                    it.isNotBlank()
                                }
                            },
                            type = NimazTextFieldType.MULTILINE,
                            label = "Value",
                            placeholder = "Enter comma-separated integers (e.g., 1, 2, 3)",
                            isError = hasError,
                            errorMessage = if (hasError) "Invalid format. Use comma-separated integers" else null,
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                    PreferenceType.STRING -> {
                        NimazTextField(
                            value = value,
                            onValueChange = { value = it },
                            type = NimazTextFieldType.MULTILINE,
                            label = "Value",
                            placeholder = "Enter string value",
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            }
        }
    )
}