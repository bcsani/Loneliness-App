package fi.tuni.lonelinessapp.ui.screens.settings

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

    // Data
    val daysEntity : StateFlow<List<DayEntity>?> = dayRepository.getAllDays()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}