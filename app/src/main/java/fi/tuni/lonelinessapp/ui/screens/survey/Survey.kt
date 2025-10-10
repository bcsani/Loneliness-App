package fi.tuni.lonelinessapp.ui.screens.survey

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SurveyDialog(
    onDismiss: () -> Unit,
    surveyViewModel: SurveyViewModel = viewModel()
) {
    val currentStep = surveyViewModel.currentStep.value
    val currentAnswer = surveyViewModel.getAnswer(currentStep)

    val question = surveyViewModel.questions[currentStep - 1]
    val options = surveyViewModel.options
    val optionValues = surveyViewModel.optionValues

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
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
                        .padding(4.dp)
                )

                // Phase
                Text(
                    "Question $currentStep of 3",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp)
                )

                // Question
                Text(
                    text = question,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                )

                // Options
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                surveyViewModel.setAnswer(
                                    currentStep,
                                    optionValues[index]
                                )
                            }
                    ) {
                        RadioButton(
                            selected = currentAnswer == optionValues[index],
                            onClick = {
                                surveyViewModel.setAnswer(
                                    currentStep,
                                    optionValues[index]
                                )
                            }
                        )
                        Text(text = option,
                            modifier = Modifier.
                                padding(start = 8.dp)
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back-button
                    Button(onClick = {
                            if (currentStep == 1) {
                                surveyViewModel.resetSurvey()
                                onDismiss()
                            } else {
                                surveyViewModel.removeAnswer(currentStep)
                                surveyViewModel.previousStep()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentStep == 1) {
                            Text("Cancel")
                        }
                        else {
                            Text("← Back")
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next-button
                    Button(
                        onClick = {
                            if (currentStep == 3) {
                                surveyViewModel.submitAnswers()
                                surveyViewModel.resetSurvey()
                                onDismiss()
                            } else {
                                surveyViewModel.nextStep()
                            }
                        },
                        enabled = currentAnswer != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentStep == 3) {
                            Text("Submit")
                        }
                        else {
                            Text("Next →")
                        }
                    }
                }
            }
        }
    }
}
