package fi.tuni.lonelinessapp.ui.screens.settings

import android.content.ClipData
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.LocalDate

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val days by settingsViewModel.daysEntity.collectAsState()

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri: Uri? ->
            uri?.let {
                val content = buildString {
                    days?.forEach { day ->
                        append(day.toString() + "\n")
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
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
        item {
            Button(
                onClick = {createFileLauncher.launch("data.txt")},
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "Data Export",
                    fontSize = 24.sp
                )
            }
        }
    }
}