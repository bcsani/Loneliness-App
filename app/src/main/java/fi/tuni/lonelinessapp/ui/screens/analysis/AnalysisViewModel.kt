package fi.tuni.lonelinessapp.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.round
import kotlinx.coroutines.flow.flatMapLatest

class AnalysisViewModel (
    private val dayRepository: DayRepository
) : ViewModel() {

    // Display (UI) state (expanded when data is connected).
    data class UiState(

        // Show placeholders.
        val isLoading: Boolean = false,

        // Error message if the search fails.
        val error: String? = null
    )

    // 'ui' is editable state that can be updated by the ViewModel.
    private val _ui = MutableStateFlow(UiState())

    // 'ui' is a read-only version that the UI can follow.
    val ui: StateFlow<UiState> = _ui

    // Data structures.
    // One day's data (date, survey result, phone usage, steps).
    data class DaySample(

        // Date.
        val date: LocalDate,

        // Query result (UCLA 0–9).
        val loneliness: Int,

        // Phone usage at night (in minutes).
        // POSSIBLE CHANGE? Depending on the format of the results.
        val nightMinutes: Int,

        // Phone usage per day (in minutes).
        // POSSIBLE CHANGE? Depending on the format in which the results come.
        val dayMinutes: Int,

        // Steps.
        val steps: Int
    )

    // Single point data to line chart.
    data class LinePoint(val xLabel: String, val y: Float)

    // Single point data for a bar chart.
    data class BarPoint (val xLabel: String, val y: Float)

    // Single slice data for pie chart.
    data class PieSlice (val label: String, val value: Float)


    val daysEntity: StateFlow<List<DayEntity>?> =
        dayRepository.getAllDays()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Let's do the conversions for charts.
    // Convert the day's data to fit a line chart (date + value).

    fun lonelinessLine(data: List<DaySample>): List<LinePoint> {
        return data.map { d ->
            // Haetaan raaka-arvo (esim. 3, 9 tai null)
            val rawLoneliness = d.loneliness

            // Muunnetaan arvo uuteen asteikkoon (0-7)
            val displayValue = when (rawLoneliness) {
                // `loneliness` on Int, joten null-tarkistusta ei tarvita,
                // mutta oletetaan että 0 on "ei vastausta"
                0 -> 0f
                in 3..9 -> (rawLoneliness - 2).toFloat() // Muunnetaan 3-9 -> 1-7
                else -> 0f   // Kaikki muut tapaukset, näytetään 0
            }

            // Luodaan kaavion piste
            LinePoint(d.date.dayOfWeek.name.take(3), displayValue)
        }
    }

    // Convert night minutes to hours for the bar chart.
    fun nightUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours(d.nightMinutes.toFloat())) }

    // Convert day minutes to hours for the bar chart.
    fun dayUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours(d.dayMinutes.toFloat())) }

    // Create the steps data as is (no change in units).
    fun stepsBars(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), d.steps.toFloat()) }

    // Create the communication application hours for the pie chart.
    fun communicationPieHours(): List<PieSlice> = listOf(
        PieSlice("WhatsApp", 2.3f),
        PieSlice("Messages", 1.7f),
        PieSlice("Calls",    0.9f),
        PieSlice("Signal",   0.6f),
        PieSlice("Telegram",  0.5f)
    )

    // Helper functions.

    // Convert minutes to hours.
    private fun minutesToHours(mins: Float): Float = mins / 60f

    // Rounds a number to one decimal place.
    private fun round1(v: Float) = (round(v * 10f) / 10f)

    // Rounds to the nearest integer.
    private fun round0(v: Float) = round(v)

}