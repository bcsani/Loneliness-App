package fi.tuni.lonelinessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fi.tuni.lonelinessapp.ui.navigation.BottomNavigation
import fi.tuni.lonelinessapp.ui.navigation.TopBar
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreen
import fi.tuni.lonelinessapp.ui.screens.home.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.settings.SettingsScreen
import fi.tuni.lonelinessapp.ui.theme.LonelinessAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LonelinessAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // Selected bottom tab
    var selectedTab by remember { mutableIntStateOf(0) }

    // Survey dialog visibility
    var showSurvey by remember { mutableStateOf(false) }

    // Settings screen visibility
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                currentTab = selectedTab,
                showSettingsScreen = showSettings,
                onSettingsClick = {showSettings = !showSettings}
            )
        },
        bottomBar = {
            BottomNavigation(
                currentTab = selectedTab,
                showSettingsScreen = showSettings,
                selectNewTab = {
                    selectedTab = it
                    showSettings = false},
                onSurveyButtonClick = { showSurvey = true }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Show main content or settings
            if (!showSettings) {
                when (selectedTab) {
                    0 -> HomeScreen()
                    1 -> AnalysisScreen()
                }
            } else {
                SettingsScreen()
            }

            // Show survey dialog
            if (showSurvey) {
                SurveyDialog(onDismiss = { showSurvey = false })
            }
        }
    }
}
