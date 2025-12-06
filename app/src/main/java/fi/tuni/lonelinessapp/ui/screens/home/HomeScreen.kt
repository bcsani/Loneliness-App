package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import fi.tuni.lonelinessapp.MainViewModel
import fi.tuni.lonelinessapp.ui.screens.analysis.ChartCard
import fi.tuni.lonelinessapp.ui.screens.analysis.applyBarDefaults
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyViewModel
import fi.tuni.lonelinessapp.ui.theme.primaryBlue

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    surveyViewModel: SurveyViewModel
) {
    val showDialog by mainViewModel.showSurvey
    val streakCount by homeViewModel.streakCount.collectAsState()
    val isResponded by homeViewModel.isResponded.collectAsState()
    var infoDialogMessage by remember { mutableStateOf<String?>(null)}
    var selectedBarMessage by remember { mutableStateOf<String?>(null) }

    // Variable for loading correlation chart and showing loading bar
    val isLoading by homeViewModel.isLoading.collectAsState()

    // Values for correlation chart
    val correlationResults by homeViewModel.correlationResults.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.getStreakCount()
        homeViewModel.calculateCorrelation()
    }

    val correlationValues = correlationResults.map { it.correlationValue }
    val labels = correlationResults.map { it.variableName }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Loading indicator
        if (isLoading) {
            item {
                CircularProgressIndicator()
            }
        }

        //  Streak Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(16.dp)),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Fire icon
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = "Streak Icon",
                        tint = Color(0xFFFF9800), // Orange
                        modifier = Modifier.size(40.dp)
                    )

                    // Streak number and text
                    Column {
                        Text(
                            text = streakCount.toString(),
                            fontSize = 36.sp,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Day Streak",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        //  Fill Daily Survey Button
        item {
            Button(
                onClick = { mainViewModel.openSurvey() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(primaryBlue)
            ) {
                if (!isResponded) {
                    Text("Fill Daily Survey", fontSize = 20.sp)
                } else {
                    Text("Refill Daily Survey", fontSize = 20.sp)
                }
            }
        }

        //  Loneliness Correlations Chart
        item {
            val infoText = "Loneliness correlations, " +
                           "shows how different things correlate with experienced loneliness"
            val title = "Correlation"
            ChartCard(
                title = title,
                onInfoClick = { infoDialogMessage = infoText }
            )  {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { context ->

                        // Creating a bar chart.
                        BarChart(context).apply {

                            // Common basic settings.
                            //applyBarDefaults(labels)
                            applyBarDefaults()

                            // Can touch
                            setTouchEnabled(true)
                            isHighlightPerTapEnabled = true
                            setScaleEnabled(false) // zoom not in use

                            axisLeft.apply {
                                xAxis.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val index = value.toInt()
                                        return labels.getOrNull(index) ?: ""
                                    }
                                }
                                xAxis.labelRotationAngle = -90f

                                // Left Y-axis limits
                                axisMinimum = -0.1f
                                axisMaximum = 0.1f

                                // Step
                                granularity = 0.05f

                                textSize = 14f

                                // Add zero line configuration
                                setDrawZeroLine(true)
                                zeroLineWidth = 2f
                            }

                            val entries = correlationValues.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), value.toFloat())
                            }

                            val dataSet = BarDataSet(entries, "Correlation").apply {
                                setDrawValues(false)
                                color = primaryBlue.toArgb()
                            }

                            // Set the data and width.
                            data = BarData(dataSet).apply {
                                xAxis.textSize = 14f
                                barWidth = 0.8f
                            }
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    if (e != null && h != null) {
                                        val index = h.x.toInt()
                                        val variableName = labels[index]
                                        val value = e.y

                                        selectedBarMessage = "$variableName: correlation = $value"
                                    }
                                }
                                override fun onNothingSelected() { }
                            })

                            // Draw the graph.
                            invalidate()
                        }
                    }
                )
            }
        }
    }

    // Survey
    if (showDialog) {
        SurveyDialog(
            onDismiss = { mainViewModel.closeSurvey() },
            surveyViewModel = surveyViewModel
        )
    }

    // Chart info message
    if (infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { infoDialogMessage = null },
            confirmButton = {
                TextButton(onClick = { infoDialogMessage = null }) {
                    Text("OK")
                }
            },
            title = { Text("Info") },
            text = { Text(infoDialogMessage!!) }
        )
    }

    // Bar info message
    if (selectedBarMessage != null) {
        AlertDialog(
            onDismissRequest = { selectedBarMessage = null },
            confirmButton = {
                TextButton(onClick = { selectedBarMessage = null }) {
                    Text("OK")
                }
            },
            title = { Text("Correlation detail") },
            text = { Text(selectedBarMessage!!) }
        )
    }
}
