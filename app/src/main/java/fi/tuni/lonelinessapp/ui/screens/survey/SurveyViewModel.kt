package fi.tuni.lonelinessapp.ui.screens.survey

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SurveyViewModel(
    private val dayRepository: DayRepository
) : ViewModel() {

    // Variable for current step of the survey dialog
    private val _currentStep = mutableIntStateOf(1)
    val currentStep: State<Int> = _currentStep

    private val _answers = mutableStateListOf<Int?>(null, null, null)

    val questions = listOf(
        "How often have you felt that you lack companionship during the past week?",
        "How often have you felt left out during past week?",
        "How often have you felt isolated from others during past week?"
    )
    val options = listOf("Often", "Sometimes", "Never")
    val optionValues = listOf(3, 2, 1)

    fun setAnswer(step: Int, value: Int) {
        if (step in 1.._answers.size) {
            _answers[step - 1] = value
        }
    }

    fun getAnswer(step: Int): Int? {
        return _answers.getOrNull(step - 1)
    }

    fun removeAnswer(step: Int) {
        if (step in 1.._answers.size) {
            _answers[step - 1] = null
        }
    }

    fun nextStep() {
        if (_currentStep.intValue < _answers.size) {
            _currentStep.intValue += 1
        }
    }

    fun previousStep() {
        if (_currentStep.intValue > 1) {
            _currentStep.intValue -= 1
        }
    }

    fun resetSurvey() {
        _currentStep.intValue = 1
        _answers.clear()
        repeat(questions.size) { _answers.add(null) }
    }

    fun submitAnswers() {
        val date = LocalDate.now()
        val loneliness = _answers.sumOf{it ?: 0}

        viewModelScope.launch(Dispatchers.IO) {
            dayRepository.saveLoneliness(date, loneliness)
        }
    }
}