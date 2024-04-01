package com.arshadshah.nimaz.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp



class Dimensions(
    val grid_0_25: Dp,
    val grid_0_5: Dp,
    val grid_1: Dp,
    val grid_1_5: Dp,
    val grid_2: Dp,
    val grid_2_5: Dp,
    val grid_3: Dp,
    val grid_3_5: Dp,
    val grid_4: Dp,
    val grid_4_5: Dp,
    val grid_5: Dp,
    val grid_5_5: Dp,
    val grid_6: Dp,
    val plane_0: Dp,
    val plane_1: Dp,
    val plane_2: Dp,
    val plane_3: Dp,
    val plane_4: Dp,
    val plane_5: Dp,
    val minimum_touch_target: Dp = 48.dp,
)

val smallDimensions = Dimensions(
    grid_0_25 = 2.dp, // Slight adjustment for finer granularity
    grid_0_5 = 4.dp,
    grid_1 = 8.dp, // Standard base unit
    grid_1_5 = 12.dp,
    grid_2 = 16.dp,
    grid_2_5 = 20.dp,
    grid_3 = 24.dp,
    grid_3_5 = 28.dp,
    grid_4 = 32.dp,
    grid_4_5 = 36.dp,
    grid_5 = 40.dp,
    grid_5_5 = 44.dp,
    grid_6 = 48.dp,
    plane_0 = 0.dp,
    plane_1 = 2.dp, // Adjusted for clarity in elevation
    plane_2 = 4.dp,
    plane_3 = 6.dp,
    plane_4 = 8.dp,
    plane_5 = 16.dp, // Keeping consistent with material design elevation
    minimum_touch_target = 48.dp, // Ensuring accessibility compliance
)

val sw360Dimensions = Dimensions(
    grid_0_25 = 4.dp, // Larger screens can handle larger base units
    grid_0_5 = 8.dp,
    grid_1 = 16.dp,
    grid_1_5 = 24.dp,
    grid_2 = 32.dp,
    grid_2_5 = 40.dp,
    grid_3 = 48.dp,
    grid_3_5 = 56.dp,
    grid_4 = 64.dp,
    grid_4_5 = 72.dp,
    grid_5 = 80.dp,
    grid_5_5 = 88.dp,
    grid_6 = 96.dp,
    plane_0 = 0.dp,
    plane_1 = 2.dp, // Even on larger screens, subtle elevations can be effective
    plane_2 = 4.dp,
    plane_3 = 8.dp,
    plane_4 = 16.dp,
    plane_5 = 24.dp, // Increased for more pronounced elevation on large devices
    minimum_touch_target = 48.dp, // Maintained for accessibility
)