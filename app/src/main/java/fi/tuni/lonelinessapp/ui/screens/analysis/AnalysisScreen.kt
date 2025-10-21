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
import kotlin.math.ceil

// Värit on nyt kovakoodattuina. Myöhemmin siirretään teeman alle.
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

    // Kytketään myöhemmin oikeaan dataan.
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    // Haetaan viikon demodata ViewModelista.
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

    // Mon/Tue/ jne.
    val dayLabels     = remember { lonelinessPts.map { it.xLabel } }

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
                            xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(true)
                            xAxis.enableGridDashedLine(10f,10f,0f)

                            axisLeft.axisMinimum = 0f
                            axisLeft.axisMaximum = 3f
                            axisLeft.granularity = 0.5f
                            axisLeft.setLabelCount(7, true)
                            axisLeft.setDrawGridLines(true)
                            axisLeft.enableGridDashedLine(10f, 10f, 0f)

                            val entries = lonelinessPts.mapIndexed { i, p -> Entry(i.toFloat(), p.y) }

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

        // Taulukko 2: Night phone usage (bar chart) – tunneissa
        item {
            ChartCard(title = "Night Phone Usage (hours)") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply { // Konfiguroidaan pylväskaavio.
                            applyBarDefaults(dayLabels) // Aja yhteiset perusasetukset (x-akseli alas, ruudukko).

                            val hours = nightPts.map { it.y } // Ota y-arvot (tunnit) listaksi.
                            applyNiceYAxis(hours, ::hourStepFor) // Säädä vasen Y-akseli järkeviin pykäliin (0.25/0.5/1/2h).

                            val set = makeBarDataSet( // Luo yhtenäinen pylvässarja ulkoasuineen.
                                label = "Night usage", // Sarjan nimi (ei näy kun legend piilossa, mutta hyvä debuggaukseen).
                                entries = toBarEntries(nightPts), // Muunna pisteet BarEntryiksi.
                                valueFormatter = object : ValueFormatter() { // Muotoile pylvään yläpuolella näkyvä teksti.
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else String.format("%.1f h", e.y) // Näytä esim. “1.5 h”.
                                }
                            )

                            data = BarData(set).apply { barWidth = 0.5f } // Aseta data ja pylväiden leveys.
                            invalidate() // Piirrä kaavio.
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
                        BarChart(ctx).apply { // Konfiguroidaan pylväskaavio.
                            applyBarDefaults(dayLabels) // Yhteiset perusasetukset.

                            val hours = dayPts.map { it.y } // Ota y-arvot (tunnit) listaksi.
                            applyNiceYAxis(hours, ::hourStepFor) // Valitse pykäläko’oiksi tunnille sopivat stepit.

                            val set = makeBarDataSet( // Yhtenäinen datasets.
                                label = "Daytime usage", // Sarjan nimi.
                                entries = toBarEntries(dayPts), // Pisteet BarEntryiksi.
                                valueFormatter = object : ValueFormatter() { // Teksti pylvään päälle.
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else String.format("%.1f h", e.y) // Näytä “x.x h”.
                                }
                            )

                            data = BarData(set).apply { barWidth = 0.5f } // Aseta data ja leveys.
                            invalidate() // Piirrä.
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
                    factory = { ctx -> // Luo BarChart.
                        BarChart(ctx).apply { // Konfigurointi.
                            applyBarDefaults(dayLabels) // Yhteiset perusasetukset.

                            val values = stepsPts.map { it.y } // Ota askelarvot.
                            applyNiceYAxis(values, ::stepStepFor) // Pykälävalinta askelille (500/1000/2000/5000).

                            val set = makeBarDataSet( // Luo datasets.
                                label = "Steps", // Nimi.
                                entries = toBarEntries(stepsPts), // Pisteet BarEntryiksi.
                                valueFormatter = object : ValueFormatter() { // Muotoile pylvään arvo.
                                    override fun getBarLabel(e: BarEntry?): String =
                                        if (e == null) "" else "%,d".format(e.y.toInt()) // Tuhaterotin (esim. 10,500).
                                }
                            )

                            data = BarData(set).apply { barWidth = 0.5f } // Aseta data ja leveys.
                            invalidate() // Piirrä.
                        }
                    }
                )
            }
        }


        // Taulukko 5 Communication Apps Usage (pie chart).
        item {
            ChartCard(title = "Communication Apps Usage (hours)") {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            description = Description().apply { text = "" }
                            legend.isEnabled = true
                            setUsePercentValues(false) // EI prosentteja
                            setDrawEntryLabels(false)
                            isRotationEnabled = false // Estää pyörimisen
                            rotationAngle = 0f        // Aloitus ylös
                            animateY(0)               // Ei animaatiota

                            val entries = commPie.map { PieEntry(it.value, it.label) }
                            val set = PieDataSet(entries, "").apply {
                                colors = PIE_COLORS
                                valueTextSize = 12f
                                valueTextColor = COLOR_TEXT_HEX
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String =
                                        String.format("%.1f h", value)
                                }
                            }
                            data = PieData(set)
                            invalidate()

                            // Näytä sovelluksen nimi ja tuntimäärä Toastina
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

// Funktio 'Chartcard' luo yhtenäisen korttipohjan graafeille.
// Themes?
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

private fun BarChart.applyBarDefaults(xLabels: List<String>) { // Yhteiset asetukset kaikille pylväskaavioille.
    description = Description().apply { text = "" } // Ei description-tekstiä.
    axisRight.isEnabled = false // Oikea Y-akseli pois.
    legend.isEnabled = false // Legend piiloon (ei tarvita).
    setTouchEnabled(true) // Salli kosketus/scroll.

    xAxis.position = XAxis.XAxisPosition.BOTTOM // X-akseli alareunaan.
    xAxis.valueFormatter = IndexAxisValueFormatter(xLabels) // X-akselille annetut labelit järjestyksessä.
    xAxis.granularity = 1f // Väli yksi indeksi kerrallaan.
    xAxis.setDrawGridLines(true) // Piirrä pystysuorat apuviivat.
    xAxis.enableGridDashedLine(10f, 10f, 0f) // Apuviivoihin katkoviiva.

    axisLeft.setDrawGridLines(true) // Piirrä vaaka-apuviivat.
    axisLeft.enableGridDashedLine(10f, 10f, 0f) // Vaaka-apuviivoihin katkoviiva.
}


private fun BarChart.applyNiceYAxis(values: List<Float>, stepFn: (Float) -> Float) { // Säädä vasen Y-akseli arvojen mukaan.
    val maxVal = (values.maxOrNull() ?: 0f).coerceAtLeast(0f) // Suurin arvo tai 0.
    val step   = stepFn(maxVal) // Valitse järkevä pykälä (tunti/askel-logiikalla).
    val axisMax = niceCeil(maxVal * 1.15f, step) // Lisää 15% “päähän” ja pyöristä ylös pykälään.

    axisLeft.apply { // Aseta vasen Y-akseli.
        axisMinimum = 0f // Ala aina nollasta.
        axisMaximum = axisMax // Yläraja lasketun mukaan.
        granularity = step // Pykäläkoon väli.
        setLabelCount(((axisMax / step).toInt() + 1).coerceAtMost(10), true) // Rajoita labelien määrä max 10.
    }
}


private fun toBarEntries(points: List<AnalysisViewModel.BarPoint>): List<BarEntry> = // Muunna BarPoint → BarEntry.
    points.mapIndexed { i, p -> BarEntry(i.toFloat(), p.y) } // X = indeksi, Y = arvo.


private fun makeBarDataSet( // Luo yhtenäinen BarDataSet saman ulkoasun mukaan.
    label: String, // Sarjan nimi (hyödyllinen debuggauksessa).
    entries: List<BarEntry>, // Pylväspisteet.
    valueFormatter: ValueFormatter? = null // Valinnainen arvoformatoija pylvään yläpuolelle.
): BarDataSet = BarDataSet(entries, label).apply { // Palauta konfiguroitu dataset.
    color = COLOR_PRIMARY_HEX // Pylvään väri (pidetään kovakoodattuna nyt).
    valueTextColor = COLOR_TEXT_HEX // Pylvään päällä näkyvän tekstin väri.
    valueTextSize = 10f // Pylvään päällä näkyvän tekstin fonttikoko.
    if (valueFormatter != null) setValueFormatter(valueFormatter) // Jos formatti annettu, käytä sitä.
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

// Pyöristetään yläraja ylöspäin lähimpään 'step' kerrannaiseen
private fun niceCeil(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val k = kotlin.math.ceil(value / step)
    return (k * step)
}

// Valitaan järkevän väli tunneille max-arvon perusteella
private fun hourStepFor(maxVal: Float): Float =
    when {
        // 15 min välein
        maxVal <= 2f  -> 0.25f
        // 30 min
        maxVal <= 4f  -> 0.5f
        // 1 h
        maxVal <= 8f  -> 1f
        // 2 h
        else          -> 2f
    }

// Valitaan järkevä väli askelille.
private fun stepStepFor(maxVal: Float): Float {
    // Selkeä tuhansien väli: 500, 1000, 2000 jne.
    return when {
        maxVal <= 4_000f  -> 500f
        maxVal <= 12_000f -> 1_000f
        maxVal <= 20_000f -> 2_000f
        else              -> 5_000f
    }
}

