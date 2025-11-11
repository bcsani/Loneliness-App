package fi.tuni.lonelinessapp.ui.screens.analysis

// Compose
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api

// MPAndroidChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.charts.BarLineChartBase

import kotlin.math.roundToInt
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel.DaySample
import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.IconButton

// Color configuration for charts.
// The colors are now hardcoded. Later we will move under the theme (?)
private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
private const val COLOR_TEXT_HEX    = 0xFF1F2937.toInt()

// pie chart colors.
private val PIE_COLORS = listOf(
    0xFF2563EB.toInt(), // blue
    0xFFF59E0B.toInt(), // amber
    0xFF10B981.toInt(), // emerald
    0xFFA855F7.toInt(), // violet
    0xFFEF4444.toInt()  // red
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
    // Default selection is Week.
    var selectedRange by remember { mutableStateOf(TimeRange.Week) }
    // AnalysisScreen-funktion sisällä, heti alussa
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }



    // ÄLÄ POISTA OIKEA VERSIO
    //Listen to the daily data provided by the ViewModel.
    val daysEntity by analysisViewModel.daysEntity.collectAsState()

    // Daily samples for the selected time.
    val samples: List<DaySample> = remember(daysEntity, selectedRange) {
        buildSamplesForRange(daysEntity, selectedRange)
    }

    //Creating data for charts.
    val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
    val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }
    // ÄLÄ POISTA OIKEA VERSIO

    // ====================== TESTIDATA ALKAA ======================/

    // 1. LUODAAN KUVITTEELLINEN TESTIDATA
    // Tämä korvaa tietokannasta tulevan datan väliaikaisesti.
    //val testSamples: List<DaySample> = remember {
        //listOf(
        //DaySample(LocalDate.now().minusDays(6), loneliness = 9, nightMinutes = 60, dayMinutes = 120, steps = 5000),
        //DaySample(LocalDate.now().minusDays(5), loneliness = 9, nightMinutes = 75, dayMinutes = 150, steps = 6200),
        //DaySample(LocalDate.now().minusDays(4), loneliness = 9, nightMinutes = 45, dayMinutes = 100, steps = 4500),
        //DaySample(LocalDate.now().minusDays(3), loneliness = 5, nightMinutes = 90, dayMinutes = 200, steps = 8000),
    //DaySample(LocalDate.now().minusDays(2), loneliness = 4, nightMinutes = 80, dayMinutes = 180, steps = 7500),
   // DaySample(LocalDate.now().minusDays(1), loneliness = 9, nightMinutes = 120, dayMinutes = 240, steps = 9500),
    //DaySample(LocalDate.now(), loneliness = 3, nightMinutes = 55, dayMinutes = 110, steps = 5200)
    //)
   // }

    // `samples`-muuttuja on nyt meidän testidatamme.
    //val samples = testSamples

    // 2. MUUNNETAAN TESTIDATA KAAVIOIDEN MUUTTUJIIN
    // Tämä on sama koodi kuin ennen, mutta se käyttää nyt `testSamples`-dataa.
    //val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
   // val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    //val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    //val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }

    // ======================= TESTIDATA LOPPUU =======================



    // Pie chart demo data. Replace with real app analytics when available.
    val commPie       = remember { analysisViewModel.communicationPieHours() }

    // X-axis texts for charts: days of the week for weeks, otherwise day number.
    val dayLabels = remember(samples, selectedRange) {
        when (selectedRange) {

            // Week.
            TimeRange.Week -> samples.map {
                it.date.dayOfWeek.name
                    .take(3)
                    .lowercase()
                    .replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }
            }

            // Month.
            TimeRange.Month -> List(samples.size) { index ->
                (index + 1).toString()
            }

            // The others don't use the day label.
            else -> emptyList()
        }
    }


    // Monthly aggregate (only 3 months / 1 year / All-time).
    val monthlyAgg = remember(samples, selectedRange) {
        if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) emptyList()
        else aggregateMonthly(samples)
    }
    val monthLabels  = remember(monthlyAgg) { monthlyAgg.map { it.label } }
    val lonMonthly   = remember(monthlyAgg) { monthlyAgg.map { it.lonAvg } }
    val nightMonthly = remember(monthlyAgg) { monthlyAgg.map { it.nightH } }
    val dayMonthly   = remember(monthlyAgg) { monthlyAgg.map { it.dayH } }
    val stepsMonthly = remember(monthlyAgg) { monthlyAgg.map { it.stepsAvg } }

    // Descriptive titles. -Mahdollisesti poistoon!
    //val periodSuffix = when (selectedRange)
    //{
        //TimeRange.Week  -> " (This Week)"
        //TimeRange.Month -> " (This Month)"
        //TimeRange.ThreeMonths -> " (Last 3 Months)"
        //TimeRange.Year -> " (This Year)"
        //TimeRange.All -> " (All Time)"
    //}

    // Show the entire analysis as a vertical list: one card per chart.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Dropdown box.
        // Time range selection. Updates the selectedRange value,
        // causing the charts below to recalculate their data.
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
                        // The `menuAnchor` modifier must be passed to the text field for correctness.
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
                                        "Week"      -> TimeRange.Week
                                        "1 Month"   -> TimeRange.Month
                                        "3 Months"  -> TimeRange.ThreeMonths
                                        "1 Year"    -> TimeRange.Year
                                        else        -> TimeRange.All
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
        if (samples.isEmpty())
        {
            item {
                ChartCard(title = "No data for the selected range") {
                    Text("Add some entries and come back – I’ll draw you a masterpiece.")
                }
            }

            // Daily view (Week / Month): show 5 charts using per-day data.
        } else if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) {

            // 1) Loneliness (line)
            // $periodSuffix. EHKÄ?
            item {
                val infoText = "Shows the monthly average of your UCLA Loneliness Scale scores. Higher values indicate greater feelings of loneliness."
                ChartCard(
                    title = "UCLA Loneliness Scale",
                    onInfoClick = { infoDialogMessage = infoText } // <-- LISÄÄ TÄMÄ
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
                            //chart.enableTapToShowValue(dayLabels) { y -> "%d".format(y.toInt()) }
                            val entries = lonelinessPts.mapIndexed { i, p -> Entry(i.toFloat(), p.y) }
                            val set = LineDataSet(entries, "Loneliness").apply {
                                color = COLOR_PRIMARY_HEX
                                setCircleColor(COLOR_PRIMARY_HEX)
                                lineWidth = 3f
                                circleRadius = 5f
                                mode = LineDataSet.Mode.LINEAR
                                setDrawValues(false)
                            }
                            chart.data = LineData(set)
                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }

                    )
                }
            }

            // 2) Night usage (bar)
            // $periodSuffix
            item {
                val infoText = "Shows the time spent on your phone in the night."
                ChartCard(
                    title = "Night time phone usage",
                    onInfoClick = { infoDialogMessage = infoText }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults(emptyList()) // staattiset
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

            // 3) Daytime usage (bar)
            // $periodSuffix
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

            // 4) Steps (bar).
            // $periodSuffix.
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

            // 5) Communications (pie).
            // $periodSuffix
            item {
                val infoText = "Shows how your communication app usage is distributed. The chart displays the total hours spent on each app during the selected period."
                ChartCard(
                    title = "Communication Apps Usage",
                    onInfoClick = { infoDialogMessage = infoText } // <-- TÄMÄ RIVI SAA NAPIN NÄKYVIIN
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

        // 3 months, 1 year, all
        } else {

            // UCLA Loneliness (monthly avg).
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
                                axisLeft.axisMaximum = 9f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(10, true)
                                axisLeft.setDrawGridLines(true)
                                axisLeft.enableGridDashedLine(10f, 10f, 0f)
                            }
                        },
                        update = { chart ->
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
                            chart.enableTapToShowValue(monthLabels) { y -> String.format("%.1f", y) }
                            val entries = lonMonthly.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                            val set = LineDataSet(entries, "Loneliness").apply {
                                color = COLOR_PRIMARY_HEX
                                setCircleColor(COLOR_PRIMARY_HEX)
                                lineWidth = 3f
                                circleRadius = 5f
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawValues(false)
                            }
                            chart.data = LineData(set)

                            chart.data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }
                    )
                }
            }

            // Night Usage.
            item {
                val infoText = "Shows the monthly average of the time spent on your phone in the night."
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
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
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


            // Day Usage (monthly avg hours).
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
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
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


            // Exercise (monthly avg steps).
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
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
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


            // Communications (donut).
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
            // Rivi, joka asettaa otsikon ja infonapin vierekkäin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Asettaa elementit reunoihin
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Otsikko
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Varmistaa, että teksti vie suurimman osan tilasta
                )

                // Infonappi (näytetään vain, jos onInfoClick on määritelty)
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

            Spacer(modifier = Modifier.height(16.dp)) // Väli otsikon ja kaavion välille

            // Kaavion sisältö (esim. AndroidView)
            content()
        }
    }
}
/** BarChart.applyBarDefaults:
 * Common baseline for bar charts so we don’t repeat ourselves.
 */
private fun BarChart.applyBarDefaults(xLabels: List<String>) {

    // No description text.
    description = Description().apply { text = "" }

    // Right Y-axis off.
    axisRight.isEnabled = false
    legend.isEnabled = false

    // Allow touch/scroll.
    setTouchEnabled(true)

    // Removing the dark blue highlight.
    isHighlightPerTapEnabled = false
    isHighlightPerDragEnabled = false

    // X-axis to the bottom.
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
    xAxis.textSize = 12f
    xAxis.granularity = 1f
    xAxis.setDrawGridLines(true)
    // Labels given to the X-axis in order.
    // Space one index at a time.
    xAxis.granularity = 1f

    // Draw vertical guides.
    xAxis.setDrawGridLines(true)

    axisLeft.setDrawAxisLine(true)
    axisLeft.axisMinimum = 0f

    xAxis.enableGridDashedLine(10f, 10f, 0f)

    axisLeft.textSize = 12f

    axisLeft.enableGridDashedLine(10f, 10f, 0f)
}

/** BarChart.applyNiceYAxis:
 * Auto-picks a “nice” max and tick step for Y-axis based on the data values.
 */
private fun BarChart.applyNiceYAxis(values: List<Float>, stepFn: (Float) -> Float) {
    val maxVal = (values.maxOrNull() ?: 0f).coerceAtLeast(0f)

    // Choose  section (using hour/step logic).
    val step   = stepFn(maxVal)

    // Add 15% to the “top” and round up to the next highest number.
    val axisMax = niceCeil(maxVal * 1.15f, step)

    // Set the left Y-axis.
    axisLeft.apply {

        // Always start from zero.
        axisMinimum = 0f

        // Upper limit as calculated.
        axisMaximum = axisMax

        // Pitch 'step'.
        granularity = step

        // Limit the number of labels to max 10.
        setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true)
    }
}

/** toBarEntries:
 * Utility mappers to convert domain points or raw floats into MPAndroidChart
 * entries. X is positional (index), Y is the value.
 */
private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =

    // X = index, Y = value.
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
    //valueFormatter: ValueFormatter? = null,
    //showValues: Boolean = true,
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
        // Every 15 minutes.
        maxVal <= 2f  -> 0.25f

        // 30 minutes.
        maxVal <= 4f  -> 0.5f

        // 1 h.
        maxVal <= 8f  -> 1f

        // 2 h.
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
    val byMonth = samples.groupBy { YearMonth.from(it.date) }.toSortedMap()
    return byMonth.map { (ym, days) ->
        val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val lon   = days.map { it.loneliness.toDouble() }.average().toFloat()
        val night = days.map { it.nightMinutes / 60f }.map { it.toDouble() }.average().toFloat()
        val day   = days.map { it.dayMinutes / 60f }.map { it.toDouble() }.average().toFloat()
        val steps = days.map { it.steps.toFloat() }.map { it.toDouble() }.average().toFloat()
        MonthBucket(label, lon, night, day, steps)
    }
}


/**
 * startDateFor: Computes an inclusive start date for the given range, anchored to `anchorDate`,
 * which must be the newest date we actually have in the database.
 *
 * Inclusivity and off-by-one notes:
 * - Week: 7 calendar days including anchor → anchor - 6 days
 * - Month / ThreeMonths / Year: use minusMonths(n).plusDays(1) to include the
 *   anchor day and avoid edge-case drops on month length transitions.
 * - All: return LocalDate.MIN so downstream filtering becomes a no-op.
 */
private fun buildSamplesForRange(
    daysEntity: List<DayEntity>?,
    range: TimeRange
): List<DaySample> {
    val entities = daysEntity ?: return emptyList()

    // Always in chronological order.
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

        // 1) Week.
        TimeRange.Week -> {
            val wantedDates = (0..6).map { i ->
                today.minusDays((6 - i).toLong())
            }
            val byDate = sorted.associateBy { it.date }

            wantedDates.map { date ->
                val e = byDate[date]
                if (e != null) {
                    e.toSample()
                } else {
                    DaySample(
                        date = date,
                        loneliness = 0,
                        nightMinutes = 0,
                        dayMinutes = 0,
                        steps = 0
                    )
                }
            }
        }

        // 2) 30 päivää.
        TimeRange.Month -> {
            val from = today.minusDays(29)
            val byDate = sorted.associateBy { it.date }

            (0..29).map { i ->
                val date = from.plusDays(i.toLong())
                val e = byDate[date]
                if (e != null) {
                    e.toSample()
                } else {
                    DaySample(
                        date = date,
                        loneliness = 0,
                        nightMinutes = 0,
                        dayMinutes = 0,
                        steps = 0
                    )
                }
            }
        }

        // 3) 3 months.
        TimeRange.ThreeMonths -> {
            val from = today.minusDays(89)
            sorted
                .filter { it.date in from..today }
                .map { it.toSample() }
        }

        // 4) year.
        TimeRange.Year -> {
            val from = today.minusDays(364)
            sorted
                .filter { it.date in from..today }
                .map { it.toSample() }
        }

        // 5) all.
        TimeRange.All -> {
            sorted.map { it.toSample() }
        }
    }
}


/**
 * enableTapToShowValue: Displays a Toast message with the bar value.
 * Usage: call in the `update` block of each BarChart when the X-labels and data are known.
 */
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

/**
 * Näyttää Toast-viestin, kun käyttäjä napauttaa viivadiagrammin datapistettä,
 * ja lukitsee samalla zoomauksen sekä pannauksen (tap-highlight jää päälle).
 */
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


/**
 * BarLineChartBase: Locks zoom and panning on all Bar/Line type charts,
 * but leaves tap-highlight working.
 */
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

