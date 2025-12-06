package fi.tuni.lonelinessapp

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
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
import fi.tuni.lonelinessapp.data.utils.CallDurationHelper
import fi.tuni.lonelinessapp.domain.service.AppUsageTracker
import fi.tuni.lonelinessapp.domain.service.BluetoothProximityManager
import fi.tuni.lonelinessapp.domain.service.SequentialPermissionManager
import fi.tuni.lonelinessapp.domain.service.StepSensorManager
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase
import fi.tuni.lonelinessapp.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // dayRepository is initialized later for the stepService.
    private lateinit var dayRepository: DayRepository
    private lateinit var stepSensorManager: StepSensorManager
    private var isServiceBound = false
    private lateinit var bluetoothManager: BluetoothProximityManager
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var appUsageTracker: AppUsageTracker

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
        val callDurationHelper = CallDurationHelper(this)
        val dayDataSource = DayDataSource(database.dayDao(), callDurationHelper)
        dayRepository = DayRepository(dayDataSource)
        val calculateCorrelationUseCase = CalculateCorrelationUseCase(dayRepository)
        val surveyViewModel = SurveyViewModel(dayRepository)
        val analysisViewModel = AnalysisViewModel(dayRepository)
        val homeViewModel = HomeViewModel(calculateCorrelationUseCase)
        val settingsViewModel = SettingsViewModel(dayRepository)
        appUsageTracker = AppUsageTracker(this)
        checkAllPermissions(analysisViewModel, dayRepository)
        analysisViewModel.updateDurations()

        enableEdgeToEdge()
        setContent {
            LonelinessAppTheme {
                MainScreen(
                    surveyViewModel=surveyViewModel,
                    analysisViewModel=analysisViewModel,
                    settingsViewModel=settingsViewModel,
                    homeViewModel=homeViewModel
                )
            }
        }

    }

    private fun initializeStepTrackingService() {
        val intent = Intent(this, StepSensorManager::class.java)

        // Start the service
        ContextCompat.startForegroundService(this, intent)

        // Bind to set the repository
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun initializeBluetoothManager(dayRepository: DayRepository, analysisViewModel: AnalysisViewModel) {
        println("Initialize bluetooth manager")
        bluetoothManager = BluetoothProximityManager(this,
            onScanStatusChanged = { isScanning ->
                println(if (isScanning) "Scanning..." else "Scanning stopped")
            },
            onError = { errorMessage ->
                showError(errorMessage)
            },
            dayRepository = dayRepository,
            analysisViewModel = analysisViewModel,
        )
        bluetoothManager.startScanning()
    }

    private fun showError(errorMessage: String) {
        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showPermissionDeniedMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        // Unbind service but don't stop (continues in background)
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        bluetoothManager.cleanup()
    }

    private fun checkAllPermissions(
        analysisViewModel: AnalysisViewModel,
        dayRepository: DayRepository
    ) {
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
                coroutineScope.launch {
                    dayRepository.updateCallDurationToday()
                    analysisViewModel.updateCallDuration()
                }
            },
            onDenied = {
                showPermissionDeniedMessage("Call log permission denied")
            },
            rationaleMessage = "We need call log permission to track call durations."
        )

        permissionManager.addPermission(
            permission = Manifest.permission.PACKAGE_USAGE_STATS,
            onGranted = {

            },
            onDenied = {
                showPermissionDeniedMessage("Usage Access")
            },
            rationaleMessage = "We need usage access permission to track your app usage time for apps like Telegram and WhatsApp."
        )

        val bluetoothPermissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        permissionManager.addMultiplePermissions(
            permissions = bluetoothPermissions,
            onGranted = {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    initializeBluetoothManager(dayRepository, analysisViewModel)
                }
            },
            onDenied = {
                showPermissionDeniedMessage("Bluetooth proximity detection")
            },
            rationaleMessage = "We need Bluetooth and location permissions to detect nearby devices and measure social interactions."
        )

        permissionManager.start()

        checkUsageStatsPermission(dayRepository, analysisViewModel)
    }

    private fun checkUsageStatsPermission(dayRepository: DayRepository,analysisViewModel: AnalysisViewModel){
        if (appUsageTracker.isUsageStatsPermissionGranted()) {
            appUsageTracker.startTracking()
            println("App Usage tracker start tracking")
            val appUsageData = appUsageTracker.getCurrentUsage()
            coroutineScope.launch {
                dayRepository.saveWhatApp(whatApps = appUsageData.whatsappUsageTime.toInt())
                dayRepository.saveTelegram(telegram = appUsageData.telegramUsageTime.toInt())
            }

            analysisViewModel.updateWhatAppsDuration()
            analysisViewModel.updateTelegramDuration()
        } else {
            showUsageStatsPermissionDialog()
        }
    }

    private fun showUsageStatsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Permission Needed")
            .setMessage("To track time usage of Telegram and WhatsApp, you need to enable Usage Access in Settings.\n\nPlease enable 'Usage access' for this app in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                openUsageStatsSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                showPermissionDeniedMessage("Usage Access")
            }
            .setCancelable(false)
            .show()
    }

    private fun openUsageStatsSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(),
               surveyViewModel: SurveyViewModel,
               analysisViewModel: AnalysisViewModel,
               settingsViewModel: SettingsViewModel,
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
                SettingsScreen(settingsViewModel=settingsViewModel)
            }

            // Show survey dialog
            if (showSurvey) {
                SurveyDialog(onDismiss = { viewModel.closeSurvey() }, surveyViewModel=surveyViewModel)
            }
        }
    }
}
