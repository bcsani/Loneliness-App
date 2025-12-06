package fi.tuni.lonelinessapp

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import fi.tuni.lonelinessapp.ui.navigation.TAB_HOME

class MainViewModel : ViewModel() {

    // Selected bottom tab
    private val _selectedTab = mutableIntStateOf(TAB_HOME)
    val selectedTab: State<Int> = _selectedTab

    // Survey dialog visibility
    private val _showSurvey = mutableStateOf(false)
    val showSurvey: State<Boolean> = _showSurvey

    // Settings screen visibility
    private val _showSettings = mutableStateOf(false)
    val showSettings: State<Boolean> = _showSettings

    fun selectTab(tab: Int) {
        _selectedTab.intValue = tab
        _showSettings.value = false
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }

    fun openSurvey() {
        _showSurvey.value = true
    }

    fun closeSurvey() {
        _showSurvey.value = false
    }
}