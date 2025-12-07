/**
 * AnalysisScreen.kt
 *
 * This file defines the "Analysis" view of the Loneliness App.
 * It visualizes:
 *  - Daily loneliness scores (UCLA 3-item scale, mapped to 0–6).
 *  - Night-time and day-time phone usage (in hours).
 *  - Daily step counts.
 *  - Communication app usage (pie chart).
 *
 * The screen supports multiple time ranges:
 *  - Last 7 days.
 *  - Last 30 days.
 *  - Last 3 months.
 *  - Past year.
 *  - All time.
 *
 * Charts are rendered using MPAndroidChart inside Jetpack Compose via AndroidView.
 * All statistics are calculated from DayEntity data in AnalysisViewModel.
 */

package fi.tuni.lonelinessapp.ui.screens.analysis

// Compose
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

// MPAndroidChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import fi.tuni.lonelinessapp.data.entity.DayEntity
import com.github.mikephil.charting.components.AxisBase

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.github.mikephil.charting.charts.BarLineChartBase
import kotlin.math.roundToInt
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel.DaySample
import kotlin.math.ceil
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.time.format.DateTimeFormatter


// Color configuration for charts.
// The colors are now hardcoded. Later we will move under the theme.
private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
private const val COLOR_TEXT_HEX    = 0xFF1F2937.toInt()

// Pie chart colors (for communication app distribution).
private val PIE_COLORS = listOf(
    0xFF2563EB.toInt(),
    0xFFF59E0B.toInt(),
    0xFF10B981.toInt(),
    0xFFA855F7.toInt(),
    0xFFEF4444.toInt()
)

// User selectable time ranges in the analysis view.
enum class TimeRange { Week, Month, ThreeMonths, Year, All }

/**
 * Configures an MPAndroidChart XAxis with different strategies depending on:
 *  - useStartEndOnly: only first and last label (e.g. All Time).
 *  - adjustForBars: add half-bar padding so first/last bars are not cut.
 *  - monthTickDays: custom ticks (e.g. 1,5,10,15,... for Month view).
 *  - everyNthLabel: show only every Nth label to avoid clutter.
 */
private fun XAxis.applyDomainAndLabels(
    labels: List<String>,
    useStartEndOnly: Boolean = false,
    forceAllLabels: Boolean = false,
    adjustForBars: Boolean = false,
    monthTickDays: List<Int>? = null,
    everyNthLabel: Int = 1,
) {
    fun resetDomainForLabels() {
        if (labels.isNotEmpty()) {
            axisMinimum = 0f
            axisMaximum = (labels.size - 1).toFloat()
        } else {
            axisMinimum = 0f
            axisMaximum = 1f
        }
    }

    // 1) All Time: just the beginning + "Now".
    if (useStartEndOnly) {
        valueFormatter = StartEndValueFormatter(labels)
        setLabelCount(2, true)

        resetDomainForLabels()

        granularity = 1f
        isGranularityEnabled = true
        setDrawGridLines(true)
        enableGridDashedLine(10f, 10f, 0f)
        return
    }

    // 2) Columns that require extra space on the edges (bar charts).
    if (adjustForBars && labels.isNotEmpty()) {
        axisMinimum = -0.5f
        axisMaximum = (labels.size - 1).toFloat() + 0.5f

        // Map axis values to label indices, taking the padding into account.
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val min = axis?.axisMinimum ?: axisMinimum
                val max = axis?.axisMaximum ?: axisMaximum
                val range = max - min
                if (range <= 0f) return ""

                val normalized = (value - min) / range
                val index = (normalized * (labels.size - 1))
                    .roundToInt()
                    .coerceIn(0, labels.lastIndex)

                if (everyNthLabel > 1 && index % everyNthLabel != 0) return ""
                return labels[index]
            }
        }

        val visibleCount = if (everyNthLabel > 1) {
            ((labels.size - 1) / everyNthLabel) + 1
        } else labels.size

        setLabelCount(visibleCount, false)

        granularity = 1f
        isGranularityEnabled = true
        setDrawGridLines(true)
        enableGridDashedLine(10f, 10f, 0f)
        return
    }

    // 3) Month view days: 1,5,10,15,20,25,30.
    val ticks = monthTickDays?.filter { it in 1..labels.size }?.sorted()
    if (!ticks.isNullOrEmpty()) {
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val idx = value.toInt()
                if (idx !in 0 until labels.size) return ""
                val day = idx + 1
                return if (day in ticks) day.toString() else ""
            }
        }

        setLabelCount(ticks.size, true)

        axisMinimum = 0f
        axisMaximum = (labels.size - 1).toFloat()

        granularity = 1f
        isGranularityEnabled = true
        setDrawGridLines(true)
        enableGridDashedLine(10f, 10f, 0f)
        return
    }

    // 4) Default: simple index-based labels, optionally skipping every Nth label.
    if (everyNthLabel > 1) {
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val i = value.toInt()
                if (i !in 0 until labels.size) return ""
                return if (i % everyNthLabel == 0) labels[i] else ""
            }
        }
    } else {
        valueFormatter = IndexAxisValueFormatter(labels)
    }

    val visibleCount = if (everyNthLabel > 1) {
        ((labels.size - 1) / everyNthLabel) + 1
    } else {
        labels.size
    }

    val count = if (forceAllLabels) visibleCount else visibleCount.coerceAtMost(12)
    setLabelCount(count, true)

    resetDomainForLabels()

    granularity = 1f
    isGranularityEnabled = true
    setDrawGridLines(true)
    enableGridDashedLine(10f, 10f, 0f)
}

/**
 * ApplyNiceYAxis: Applies a “nice” Y-axis range to a BarChart based on given values and a step function.
 * - If all values are 0 or missing, shows a small default range.
 * - Otherwise, expands slightly above the max and chooses a clean step (e.g. 0.5h, 1000 steps).
 *
 * @param values  Data values to analyze (e.g. hours or steps).
 * @param stepFn  Strategy for choosing the axis step size based on maximum value.
 */
private fun BarChart.applyNiceYAxis(values: List<Float>, stepFn: (Float) -> Float) {
    val maxVal = values.maxOrNull() ?: 0f
    if (maxVal <= 0f) {
        val step = stepFn(1f)
        axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = step * 4f
            granularity = step
            setLabelCount(5, true)
            setDrawGridLines(true)
            enableGridDashedLine(10f, 10f, 0f)
        }
        return
    }
    val step   = stepFn(maxVal)
    val axisMax = niceCeil(maxVal * 1.15f, step)
    val labelCount = (axisMax / step).toInt() + 1

    axisLeft.apply {
        axisMinimum = 0f
        axisMaximum = axisMax
        granularity = step
        setLabelCount(labelCount, true)
        setDrawGridLines(true)
        enableGridDashedLine(10f, 10f, 0f)
    }
}

/**
 * fallbackDayLabels: Returns fallback day labels when we have no real samples.
 * Used to keep the chart structure visible even when there's no data.
 */
private fun fallbackDayLabels(range: TimeRange, samplesCount: Int): List<String> = when (range) {
    TimeRange.Week  -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    TimeRange.Month -> (1..samplesCount.coerceAtLeast(30)).map { it.toString() }
    else -> emptyList()
}


/**
 * Main composable for the Analysis screen.
 *
 * - Lets the user pick a time range (Last 7 days / 30 days / 3 months / year / all time).
 * - Builds daily or monthly aggregates from DayEntity data via AnalysisViewModel.
 * - Renders 5 charts:
 *   1. Loneliness score (line chart).
 *   2. Night-time phone usage (bar chart).
 *   3. Day-time phone usage (bar chart).
 *   4. Steps (bar chart).
 *   5. Communication apps usage (pie chart).
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    var selectedRange by remember { mutableStateOf(TimeRange.Week) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }

    // ====================== TEST DATA (WEEK VIEW) ======================
    // This block provides hardcoded data for the "Week" view to test how null/zero values are rendered.
    //o restore live data, comment out this entire block and uncomment the "ORIGINAL DATA LOADING" block below.
    //val daysEntity by analysisViewModel.daysEntity.collectAsState()
    //val samples: List<DaySample> = remember(daysEntity, selectedRange) {
    //if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month || selectedRange == TimeRange.ThreeMonths
    //) {
    //val today = LocalDate.now()
    //val testData = listOf(
    //DaySample(today.minusDays(6), loneliness = 4, nightMinutes = 60, dayMinutes = 120, steps = 5000),
    //DaySample(today.minusDays(5), loneliness = 7, nightMinutes = 75, dayMinutes = 150, steps = 6200),
    //DaySample(today.minusDays(4), loneliness = 5, nightMinutes = 0,  dayMinutes = 100, steps = 4500), // <-- Null loneliness, zero night usage
    //DaySample(today.minusDays(3), loneliness = null, nightMinutes = 90, dayMinutes = 200, steps = 8000),
    //DaySample(today.minusDays(2), loneliness = 4, nightMinutes = 80, dayMinutes = 0,   steps = 0),     // <-- Zero day usage and steps
    //DaySample(today.minusDays(1), loneliness = 5, nightMinutes = 120, dayMinutes = 240, steps = 9500),
    //DaySample(today,              loneliness = 4, nightMinutes = 55, dayMinutes = 110, steps = 5200)
    //)
    //val testDataMap = testData.associateBy { it.date }
    //val wantedDates = (0..6).map { i -> today.minusDays((6 - i).toLong()) }
    //wantedDates.map { date ->
    //testDataMap[date] ?: DaySample(
    //date = date,
    //loneliness = null,
    //nightMinutes = 0,
    //dayMinutes = 0,
    //steps = 0
    //)
    //}
    //}
    //else {
    //buildSamplesForRange(daysEntity, selectedRange)
    //}
    //}
    // ======================= END OF TEST DATA =======================

    // ================= ORIGINAL DATA LOADING (Commented out) ================
    val daysEntity by analysisViewModel.daysEntity.collectAsState()

    // Build samples based on the selected time range.
    val samples: List<DaySample> = remember(daysEntity, selectedRange) {
        buildSamplesForRange(daysEntity, selectedRange)
    }
    // ========================================================================

    // Domain-specific points for each chart.
    val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
    val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }
    val commPie       = remember { analysisViewModel.communicationPieHours() }

    // Day labels for the selected period (shown even if data is empty).
    val dayLabels = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.Week  -> (samples.takeIf { it.isNotEmpty() } ?: emptyList()).map {
                it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }.ifEmpty { fallbackDayLabels(TimeRange.Week, 7) }

            TimeRange.Month -> (if (samples.isNotEmpty()) List(samples.size) { (it + 1).toString() } else fallbackDayLabels(TimeRange.Month, 30))

            else -> emptyList()
        }
    }

    // Labels used in Toast messages: always the real date (e.g. 4.12.2025).
    val toastLabels = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.Week, TimeRange.Month -> {
                val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
                samples.map { sample ->
                    sample.date.format(formatter)
                }
            }
            else -> emptyList()
        }
    }

    // Ticks for X-axis in Month view (days: 1,5,10,15,20,25,30).
    val monthTicks = if (selectedRange == TimeRange.Month)
        listOf(1, 5, 10, 15, 20, 25, 30)
    else
        null

    // Monthly or period aggregates depending on time range.
    val monthlyAgg = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.ThreeMonths, TimeRange.Year -> aggregateMonthly(samples)
            TimeRange.All -> analysisViewModel.aggregateIntoTwelvePeriods(samples)
            else -> emptyList()
        }
    }

    // X-axis labels for monthly/period charts.
    val monthLabels = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.label }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.label }
            else -> emptyList()
        }
    }

    // Monthly series for loneliness, night usage, day usage, and steps.
    val lonMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.lonAvg }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.lonAvg }
            else -> emptyList()
        }
    }
    val nightMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.nightH }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.nightH }
            else -> emptyList()
        }
    }
    val dayMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.dayH }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.dayH }
            else -> emptyList()
        }
    }
    val stepsMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.stepsAvg }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.stepsAvg }
            else -> emptyList()
        }
    }

    // Scrollable layout: one card per chart.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Time range dropdown.
        item {
            var expanded by remember { mutableStateOf(false) }
            val options = listOf("Last 7 days", "Last 30 Days", "Last 3 Months", "Past Year", "All Time")
            var selectedOptionText by remember { mutableStateOf(options[0]) }

            Box(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor().width(150.dp),
                        readOnly = true,
                        value = selectedOptionText,
                        onValueChange = { },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        options.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedOptionText = selectionOption
                                    selectedRange = when (selectionOption) {
                                        "Last 7 days" -> TimeRange.Week
                                        "Last 30 Days" -> TimeRange.Month
                                        "Last 3 Months" -> TimeRange.ThreeMonths
                                        "Past Year" -> TimeRange.Year
                                        "All Time"       -> TimeRange.All
                                        else             -> TimeRange.Week
                                    }
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }

        // Daily view (Week / Month): show 5 charts using per-day data.
        if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) {

            // 1) Loneliness (line chart).
            item {
                val infoText = "Shows your Loneliness scale scores from answers given to survey.\n" +
                        "Higher values indicate greater feelings of loneliness. Zero equals not feeling lonely. \n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Loneliness Score",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {

                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->

                            // Configure the LineChart used for daily loneliness scores.
                            LineChart(ctx).apply {
                                description = Description().apply { text = "" }
                                axisRight.isEnabled = false
                                legend.isEnabled = false
                                axisLeft.textSize = 14f
                                xAxis.textSize = 14f
                                setTouchEnabled(true)
                                setPinchZoom(false)
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                                xAxis.setDrawGridLines(true)
                                xAxis.enableGridDashedLine(10f, 10f, 0f)
                                axisLeft.axisMinimum = 0f
                                axisLeft.axisMaximum = 6.05f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(7, true)
                                axisLeft.setDrawGridLines(true)
                                lockZoomPanKeepTap()
                            }
                        },
                        update = { chart ->

                            chart.xAxis.applyDomainAndLabels(
                                labels = dayLabels,
                                monthTickDays = monthTicks
                            )
                            chart.lockZoomPanKeepTap()
                            chart.enableTapToShowValue(toastLabels) { y -> String.format("%.1f", y) }

                            val dataSets = buildLonelinessDataSets(lonelinessPts)
                            chart.data = LineData(dataSets)

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 2) Night usage (bar chart).
            item {
                val infoText = "Shows the time spent on your phone at night in hours. \n" +
                        "Night usage is 10.00 pm - 06.00 am. \n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Night time phone usage",
                    onInfoClick = { infoDialogMessage = infoText }

                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(false)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->

                            chart.xAxis.applyDomainAndLabels(
                                labels = dayLabels,
                                forceAllLabels = (selectedRange == TimeRange.Week),
                                adjustForBars = (selectedRange == TimeRange.Week),
                                monthTickDays = monthTicks
                            )
                            chart.applyNiceYAxis(nightPts.map { it.y }, ::hourStepFor)
                            chart.enableTapToShowValue(toastLabels) { y -> String.format("%.1f h", y) }

                            val entries = toBarEntries(nightPts)

                            val set = makeBarDataSet(
                                label = "Night usage",
                                entries = entries,
                            )

                            val barData = BarData(set).apply { barWidth = 0.7f }
                            chart.data = barData

                            val minX = entries.minOfOrNull { it.x } ?: 0f
                            val maxX = entries.maxOfOrNull { it.x } ?: 0f
                            val halfWidth = barData.barWidth / 2f

                            chart.xAxis.axisMinimum = minX - halfWidth
                            chart.xAxis.axisMaximum = maxX + halfWidth

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 3) Daytime usage (bar chart).
            item {
                val infoText = "Shows the time spent on your phone during the day in hours.\n" +
                        "Daily usage is 06.00 am - 10.00 pm.\n" +
                        "Shows values from selected period of time. "

                ChartCard(
                    title = "Day time phone usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(false)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.applyDomainAndLabels(
                                labels = dayLabels,
                                forceAllLabels = (selectedRange == TimeRange.Week),
                                adjustForBars = (selectedRange == TimeRange.Week),
                                monthTickDays = monthTicks
                            )
                            chart.applyNiceYAxis(dayPts.map { it.y }, ::hourStepFor)
                            chart.enableTapToShowValue(toastLabels) { y -> String.format("%.1f h", y) }

                            val entries = toBarEntries(dayPts)

                            val set = makeBarDataSet(
                                label = "Daytime usage",
                                entries = entries,
                            )

                            val barData = BarData(set).apply { barWidth = 0.7f }
                            chart.data = barData

                            val minX = entries.minOfOrNull { it.x } ?: 0f
                            val maxX = entries.maxOfOrNull { it.x } ?: 0f
                            val halfWidth = barData.barWidth / 2f

                            chart.xAxis.axisMinimum = minX - halfWidth
                            chart.xAxis.axisMaximum = maxX + halfWidth

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }


                    )
                }
            }

            // 4) Steps (bar chart).
            item {
                val infoText = "Shows the number of steps taken during the selected period of time.\n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Steps",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->

                            // BarChart for steps per day.
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(false)
                                setPinchZoom(false)
                                axisLeft.axisMinimum = 0f
                                axisLeft.granularity = 1000f
                                axisLeft.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String =
                                        if (value == 0f) "0" else "%,d".format(value.roundToInt())
                                }
                            }
                        },
                        update = { chart ->
                            chart.xAxis.applyDomainAndLabels(
                                labels = dayLabels,
                                forceAllLabels = (selectedRange == TimeRange.Week),
                                adjustForBars = (selectedRange == TimeRange.Week),
                                monthTickDays = monthTicks
                            )
                            chart.applyNiceYAxis(stepsPts.map { it.y }, ::stepStepFor)
                            chart.enableTapToShowValue(toastLabels) { y -> "%,d".format(y.toInt()) }

                            // Same edge logic as in Day time chart.
                            val entries = toBarEntries(stepsPts)

                            val set = makeBarDataSet(
                                label = "Steps",
                                entries = entries,
                            )

                            val barData = BarData(set).apply { barWidth = 0.7f }
                            chart.data = barData

                            val minX = entries.minOfOrNull { it.x } ?: 0f
                            val maxX = entries.maxOfOrNull { it.x } ?: 0f
                            val halfWidth = barData.barWidth / 2f

                            chart.xAxis.axisMinimum = minX - halfWidth
                            chart.xAxis.axisMaximum = maxX + halfWidth

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 5) Communications (pie chart).
            item {
                val infoText = "Shows how your communication app usage is distributed. \n" +
                        "The chart displays the total hours spent on each app during the selected time period."
                ChartCard(
                    title = "Communication Apps Usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        factory = { ctx ->

                            // Pie chart for communication app usage distribution.
                            PieChart(ctx).apply {
                                description = Description().apply { text = "" }
                                legend.isEnabled = false
                                setUsePercentValues(false)
                                setDrawEntryLabels(false)
                                isRotationEnabled = false
                                rotationAngle = 0f
                                animateY(0)
                                holeRadius = 45f
                                enableToastOnSliceClick()
                            }
                        },
                        update = { pie ->
                            val entries = commPie.filter { it.value > 0f }
                                .map { PieEntry(it.value, it.label) }
                            if (entries.isEmpty()) {
                                pie.centerText = "No chart data available"
                                pie.setCenterTextSize(16f)
                                pie.setCenterTextColor(android.graphics.Color.BLACK)
                                pie.legend.isEnabled = false
                                pie.data = PieData(PieDataSet(emptyList(), ""))
                            } else {
                                pie.legend.isEnabled = true
                                pie.legend.apply {
                                    textSize = 16f
                                    isWordWrapEnabled = true
                                    maxSizePercent = 0.80f
                                    verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                    horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                                    orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                    setDrawInside(false)
                                }
                                val set = PieDataSet(entries, "").apply {
                                    colors = PIE_COLORS
                                    sliceSpace = 2f
                                    valueTextSize = 14f
                                    valueTextColor = COLOR_TEXT_HEX
                                    valueFormatter = object : ValueFormatter() {
                                        @SuppressLint("DefaultLocale")
                                        override fun getFormattedValue(value: Float) =
                                            String.format("%.1f h", value)
                                    }
                                }
                                pie.data = PieData(set)
                            }
                            pie.data.notifyDataChanged()
                            pie.notifyDataSetChanged()
                            pie.invalidate()
                        }
                    )
                }
            }

        // 3 months, 1 year, All time: aggregated view.
        } else {

            // 1) Loneliness (line chart) for monthly/period aggregates.
            item {
                val infoText = "Shows your Loneliness scale scores from answers given to survey.\n" +
                        "Higher values indicate greater feelings of loneliness. Zero equals not feeling lonely. \n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Loneliness Score",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            LineChart(ctx).apply {

                                description = Description().apply { text = "" }
                                axisRight.isEnabled = false
                                legend.isEnabled = false

                                setExtraOffsets(20f, 0f, 16f, 0f)

                                axisLeft.textSize = 14f
                                xAxis.textSize = 14f
                                setTouchEnabled(true)
                                setPinchZoom(false)

                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                                xAxis.setDrawGridLines(true)
                                xAxis.enableGridDashedLine(10f, 10f, 0f)

                                axisLeft.axisMinimum = 0f
                                axisLeft.axisMaximum = 6.05f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(7, true)
                                axisLeft.setDrawGridLines(true)
                                axisLeft.enableGridDashedLine(10f, 10f, 0f)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.applyDomainAndLabels(
                                labels = monthLabels,
                                useStartEndOnly = (selectedRange == TimeRange.All),
                                everyNthLabel = if (selectedRange == TimeRange.Year) 2 else 1
                            )


                            val segmentedSets = buildLonelinessDataSets(
                                lonMonthly.mapIndexed { i, v ->
                                    AnalysisViewModel.LinePoint(
                                        xLabel = monthLabels.getOrElse(i) { "" },
                                        y = v
                                    )
                                }
                            )

                            chart.data = if (segmentedSets.isEmpty()) {
                                LineData()
                            } else {
                                LineData(segmentedSets)
                            }

                            chart.lockZoomPanKeepTap()
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f", y) }

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }
                    )
                }
            }

            // 2) Night usage (bar chart) for monthly/period aggregates.
            item {
                val infoText = "Shows the time spent on your phone at night in hours. \n" +
                        "Night usage is 10.00 pm - 06.00 am. \n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Night Usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(true)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->

                            if (selectedRange == TimeRange.All) {

                                // ALL TIME: only "start" + "Now", but with little space on the edges for the columns.
                                chart.xAxis.apply {
                                    val count = monthLabels.size.coerceAtLeast(1)

                                    axisMinimum = -0.5f
                                    axisMaximum = (count - 1).toFloat() + 0.5f

                                    valueFormatter = StartEndValueFormatter(monthLabels)
                                    setLabelCount(2, true)

                                    granularity = 1f
                                    isGranularityEnabled = true
                                    setDrawGridLines(true)
                                    enableGridDashedLine(10f, 10f, 0f)
                                }
                            } else {
                                // YEAR / 3 MONTHS: show multiple labels, still with bar padding.
                                chart.xAxis.applyDomainAndLabels(
                                    labels = monthLabels,
                                    useStartEndOnly = false,
                                    adjustForBars = true,
                                    everyNthLabel = if (selectedRange == TimeRange.Year) 2 else 1
                                )
                            }

                            chart.applyNiceYAxis(nightMonthly, ::hourStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f h", y) }

                            val entries = toBarEntriesFromFloats(nightMonthly)
                            val set = makeBarDataSet(
                                label = "Night usage",
                                entries = entries,
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }




                    )
                }
            }

            // 3) Daytime usage (bar chart) for monthly/period aggregates.
            item {
                val infoText = "Shows the time spent on your phone during the day in hours.\n" +
                        "Daily usage is 06.00 am - 10.00 pm.\n" +
                        "Shows values from selected period of time. "
                ChartCard(
                    title = "Day Usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(true)
                                setPinchZoom(false)
                                setExtraOffsets(20f, 0f, 16f, 0f)

                            }
                        },
                        update = { chart ->

                            if (selectedRange == TimeRange.All) {

                                // All time: start + Now labels with bar padding.
                                chart.xAxis.apply {
                                    val count = monthLabels.size.coerceAtLeast(1)
                                    axisMinimum = -0.5f
                                    axisMaximum = (count - 1).toFloat() + 0.5f

                                    valueFormatter = StartEndValueFormatter(monthLabels)
                                    setLabelCount(2, true)

                                    granularity = 1f
                                    isGranularityEnabled = true
                                    setDrawGridLines(true)
                                    enableGridDashedLine(10f, 10f, 0f)
                                }
                            } else {

                                // 3 months / year.
                                chart.xAxis.applyDomainAndLabels(
                                    labels = monthLabels,
                                    useStartEndOnly = false,
                                    adjustForBars = true,
                                    everyNthLabel = if (selectedRange == TimeRange.Year) 2 else 1
                                )
                            }

                            chart.applyNiceYAxis(dayMonthly, ::hourStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f h", y) }

                            val entries = toBarEntriesFromFloats(dayMonthly)
                            val set = makeBarDataSet(
                                label = "Day usage",
                                entries = entries,
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 4) Steps (bar chart) for monthly/period aggregates.
            item {
                val infoText = "Shows the number of steps taken during the selected period of time.\n" +
                        "Shows values from selected period of time."
                ChartCard(
                    title = "Steps",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults()
                                setTouchEnabled(true)
                                setPinchZoom(false)
                                axisLeft.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float) = "%,d".format(value.toInt())
                                }
                            }
                        },
                        update = { chart ->

                            if (selectedRange == TimeRange.All) {
                                chart.xAxis.apply {
                                    val count = monthLabels.size.coerceAtLeast(1)
                                    axisMinimum = -0.5f
                                    axisMaximum = (count - 1).toFloat() + 0.5f

                                    valueFormatter = StartEndValueFormatter(monthLabels)
                                    setLabelCount(2, true)

                                    granularity = 1f
                                    isGranularityEnabled = true
                                    setDrawGridLines(true)
                                    enableGridDashedLine(10f, 10f, 0f)
                                }
                            } else {
                                chart.xAxis.applyDomainAndLabels(
                                    labels = monthLabels,
                                    useStartEndOnly = false,
                                    adjustForBars = true,
                                    everyNthLabel = if (selectedRange == TimeRange.Year) 2 else 1
                                )
                            }

                            chart.applyNiceYAxis(stepsMonthly, ::stepStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> "%,d".format(y.toInt()) }

                            val entries = toBarEntriesFromFloats(stepsMonthly)
                            val set = makeBarDataSet(
                                label = "Steps",
                                entries = entries,
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }



                    )
                }
            }

            // 5) Communications (pie chart) – same logic as daily view, but using the full range.
            item {
                val infoText = "Shows how your communication app usage is distributed. \n" +
                        "The chart displays the total hours spent on each app during the selected time period."
                ChartCard(
                    title = "Communication Apps Usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        factory = { ctx ->
                            PieChart(ctx).apply {
                                description = Description().apply { text = "" }
                                legend.isEnabled = false
                                setUsePercentValues(false)
                                setDrawEntryLabels(false)
                                isRotationEnabled = false
                                rotationAngle = 0f
                                animateY(0)
                                holeRadius = 45f
                                enableToastOnSliceClick()
                            }
                        },
                        update = { pie ->
                            val entries = commPie.filter { it.value > 0f }.map { PieEntry(it.value, it.label) }

                            if (entries.isEmpty()) {
                                pie.centerText = "No chart data available"
                                pie.setCenterTextSize(16f)
                                pie.setCenterTextColor(android.graphics.Color.BLACK)
                                pie.legend.isEnabled = false
                                pie.data = PieData(PieDataSet(emptyList(), ""))
                            } else {
                                pie.legend.isEnabled = true
                                pie.legend.apply {
                                    textSize = 16f
                                    isWordWrapEnabled = true
                                    maxSizePercent = 0.80f
                                    verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                    horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                                    orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                    setDrawInside(false)
                                }
                                val set = PieDataSet(entries, "").apply {
                                    colors = PIE_COLORS
                                    sliceSpace = 2f
                                    valueTextSize = 14f
                                    valueTextColor = COLOR_TEXT_HEX
                                    valueFormatter = object : ValueFormatter() {
                                        @SuppressLint("DefaultLocale")
                                        override fun getFormattedValue(value: Float) = String.format("%.1f h", value)
                                    }
                                }
                                pie.data = PieData(set)
                            }

                            pie.data.notifyDataChanged(); pie.notifyDataSetChanged(); pie.invalidate()
                        }
                    )
                }
            }

        }
    }

    // Info dialog for chart descriptions (triggered by the info icon in ChartCard).
    if (infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { infoDialogMessage = null },
            title = { Text("Information") },
            text = { Text(infoDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { infoDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * ChartCard:
 * Small helper to standardize chart container look and title + info-icon row.
 *
 * @param title        Card title text shown at the top.
 * @param onInfoClick  Optional callback when the info icon is pressed.
 * @param content      Chart content composable.
 */
@Composable
fun ChartCard(
    title: String, onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show info about $title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * applyBarDefaults:
 * Applies common baseline configuration for all BarCharts in this screen.
 * This keeps styling (axes, grid, legend, padding) consistent across charts.
 */
fun BarChart.applyBarDefaults() {
    description = Description().apply { text = "" }
    axisRight.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isHighlightPerTapEnabled = false
    isHighlightPerDragEnabled = false

    setExtraOffsets(20f, 0f, 20f, 8f)

    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textSize = 14f
    xAxis.granularity = 1f
    xAxis.setDrawGridLines(true)
    axisLeft.setDrawAxisLine(true)
    axisLeft.axisMinimum = 0f
    xAxis.enableGridDashedLine(10f, 10f, 0f)
    axisLeft.textSize = 12f
    axisLeft.enableGridDashedLine(10f, 10f, 0f)
}

/** toBarEntries:
 * Converts a list of BarPoint (domain type from ViewModel) into BarEntry for MPAndroidChart.
 * X = index position, Y = value.
*/
private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) }

/** toBarEntriesFromFloats:
 * Converts a list of Float values into BarEntry list for MPAndroidChart.
 * X = index position, Y = value.
 */
private fun toBarEntriesFromFloats(values: List<Float>): List<BarEntry> =
    values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

/** makeBarDataSet:
 * Creates a unified BarDataSet with consistent styling.
 *
 * @param label    Legend label (currently hidden in UI).
 * @param entries  Data entries for the bar chart.
 */
private fun makeBarDataSet(
    label: String,
    entries: List<BarEntry>,
): BarDataSet = BarDataSet(entries, label).apply {
    color = COLOR_PRIMARY_HEX
    valueTextSize = 12f
    valueTextColor = COLOR_TEXT_HEX
    setDrawValues(false)
}

/** enableToastOnSliceClick:
 * Adds a simple toast on pie slice selection.
 * Shows "<label> – X.X h" when a slice is tapped.
 */
private fun PieChart.enableToastOnSliceClick() {
    setOnChartValueSelectedListener(object :
        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(
            e: Entry?,
            h: com.github.mikephil.charting.highlight.Highlight?
        ) {
            if (e is PieEntry) {
                val label = e.label
                val hours = e.value
                android.widget.Toast.makeText(
                    context,
                    "$label – %.1f h".format(hours),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onNothingSelected() { }
    })
}

/** niceCeil:
 * Rounds up [value] to the nearest multiple of [step].
 * If step <= 0, the original value is returned.
 */
private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = ceil(value / step)
    return (k * step)
}

/** hourStepFor:
 * Policies for selecting “nice” Y-axis step sizes for hours.
 */
private fun hourStepFor(maxVal: Float): Float =
    when {
        maxVal <= 2f  -> 0.25f
        maxVal <= 4f  -> 0.5f
        maxVal <= 8f  -> 1f
        else          -> 2f
    }

/** stepStepFor:
 * Policies for selecting “nice” Y-axis step sizes for steps.
 */
private fun stepStepFor(maxVal: Float): Float =
    when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }

/** MonthBucket:
 *  Represents monthly aggregates for:
 *  - Average loneliness
 *  - Night usage (hours)
 *  - Day usage (hours)
 *  - Average steps
 *
 * @param label    Month label (e.g. "Jan", "Feb 2025").
 */
private data class MonthBucket(
    val label: String,
    val lonAvg: Float,
    val nightH: Float,
    val dayH: Float,
    val stepsAvg: Float
)

/** aggregateMonthly:
 *
 * Aggregates daily samples into per-month averages.
 *
 * - Loneliness: average of transformed scores (mapping original scale to 1..7).
 * - Night / day usage: average minutes, converted to hours.
 * - Steps: average daily steps.
 *
 * Returns one MonthBucket per calendar month between first and last sample.
 */
private fun aggregateMonthly(samples: List<DaySample>): List<MonthBucket> {
    if (samples.isEmpty()) return emptyList()

    val sortedSamples = samples.sortedBy { it.date }
    val firstMonth = YearMonth.from(sortedSamples.first().date)
    val lastMonth = YearMonth.from(sortedSamples.last().date)
    val samplesByMonth = sortedSamples.groupBy { YearMonth.from(it.date) }

    val monthBuckets = mutableListOf<MonthBucket>()
    var currentMonth = firstMonth
    while (!currentMonth.isAfter(lastMonth)) {
        val daysInMonth = samplesByMonth[currentMonth] ?: emptyList()

        val label = currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        val lonValues = daysInMonth.mapNotNull { it.loneliness }.filter { it > 0 }
        val lonAvg = if (lonValues.isNotEmpty()) {
            lonValues.map { (it - 2).coerceIn(1, 7) }.average().toFloat()
        } else {
            Float.NaN
        }

        val night = (daysInMonth.mapNotNull { it.nightMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
        val day = (daysInMonth.mapNotNull { it.dayMinutes }.average().toFloat() / 60f).let { if (it.isNaN()) 0f else it }
        val steps = daysInMonth.map { it.steps }.average().toFloat()

        monthBuckets.add(MonthBucket(
            label = label,
            lonAvg = if(lonAvg.isNaN()) Float.NaN else lonAvg,
            nightH = if(night.isNaN()) 0f else night,
            dayH = if(day.isNaN()) 0f else day,
            stepsAvg = if(steps.isNaN()) 0f else steps
        ))

        currentMonth = currentMonth.plusMonths(1)
    }
    return monthBuckets
}

/**
 * buildSamplesForRange:
 * Builds a list of DaySample for the selected time range, anchored to today's date.
 *
 * - Week: 7 last days, missing dates filled with zero/empty samples.
 * - Month: last 30 days, missing dates filled.
 * - 3 Months / Year: direct filter of existing entities.
 * - All: all samples in DB.
 */
private fun buildSamplesForRange(
    daysEntity: List<DayEntity>?,
    range: TimeRange
): List<DaySample> {
    val entities = daysEntity ?: return emptyList()
    val sorted = entities.sortedBy { it.date }
    val today = LocalDate.now()

    fun DayEntity.toSample() = DaySample(
        date = date,
        loneliness = loneliness,
        nightMinutes = nightMinutes,
        dayMinutes = dayMinutes,
        steps = steps
    )

    return when (range) {
        TimeRange.Week -> {
            val wantedDates = (0..6).map { i -> today.minusDays((6 - i).toLong()) }
            val byDate = sorted.associateBy { it.date }
            wantedDates.map { date ->
                byDate[date]?.toSample() ?: DaySample(
                    date = date,
                    loneliness = null,
                    nightMinutes = 0,
                    dayMinutes = 0,
                    steps = 0
                )
            }
        }
        TimeRange.Month -> {
            val from = today.minusDays(29)
            val byDate = sorted.associateBy { it.date }
            (0..29).map { i ->
                val date = from.plusDays(i.toLong())
                byDate[date]?.toSample() ?: DaySample(date, null, 0, 0, 0)
            }
        }
        TimeRange.ThreeMonths -> {
            val from = today.minusDays(89)
            sorted.filter { it.date in from..today }.map { it.toSample() }
        }
        TimeRange.Year -> {
            val from = today.minusDays(364)
            sorted.filter { it.date in from..today }.map { it.toSample() }
        }
        TimeRange.All -> {
            sorted.map { it.toSample() }
        }
    }
}

/**
 * enableTapToShowValue:
 * Enables tap-to-show-value on BarCharts.
 * Shows a Toast message with the label (date/month) and formatted value.
 */
private fun BarChart.enableTapToShowValue(
    labels: List<String>,
    format: (Float) -> String
) {
    lockZoomPanKeepTap()
    setOnChartValueSelectedListener(object :
        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
            if (e is BarEntry) {
                val i = e.x.toInt().coerceIn(labels.indices)
                val label = labels.getOrElse(i) { "" }
                android.widget.Toast.makeText(context, "$label: ${format(e.y)}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        override fun onNothingSelected() {}
    })
}

/**
 * enableTapToShowValue:
 * Enables tap-to-show-value on LineCharts.
 * Shows a Toast message with the label (date/month) and formatted value.
 */
private fun LineChart.enableTapToShowValue(
    labels: List<String>,
    format: (Float) -> String
) {
    lockZoomPanKeepTap()
    setOnChartValueSelectedListener(object :
        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
            if (e != null) {
                val i = e.x.toInt().coerceIn(labels.indices)
                val label = labels.getOrElse(i) { "" }
                android.widget.Toast.makeText(context, "$label: ${format(e.y)}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        override fun onNothingSelected() {}
    })
}

/**
 * BarChart.lockZoomPanKeepTap:
 * Locks zoom/pan for a BarLineChartBase while still allowing highlight on tap.
 * Used to keep charts readable and static, but interactive via toast values.
 */
private fun BarLineChartBase<*>.lockZoomPanKeepTap() {
    setTouchEnabled(true)
    setDragEnabled(false)
    setScaleEnabled(false)
    isScaleXEnabled = false
    isScaleYEnabled = false
    setPinchZoom(false)
    isDoubleTapToZoomEnabled = false
    isHighlightPerTapEnabled = true
    isHighlightPerDragEnabled = false
}

/**
 * StartEndValueFormatter:
 * Formatter used for All Time / aggregated X-axis.
 * Only shows:
 *  - First label as-is
 *  - Last label as "Now"
 * Everything in-between is hidden to avoid clutter.
 */
class StartEndValueFormatter(private val labels: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {

        val index = value.toInt()
        val lastIndex = labels.lastIndex

        if (index == 0) return labels.firstOrNull() ?: ""
        if (index == lastIndex) return "Now"
        return ""
    }
}


/**
 * buildLonelinessDataSets:
 * Builds one or more LineDataSets for loneliness values, splitting at NaN gaps.
 * This allows rendering discontinuous lines where data is missing.
 */
private fun buildLonelinessDataSets(points: List<AnalysisViewModel.LinePoint>): List<ILineDataSet> {
    val sets = mutableListOf<ILineDataSet>()
    var run = mutableListOf<Entry>()

    fun flush() {
        if (run.isNotEmpty()) {
            sets += LineDataSet(run, "Loneliness").apply {
                color = COLOR_PRIMARY_HEX
                setCircleColor(COLOR_PRIMARY_HEX)
                lineWidth = 3f
                circleRadius = 5f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
            }
            run = mutableListOf()
        }
    }

    points.forEachIndexed { i, p -> if (p.y.isNaN()) flush() else run += Entry(i.toFloat(), p.y) }
    flush()
    return sets
}
