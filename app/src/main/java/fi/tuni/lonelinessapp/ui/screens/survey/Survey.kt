package fi.tuni.lonelinessapp.ui.screens.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SurveyDialog(
    onDismiss: () -> Unit,
    surveyViewModel: SurveyViewModel = viewModel()
) {

    val currentStep = surveyViewModel.currentStep.value

    val questions = listOf(
        "How often have you felt that you lack companionship during the past week?",
        "How often have you felt left out during past week?",
        "How often have you felt isolated from others during past week?"
    )
    val options = listOf("Often", "Sometimes", "Never")
    val optionValues = listOf(1, 2, 3)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Daily Survey",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(12.dp)
                )

                // Phase
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Question $currentStep of 3",
                        modifier = Modifier
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    StepProgressBar(currentStep, totalSteps = 3)
                }

                // Question
                Text(
                    text = questions[currentStep - 1],
                    modifier = Modifier.padding(8.dp)
                )

                // Options
                options.forEachIndexed { index, option ->
                    Button(
                        onClick = {
                            surveyViewModel.addAnswer(optionValues[index])
                            if (currentStep < 3) {
                                surveyViewModel.nextStep()
                            } else {
                                // ___________________
                                // Process the answers
                                // ___________________
                                surveyViewModel.resetSurvey()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(option)
                    }
                }

                // Back-button
                TextButton(onClick = {
                    if (currentStep == 1) {
                        surveyViewModel.resetSurvey()
                        onDismiss()
                    } else {
                        surveyViewModel.previousStep()
                        surveyViewModel.removeAnswer()
                    }
                }) {
                    Text("← Back")
                }
            }
        }
    }
}

@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) {
                            Color.Blue
                        }
                        else {
                            Color.Gray
                        }
                    )
            )
        }
    }
}