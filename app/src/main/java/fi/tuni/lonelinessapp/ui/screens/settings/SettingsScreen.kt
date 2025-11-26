package fi.tuni.lonelinessapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val days by settingsViewModel.daysEntity.collectAsState()
    var enableShare = false
    val showResetDialog by settingsViewModel.showResetDialog
    val showAboutApp by settingsViewModel.showAboutApp
    val showAboutData by settingsViewModel.showAboutData

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                // Download
                val content = formatContent(days)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
                }
                if (enableShare) {
                    // Share sheet
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, it)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share file"))
                }
                // Notification
                Toast.makeText(context, "Export successful", Toast.LENGTH_LONG).show()
            }
        }
    )

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ){
        // About App -item
        item {
            Button(
                onClick = { settingsViewModel.openShowAbout() },
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "About App",
                    fontSize = 24.sp
                )
            }
        }

        // About Data -item
        item {
            Button(
                onClick = { settingsViewModel.openShowData() },
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "About Data",
                    fontSize = 24.sp
                )
            }
        }

        item {
            Button(
                onClick = {
                    enableShare = false
                    createFileLauncher.launch("data_${LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}" +
                            "_${LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH-mm"))}.csv")},
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Export Data",
                    fontSize = 24.sp
                )
            }
        }

        item {
            Button(
                onClick = {
                    enableShare = true
                    createFileLauncher.launch("data_${LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}" +
                            "_${LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH-mm"))}.csv")
                },
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Export & Share Data",
                    fontSize = 24.sp
                )
            }
        }

        item {
            Button(
                onClick = {
                    settingsViewModel.openResetDialog()
                },
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Reset",
                    fontSize = 24.sp
                )
            }
        }
    }
    // Open popups when activated
    if(showResetDialog) {
        ResetDialog(onDismiss = {settingsViewModel.closeResetDialog()}, settingsViewModel)
    }
    if(showAboutApp) {
        AboutApp(onDismiss = {settingsViewModel.closeShowAbout()})
    }
    if(showAboutData) {
        AboutData(onDismiss = {settingsViewModel.closeShowData()})
    }
}

fun formatContent(days: List<DayEntity>?): String {
    return buildString {
        append("date,loneliness,nightMinutes,dayMinutes,steps\n")
        days?.forEach { day ->
            append("${day.date},${day.loneliness},${day.nightMinutes},${day.dayMinutes},${day.steps}\n")
        }
    }
}

@Composable
fun ResetDialog(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel

){
    // Time left in seconds
    var timeLeft by remember { mutableIntStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }

    val startTimer = {
        if(!isTimerRunning) {
            isTimerRunning = true

            object : CountDownTimer(15000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeft = (millisUntilFinished / 1000).toInt()
                }

                override fun onFinish() {
                    timeLeft = 0
                    isTimerRunning = false
                    settingsViewModel.resetData()
                    onDismiss()
                }
            }.start()
        }
    }



    Dialog(onDismissRequest =  onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Info text
                    if (!isTimerRunning) {
                        Text(
                            text = "Are you sure you want to reset the app?",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        // Info text
                        Text(
                            text = "This action will permanently clear all data from the app.",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        // Info text
                        Text(
                            text = "This action cannot be undone.",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cancel button
                            Button(
                                onClick = {
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Reset button
                            Button(
                                onClick = startTimer,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Reset")
                            }
                        }
                    } else {

                        Text(
                            text = "Reset in",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$timeLeft seconds",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        // Cancel reset
                        Button(
                            onClick = {
                                isTimerRunning = false
                                onDismiss()
                            },
                            modifier = Modifier

                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutApp(
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // top row with title and close button
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //title
                    Text(
                        text = "About App",
                        modifier = Modifier.weight(2f)
                            .padding(4.dp),
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                    Button(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                }
                // LazyColumn for text content
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    item{
                        Text(
                            text = "Information about app:\n" +
                                    "Something about app and how it works",
                            modifier = Modifier.weight(1f)

                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutData(
    onDismiss: () -> Unit,

    ) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // top row with title and close button
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // title
                    Text(
                        text = "About App",
                        modifier = Modifier.weight(2f)
                            .padding(4.dp),
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                }
                // LazyColumn for text content
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    item{
                        Text(
                            text = "Information about data:\n" +
                                    "Something about data and what data is collected",
                            modifier = Modifier.weight(1f)

                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}