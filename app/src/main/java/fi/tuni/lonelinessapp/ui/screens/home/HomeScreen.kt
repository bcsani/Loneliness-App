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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun HomeScreen(modifier: Modifier = Modifier, homeScreenViewModel: HomeScreenViewModel) {
    // Variable for daily survey dialog
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
                    .height(100.dp),
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
                                text = "2", // Temporarily hardcoded value, to be replaced later
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
                onClick = {showDialog = true},
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
    }

    if(showDialog){
        SurveyDialog(
            onDismiss = {showDialog = false},
            viewModel = homeScreenViewModel
        )
    }
}
