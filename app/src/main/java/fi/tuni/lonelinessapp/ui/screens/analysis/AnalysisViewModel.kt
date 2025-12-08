package fi.tuni.lonelinessapp.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class AnalysisViewModel (
    val dayRepository: DayRepository
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
        val nightMinutes: Int?,

        // Phone usage per day (in minutes).
        val dayMinutes: Int?,

        // Steps.
        val steps: Int?
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
    private val initialValue = 0.0f

    val daysEntity: StateFlow<List<DayEntity>?> =
        dayRepository.getAllDays()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    private val _callDuration = MutableStateFlow(initialValue)
    private val _signalDuration = MutableStateFlow(initialValue)
    private val _signalAmount = MutableStateFlow(0)
    private val _whatAppsDuration = MutableStateFlow(initialValue)
    private val _telegramDuration = MutableStateFlow(initialValue)

    private fun getCallDuration(): Float = _callDuration.value
    private fun getSignalDuration(): Float = _signalDuration.value
    private fun getWhatAppsDuration(): Float = _whatAppsDuration.value
    private fun getTelegramDuration(): Float = _telegramDuration.value


    // This function set the call duration for today when the user accept call log tracking
    fun updateCallDuration() {
        try {
            viewModelScope.launch {
                dayRepository.getCallsToday().collect { duration ->
                    val callDurationSec = duration?.toFloat() ?: 0f
                    _callDuration.value = secondsToHours(callDurationSec)
                }
            }
        } catch (e: NullPointerException) {
            _callDuration.value = initialValue
        }
    }

    // This function set the signal duration for today
    fun updateSignalDuration() {
        try {
            viewModelScope.launch {
                dayRepository.getSignalToday().collect { duration ->
                    val signalDurationSec = duration?.toFloat() ?: 0f
                    _signalDuration.value = secondsToHours(signalDurationSec)
                }
            }
        } catch (e: NullPointerException) {
            _signalDuration.value = initialValue
        }
    }

    fun updateSignalAmount() {
        try {
            viewModelScope.launch {
                dayRepository.getSignalAmountToday().collect { signalAmount ->
                    _signalAmount.value = (signalAmount ?: 0f) as Int


            }
        } catch (e: NullPointerException) {
        _signalDuration.value = initialValue
        }
    }

    fun updateWhatAppsDuration() {
        try {
            viewModelScope.launch {
                dayRepository.getWhatAppsToday().collect { duration ->
                    val whatAppsDurationMin = duration?.toFloat() ?: 0f
                    _whatAppsDuration.value = minutesToHours(whatAppsDurationMin)
                }

            }
        } catch (e: NullPointerException) {
            _whatAppsDuration.value = initialValue
        }
    }

    // This function set the telegram's usage duration for today when user accept app tracking
    fun updateTelegramDuration() {
        try {
            viewModelScope.launch {
                dayRepository.getTelegramToday().collect { duration ->
                    val telegramDurationMin = duration?.toFloat() ?: 0f
                    _telegramDuration.value = minutesToHours(telegramDurationMin)
                }
            }
        } catch (e: NullPointerException) {
            _telegramDuration.value = initialValue
        }
    }

    fun updateDurations() {
        updateCallDuration()
        updateSignalDuration()
        updateWhatAppsDuration()
        updateTelegramDuration()
    }

    // Let's do the conversions for charts.
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
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours((d.nightMinutes ?: 0.0).toFloat())) }

    // Convert day minutes to hours for the bar chart.
    fun dayUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours((d.dayMinutes ?: 0.0).toFloat())) }

    // Create the steps data as is (no change in units).
    fun stepsBars(data: List<DaySample>): List<BarPoint> =
        data.mapNotNull { d ->
            d.steps?.let { steps ->
                BarPoint(d.date.dayOfWeek.name.take(3), steps.toFloat())
            }
        }

    // Create the communication application hours for the pie chart.
    fun communicationPieHours(): List<PieSlice> {
        val callDuration = getCallDuration()
        val signalDuration = getSignalDuration()
        val whatAppsDuration = getWhatAppsDuration()
        val telegramDuration = getTelegramDuration()


        return listOf(
            PieSlice("WhatsApp", whatAppsDuration),
            PieSlice("Messages", 0.0f),
            PieSlice("Calls",    callDuration),
            PieSlice("Signal",   signalDuration),
            PieSlice("Telegram",  telegramDuration)
        )
    }


    fun aggregateIntoTwelvePeriods(samples: List<DaySample>): List<AggregateBucket> {
        val valid = samples.filter {
            (it.loneliness != null && it.loneliness in 3..9) ||
                    (it.nightMinutes ?: 0) > 0 ||
                    (it.dayMinutes ?: 0) > 0 ||
                    (it.steps ?: 0) > 0
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

                val nightH   = (inPeriod.mapNotNull { it.nightMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
                val dayH     = (inPeriod.mapNotNull { it.dayMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
                val stepsAvg = inPeriod.mapNotNull { it.steps }.average().toFloat().let { if (it.isNaN()) 0f else it }

                out += AggregateBucket(label, lonAvg, nightH, dayH, stepsAvg)
            }
        }
        return out
    }

    // Convert minutes to hours.
    private fun minutesToHours(mins: Float): Float = mins / 60f
    private fun secondsToHours(seconds: Float): Float = seconds / 3600f

}