package fi.tuni.lonelinessapp.ui.step

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.datasource.StepCounterDataSource
import fi.tuni.lonelinessapp.data.repository.StepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class StepViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StepRepository(StepCounterDataSource(application))

    val steps = repository.steps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

}