package fi.tuni.lonelinessapp.ui.screens.survey

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel

class SurveyViewModel : ViewModel() {

    // Variable for current step of the survey dialog
    private val _currentStep = mutableIntStateOf(1)
    val currentStep: State<Int> = _currentStep

    private var answers = mutableListOf<Int>()

    fun addAnswer(value: Int) {
        answers.add(value)
    }

    fun removeAnswer() {
        answers.removeAt(answers.size - 1)
    }

    fun nextStep() {
        _currentStep.intValue += 1
    }

    fun previousStep() {
        _currentStep.intValue -= 1
    }

    fun resetSurvey() {
        _currentStep.intValue = 1
        answers.clear()
    }

}