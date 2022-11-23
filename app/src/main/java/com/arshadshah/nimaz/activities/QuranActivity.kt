package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.components.ui.quran.MoreMenu
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.navigation.QuranNavGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.launch

class QuranActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NimazTheme {
                val navController = rememberNavController()
                val (menuOpen, setMenuOpen) = remember { mutableStateOf(false) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {Text("Quran", style = MaterialTheme.typography.titleLarge)},
                            navigationIcon = {
                                IconButton(onClick = {
                                    //if nav destination is quran screen then finish activity
                                    if (navController.currentDestination?.route == "quran") {
                                        finish()
                                    } else {
                                        navController.popBackStack()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                //open the menu
                                IconButton(onClick = { setMenuOpen(true) }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Menu"
                                    )
                                }
                                MoreMenu(
                                    menuOpen = menuOpen,
                                    setMenuOpen = setMenuOpen
                                )
                            }
                        )
                    }

                ) { it ->
                    QuranNavGraph(navController = navController, it)
                }
            }
        }
    }
}