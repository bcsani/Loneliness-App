package fi.tuni.lonelinessapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.ui.navigation.BottomNavigation
import fi.tuni.lonelinessapp.ui.navigation.TopBar
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreen
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.settings.SettingsScreen
import fi.tuni.lonelinessapp.ui.theme.LonelinessAppTheme
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyViewModel
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel
import fi.tuni.lonelinessapp.ui.screens.home.HomeViewModel
import fi.tuni.lonelinessapp.data.AppDatabase
import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import fi.tuni.lonelinessapp.data.repository.DayRepository
import fi.tuni.lonelinessapp.domain.service.StepSensorManager
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase

class MainActivity : ComponentActivity() {

    private val activityRecognitionPermission = Manifest.permission.ACTIVITY_RECOGNITION

    private lateinit var dayRepository: DayRepository
    // Step tracking service
    private lateinit var stepSensorManager: StepSensorManager
    private var isServiceBound = false

    // Use ActivityResultLauncher for better permission handling
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Service is connected, but we're using startService instead of bindService
            // So we'll handle service setup differently
            println("Service connected")
            val binder = service as StepSensorManager.StepTrackingBinder
            stepSensorManager = binder.getService()
            isServiceBound = true

            stepSensorManager.setDayRepository(dayRepository)

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            println("Service disconnected")
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(applicationContext)

        val dayDataSource = DayDataSource(database.dayDao())
        dayRepository = DayRepository(dayDataSource)
        val calculateCorrelationUseCase = CalculateCorrelationUseCase(dayRepository)
        val surveyViewModel = SurveyViewModel(dayRepository)
        val analysisViewModel = AnalysisViewModel(dayRepository)
        val homeViewModel = HomeViewModel(calculateCorrelationUseCase)

        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                initializeStepTrackingService(dayRepository)
            } else {
                // Handle permission denial
                Toast.makeText(this, "Permission denied - step tracking disabled", Toast.LENGTH_LONG).show()
            }
        }

        // Check and request permission
        checkPermission()

        enableEdgeToEdge()
        setContent {
            LonelinessAppTheme {
                MainScreen(
                    surveyViewModel=surveyViewModel,
                    analysisViewModel=analysisViewModel,
                    homeViewModel=homeViewModel
                )
            }
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                activityRecognitionPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeStepTrackingService(dayRepository)
            }
            else -> {
                permissionLauncher.launch(activityRecognitionPermission)
            }
        }
    }

    private fun initializeStepTrackingService(dayRepository: DayRepository) {
        val intent = Intent(this, StepSensorManager::class.java)

        // Start the service first
        ContextCompat.startForegroundService(this, intent)

        // Then bind to set the repository
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind service but don't stop it (continues in background)
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    surveyViewModel: SurveyViewModel,
    analysisViewModel: AnalysisViewModel,
    homeViewModel: HomeViewModel
) {

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
                    0 -> HomeScreen(viewModel, homeViewModel=homeViewModel, surveyViewModel=surveyViewModel)
                    1 -> AnalysisScreen(analysisViewModel=analysisViewModel)
                }
            } else {
                SettingsScreen()
            }

            // Show survey dialog
            if (showSurvey) {
                SurveyDialog(onDismiss = { viewModel.closeSurvey() }, surveyViewModel=surveyViewModel)
            }
        }
    }
}
