package fi.tuni.lonelinessapp

import android.os.Bundle
import android.content.Context
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import fi.tuni.lonelinessapp.ui.navigation.BottomNavigation
import fi.tuni.lonelinessapp.ui.navigation.TopBar
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreen
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.settings.SettingsScreen
import fi.tuni.lonelinessapp.ui.theme.LonelinessAppTheme
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyViewModel
import fi.tuni.lonelinessapp.data.AppDatabase
import fi.tuni.lonelinessapp.data.datasource.AllQuestionDataSource
import fi.tuni.lonelinessapp.data.repository.AllQuestionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "all_question_db"
        ).build()

        val allQuestionDataSource = AllQuestionDataSource(database.allQuestionDao())
        val allQuestionRepository = AllQuestionRepository(allQuestionDataSource)
        val surveyViewModel = SurveyViewModel(allQuestionRepository)

        enableEdgeToEdge()
        setContent {
            LonelinessAppTheme {
                MainScreen(surveyViewModel=surveyViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), surveyViewModel: SurveyViewModel) {

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
                    0 -> HomeScreen(viewModel, surveyViewModel=surveyViewModel)
                    1 -> AnalysisScreen()
                }
            } else {
                SettingsScreen()
            }

            // Show survey dialog
            if (showSurvey) {
                SurveyDialog(onDismiss = { viewModel.closeSurvey()}, surveyViewModel=surveyViewModel)
            }
        }
    }
}
