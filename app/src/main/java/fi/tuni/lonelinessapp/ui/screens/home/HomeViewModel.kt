package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    // Streak count
    private val _streakCount = mutableIntStateOf(3) // Hardcoded value for testing
    val streakCount: State<Int> = _streakCount

}
