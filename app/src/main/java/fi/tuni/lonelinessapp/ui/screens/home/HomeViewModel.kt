package fi.tuni.lonelinessapp.ui.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.repository.DayRepository
import fi.tuni.lonelinessapp.domain.model.CorrelationResult
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val calculateCorrelationUseCase: CalculateCorrelationUseCase,
    private val dayRepository: DayRepository
) : ViewModel() {

    // Streak count
    private val _streakCount = mutableIntStateOf(3) // Hardcoded value for testing
    val streakCount: State<Int> = _streakCount

    // Loading variable for initializing the correlation results
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _correlationResults = MutableStateFlow<List<CorrelationResult>>(emptyList())
    val correlationResults: StateFlow<List<CorrelationResult>> = _correlationResults.asStateFlow()
//    private val _stepsToday = MutableStateFlow(0)
//    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    fun calculateCorrelation(){
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val results = calculateCorrelationUseCase()
                _correlationResults.value = results
                _isLoading.value = false

            } catch (e: Exception) {
                println("Failed to calculate correlations: ${e.message}")
            }
        }
    }

//    fun onStepsUpdated(steps: Int) {
//        _stepsToday.value = steps
//        viewModelScope.launch {
//            val today = LocalDate.now()
//            dayRepository.saveSteps(today, steps)
//        }
//    }
}
