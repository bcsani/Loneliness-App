package fi.tuni.lonelinessapp.ui.screens.analysis

// Compose
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// 1) Loneliness Level (line)
// 2) Night Phone Usage (bar)  -> tunneissa (1 desimaali)
// 3) Daytime Phone Usage (bar)-> tunneissa (1 desimaali)
// 4) Exercise (steps) (bar)
// 5) Communication Apps Usage (pie)

@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,

    // Kytketään myöhemmin oikeaan dataan.
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    // Testidata.
    val days = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    // Testidata kyselyn tulokset.
    val loneliness = listOf(2.4f, 1.8f, 2.1f, 1.5f, 1.2f, 1.0f, 1.4f)

    // Testidata puhelimen käyttö yöllä.
    val nightMinutes = listOf(38f, 29f, 47f, 22f, 35f, 54f, 31f)

    // Testidata puhelimen käyttö yöllä.
    val dayMinutes   = listOf(165f, 150f, 180f, 140f, 172f, 210f, 580f)

    // Testidata askeleet.
    val steps = listOf(8000f, 9000f, 7500f, 10000f, 8200f, 20000f, 11000f)

    // Testidata sovellusten käyttö: prosenttiosuuksina.
    val commApps = linkedMapOf(
        "WhatsApp" to 39f,
        "Messages" to 28f,
        "Calls" to 16f,
        "Signal" to 10f,
        "Telegram" to 7f
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Taulukko 1: Loneliness LeveL (line chart)
        item {
            ChartCard(title = "Loneliness Level (This Week)") {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            description = Description().apply { text = "" }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)
                            setPinchZoom(false)

                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(days)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f,10f,0f)

                            axisLeft.axisMinimum = 0f
                            axisLeft.axisMaximum = 3f
                            axisLeft.granularity = 0.5f
                            axisLeft.setLabelCount(7, true)
                            axisLeft.setDrawGridLines(true)
                            axisLeft.enableGridDashedLine(10f, 10f, 0f)

                            val entries = loneliness.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                            val set = LineDataSet(entries, "Loneliness").apply {
                                color = 0xFF2563EB.toInt()
                                setCircleColor(0xFF2563EB.toInt())
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

        // Taulukko 2: Night phone usage (bar chart) – tunneissa
        item {
            ChartCard(title = "Night Phone Usage (hours)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description = Description().apply { text = "" }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)

                            // X-akseli
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(days)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f, 10f, 0f)

                            // MINUUTIT -> TUNNIT ja mukautuva akseli
                            val hours = nightMinutes.map { it / 60f }
                            val maxH  = (hours.maxOrNull() ?: 0f).coerceAtLeast(0f)
                            val step  = hourStepFor(maxH)
                            val axisMax = niceCeil(maxH * 1.15f, step)

                            axisLeft.apply {
                                axisMinimum = 0f
                                axisMaximum = axisMax
                                granularity = step
                                setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true)
                                setDrawGridLines(true)
                                enableGridDashedLine(10f, 10f, 0f)
                            }

                            val entries = hours.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
                            val set = BarDataSet(entries, "Night usage").apply {
                                color = 0xFF2563EB.toInt()
                                valueTextColor = 0xFF1F2937.toInt()
                                valueTextSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else String.format("%.1f h", e.y)
                                }
                            }
                            data = BarData(set).apply { barWidth = 0.5f }

                            invalidate()
                        }
                    }
                )
            }
        }


        // Taulukko 3: Daytime phone usage (bar chart) – tunneissa.
        item {
            ChartCard(title = "Daytime Phone Usage (6am–10pm, hours)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description = Description().apply { text = "" }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)

                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(days)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f, 10f, 0f)

                            // MINUUTIT -> TUNNIT ja mukautuva akseli
                            val hours = dayMinutes.map { it / 60f }
                            val maxH  = (hours.maxOrNull() ?: 0f).coerceAtLeast(0f)
                            val step  = hourStepFor(maxH)
                            val axisMax = niceCeil(maxH * 1.15f, step)

                            axisLeft.apply {
                                axisMinimum = 0f
                                axisMaximum = axisMax
                                granularity = step
                                setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true)
                                setDrawGridLines(true)
                                enableGridDashedLine(10f, 10f, 0f)
                            }

                            val entries = hours.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
                            val set = BarDataSet(entries, "Daytime usage").apply {
                                color = 0xFF2563EB.toInt()
                                valueTextColor = 0xFF1F2937.toInt()
                                valueTextSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else String.format("%.1f h", e.y)
                                }
                            }
                            data = BarData(set).apply { barWidth = 0.5f }

                            invalidate()
                        }
                    }
                )
            }
        }




        // Taulukko 4: Exercise (bar chart).
        item {
            ChartCard(title = "Exercise (steps)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description = Description().apply { text = "" }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)

                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(days)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f, 10f, 0f)

                            // Mukautuva Y-akseli askelille
                            val maxSteps = (steps.maxOrNull() ?: 0f).coerceAtLeast(0f)
                            val stepS    = stepStepFor(maxSteps)
                            val axisMaxS = niceCeil(maxSteps * 1.15f, stepS)

                            axisLeft.apply {
                                axisMinimum = 0f
                                axisMaximum = axisMaxS
                                granularity = stepS
                                setLabelCount(((axisMaxS / stepS).toInt() + 1).coerceAtMost(10), true)
                                setDrawGridLines(true)
                                enableGridDashedLine(10f, 10f, 0f)
                            }

                            val entries = steps.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
                            val set = BarDataSet(entries, "Steps").apply {
                                color = 0xFF2563EB.toInt()
                                valueTextColor = 0xFF1F2937.toInt()
                                valueTextSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else "%,d".format(e.y.toInt())
                                }
                            }
                            data = BarData(set).apply { barWidth = 0.5f }

                            invalidate()
                        }
                    }
                )
            }
        }


        // Taulukko 5 Communication Apps Usage (pie chart).
        item {
            ChartCard(title = "Communication Apps Usage") {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            description = Description().apply { text = "" }
                            legend.isEnabled = true
                            setUsePercentValues(true)
                            setDrawEntryLabels(false)

                            val entries = commApps.map { PieEntry(it.value, it.key) }
                            val set = PieDataSet(entries, "").apply {
                                // yksinkertainen väripaletti (voit vaihtaa brändiin)
                                colors = listOf(
                                    // blue.
                                    0xFF2563EB.toInt(),
                                    // amber.
                                    0xFFF59E0B.toInt(),
                                    // emerald.
                                    0xFF10B981.toInt(),
                                    // violet.
                                    0xFFA855F7.toInt(),
                                    // red.
                                    0xFFEF4444.toInt()
                                )
                                valueTextSize = 12f
                                valueTextColor = 0xFF1F2937.toInt()
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val totalMinutes = 420f
                                        val minutes = (totalMinutes * (value / 100f))
                                        val hoursPart = minutes.toInt() / 60
                                        val minsPart = (minutes % 60).toInt()
                                        return "%dh %02dmin".format(hoursPart, minsPart)
                                    }
                                }
                            }
                            data = PieData(set)
                            invalidate()
                            // Näytä sovelluksen nimi Toastina kun viipaletta painetaan
                            setOnChartValueSelectedListener(object :
                                com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                                override fun onValueSelected(
                                    e: com.github.mikephil.charting.data.Entry?,
                                    h: com.github.mikephil.charting.highlight.Highlight?
                                ) {
                                    if (e is PieEntry) {
                                        val label = e.label          // Sovelluksen nimi (esim. "WhatsApp")
                                        val value = e.value          // Käyttöaika minuutteina (float)
                                        val hours = value.toInt() / 60
                                        val mins = (value % 60).toInt()
                                        android.widget.Toast.makeText(
                                            context,
                                            "$label – %dh %02dmin".format(hours, mins),
                                            android.widget.Toast.LENGTH_LONG
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

// Funktio 'Chartcard' luo yhtenäisen korttipohjan graafeille.
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

// 'UnitValueFormatter' muotoilee pylvään arvot yksiköllä ja desimaalimäärällä.
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

// Pyöristää ylärajan nätisti ylöspäin lähimpään 'step' kerrannaiseen
private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = kotlin.math.ceil(value / step)
    return (k * step)
}

// Valitsee järkevän stepin tunneille max-arvon perusteella
private fun hourStepFor(maxVal: Float): Float =
    when {
        maxVal <= 2f  -> 0.25f   // 15 min välein
        maxVal <= 4f  -> 0.5f    // 30 min
        maxVal <= 8f  -> 1f      // 1 h
        else          -> 2f      // 2 h
    }

// Valitsee järkevän stepin askelille
private fun stepStepFor(maxVal: Float): Float {
    // “kaunis” tuhansien väli: 500, 1000, 2000 jne.
    return when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }
}

