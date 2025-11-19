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


    data class PeriodBucket(
        val label: String,      // e.g., "Jan 24 - Mar 24"
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

    fun lonelinessLine(data: List<DaySample>): List<LinePoint> {
        return data.map { d ->
            val rawLoneliness = d.loneliness

            // Päätellään mitä piirretään:
            val displayValue = when {
                // 0 = "ei vastausta" -> ei pisteitä, ei viivaa
                rawLoneliness <= 0 -> Float.NaN

                // Normaali UCLA 3–9 -> muunnetaan 1–7 -asteikolle
                rawLoneliness in 3..9 -> (rawLoneliness - 2).toFloat()

                // Kaikki muut roskat -> myös aukko
                else -> Float.NaN
            }

            // X-akselin label: ma, ti, ke...
            LinePoint(
                d.date.dayOfWeek.name.take(3),
                displayValue
            )
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


    fun aggregateIntoTwelvePeriods(samples: List<DaySample>): List<PeriodBucket> {
        val validSamples = samples.filter { it.loneliness != null || it.nightMinutes != null || it.dayMinutes != null || it.steps != null }
        if (validSamples.isEmpty()) return emptyList()

        val firstDate = validSamples.minOf { it.date }
        val lastDate = LocalDate.now()

        val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate) + 1
        if (totalDays <= 0) return emptyList()

        val numPeriods = 12
        val periodLengthDays = (totalDays.toFloat() / numPeriods).coerceAtLeast(1.0f)

        val buckets = mutableListOf<PeriodBucket>()
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())

        for (i in 0 until numPeriods) {
            val periodStartDate = firstDate.plusDays((i * periodLengthDays).toLong())
            val periodEndDate = firstDate.plusDays(((i + 1) * periodLengthDays).toLong() - 1).coerceAtMost(lastDate)

            val samplesInPeriod = validSamples.filter {
                !it.date.isBefore(periodStartDate) && !it.date.isAfter(periodEndDate)
            }

            val startLabel = periodStartDate.format(monthYearFormatter)
            val endLabel = periodEndDate.format(monthYearFormatter)
            val label = if (startLabel == endLabel) startLabel else "$startLabel - $endLabel"

            if (samplesInPeriod.isNotEmpty()) {
                val lonValues = samplesInPeriod.mapNotNull { it.loneliness }.filter { it > 0 }
                val lonAvg = if (lonValues.isNotEmpty()) {
                    lonValues.map { (it - 2).coerceIn(1, 7) }.average().toFloat()
                } else {
                    Float.NaN
                }

                val nightH = samplesInPeriod.mapNotNull { it.nightMinutes }.average().toFloat() / 60f
                val dayH = samplesInPeriod.mapNotNull { it.dayMinutes }.average().toFloat() / 60f
                val stepsAvg = samplesInPeriod.mapNotNull { it.steps }.average().toFloat()

                buckets.add(PeriodBucket(label, lonAvg, nightH, dayH, stepsAvg))
            } else {
                 buckets.add(PeriodBucket(label, Float.NaN, 0f, 0f, 0f))
            }
        }
        return buckets
    }
    // Convert minutes to hours.

    private fun minutesToHours(mins: Float): Float = mins / 60f
    // Rounds a number to one decimal place.
    private fun round1(v: Float) = (round(v * 10f) / 10f)

    // Rounds to the nearest integer.
    private fun round0(v: Float) = round(v)

}
