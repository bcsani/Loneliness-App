package fi.tuni.lonelinessapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.repository.DayRepository
import fi.tuni.lonelinessapp.domain.model.CorrelationResult
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dayRepository: DayRepository,
    private val calculateCorrelationUseCase: CalculateCorrelationUseCase
) : ViewModel() {

    // Streak count
    private var _streakCount = MutableStateFlow(0)
    val streakCount = _streakCount

    private var _isResponded = MutableStateFlow(false)
    val isResponded = _isResponded

    // Loading variable for initializing the correlation results
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _correlationResults = MutableStateFlow<List<CorrelationResult>>(emptyList())
    val correlationResults: StateFlow<List<CorrelationResult>> = _correlationResults.asStateFlow()
    private val _signalAmount = MutableStateFlow(0)
    val signalAmount = _signalAmount.asStateFlow()

    fun getStreakCount() {
        viewModelScope.launch(Dispatchers.IO) {
            dayRepository.getStreak().collect { streak ->
                _streakCount.value = streak
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            dayRepository.isResponded().collect { isResponded ->
                _isResponded.value = isResponded
            }
        }
    }

    fun calculateCorrelation(){
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val results = calculateCorrelationUseCase()
                _correlationResults.value = results
                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                println("Failed to calculate correlations: ${e.message}")
            }
        }
    }

    fun getSignalAmount() {
        viewModelScope.launch {
            try {
                dayRepository.getSignalAmountToday().collect { signalAmount ->
                    println("Signal Amount $signalAmount")
                    _signalAmount.value = signalAmount ?: 0
                }
            } catch (e: Exception) {
                println("Failed to get signal amount: ${e.message}")
            }
        }
    }
}
