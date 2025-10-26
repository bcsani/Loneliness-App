package fi.tuni.lonelinessapp.ui.screens.analysis

// Compose
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.roundToInt

// The colors are now hardcoded. Later we will move under the theme?
private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
private const val COLOR_TEXT_HEX    = 0xFF1F2937.toInt()
private val PIE_COLORS = listOf(

    0xFF2563EB.toInt(), // blue
    0xFFF59E0B.toInt(), // amber
    0xFF10B981.toInt(), // emerald
    0xFFA855F7.toInt(), // violet
    0xFFEF4444.toInt()  // red
)

// 1) Loneliness Level (line)
// 2) Night Phone Usage (bar)
// 3) Daytime Phone Usage (bar)
// 4) Exercise (steps) (bar)
// 5) Communication Apps Usage (pie)
@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,

    // Will be connected to the correct data later.
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    // Retrieve the week's demo data from the ViewModel.
    val week = remember { analysisViewModel.loadCurrentWeek() }

    // List<LinePoint>
    val lonelinessPts = remember { analysisViewModel.lonelinessLine(week) }

    // List<BarPoint> (h)
    val nightPts      = remember { analysisViewModel.nightUsageBarsHours(week) }

    // List<BarPoint> (h)
    val dayPts        = remember { analysisViewModel.dayUsageBarsHours(week) }

    // List<BarPoint>
    val stepsPts      = remember { analysisViewModel.stepsBars(week) }

    // List<PieSlice>
    val commPie       = remember { analysisViewModel.communicationPieHours() }

    // Mon/Tue..
    val dayLabels     = remember { lonelinessPts.map { it.xLabel } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Chart 1: Loneliness LeveL (line chart)
        item {
            ChartCard(title = "Loneliness Level (This Week)") {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            description = Description().apply { text = "" }
                            axisRight.isEnabled = false
                            legend.isEnabled = false

                            axisLeft.textSize = 12f
                            xAxis.textSize = 12f
                            setTouchEnabled(false)
                            setPinchZoom(false)

                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f, 10f, 0f)


                            axisLeft.axisMinimum = 0f
                            axisLeft.axisMaximum = 6f
                            axisLeft.granularity = 1f
                            axisLeft.spaceTop = 0f
                            //axisLeft.setLabelCount(7, true)
                            axisLeft.setDrawGridLines(true)
                            axisLeft.enableGridDashedLine(10f, 10f, 0f)

                            val entries =
                                lonelinessPts.mapIndexed { i, p -> Entry(i.toFloat(), p.y) }

                            val set = LineDataSet(entries, "Loneliness").apply {
                                color = COLOR_PRIMARY_HEX
                                setCircleColor(COLOR_PRIMARY_HEX)

                                lineWidth = 3f
                                circleRadius = 5f
                                mode = LineDataSet.Mode.HORIZONTAL_BEZIER

                                setDrawValues(false)
                            }
                            data = LineData(set)
                            invalidate()
                        }
                    }
                )
            }
        }

        // Chart 2: Night phone usage (bar chart) – hours.
        item {
            ChartCard(title = "Night Phone Usage (hours)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->

                        // Configure the bar chart.
                        BarChart(ctx).apply {

                            // Run common basic settings (x-axis down, grid).
                            applyBarDefaults(dayLabels)
                            setTouchEnabled(false)
                            setPinchZoom(false)

                            // Get the y-values (hours) as a list.
                            val hours = nightPts.map { it.y }

                            // Adjust the left Y-axis to reasonable increments (0.25/0.5/1/2h).
                            applyNiceYAxis(hours, ::hourStepFor)

                            // Create a unified set of columns with their appearance.
                            val set = makeBarDataSet(

                                // Series name (not visible when legend is hidden, but good for debugging).
                                label = "Night usage",

                                // Convert points to BarEntry.
                                entries = toBarEntries(nightPts),

                                // Format the text above the column.
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =

                                        // Display e.g. “1.5 h”.
                                        if (e == null) "" else String.format("%.1f h", e.y)
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
            ChartCard(title = "Daytime Phone Usage (6am–10pm, hours)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
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
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =

                                        // Display “x.x h”.
                                        if (e == null) "" else String.format("%.1f h", e.y)
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

        // Chart 4: Exercise (bar chart).

        item {
            ChartCard(title = "Exercise (steps)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth().height(240.dp),

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
                                    if (value == 0f) return "0"
                                    return "%,d".format(value.roundToInt())
                                }
                            }

                            // Create datasets.
                            val set = makeBarDataSet(
                                // name
                                label = "Steps",

                                // Points to BarEntry.
                                entries = toBarEntries(stepsPts),

                                // Format the column value.
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else "%,d".format(e.y.toInt())
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
        // Chart 5: Communication Apps Usage (pie chart).
        item {
            ChartCard(title = "Communication Apps Usage (hours)") {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            description = Description().apply { text = "" }

                            // MUOKKAUS: Poistetaan selite (legend) oletuksena. Se lisätään takaisin vain, jos dataa on.
                            legend.isEnabled = false

                            // Yleiset asetukset, jotka ovat aina voimassa
                            setUsePercentValues(false)
                            setDrawEntryLabels(false)
                            isRotationEnabled = false
                            rotationAngle = 0f
                            animateY(0)
                            holeRadius = 45f // Kaavion keskellä olevan reiän koko
                            val entries = commPie
                                .filter { it.value > 0f }
                                .map { PieEntry(it.value, it.label) }

                            // MUOKKAUS: Tarkistetaan, onko dataa.
                            if (entries.sumOf { it.value.toDouble() } == 0.0) {
                                // --- KUN DATA ON NOLLA ---
                                // Näytetään keskellä viesti ja tyhjennetään data.
                                centerText = "No chart data available"
                                setCenterTextSize(16f)
                                setCenterTextColor(android.graphics.Color.BLACK) // Muutettu suoraan mustaksi

                                // MUOKKAUS: Aseta tyhjä PieData-objekti nullin sijaan.
                                data = PieData(PieDataSet(emptyList(), "")) // <-- TÄMÄ ON

                            } else {
                                // --- KUN DATA EI OLE NOLLA ---
                                // Otetaan selite takaisin käyttöön ja muotoillaan se.
                                legend.isEnabled = true
                                legend.apply {
                                    textSize = 16f
                                    isWordWrapEnabled = true
                                    setMaxSizePercent(0.80f)
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
                                        override fun getFormattedValue(value: Float): String =
                                            String.format("%.1f h", value)
                                    }
                                }
                                data = PieData(set)
                            }

                            invalidate() // Piirretään kaavio uudelleen

                            // Toast-ilmoituksen logiikka pysyy samana.
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

                                override fun onNothingSelected() {}
                            })
                        }
                    }
                )
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
// Common settings for all bar charts.
private fun BarChart.applyBarDefaults(xLabels: List<String>) {

    // No description text.
    description = Description().apply { text = "" }

    // Right Y-axis off.
    axisRight.isEnabled = false
    legend.isEnabled = false

    // Allow touch/scroll.
    setTouchEnabled(true) // MUOKATTU aiemmin, pidetään näin

    // Removing the dark blue highlight.
    setHighlightPerTapEnabled(false)

    isHighlightPerDragEnabled = false

    // X-axis to the bottom.
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
    xAxis.textSize = 12f // <-- UUSI: Suurennetaan X-akselin tekstejä

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

// Convert BarPoint → BarEntry.
private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> =

    // X = index, Y = value.
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) }

// Create a unified BarDataSet with the same layout.
// Create a unified BarDataSet with the same layout.
private fun makeBarDataSet(

    // 'Series' name.
    label: String,

    // Column points.
    entries: List<BarEntry>,

    // Optional value formatter for the top of the column.
    valueFormatter: ValueFormatter? = null

// Return the configured dataset.
): BarDataSet = BarDataSet(entries, label).apply {

    // Column color.
    color = COLOR_PRIMARY_HEX

    // Set the text size for values on top of bars.
    this.valueFormatter = valueFormatter
    valueTextSize = 12f // <-- UUSI: Suurennetaan arvojen tekstejä
    valueTextColor = COLOR_TEXT_HEX
}



// 'UnitValueFormatter' formats column values ​​with a unit and decimal number.
private class UnitValueFormatter(
    private val unit: String,
    private val decimals: Int
) : ValueFormatter() {
    override fun getBarLabel(barEntry: BarEntry?): String {
        val v = barEntry?.y ?: return ""
        return if (decimals == 0) "${v.toInt()}${if (unit.isNotEmpty()) " $unit" else ""}"
        else "%.${decimals}f %s".format(v, unit).trim()
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
private fun stepStepFor(maxVal: Float): Float {

    // Clear thousands separator: 500, 1000, 2000, etc. MUTTA onko selkeä?
    return when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }
}

