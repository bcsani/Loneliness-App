package fi.tuni.lonelinessapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.data.entity.DayEntity

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val days by settingsViewModel.daysEntity.collectAsState()
    var enableShare = false

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
                    createFileLauncher.launch("data.csv")},
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
                    createFileLauncher.launch("data.csv")
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