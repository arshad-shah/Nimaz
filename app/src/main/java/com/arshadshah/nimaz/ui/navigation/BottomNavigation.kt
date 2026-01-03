package com.arshadshah.nimaz.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Custom colors for the bottom navigation
    val navigationBarColor = MaterialTheme.colorScheme.surface
    val selectedItemColor = MaterialTheme.colorScheme.primary
    val unselectedItemColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.PrayerTimesScreen,
        BottomNavItem.QuranScreen,
        BottomNavItem.MoreScreen,
        BottomNavItem.SettingsScreen
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(navigationBarColor)
            .navigationBarsPadding(),
        containerColor = Color.Transparent,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen_route
            val iconSize by animateDpAsState(
                targetValue = if (selected) 28.dp else 24.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ), label = ""
            )

            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            painter = if (selected) {
                                painterResource(id = item.icon)
                            } else {
                                painterResource(id = item.icon_empty)
                            },
                            contentDescription = item.title,
                            tint = if (selected) selectedItemColor else unselectedItemColor
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) selectedItemColor else unselectedItemColor
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.screen_route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedItemColor,
                    unselectedIconColor = unselectedItemColor,
                    selectedTextColor = selectedItemColor,
                    unselectedTextColor = unselectedItemColor,
                    indicatorColor = selectedItemColor.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnhancedBottomNavigationBarPreview() {
    val navController = rememberNavController()
    NimazTheme {
        BottomNavigationBar(navController = navController)
    }
}