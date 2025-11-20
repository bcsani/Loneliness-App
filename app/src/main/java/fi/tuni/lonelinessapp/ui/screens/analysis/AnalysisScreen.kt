package fi.tuni.lonelinessapp.ui.screens.analysis

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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.github.mikephil.charting.charts.BarLineChartBase
import kotlin.math.roundToInt
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet


private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
private const val COLOR_TEXT_HEX    = 0xFF1F2937.toInt()

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
 * Composable that displays the analysis page: it builds the data series for the
 * selected time range and displays 5 daily charts (Week/Month) or monthly
 * aggregates (3 Months / Year / All).
 * Data is provided by AnalysisViewModel via StateFlow and re-computed whenever
 * the selected time range changes.
 */
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
    val daysEntity by analysisViewModel.daysEntity.collectAsState()
    val samples: List<DaySample> = remember(daysEntity, selectedRange) {
        if (selectedRange == TimeRange.Week) {
            val today = LocalDate.now()
            val testData = listOf(
                DaySample(today.minusDays(6), loneliness = 4, nightMinutes = 60, dayMinutes = 120, steps = 5000),
                DaySample(today.minusDays(5), loneliness = 7, nightMinutes = 75, dayMinutes = 150, steps = 6200),
                DaySample(today.minusDays(4), loneliness = 5, nightMinutes = 0, dayMinutes = 100, steps = 4500), // <-- Null loneliness, zero night usage
                DaySample(today.minusDays(3), loneliness = null, nightMinutes = 90, dayMinutes = 200, steps = 8000),
                DaySample(today.minusDays(2), loneliness = 4, nightMinutes = 80, dayMinutes = 0, steps = 0),     // <-- Zero day usage and steps
                DaySample(today.minusDays(1), loneliness = 5, nightMinutes = 120, dayMinutes = 240, steps = 9500),
                DaySample(today, loneliness = 4, nightMinutes = 55, dayMinutes = 110, steps = 5200)
            )
            val testDataMap = testData.associateBy { it.date }
            val wantedDates = (0..6).map { i -> today.minusDays((6 - i).toLong()) }
            wantedDates.map { date ->
                testDataMap[date] ?: DaySample(date, -1, -1, -1, -1)
            }
        } else {
        buildSamplesForRange(daysEntity, selectedRange)
       }
    }
    // ======================= END OF TEST DATA =======================

    // ================= ORIGINAL DATA LOADING (Commented out) ================
    //val daysEntity by analysisViewModel.daysEntity.collectAsState()
    //val samples: List<DaySample> = remember(daysEntity, selectedRange) {
      //buildSamplesForRange(daysEntity, selectedRange)
     //}
    // ========================================================================


    val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
    val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }
    val commPie       = remember { analysisViewModel.communicationPieHours() }

    val dayLabels = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.Week -> samples.map {
                it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
            TimeRange.Month -> List(samples.size) { (it + 1).toString() }
            else -> emptyList()
        }
    }

    val monthlyAgg = remember(samples, selectedRange) {
        if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) emptyList()
        else if (selectedRange == TimeRange.All) {
            analysisViewModel.aggregateIntoTwelvePeriods(samples)
        } else {
            aggregateMonthly(samples)
        }
    }

    val monthLabels = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.PeriodBucket -> (monthlyAgg as List<AnalysisViewModel.PeriodBucket>).map { it.label }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.label }
            else -> emptyList()
        }
    }

    val lonMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.PeriodBucket -> (monthlyAgg as List<AnalysisViewModel.PeriodBucket>).map { it.lonAvg }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.lonAvg }
            else -> emptyList()
        }
    }
    val nightMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.PeriodBucket -> (monthlyAgg as List<AnalysisViewModel.PeriodBucket>).map { it.nightH }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.nightH }
            else -> emptyList()
        }
    }
    val dayMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.PeriodBucket -> (monthlyAgg as List<AnalysisViewModel.PeriodBucket>).map { it.dayH }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.dayH }
            else -> emptyList()
        }
    }
    val stepsMonthly = remember(monthlyAgg) {
        when (monthlyAgg.firstOrNull()) {
            is AnalysisViewModel.PeriodBucket -> (monthlyAgg as List<AnalysisViewModel.PeriodBucket>).map { it.stepsAvg }
            is MonthBucket -> (monthlyAgg as List<MonthBucket>).map { it.stepsAvg }
            else -> emptyList()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        if (samples.isEmpty()) {
            item {
                ChartCard(title = "No data for the selected range") {
                    Text("Add some entries and come back – I’ll draw you a masterpiece.")
                }
            }
        } else if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) {
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
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            chart.lockZoomPanKeepTap()
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f", y) }

                            val dataSets = buildLonelinessDataSets(lonelinessPts) // <— vain tämä
                            chart.data = LineData(dataSets)

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }


                    )
                }
            }

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
                                applyBarDefaults(emptyList())
                                setTouchEnabled(false)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            chart.applyNiceYAxis(nightPts.map { it.y }, ::hourStepFor)
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f h", y) }
                            val set = makeBarDataSet(
                                label = "Night usage",
                                entries = toBarEntries(nightPts),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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
                                applyBarDefaults(emptyList())
                                setTouchEnabled(false)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            chart.applyNiceYAxis(dayPts.map { it.y }, ::hourStepFor)
                            chart.enableTapToShowValue(dayLabels) { y -> String.format("%.1f h", y) }
                            val set = makeBarDataSet(
                                label = "Daytime usage",
                                entries = toBarEntries(dayPts),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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
                                applyBarDefaults(emptyList())
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
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            chart.applyNiceYAxis(stepsPts.map { it.y }, ::stepStepFor)
                            chart.enableTapToShowValue(dayLabels) { y -> "%,d".format(y.toInt()) }
                            val set = makeBarDataSet(
                                label = "Steps",
                                entries = toBarEntries(stepsPts),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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
        } else {
            val xAxisFormatter = if (selectedRange == TimeRange.All) {
                StartEndValueFormatter(monthLabels.firstOrNull() ?: "")
            } else {
                IndexAxisValueFormatter(monthLabels)
            }

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
                            // X-akselin formatter (esim. All Time: vain eka ja vika)
                            chart.xAxis.valueFormatter = xAxisFormatter

                            // Muodosta segmentoitu viiva: NaN -> katkos
                            val segmentedSets = buildLonelinessDataSets(
                                lonMonthly.mapIndexed { i, v ->
                                    AnalysisViewModel.LinePoint(
                                        xLabel = monthLabels.getOrElse(i) { "" },
                                        y = v
                                    )
                                }
                            )

                            chart.data = if (segmentedSets.isEmpty()) {
                                LineData() // ei dataa
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
                                applyBarDefaults(emptyList())
                                setTouchEnabled(true)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = xAxisFormatter
                            chart.applyNiceYAxis(nightMonthly, ::hourStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f h", y) }
                            val set = makeBarDataSet(
                                label = "Night usage",
                                entries = toBarEntriesFromFloats(nightMonthly),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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
                                applyBarDefaults(emptyList())
                                setTouchEnabled(true)
                                setPinchZoom(false)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = xAxisFormatter
                            chart.applyNiceYAxis(dayMonthly, ::hourStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f h", y) }
                            val set = makeBarDataSet(
                                label = "Day usage",
                                entries = toBarEntriesFromFloats(dayMonthly),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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
                                applyBarDefaults(emptyList())
                                setTouchEnabled(true)
                                setPinchZoom(false)
                                axisLeft.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float) = "%,d".format(value.toInt())
                                }
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = xAxisFormatter
                            chart.applyNiceYAxis(stepsMonthly, ::stepStepFor)
                            chart.enableTapToShowValue(monthLabels) { y -> "%,d".format(y.toInt()) }
                            val set = makeBarDataSet(
                                label = "Steps",
                                entries = toBarEntriesFromFloats(stepsMonthly),
                            )
                            chart.data = BarData(set).apply { barWidth = 0.7f }
                            chart.data.notifyDataChanged(); chart.notifyDataSetChanged(); chart.invalidate()
                        }
                    )
                }
            }

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

private fun BarChart.applyBarDefaults(xLabels: List<String>) {
    description = Description().apply { text = "" }
    axisRight.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isHighlightPerTapEnabled = false
    isHighlightPerDragEnabled = false
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
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

private fun BarChart.applyNiceYAxis(values: List<Float>, stepFn: (Float) -> Float) {
    val maxVal = (values.maxOrNull() ?: 0f).coerceAtLeast(0f)
    val step   = stepFn(maxVal)
    val axisMax = niceCeil(maxVal * 1.15f, step)
    axisLeft.apply {
        axisMinimum = 0f
        axisMaximum = axisMax
        granularity = step
        setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true)
    }
}

private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) }

private fun toBarEntriesFromFloats(values: List<Float>): List<BarEntry> =
    values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

private fun makeBarDataSet(
    label: String,
    entries: List<BarEntry>,
): BarDataSet = BarDataSet(entries, label).apply {
    color = COLOR_PRIMARY_HEX
    valueTextSize = 12f
    valueTextColor = COLOR_TEXT_HEX
    setDrawValues(false)
}

private fun PieChart.enableToastOnSliceClick() {
    setOnChartValueSelectedListener(object :
        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(
            e: com.github.mikephil.charting.data.Entry?,
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

private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = kotlin.math.ceil(value / step)
    return (k * step)
}

private fun hourStepFor(maxVal: Float): Float =
    when {
        maxVal <= 2f  -> 0.25f
        maxVal <= 4f  -> 0.5f
        maxVal <= 8f  -> 1f
        else          -> 2f
    }

private fun stepStepFor(maxVal: Float): Float =
    when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }

private data class MonthBucket(
    val label: String,
    val lonAvg: Float,
    val nightH: Float,
    val dayH: Float,
    val stepsAvg: Float
)

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

        val night = daysInMonth.mapNotNull { it.nightMinutes }.average().toFloat() / 60f
        val day = daysInMonth.mapNotNull { it.dayMinutes }.average().toFloat() / 60f
        val steps = daysInMonth.mapNotNull { it.steps }.average().toFloat()

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

private fun BarChart.enableTapToShowValue(
    labels: List<String>,
    format: (Float) -> String
) {
    lockZoomPanKeepTap()
    setOnChartValueSelectedListener(object :
        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
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
        override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
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
    setScaleXEnabled(false)
    setScaleYEnabled(false)
    setPinchZoom(false)
    setDoubleTapToZoomEnabled(false)
    isHighlightPerTapEnabled = true
    isHighlightPerDragEnabled = false
}

private fun PieChart.noZoomNoPanKeepTap() {
    setTouchEnabled(true)
    isRotationEnabled = false
    isHighlightPerTapEnabled = true
}

class StartEndValueFormatter(private val firstLabel: String) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return when (value.toInt()) {
            0 -> firstLabel
            11 -> "Now"
            else -> ""
        }
    }
}
// Luo useita LineDataSettejä, yksi "katkeamaton pätkä" kerrallaan.
// Päivä, jonka y on NaN, aiheuttaa katkoksen.
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

private fun buildSegmentedLineDataFromFloats(values: List<Float>): List<ILineDataSet> {
    val sets = mutableListOf<ILineDataSet>()
    var run = mutableListOf<Entry>()
    fun flush(){ if(run.isNotEmpty()){
        sets += LineDataSet(run, "Loneliness").apply {
            color = COLOR_PRIMARY_HEX; setCircleColor(COLOR_PRIMARY_HEX)
            lineWidth = 3f; circleRadius = 5f; mode = LineDataSet.Mode.LINEAR; setDrawValues(false)
        }; run = mutableListOf() } }
    values.forEachIndexed { i, y -> if (y.isNaN()) flush() else run += Entry(i.toFloat(), y) }
    flush(); return sets
}
