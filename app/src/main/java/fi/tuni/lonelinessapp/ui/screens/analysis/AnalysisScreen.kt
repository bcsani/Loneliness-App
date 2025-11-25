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
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel.DaySample
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

// Color configuration for charts.
// The colors are now hardcoded. Later we will move under the theme (?)
private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
private const val COLOR_TEXT_HEX    = 0xFF1F2937.toInt()

// pie chart colors.
private val PIE_COLORS = listOf(
    0xFF2563EB.toInt(),
    0xFFF59E0B.toInt(),
    0xFF10B981.toInt(),
    0xFFA855F7.toInt(),
    0xFFEF4444.toInt()
)

// User selectable time ranges in the analysis view.
enum class TimeRange { Week, Month, ThreeMonths, Year, All }

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

    // 1) All Time: just the beginning + "Now"
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

    // 2) Columns that require extra space on the edges.
    if (adjustForBars && labels.isNotEmpty()) {
        axisMinimum = -0.5f
        axisMaximum = (labels.size - 1).toFloat() + 0.5f

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

    // 4) Default: months/weeks without special logic.
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

// Y-axis “nice” – if maximum is 0/missing, show a reasonable fallback range.
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
    axisLeft.apply {
        axisMinimum = 0f
        axisMaximum = axisMax
        granularity = step
        setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true)
        setDrawGridLines(true)
        enableGridDashedLine(10f, 10f, 0f)
    }
}

// Day labels for the selected period, even if all day values ​​are empty.
private fun fallbackDayLabels(range: TimeRange, samplesCount: Int): List<String> = when (range) {
    TimeRange.Week  -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    TimeRange.Month -> (1..samplesCount.coerceAtLeast(30)).map { it.toString() }
    else -> emptyList()
}

/**
 * Composable that displays the analysis page: it builds the data series for the
 * selected time range and displays 5 daily charts (Week/Month) or monthly
 * aggregates (3 Months / Year / All).
 * Data is provided by AnalysisViewModel via StateFlow and re-computed whenever
 * the selected time range changes.
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
    // ... (kommentoitu testidata, jätetty koskematta)
    // ======================= END OF TEST DATA =======================

    // ================= ORIGINAL DATA LOADING (Commented out) ================
    val daysEntity by analysisViewModel.daysEntity.collectAsState()
    val samples: List<DaySample> = remember(daysEntity, selectedRange) {
        buildSamplesForRange(daysEntity, selectedRange)
    }
    // ========================================================================

    val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
    val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }
    val commPie       = remember { analysisViewModel.communicationPieHours() }

    // Päivä- ja kuukausilabelit (näkyvät myös tyhjällä datalla)
    val dayLabels = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.Week  -> (samples.takeIf { it.isNotEmpty() } ?: emptyList()).map {
                it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }.ifEmpty { fallbackDayLabels(TimeRange.Week, 7) }

            TimeRange.Month -> (if (samples.isNotEmpty()) List(samples.size) { (it + 1).toString() } else fallbackDayLabels(TimeRange.Month, 30))

            else -> emptyList()
        }
    }
    val monthTicks = if (selectedRange == TimeRange.Month)
        listOf(1, 5, 10, 15, 20, 25, 30)
    else
        null

    val monthlyAgg = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.ThreeMonths, TimeRange.Year -> aggregateMonthly(samples)
            TimeRange.All -> analysisViewModel.aggregateIntoTwelvePeriods(samples)
            else -> emptyList()
        }
    }

    val monthLabels = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.AggregateBucket -> (monthlyAgg as List<AnalysisViewModel.AggregateBucket>).map { it.label }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.label }
            else -> emptyList()
        }
    }

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

    // Show the entire analysis as a vertical list: one card per chart.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Dropdown box.
        item {
            var expanded by remember { mutableStateOf(false) }
            val options = listOf("Week", "1 Month", "3 Months", "1 Year", "All Time")
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
                                        "Week" -> TimeRange.Week
                                        "1 Month" -> TimeRange.Month
                                        "3 Months" -> TimeRange.ThreeMonths
                                        "1 Year" -> TimeRange.Year
                                        else -> TimeRange.All
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

        // Empty state.
        if (samples.isEmpty()) {
            item {
                ChartCard(title = "No data for the selected range") {
                    Text("Add some entries and come back – I’ll draw you a masterpiece.")
                }
            }

            // Daily view (Week / Month): show 5 charts using per-day data.
        } else if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) {

            // 1) Loneliness(line chart).
            item {
                val infoText = "Shows your UCLA Loneliness Scale scores for the selected period. Higher values indicate greater feelings of loneliness."
                ChartCard(
                    title = "UCLA Loneliness Scale",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {

                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
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
                                axisLeft.axisMaximum = 7.05f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(8, true)
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
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f", y) }

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
                val infoText = "Shows the time spent on your phone at night."
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
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f h", y) }

                            // --- UUSI: sama reuna-logiikka kuin Day time -kaaviossa ---
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
                            // -----------------------------------------------------------

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 3) Daytime usage (bar chart).
            item {
                val infoText = "Shows the time spent on your phone during the day."
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
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f h", y) }

                            val entries = toBarEntries(dayPts)

                            val set = makeBarDataSet(
                                label = "Daytime usage",
                                entries = entries,
                            )

                            val barData = BarData(set).apply { barWidth = 0.7f }
                            chart.data = barData

                            // THIS FIXES THE FIRST & LAST BEAM CUTTING
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
                val infoText = "Shows the number of steps taken during the selected period."
                ChartCard(
                    title = "Steps taken",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
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
                            chart.enableTapToShowValue(dayLabels) { y -> "%,d".format(y.toInt()) }

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
                val infoText = "Shows how your communication app usage is distributed. The chart displays the total hours spent on each app during the selected period."
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

            // 3 months, 1 year.
        } else {
            if (selectedRange == TimeRange.All) {
                StartEndValueFormatter(monthLabels) // <-- FIX: This is a List<String>
            } else {
                IndexAxisValueFormatter(monthLabels)
            }

            // 1) Loneliness(line chart).
            item {
                val infoText = "Shows the monthly average of your UCLA Loneliness Scale scores. Higher values indicate greater feelings of loneliness."
                ChartCard(
                    title = "UCLA Loneliness Scale",
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
                                axisLeft.axisMaximum = 7.05f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(8, true)
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

            // 2) Night usage (bar chart).
            item {
                val infoText = "Shows the monthly average of the time spent on your phone at night."
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
                                // YEAR / 3 MONTHS
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

            // 3) Daytime usage (bar chart).
            item {
                val infoText = "Shows the monthly average of the time spent on your phone during the day."
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

            // 4) Steps (bar chart).
            item {
                val infoText = "Shows the monthly average of the number of steps taken."
                ChartCard(
                    title = "Exercise",
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

            // 5) Communications (pie chart).
            item {
                val infoText = "Shows how your communication app usage is distributed. The chart displays the total hours spent on each app during the selected period."
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

/** Chartcard: function creates a uniform card template for graphs.
 * Small helper to standardize chart container look
 */
@Composable
private fun ChartCard(
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

/** BarChart.applyBarDefaults:
 * Common baseline for bar charts so we don’t repeat ourselves.
 */
private fun BarChart.applyBarDefaults() {
    description = Description().apply { text = "" }
    axisRight.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isHighlightPerTapEnabled = false
    isHighlightPerDragEnabled = false
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textSize = 12f
    xAxis.granularity = 1f
    xAxis.setDrawGridLines(true)
    xAxis.granularity = 1f
    xAxis.setDrawGridLines(true)
    axisLeft.setDrawAxisLine(true)
    axisLeft.axisMinimum = 0f
    xAxis.enableGridDashedLine(10f, 10f, 0f)
    axisLeft.textSize = 12f
    axisLeft.enableGridDashedLine(10f, 10f, 0f)
}

/** toBarEntries:
 * Utility mappers to convert domain points or raw floats into MPAndroidChart
 * entries. X is positional (index), Y is the value.
 */
private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) }

/** toBarEntriesFromFloats:
 * Utility mappers to convert domain points or raw floats into MPAndroidChart
 * entries. X is positional (index), Y is the value.
 */
private fun toBarEntriesFromFloats(values: List<Float>): List<BarEntry> =
    values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

/** makeBarDataSet:
 * Creates a unified BarDataSet with consistent styling. If a ValueFormatter is
 * provided, we draw labels on bars; otherwise hide value labels to reduce noise.
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

/** PieChart.enableToastOnSliceClick:
 * Attaches a simple toast on slice selection for quick feedback. Safe no-op on
 * empty data because MPAndroidChart won't trigger selection callbacks then.
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
 * Rounds up to the nearest multiple of 'step'. If step <= 0, returns value.
 */
private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = kotlin.math.ceil(value / step)
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
 *  Aggregates daily samples into per-month averages (loneliness, hours, steps).
 */
private data class MonthBucket(
    val label: String,
    val lonAvg: Float,
    val nightH: Float,
    val dayH: Float,
    val stepsAvg: Float
)

/** aggregateMonthly:
 *  Aggregates daily samples into per-month averages (loneliness, hours, steps).
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

        val night = daysInMonth.map { it.nightMinutes }.average().toFloat() / 60f
        val day = daysInMonth.map { it.dayMinutes }.average().toFloat() / 60f
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
 * startDateFor: Computes an inclusive start date for the given range, anchored to anchorDate,
 * which must be the newest date we actually have in the database.
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
 * enableTapToShowValue: Displays a Toast message with the bar value.
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

class StartEndValueFormatter(private val labels: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        val lastIndex = labels.lastIndex

        if (index == 0) return labels.firstOrNull() ?: ""
        if (index == lastIndex) return "Now"

        return ""
    }
}

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
