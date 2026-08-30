package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.MealViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Generate : Screen("generate", "Generate", Icons.Default.Restaurant)
    object Saved : Screen("saved", "Saved", Icons.Default.Bookmark)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: MealViewModel = viewModel()
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Generate) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                listOf(Screen.Generate, Screen.Saved, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedTab == screen,
                        onClick = { selectedTab = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                Screen.Generate -> HomeScreen(viewModel)
                Screen.Saved -> SavedMealsScreen(viewModel)
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}
