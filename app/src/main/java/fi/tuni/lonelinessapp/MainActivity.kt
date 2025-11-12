package fi.tuni.lonelinessapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import fi.tuni.lonelinessapp.domain.service.SequentialPermissionManager
import fi.tuni.lonelinessapp.domain.service.StepSensorManager
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase

class MainActivity : ComponentActivity() {

    // dayRepository is initialized later for the stepService.
    private lateinit var dayRepository: DayRepository
    private lateinit var stepSensorManager: StepSensorManager
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Create step service with binder
            val binder = service as StepSensorManager.StepTrackingBinder
            stepSensorManager = binder.getService()
            isServiceBound = true

            stepSensorManager.setDayRepository(dayRepository)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
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

        checkAllPermissions()
    }

    private fun initializeStepTrackingService() {
        val intent = Intent(this, StepSensorManager::class.java)

        // Start the service
        ContextCompat.startForegroundService(this, intent)

        // Bind to set the repository
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

    }

    private fun showPermissionDeniedMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind service but don't stop (continues in background)
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun checkAllPermissions() {
        val permissionManager = SequentialPermissionManager(this)

        permissionManager.addPermission(
            permission = Manifest.permission.ACTIVITY_RECOGNITION,
            onGranted = {
                initializeStepTrackingService()
            },
            onDenied = {
                showPermissionDeniedMessage("Activity Recognition")
            },
            rationaleMessage = "We need activity recognition permission to track your steps and physical activity."
        )

        permissionManager.addPermission(
            permission = Manifest.permission.READ_CALL_LOG,
            onGranted = {
                println("Call log permission granted")
            },
            onDenied = {
                showPermissionDeniedMessage("Call log permission denied")
            },
            rationaleMessage = "We need call log permission to track call durations."
        )

        permissionManager.start()
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
