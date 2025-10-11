package fi.tuni.lonelinessapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.databinding.ActivityMainBinding
import fi.tuni.lonelinessapp.service.StepForegroundService
import fi.tuni.lonelinessapp.ui.navigation.BottomNavigation
import fi.tuni.lonelinessapp.ui.navigation.TopBar
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreen
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.settings.SettingsScreen
import fi.tuni.lonelinessapp.ui.step.StepViewModel
import fi.tuni.lonelinessapp.ui.theme.LonelinessAppTheme

class MainActivity : ComponentActivity () {
    private lateinit var binding: ActivityMainBinding
    private val stepViewModel: StepViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startStepService()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LonelinessAppTheme {
                MainScreen()
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            startStepService()
        }
    }

    private fun startStepService() {
        println("Hello")
        val intent = Intent(this, StepForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {

    // Selected bottom tab
    val selectedTab by viewModel.selectedTab

    // Survey dialog visibility
    val showSettings by viewModel.showSettings

    // Settings screen visibility
    val showSurvey by viewModel.showSurvey

    Scaffold(
        topBar = {
            TopBar(
                currentTab = selectedTab,
                showSettingsScreen = showSettings,
                onSettingsClick = {viewModel.toggleSettings()}
            )
        },
        bottomBar = {
            BottomNavigation(
                currentTab = selectedTab,
                showSettingsScreen = showSettings,
                selectNewTab = { viewModel.selectTab(it) },
                onSurveyButtonClick = { viewModel.openSurvey() }
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
                    0 -> HomeScreen(viewModel)
                    1 -> AnalysisScreen()
                }
            } else {
                SettingsScreen()
            }

            // Show survey dialog
            if (showSurvey) {
                SurveyDialog(onDismiss = { viewModel.closeSurvey() })
            }
        }
    }
}
