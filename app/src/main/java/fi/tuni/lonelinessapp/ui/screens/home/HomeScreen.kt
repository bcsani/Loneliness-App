package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import fi.tuni.lonelinessapp.MainViewModel
import fi.tuni.lonelinessapp.ui.screens.analysis.ChartCard
import fi.tuni.lonelinessapp.ui.screens.analysis.applyBarDefaults
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape

private const val COLOR_PRIMARY_HEX = 0xFF2563EB.toInt()
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    surveyViewModel: SurveyViewModel,
) {
    val showDialog by mainViewModel.showSurvey
    val streakCount by homeViewModel.streakCount

    // Variable for loading correlation chart and showing loading bar
    val isLoading by homeViewModel.isLoading.collectAsState()

    // Values for correlation chart
    val correlationResults by homeViewModel.correlationResults.collectAsState()
    LaunchedEffect(Unit) {
        homeViewModel.calculateCorrelation()
    }

    val correlationValues = correlationResults.map { it.correlationValue }
    val entries = correlationValues.mapIndexed { index, value ->
        BarEntry(index.toFloat(), value.toFloat())
    }
    val labels = correlationResults.map { it.variableName }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
                    .border(1.dp, Color.Gray, RoundedCornerShape(16.dp)), // 👈 reunus lisätty ,
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
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = "Streak Icon",
                        tint = Color(0xFFFF9800), // Orange
                        modifier = Modifier.size(40.dp)
                    )

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(COLOR_PRIMARY_HEX))
            ) {
                Text("Fill Daily Survey", fontSize = 20.sp)
            }
        }

        //  Loneliness Correlations Chart
        // Correlation chart
        item {
            ChartCard(title = "Loneliness correlations") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { context ->

                        // Creating a bar chart.
                        BarChart(context).apply {

                            // Common basic settings.
                            applyBarDefaults(labels)

                            axisLeft.apply {
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

                            val dataSet = BarDataSet(entries, "Correlation").apply {
                                setDrawValues(false)
                                color = 0xFF4169E1.toInt()
                            }


                            // Set the data and width.
                            data = BarData(dataSet).apply {
                                xAxis.textSize = 14f
                                barWidth = 0.8f
                            }

                            // Draw the graph.
                            invalidate()
                        }
                    }
                )
            }
        }

        if (showDialog) {
            item {
                SurveyDialog(
                    onDismiss = { mainViewModel.closeSurvey() },
                    surveyViewModel = surveyViewModel
                )
            }
        }
    }
}


