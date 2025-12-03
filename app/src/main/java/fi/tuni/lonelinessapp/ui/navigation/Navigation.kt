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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTab: Int,
    showSettingsScreen: Boolean,
    onSettingsClick: () -> Unit
) {
    val titles = listOf("Home", "Analysis", "Settings")

    Column {
        TopAppBar(
            title = {
                Image(
                    painter = painterResource(id = R.drawable.logo_small), // <-- Varmista, että tämä vastaa tiedostonimeäsi
                    contentDescription = "App Logo",
                    modifier = Modifier.height(45.dp)
                )
            },
            actions = {
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // line under top bar
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
            val iconColor = if (isSelected) primaryBlue else Color.Gray

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

        BottomNavItem(iconVector = Icons.Filled.Home, label = "Home", index = 0, onClick = { selectNewTab(0) })

        BottomNavItem(iconVector = Icons.Filled.AddCircle, label = "Survey", index = -1, onClick = onSurveyButtonClick)

        BottomNavItem(iconVector = Icons.Filled.Analytics, label = "Analysis", index = 1, onClick = { selectNewTab(1) })
    }
}