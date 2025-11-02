package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarEntry

class HomeViewModel : ViewModel() {

    // Streak count
    private val _streakCount = mutableIntStateOf(3) // Hardcoded value for testing
    val streakCount: State<Int> = _streakCount

    // ---------------------
    // Add right values here
    // ---------------------
    val correlationValues = listOf(0.2, -0.5, 0.6, 0.1)

    val entries = correlationValues.mapIndexed { index, value ->
        BarEntry(index.toFloat(), value.toFloat())
    }

    val labels = listOf(
        "Phone",
        "Exercise",
        "Messages",
        "Calls"
    )
}
