package fi.tuni.lonelinessapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


private val COLOR_PRIMARY = Color(0xFF2563EB)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTab: Int,
    showSettingsScreen: Boolean,
    onSettingsClick: () -> Unit
) {
    val titles = listOf("Home", "Analysis", "Settings")

    Column { // Column mahdollistaa viivan lisäämisen
        TopAppBar(
            title = {
                Text(
                    if (showSettingsScreen) titles.last() else titles[currentTab]
                )
            },
            actions = {
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Viiva TopBarin alaosaan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray) // Haluttu väri viivalle
        )
    }
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

        @Composable
        fun BottomNavItem(
            iconVector: ImageVector,
            label: String,
            index: Int,
            onClick: () -> Unit
        ) {
            val isSelected = index >= 0 && currentTab == index && !showSettingsScreen
            val iconColor = if (isSelected) COLOR_PRIMARY else Color.Black

            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(
                            imageVector = iconVector,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )


                        Text(
                            text = label,
                            color = iconColor
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(24.dp)
                                    .background(Color.Blue)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                },
                selected = isSelected,
                onClick = onClick
            )
        }

        // Home tab (Material Icon)
        BottomNavItem(iconVector = Icons.Filled.Home, label = "Home", index = 0, onClick = { selectNewTab(0) })

        // Survey tab (oma SVG, ei alaviivaa)
        BottomNavItem(iconVector = Icons.Filled.AddCircle, label = "Survey", index = -1, onClick = onSurveyButtonClick)

        // Analysis tab (Material Icon)
        BottomNavItem(iconVector = Icons.Filled.Analytics, label = "Analysis", index = 1, onClick = { selectNewTab(1) })
    }
}