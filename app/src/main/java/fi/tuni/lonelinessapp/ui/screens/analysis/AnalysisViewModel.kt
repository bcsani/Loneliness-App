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

class AnalysisViewModel(
    private val dayRepository: DayRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    data class DaySample(
        val date: LocalDate,
        val loneliness: Int?,
        val nightMinutes: Int?,
        val dayMinutes: Int?,
        val steps: Int?
    )

    data class LinePoint(val xLabel: String, val y: Float)
    data class BarPoint(val xLabel: String, val y: Float)
    data class PieSlice(val label: String, val value: Float)

    // New data class for "All Time" aggregation
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

    fun lonelinessLine(data: List<DaySample>): List<LinePoint> {
        return data.map { d ->
            val yValue = when (val loneliness = d.loneliness) {
                null, 0 -> Float.NaN // Create a gap for missing data
                else -> (loneliness - 2).coerceIn(1, 7).toFloat()
            }
            LinePoint(d.date.dayOfWeek.name.take(3), yValue)
        }
    }

    fun nightUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d ->
            val y = d.nightMinutes?.let { minutesToHours(it.toFloat()) } ?: 0f
            BarPoint(d.date.dayOfWeek.name.take(3), y)
        }

    fun dayUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d ->
            val y = d.dayMinutes?.let { minutesToHours(it.toFloat()) } ?: 0f
            BarPoint(d.date.dayOfWeek.name.take(3), y)
        }

    fun stepsBars(data: List<DaySample>): List<BarPoint> =
        data.map { d ->
            val y = d.steps?.toFloat() ?: 0f
            BarPoint(d.date.dayOfWeek.name.take(3), y)
        }

    fun communicationPieHours(): List<PieSlice> = listOf(
        PieSlice("WhatsApp", 2.3f),
        PieSlice("Messages", 1.7f),
        PieSlice("Calls", 0.9f),
        PieSlice("Signal", 0.6f),
        PieSlice("Telegram", 0.5f)
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

    private fun minutesToHours(mins: Float): Float = mins / 60f
}
