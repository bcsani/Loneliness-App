package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.MainViewModel
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
    surveyViewModel: SurveyViewModel
) {
    val showDialog by mainViewModel.showSurvey
    val streakCount by homeViewModel.streakCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

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
                ) {
                    Text(
                        text = "Home Screen",
                        modifier = modifier.align(Alignment.Center),
                        fontSize = 32.sp
                    )
                }
            }
        }

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
                ) {
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
                onClick = { mainViewModel.openSurvey() },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(COLOR_PRIMARY_HEX))
            ) {
                Text("Fill Daily Survey", fontSize = 20.sp)
            }
        }

        //  Loneliness Correlations Chart

        // This is a temporary demo element
        item {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Loneliness Correlations",
                        modifier = modifier.align(Alignment.Center),
                        fontSize = 32.sp
                    )
                }
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



        // CORRECTED SECTION: The if-statement is now inside an item block.
        item {
            if (showDialog) {
                SurveyDialog(
                    onDismiss = { mainViewModel.closeSurvey() },
                    surveyViewModel = surveyViewModel
                )
            }
        }
    }
}