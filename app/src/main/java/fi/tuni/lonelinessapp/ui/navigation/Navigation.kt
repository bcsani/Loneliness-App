package fi.tuni.lonelinessapp.ui.navigation

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fi.tuni.lonelinessapp.R
import fi.tuni.lonelinessapp.ui.theme.primaryBlue

const val TAB_HOME = 1
const val TAB_SURVEY = 2
const val TAB_ANALYSIS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onSettingsClick: () -> Unit
) {
    Column {
        TopAppBar(
            // App logo
            title = {
                Image(
                    painter = painterResource(id = R.drawable.logo_small), // <-- Logo file
                    contentDescription = "App Logo",
                    modifier = Modifier.height(45.dp)
                )
            },

            // Settings button
            actions = {
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Line under top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray)
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

        // Function to create icons to bottom navigation
        @Composable
        fun BottomNavItem(
            iconVector: ImageVector,
            label: String,
            index: Int,
            onClick: () -> Unit
        ) {
            val isSelected = currentTab == index && !showSettingsScreen
            val iconColor = if (isSelected) primaryBlue else Color.Gray

            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // Icon for item
                        Icon(
                            imageVector = iconVector,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )

                        // Text for item
                        Text(
                            text = label,
                            color = iconColor
                        )

                        // Line for item
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(24.dp)
                                    .background(primaryBlue)
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

        // Create bottom navigation items
        BottomNavItem(iconVector = Icons.Filled.Home, label = "Home",
                      index = TAB_HOME, onClick = { selectNewTab(TAB_HOME) })

        BottomNavItem(iconVector = Icons.Filled.AddCircle, label = "Survey",
                      index = TAB_SURVEY, onClick = onSurveyButtonClick)

        BottomNavItem(iconVector = Icons.Filled.Analytics, label = "Analysis",
                      index = TAB_ANALYSIS, onClick = { selectNewTab(TAB_ANALYSIS) })
    }
}