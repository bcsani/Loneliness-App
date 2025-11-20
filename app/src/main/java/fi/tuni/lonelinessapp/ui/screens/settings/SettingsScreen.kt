package fi.tuni.lonelinessapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    if(showResetDialog) {
        ResetDialog(onDismiss = {settingsViewModel.closeResetDialog()})
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
    onDismiss: () -> Unit

){
    Dialog(onDismissRequest =  onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ){
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Info text
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
                    Button(onClick = {
                        onDismiss()
                    },
                        modifier = Modifier.weight(1f)
                    ) {
                       Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reset button
                    Button(
                        onClick = {

                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}