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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class AnalysisViewModel (
    dayRepository: DayRepository
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
        val loneliness: Int?,

        // Phone usage at night (in minutes).
        val nightMinutes: Int,

        // Phone usage per day (in minutes).
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


    data class AggregateBucket(
        val label: String,
        val lonAvg: Float,
        val nightH: Float,
        val dayH: Float,
        val stepsAvg: Float
    )


    val daysEntity: StateFlow<List<DayEntity>?> =
        dayRepository.getAllDays()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Let's do the conversions for charts.
    // Convert the day's data to fit a line chart (date + value).
    // Convert the day's data to fit a line chart (date + value).
    fun lonelinessLine(data: List<DaySample>): List<LinePoint> =
        data.map { d ->
            val y = d.loneliness
                ?.takeIf { it in 3..9 }
                ?.let { (it - 3).toFloat() }   // skaalataan 1–7
                ?: Float.NaN


            LinePoint(d.date.dayOfWeek.name.take(3), y)
        }

    // Convert night minutes to hours for the bar chart.
    fun nightUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d ->
            BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours(d.nightMinutes.toFloat())) }

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




    fun aggregateIntoTwelvePeriods(samples: List<DaySample>): List<AggregateBucket> {
        val valid = samples.filter {
            (it.loneliness != null && it.loneliness in 3..9) ||
                    it.nightMinutes > 0 || it.dayMinutes > 0 || it.steps > 0
        }
        if (valid.isEmpty()) return emptyList()

        val firstDate = valid.minOf { it.date }
        val lastDate = LocalDate.now()
        val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate) + 1
        if (totalDays <= 0) return emptyList()

        val numPeriods = 12
        val periodLen = (totalDays.toFloat() / numPeriods).coerceAtLeast(1f)
        val fmt = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())

        val out = mutableListOf<AggregateBucket>()
        for (i in 0 until numPeriods) {
            val start = firstDate.plusDays((i * periodLen).toLong())
            val end   = firstDate.plusDays(((i + 1) * periodLen).toLong() - 1).coerceAtMost(lastDate)

            val inPeriod = valid.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            val label = start.format(fmt).let { a ->
                val b = end.format(fmt); if (a == b) a else "$a - $b"
            }

            if (inPeriod.isEmpty()) {
                out += AggregateBucket(label, Float.NaN, 0f, 0f, 0f)
            } else {
                val lonAvg = inPeriod.mapNotNull { it.loneliness }
                    .filter { it in 3..9 }
                    .map { (it - 3).coerceIn(0,6) }
                    .average().toFloat().let { if (it.isNaN()) Float.NaN else it }

                val nightH   = (inPeriod.map { it.nightMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
                val dayH     = (inPeriod.map { it.dayMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
                val stepsAvg =  inPeriod.map { it.steps }.average().toFloat().let { if (it.isNaN()) 0f else it }

                out += AggregateBucket(label, lonAvg, nightH, dayH, stepsAvg)
            }
        }
        return out
    }

    // Convert minutes to hours.
    private fun minutesToHours(mins: Float): Float = mins / 60f

}