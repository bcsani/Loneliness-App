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

import kotlin.math.roundToInt
import fi.tuni.lonelinessapp.ui.screens.analysis.AnalysisViewModel.DaySample
import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalDate


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
  * AnalysisScreen: displays line, bar, and pie charts of daily/monthly data.
  * Data comes from the Room database via the AnalysisViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    // Default: Week
    var selectedRange by remember { mutableStateOf(TimeRange.Week) }
     // Listen to the daily data provided by the ViewModel.
    val daysEntity by analysisViewModel.daysEntity.collectAsState()

    // Daily samples for the selected time.
     val samples: List<DaySample> = remember(daysEntity, selectedRange) {
         buildSamplesForRange(daysEntity, selectedRange)
     }

     // creating data for charts.
    val lonelinessPts = remember(samples) { analysisViewModel.lonelinessLine(samples) }
    val nightPts      = remember(samples) { analysisViewModel.nightUsageBarsHours(samples) }
    val dayPts        = remember(samples) { analysisViewModel.dayUsageBarsHours(samples) }
    val stepsPts      = remember(samples) { analysisViewModel.stepsBars(samples) }

    // Pie chart demo data.
    val commPie       = remember { analysisViewModel.communicationPieHours() }

     // X-axis texts for charts: days of the week for weeks, otherwise day number.
    val dayLabels = remember(samples, selectedRange) {
        when (selectedRange) {
            TimeRange.Week -> {
                samples.map {
                    it.date.dayOfWeek.name
                        .take(3)
                        .lowercase()
                        .replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }
                }  }
            else -> {
                samples.map { it.date.dayOfMonth.toString() }
            }
        }
    }

    // Monthly aggregate (only 3 months / 1 year / All).
    val monthlyAgg = remember(samples, selectedRange) {
        if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month) emptyList()
        else aggregateMonthly(samples)
    }
    val monthLabels  = remember(monthlyAgg) { monthlyAgg.map { it.label } }
    val lonMonthly   = remember(monthlyAgg) { monthlyAgg.map { it.lonAvg } }
    val nightMonthly = remember(monthlyAgg) { monthlyAgg.map { it.nightH } }
    val dayMonthly   = remember(monthlyAgg) { monthlyAgg.map { it.dayH } }
    val stepsMonthly = remember(monthlyAgg) { monthlyAgg.map { it.stepsAvg } }

    // Descriptive titles.
    val periodSuffix = when (selectedRange)
    {
        TimeRange.Week  -> " (This Week)"
        TimeRange.Month -> " (This Month)"
        TimeRange.ThreeMonths -> " (Last 3 Months)"
        TimeRange.Year -> " (This Year)"
        TimeRange.All -> " (All Time)"
    }

    // Show the entire analysis as a vertical list (one card per chart).
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Dropdown box.
        // Time range selection. Updates the selectedRange value, causing the charts below
        // to recalculate their data.
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
                        onValueChange = {},
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

        // No data at all → show “No data…”.
        if (samples.isEmpty())
        {
            item {
                ChartCard(title = "No data for the selected range") {
                    Text("Add some entries and come back – I’ll draw you a masterpiece.")
                }
            }

        } else if (selectedRange == TimeRange.Week || selectedRange == TimeRange.Month)
        // Week or month → show 5 charts of daily data.
        {
            // 1) Loneliness (line)
            item {
                ChartCard(title = "Loneliness Level$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            LineChart(ctx).apply {
                                description = Description().apply { text = "" }
                                axisRight.isEnabled = false
                                legend.isEnabled = false

                                axisLeft.textSize = 14f
                                xAxis.textSize = 14f
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                                xAxis.granularity = 1f
                                xAxis.setDrawGridLines(true)
                                xAxis.enableGridDashedLine(10f, 10f, 0f)

                                axisLeft.axisMinimum = 0f
                                axisLeft.axisMaximum = 9f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(10, true)
                                axisLeft.setDrawGridLines(true)

                                val entries = lonelinessPts.mapIndexed { i, p -> Entry(i.toFloat(), p.y) }
                                val set = LineDataSet(entries, "Loneliness").apply {
                                    color = COLOR_PRIMARY_HEX
                                    setCircleColor(COLOR_PRIMARY_HEX)
                                    lineWidth = 3f

                                    // Muutetaan pilkun kokoa.
                                    circleRadius = 5f
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    setDrawValues(false)
                                }
                                data = LineData(set)
                                invalidate()
                            }
                        }
                    )
                }
            }

            // 2) Night usage (bar)
            item {
                ChartCard(title = "Night Phone Usage (hours)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults(dayLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                val hours = nightPts.map { it.y }
                                applyNiceYAxis(hours, ::hourStepFor)

                                val set = makeBarDataSet(
                                    label = "Night usage",
                                    entries = toBarEntries(nightPts),

                                    valueFormatter = weekOnlyLabelFormatter(selectedRange) { y ->
                                        String.format("%.1f h", y)
                                    }

                                )
                                // Set the data and column width.
                                data = BarData(set).apply { barWidth = 0.7f }

                                // Draw the graph.
                                invalidate()
                            }
                        }
                    )
                }
            }

            // Chart 3: Daytime phone usage (bar chart) – hours.
            item {
                ChartCard(title = "Daytime Phone Usage (6am–10pm, hours)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->

                            // Creating a bar chart.
                            BarChart(ctx).apply {

                                // Common basic settings.
                                applyBarDefaults(dayLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                // Take the y-values (hours) as a list.
                                val hours = dayPts.map { it.y }

                                // Set suitable steps.
                                applyNiceYAxis(hours, ::hourStepFor)

                                // Uniform datasets.
                                val set = makeBarDataSet(
                                    // Series name.
                                    label = "Daytime usage",

                                    // Points to BarEntry.
                                    entries = toBarEntries(dayPts),

                                    // Text on top of the column.
                                    valueFormatter = weekOnlyLabelFormatter(selectedRange) { y ->
                                        String.format("%.1f h", y)
                                    }

                                )

                                // Set the data and width.
                                data = BarData(set).apply { barWidth = 0.7f }

                                // Draw the graph.
                                invalidate()
                            }
                        }
                    )
                }
            }

            // 4) Steps (bar)
            item {
                ChartCard(title = "Exercise (steps)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),

                        // Creating a BarChart.
                        factory = { ctx ->

                            // Configuration.
                            BarChart(ctx).apply {

                                // Common basic settings.
                                applyBarDefaults(dayLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                //  Asetetaan Y-akselin alkamaan nollasta.
                                axisLeft.axisMinimum = 0f

                                // Pakotetaan Y-akselin välit olemaan aina 1000 askelta.
                                axisLeft.granularity = 1000f

                                // Muotoillaan Y-akselin luvut kokonaisluvuiksi.
                                axisLeft.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        // Näytetään 0, jos arvo on 0, muuten tuhaterottimella.
                                        return if (value == 0f)
                                        {
                                            "0"
                                        }
                                        else
                                        {
                                            "%,d".format(value.roundToInt())
                                        }
                                    }
                                }

                                // Create datasets.
                                val set = makeBarDataSet(
                                    label = "Steps",

                                    // Points to BarEntry.
                                    entries = toBarEntries(stepsPts),

                                    // Format the column value.
                                    valueFormatter = weekOnlyLabelFormatter(selectedRange) { y ->
                                        y.toInt().toString()
                                    }

                                )

                                // Set the data and width.
                                data = BarData(set).apply { barWidth = 0.7f }

                                // Draw the graph.
                                invalidate()
                            }
                        }
                    )
                }
            }

            // Chart 5: Communication Apps Usage (pie chart).
            item {
                ChartCard(title = "Communication Apps Usage$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        factory = { ctx ->
                            PieChart(ctx).apply {
                                description = Description().apply { text = "" }
                                // MUOKKAUS: Poistetaan selite (legend) oletuksena. Se lisätään takaisin vain, jos dataa on.
                                legend.isEnabled = false
                                // Määritellään, mihin muotoon selite asetetaan.

                                // Yleiset asetukset, jotka ovat aina voimassa.
                                setUsePercentValues(false)
                                setDrawEntryLabels(false)

                                // Prevents rotation.
                                isRotationEnabled = false

                                // Start up.
                                rotationAngle = 0f

                                // No animations.
                                animateY(0)

                                // Kaavion keskellä olevan reiän koko
                                holeRadius = 45f

                                val entries = commPie
                                    .filter { it.value > 0f }
                                    .map { PieEntry(it.value, it.label) }

                                // MUOKKAUS: Tarkistetaan, onko dataa.
                                if (entries.isEmpty()) {

                                    // --- KUN DATA ON NOLLA ---
                                    // Näytetään keskellä viesti ja tyhjennetään data.
                                    centerText = "No chart data available"
                                    setCenterTextSize(16f)
                                    setCenterTextColor(android.graphics.Color.BLACK)

                                    // MUOKKAUS: Aseta tyhjä PieData-objekti nullin sijaan.
                                    data = PieData(PieDataSet(emptyList(), ""))
                                } else {
                                    // --- KUN DATA EI OLE NOLLA ---
                                    // Otetaan selite takaisin käyttöön ja muotoillaan se.
                                    legend.isEnabled = true
                                    legend.apply {
                                        textSize = 16f
                                        isWordWrapEnabled = true
                                        maxSizePercent = 0.80f
                                        verticalAlignment =
                                            com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                        horizontalAlignment =
                                            com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                                        orientation =
                                            com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                        setDrawInside(false)
                                    }

                                    // Luodaan ja asetetaan datajoukko normaalisti.
                                    val set = PieDataSet(entries, "").apply {
                                        colors = PIE_COLORS
                                        sliceSpace = 2f
                                        valueTextSize = 14f
                                        valueTextColor = COLOR_TEXT_HEX
                                        valueFormatter = object : ValueFormatter() {
                                            @SuppressLint("DefaultLocale")
                                            override fun getFormattedValue(value: Float): String =
                                                String.format("%.1f h", value)
                                        }
                                    }
                                    data = PieData(set)
                                }

                                // Toast-ilmoituksen logiikka pysyy samana.
                                enableToastOnSliceClick()

                                // Piirretään kaavio uudelleen
                                invalidate()
                            }

                        }
                    )
                }
            }
        } else {
            // 3 months / 1 year / all = monthly aggregates.

            // UCLA Loneliness (monthly avg).
            item {
                ChartCard(title = "UCLA Loneliness Scale$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            LineChart(ctx).apply {
                                description = Description().apply { text = "" }
                                axisRight.isEnabled = false
                                legend.isEnabled = false

                                axisLeft.textSize = 14f
                                xAxis.textSize = 14f
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
                                xAxis.granularity = 1f
                                xAxis.setDrawGridLines(true)
                                xAxis.enableGridDashedLine(10f, 10f, 0f)

                                axisLeft.axisMinimum = 0f
                                axisLeft.axisMaximum = 9f
                                axisLeft.granularity = 1f
                                axisLeft.setLabelCount(10, true)
                                axisLeft.setDrawGridLines(true)
                                axisLeft.enableGridDashedLine(10f, 10f, 0f)

                                val entries = lonMonthly.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                                val set = LineDataSet(entries, "Loneliness").apply {
                                    color = COLOR_PRIMARY_HEX
                                    setCircleColor(COLOR_PRIMARY_HEX)
                                    lineWidth = 3f
                                    circleRadius = 5f
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    setDrawValues(false)
                                }
                                data = LineData(set)
                                invalidate()
                            }
                        }
                    )
                }
            }

            // Night Usage (monthly avg hours)
            item {
                ChartCard(title = "Night Usage (hours)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults(monthLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                applyNiceYAxis(nightMonthly, ::hourStepFor)
                                val set = makeBarDataSet(
                                    label = "Night usage",
                                    entries = toBarEntriesFromFloats(nightMonthly),
                                    valueFormatter = threeMonthsOnlyFormatter(selectedRange) { y ->
                                        String.format("%.1f h", y)
                                    }


                                )
                                data = BarData(set).apply { barWidth = 0.7f }
                                invalidate()
                            }
                        }
                    )
                }
            }

            // Day Usage (monthly avg hours)
            item {
                ChartCard(title = "Day Usage (hours)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults(monthLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                applyNiceYAxis(dayMonthly, ::hourStepFor)
                                val set = makeBarDataSet(
                                    label = "Day usage",
                                    entries = toBarEntriesFromFloats(dayMonthly),
                                    valueFormatter = threeMonthsOnlyFormatter(selectedRange) { y ->
                                        String.format("%.1f h", y)
                                    }
                                )
                                data = BarData(set).apply { barWidth = 0.7f }
                                invalidate()
                            }
                        }
                    )
                }
            }

            // Exercise (monthly avg steps)
            item {
                ChartCard(title = "Exercise (steps)$periodSuffix") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                applyBarDefaults(monthLabels)
                                setTouchEnabled(false)
                                setPinchZoom(false)

                                applyNiceYAxis(stepsMonthly, ::stepStepFor)

                                // Y-akseli ilman desimaaleja
                                axisLeft.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return value.toInt().toString()
                                    }
                                }



                                val set = makeBarDataSet(
                                    label = "Steps",
                                    entries = toBarEntriesFromFloats(stepsMonthly),
                                    valueFormatter = threeMonthsOnlyFormatter(selectedRange) { y ->
                                        y.toInt().toString()
                                    }

                                )
                                data = BarData(set).apply { barWidth = 0.7f }
                                invalidate()
                            }
                        }
                    )
                }
            }


            // Communications (donut)
            item {
                ChartCard(title = "Communication Apps Usage$periodSuffix") {
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

                                val entries = commPie
                                    .filter { it.value > 0f }
                                    .map { PieEntry(it.value, it.label) }

                                if (entries.isEmpty()) {
                                    centerText = "No chart data available"
                                    setCenterTextSize(16f)
                                    setCenterTextColor(android.graphics.Color.BLACK)
                                    data = PieData(PieDataSet(emptyList(), ""))
                                } else {
                                    legend.isEnabled = true
                                    legend.apply {
                                        textSize = 16f
                                        isWordWrapEnabled = true
                                        maxSizePercent = 0.80f
                                        verticalAlignment =
                                            com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                        horizontalAlignment =
                                            com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                                        orientation =
                                            com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                        setDrawInside(false)
                                    }
                                    val set = PieDataSet(entries, "").apply {
                                        colors = PIE_COLORS
                                        sliceSpace = 2f
                                        valueTextSize = 14f
                                        valueTextColor = COLOR_TEXT_HEX
                                        valueFormatter = object : ValueFormatter() {
                                            @SuppressLint("DefaultLocale")
                                            override fun getFormattedValue(value: Float): String =
                                                String.format("%.1f h", value)
                                        }
                                    }
                                    data = PieData(set)
                                }

                                enableToastOnSliceClick()


                                invalidate()
                            }
                        }
                    )
                }
            }
        }
    }
}


// The 'Chartcard' function creates a uniform card template for graphs.
@Composable
private fun ChartCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

// Common settings for all bar charts.
private fun BarChart.applyBarDefaults(xLabels: List<String>) {

    // No description text.
    description = Description().apply { text = "" }

    // Right Y-axis off.
    axisRight.isEnabled = false
    legend.isEnabled = false

    // Allow touch/scroll.
    setTouchEnabled(true)  // MUOKATTU aiemmin, pidetään näin

    // Removing the dark blue highlight.
    isHighlightPerTapEnabled = false
    isHighlightPerDragEnabled = false

    // X-axis to the bottom.
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
    xAxis.textSize = 12f// <-- UUSI: Suurennetaan X-akselin tekstejä

    // Labels given to the X-axis in order.
    // Space one index at a time.
    xAxis.granularity = 1f

    // Draw vertical guides.
    xAxis.setDrawGridLines(true)

    // Apuviivoihin katkoviiva. OR DO WE WANT?
    xAxis.enableGridDashedLine(10f, 10f, 0f)

    // Draw vertical guides.
    axisLeft.setDrawGridLines(true)
    axisLeft.textSize = 12f

    // Vaaka-apuviivoihin katkoviiva, OR DO WE WANT?
    axisLeft.enableGridDashedLine(10f, 10f, 0f)
}

// Adjust the left Y-axis according to the values.
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

// Sama layout kuin steps-kaaviossa,
// mutta y-akselin väli ja formatteri annetaan ulkoa.
private fun BarChart.applyStepsLikeLayout(
    xLabels: List<String>,
    yMin: Float = 0f,
    yMax: Float? = null,
    yGranularity: Float = 1f,
    yFormatter: ValueFormatter
) {
    applyBarDefaults(xLabels)

    axisLeft.axisMinimum = yMin
    axisLeft.granularity = yGranularity
    axisLeft.valueFormatter = yFormatter

    if (yMax != null) {
        axisLeft.axisMaximum = yMax
        // jos haluat tasaiset labelit
        axisLeft.setLabelCount(((yMax - yMin) / yGranularity).toInt() + 1, true)
    }
}


// Convert BarPoint → BarEntry.
private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =

    // X = index, Y = value.
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) }

private fun toBarEntriesFromFloats(values: List<Float>): List<BarEntry> =
    values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

// Create a unified BarDataSet with the same layout.
private fun makeBarDataSet(
    label: String,
    entries: List<BarEntry>,
    valueFormatter: ValueFormatter? = null,
    showValues: Boolean = true,   // <-- UUSI
): BarDataSet = BarDataSet(entries, label).apply {
    color = COLOR_PRIMARY_HEX
    valueTextSize = 12f
    valueTextColor = COLOR_TEXT_HEX

    if (valueFormatter != null) {
        setValueFormatter(valueFormatter)
    }

    // lopullinen päätös tulee tästä
    setDrawValues(showValues && valueFormatter != null)
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

// Luvut vain viikon kohdalle.
private fun weekOnlyLabelFormatter(
    selectedRange: TimeRange,
    format: (Float) -> String
): ValueFormatter = object : ValueFormatter() {
    override fun getBarLabel(e: BarEntry?): String {
        if (selectedRange != TimeRange.Week) return ""
        if (e == null) return ""
        return format(e.y)
    }
}

// Näytä arvot vain, jos valittuna on 3 kk
private fun threeMonthsOnlyFormatter(
    selectedRange: TimeRange,
    format: (Float) -> String
): ValueFormatter = object : ValueFormatter() {
    override fun getBarLabel(e: BarEntry?): String {
        if (selectedRange != TimeRange.ThreeMonths) return ""
        if (e == null) return ""
        return format(e.y)
    }
}


// Round the upper limit up to the nearest multiple of 'step'.
private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = kotlin.math.ceil(value / step)
    return (k * step)
}

// Choose interval for hours based on the max value.
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

// Choose a  interval for the steps.
private fun stepStepFor(maxVal: Float): Float =
    when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }

// --- Agregaatiot & datan koonti ---

private data class MonthBucket(
    val label: String,
    val lonAvg: Float,
    val nightH: Float,
    val dayH: Float,
    val stepsAvg: Float
)

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

private fun buildSamplesForRange(
    daysEntity: List<DayEntity>?,
    range: TimeRange
): List<DaySample> {
    val entities = daysEntity ?: return emptyList()

    // Always arranged in chronological order
    val sorted = entities.sortedBy { it.date }

    return when (range) {

        // WEEK: up to this day
        TimeRange.Week -> {
            val today = LocalDate.now()

            // 7 days: (today - 6) ... today.
            val wantedDates = (0..6).map { i ->
                today.minusDays((6 - i).toLong())
            }

            // jos DayEntity.date on LocalDate -> tää riittää
            // jos se on LocalDateTime -> muuta tähän: .associateBy { it.date.toLocalDate() }
            val byDate = sorted.associateBy { it.date }

            wantedDates.map { date ->
                val e = byDate[date]
                if (e != null) {
                    DaySample(
                        date = e.date,
                        loneliness = e.loneliness,
                        nightMinutes = e.nightMinutes,
                        dayMinutes = e.dayMinutes,
                        steps = e.steps
                    )
                } else {
                    // puuttuva päivä → näytetään se silti, mutta nollilla
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

        // 1 month.
        TimeRange.Month -> {
            val last30 = sorted.takeLast(30)
            last30.map {
                DaySample(
                    date = it.date,
                    loneliness = it.loneliness,
                    nightMinutes = it.nightMinutes,
                    dayMinutes = it.dayMinutes,
                    steps = it.steps
                )
            }
        }

        // Three months.
        TimeRange.ThreeMonths -> {
            val last90 = sorted.takeLast(90)
            last90.map {
                DaySample(
                    date = it.date,
                    loneliness = it.loneliness,
                    nightMinutes = it.nightMinutes,
                    dayMinutes = it.dayMinutes,
                    steps = it.steps
                )
            }
        }

        // Year.
        TimeRange.Year -> {
            val last365 = sorted.takeLast(365)
            last365.map {
                DaySample(
                    date = it.date,
                    loneliness = it.loneliness,
                    nightMinutes = it.nightMinutes,
                    dayMinutes = it.dayMinutes,
                    steps = it.steps
                )
            }
        }

        //  all.
        TimeRange.All -> {
            sorted.map {
                DaySample(
                    date = it.date,
                    loneliness = it.loneliness,
                    nightMinutes = it.nightMinutes,
                    dayMinutes = it.dayMinutes,
                    steps = it.steps
                )
            }
        }
    }
}

