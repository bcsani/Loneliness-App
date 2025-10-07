package fi.tuni.lonelinessapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomNavigation(
    currentTab: Int,
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
            selected = currentTab == 0,
            onClick = { selectNewTab(0) }
        )

        // Survey popup
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
            selected = currentTab == 1,
            onClick = { selectNewTab(1) }
        )
    }
}
