package fi.tuni.lonelinessapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.io.File
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
    val showResetDialog by settingsViewModel.showResetDialog
    val showAboutApp by settingsViewModel.showAboutApp
    val showAboutData by settingsViewModel.showAboutData

    // File launcher for exporting data
    val createFileLauncher = rememberLauncherForActivityResult(
        // Make csv file
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                // Content for file
                val content = formatContent(days)

                try {
                    // Download file
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(content.toByteArray())
                    }
                    // Notification, successful
                    Toast.makeText(
                        context,
                        "Export successful",
                        Toast.LENGTH_LONG
                    ).show()
                }
                catch (e : Exception) {
                    // Notification, failed
                    Toast.makeText(
                        context,
                        "Export failed ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ){
        // About App
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

        // About Data
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

        // Export Data
        item {
            Button(
                onClick = { createFileLauncher.launch(generateFileName()) },
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

        // Share Data
        item {
            Button(
                onClick = {
                    try {
                        // Make file to cache
                        val cacheFile = File(context.cacheDir, generateFileName())

                        // Content for file
                        cacheFile.writeText(formatContent(days))

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            cacheFile
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share file")
                        )
                    }
                    catch (e : Exception) {
                        Toast.makeText(
                            context,
                            "Share failed ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Share Data",
                    fontSize = 24.sp
                )
            }
        }

        // Reset
        item {
            Button(
                onClick = { settingsViewModel.openResetDialog() },
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
                // Top row with title and close button
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
                    Text(
                        text = "About App",
                        modifier = Modifier
                            .weight(2f)
                            .padding(4.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Close button
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Close")
                    }
                }

                // LazyColumn for text
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    item{
                        Text(
                            text = "Information about app:\n" +
                                    "Something about app and how it works",
                            modifier = Modifier
                                .weight(1f)
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
                // Top row with title and close button
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
                    Text(
                        text = "About App",
                        modifier = Modifier
                            .weight(2f)
                            .padding(4.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Close button
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Close")
                    }

                }
                // LazyColumn for text
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    item{
                        Text(
                            text = "Information about data:\n" +
                                    "Something about data and what data is collected",
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

fun generateFileName(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm"))
    return "data_${date}" + "_${time}.csv"
}

fun formatContent(days: List<DayEntity>?): String {
    return buildString {
        append(
            "Date," +
            "Loneliness," +
            "NightMinutes," +
            "DayMinutes," +
            "Steps," +
            "WhatApps," +
            "Messages," +
            "Calls," +
            "Signal," +
            "Telegram" +
            "\n")
        days?.forEach { day ->
            append(
                "${day.date}," +
                "${day.loneliness}," +
                "${day.nightMinutes}," +
                "${day.dayMinutes}," +
                "${day.steps}," +
                "${day.whatApps}," +
                "${day.messages}," +
                "${day.calls}," +
                "${day.signal}," +
                "${day.telegram}" +
                "\n")
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
            modifier = Modifier
                .fillMaxSize(),
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
                    // First reset-info popup
                    if (!isTimerRunning) {

                        // Info text
                        Text(
                            text = "Are you sure you want to reset the app?",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Info text
                        Text(
                            text = "This action will permanently clear all data from the app.",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Info text
                        Text(
                            text = "This action cannot be undone.",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Buttons in row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cancel button
                            Button(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Reset button
                            Button(
                                onClick = startTimer,
                                modifier = Modifier
                                    .weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Reset")
                            }
                        }
                    }

                    // Timer popup after first popup
                    else {

                        // Info text
                        Text(
                            text = "Reset in",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Time in seconds
                        Text(
                            text = "$timeLeft seconds",
                            modifier = Modifier
                                .padding(4.dp),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        // Cancel button
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
