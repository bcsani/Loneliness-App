package fi.tuni.lonelinessapp.ui.screens.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val dayRepository: DayRepository
) : ViewModel() {

    // Reset dialog visibility
    private val _showResetDialog = mutableStateOf(false)
    val showResetDialog: State<Boolean> = _showResetDialog

    fun openResetDialog() {
        _showResetDialog.value = true
    }

    fun closeResetDialog() {
        _showResetDialog.value = false
    }

    // Data
    val daysEntity : StateFlow<List<DayEntity>?> = dayRepository.getAllDays()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}