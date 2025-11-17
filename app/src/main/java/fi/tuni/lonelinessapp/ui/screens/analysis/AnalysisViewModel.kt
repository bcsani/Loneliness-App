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

    // Start date 6 days ago.
    val start = java.time.LocalDate.now().minusDays(6)

    // Example data.
    val daysEntity : StateFlow<List<DayEntity>?> = dayRepository.getDaysFromDate(start)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Call log
    val callDuration: Long = dayRepository.getTotalCallDurationToday()

    // Let's do the conversions for charts.
    // Convert the day's data to fit a line chart (date + value).
    fun lonelinessLine(data: List<DaySample>): List<LinePoint> =
        data.map { d -> LinePoint(d.date.dayOfWeek.name.take(3), d.loneliness.toFloat()) }

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
    fun communicationPieHours(
        whatApps: Int,
        messages: Int,
        calls: Int,
        signal: Int,
        telegram: Int
    ): List<PieSlice>? = listOf(
        PieSlice("WhatsApp", minutesToHours(whatApps.toFloat())),
        PieSlice("Messages", minutesToHours(messages.toFloat())),
        PieSlice("Calls",    minutesToHours(callDuration.toFloat())),
        PieSlice("Signal",   minutesToHours(signal.toFloat())),
        PieSlice("Telegram",  minutesToHours(telegram.toFloat()))
    )

    // Helper functions.

    // Convert minutes to hours.
    private fun minutesToHours(mins: Float): Float = mins / 60f

    // Rounds a number to one decimal place.
    private fun round1(v: Float) = (round(v * 10f) / 10f)

    // Rounds to the nearest integer.
    private fun round0(v: Float) = round(v)


}