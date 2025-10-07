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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fi.tuni.lonelinessapp.data.datasource.AllQuestionDataSource
import fi.tuni.lonelinessapp.data.repository.AllQuestionRepository
import fi.tuni.lonelinessapp.ui.navigation.BottomNavigation
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreen
import fi.tuni.lonelinessapp.ui.screens.home.HomeScreenViewModel
import fi.tuni.lonelinessapp.ui.screens.profile.ProfileScreen
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
    // Create Data Source, Repository and View Model
    val allQuestionDataSource = AllQuestionDataSource()
    val allQuestionRepository = AllQuestionRepository(allQuestionDataSource)
    val homeScreenViewModel = HomeScreenViewModel(allQuestionRepository)

    // selectedTab is responsible for keeping
    // track of which tab (bottom navigation) is selected.
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigation(
                currentTab = selectedTab,
                selectNewTab = {selectedTab = it}
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Depending on the selected tab (selectedTab),
            // display different composable functions.
            when (selectedTab) {
                0 -> HomeScreen(homeScreenViewModel=homeScreenViewModel)
                1 -> AnalysisScreen()
                2 -> ProfileScreen()
            }
        }
    }
}
