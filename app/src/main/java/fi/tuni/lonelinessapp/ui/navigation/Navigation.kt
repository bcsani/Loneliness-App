package fi.tuni.lonelinessapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTab: Int,
    showSettingsScreen: Boolean,
    onSettingsClick: () -> Unit
) {
    // Tab titles
    val titles = listOf(
        "Home",
        "Analysis",
        "Settings"
    )

    TopAppBar(
        // Show title based on screen
        title = {
            Text(
                if (showSettingsScreen) titles.last() else titles[currentTab]
            )
        },
        // Settings icon button
        actions = {
            IconButton(onClick = { onSettingsClick() }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings")
            }
        }
    )
}

@Composable
fun BottomNavigation(
    currentTab: Int,
    showSettingsScreen: Boolean,
    selectNewTab: (Int) -> Unit,
    onSurveyButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {

        // Home tab
        NavigationBarItem(
            icon = { Icon(
                Icons.Filled.Home,
                contentDescription = "Home"
            ) },
            label = { Text("Home") },
            selected = currentTab == 0 && !showSettingsScreen,
            onClick = { selectNewTab(0) }
        )

        // Survey button for popup
        NavigationBarItem(
            icon = { Icon(
                Icons.Filled.AddCircle,
                contentDescription = "Survey"
            ) },
            label = { Text("Survey") },
            selected = false,
            onClick = { onSurveyButtonClick() }
        )

        // Analysis tab
        NavigationBarItem(
            icon = { Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Analysis"
            ) },
            label = { Text("Analysis") },
            selected = currentTab == 1 && !showSettingsScreen,
            onClick = { selectNewTab(1) }
        )
    }
}
