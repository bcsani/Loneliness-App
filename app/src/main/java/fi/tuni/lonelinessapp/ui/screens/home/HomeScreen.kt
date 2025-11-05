package fi.tuni.lonelinessapp.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import fi.tuni.lonelinessapp.MainViewModel
import fi.tuni.lonelinessapp.domain.service.StepSensorManager
import fi.tuni.lonelinessapp.ui.screens.analysis.ChartCard
import fi.tuni.lonelinessapp.ui.screens.analysis.applyBarDefaults
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyViewModel

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    surveyViewModel: SurveyViewModel,
    context: Context = LocalContext.current
) {

    // Variable for daily survey dialog
    val showDialog by mainViewModel.showSurvey

    // Variable for streak count
    val streakCount by homeViewModel.streakCount

    // Variable for loading correlation chart and showing loading bar
    val isLoading by homeViewModel.isLoading.collectAsState()

    // Values for correlation chart
    val correlationResults by homeViewModel.correlationResults.collectAsState()
    LaunchedEffect(Unit) {
        homeViewModel.calculateCorrelation()
    }
//    val stepsToday by homeViewModel.stepsToday.collectAsState()


    val correlationValues = correlationResults.map { it.correlationValue }
    val entries = correlationValues.mapIndexed { index, value ->
        BarEntry(index.toFloat(), value.toFloat())
    }
    val labels = correlationResults.map { it.variableName }


//    var permissionGranted by remember {
//        mutableStateOf(
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACTIVITY_RECOGNITION
//            ) == PackageManager.PERMISSION_GRANTED
//        )
//    }

//    val stepManager = remember {
//        StepSensorManager(context) { steps ->
//            homeViewModel.onStepsUpdated(steps)
//        }
//    }
//
//    LaunchedEffect(permissionGranted) {
//        if(permissionGranted) {
//            println("Let's start tracking")
//            stepManager.startTracking()
//        }
//    }
//
//    DisposableEffect(Unit) {
//        println("Let's stop tracking")
//        onDispose {
//            stepManager.stopTracking()
//        }
//    }


    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (isLoading) {
            item {
                CircularProgressIndicator()
            }
        }
        // This is a temporary demo element
        item {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                ){
                    Text(
                        text = "Home Screen",
                        modifier = modifier.align(Alignment.Center),
                        fontSize = 32.sp
                    )
                }
            }
        }

        //Streak Card
        item {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                ){
                    Row(
                        modifier = modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Streak Icon",
                            modifier = modifier.size(64.dp)
                        )

                        Column(
                            modifier = modifier,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = streakCount.toString(),
                                modifier = modifier,
                                fontSize = 36.sp
                            )
                            Text(
                                text = "Day Streak",
                                modifier = modifier,
                                fontSize = 16.sp
                            )
                        }
                    }

                }
            }
        }

        // Survey Button
        item {
            Button(
                onClick = {mainViewModel.openSurvey()},
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Fill Daily Survey",
                    fontSize = 24.sp
                )
            }
        }

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
    }

    if(showDialog){
        SurveyDialog( onDismiss = { mainViewModel.closeSurvey()}, surveyViewModel = surveyViewModel)
    }
}
