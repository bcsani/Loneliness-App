package fi.tuni.lonelinessapp.ui.screens.home

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.MainViewModel
import fi.tuni.lonelinessapp.ui.screens.survey.SurveyDialog

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel()
) {

    // Variable for daily survey dialog
    val showDialog by mainViewModel.showSurvey

    // Variable for streak count
    val streakCount by homeViewModel.streakCount

    LazyColumn(
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
                ){
                    Text(
                        text = "Loneliness Correlations",
                        modifier = modifier.align(Alignment.Center),
                        fontSize = 32.sp
                    )
                }
            }
        }

        // This is a temporary demo element
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
                    Text(
                        text = "This Week",
                        modifier = modifier.align(Alignment.Center),
                        fontSize = 32.sp
                    )
                }
            }
        }
    }

    if(showDialog){
        SurveyDialog(
            onDismiss = { mainViewModel.closeSurvey() }
        )
    }
}
