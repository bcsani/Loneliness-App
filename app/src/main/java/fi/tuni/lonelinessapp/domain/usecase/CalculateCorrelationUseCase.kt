package fi.tuni.lonelinessapp.domain.usecase

import fi.tuni.lonelinessapp.domain.model.CorrelationResult
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.flow.first
import kotlin.math.sqrt

class CalculateCorrelationUseCase (
    private val dayRepository: DayRepository
) {
    suspend operator fun invoke(): List<CorrelationResult> {
        // Get all days values from database
        val days = dayRepository.getAllDays().first()

        // Retrieve all data from days value
        val lonelinessData = days.map { it.loneliness.toDouble() }
        val nightMinutesData = days.map { it.nightMinutes.toDouble() }
        val dayMinutesData = days.map { it.dayMinutes.toDouble() }
        val stepsData = days.map { it.steps.toDouble()}

        // Calculate the correlations and return the results
        val results = listOfNotNull(
            calculatePearsonCorrelation(
                variableName = "Night Minutes",
                lonelinessData = lonelinessData,
                variableData = nightMinutesData
            ),
            calculatePearsonCorrelation(
                variableName = "Day Minutes",
                lonelinessData = lonelinessData,
                variableData = dayMinutesData
            ),
            calculatePearsonCorrelation(
                variableName = "Steps",
                lonelinessData = lonelinessData,
                variableData = stepsData
            )
        )

        return results
    }

    private fun calculatePearsonCorrelation(
        variableName: String,
        lonelinessData: List<Double>,
        variableData: List<Double>
    ): CorrelationResult? {
        if (lonelinessData.size != variableData.size || lonelinessData.size < 2) {
            return null
        }

        // Calculate correlation value and return with its own value and variable's name
        val correlationValue = calculatePearsonCoefficient(lonelinessData, variableData)

        return CorrelationResult(
            variableName= variableName,
            correlationValue = correlationValue
        )
    }

    private fun calculatePearsonCoefficient(x: List<Double>, y: List<Double>): Double {
        require(x.size == y.size) { "Datasets must have the same size" }
        require(x.size >= 2) {"At least 2 data points required"}

        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (xi, yi) -> xi * yi }
        val sumX2 = x.sumOf { it * it }
        val sumY2 = y.sumOf { it * it }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

        return if (denominator == 0.0) {
            0.0
        } else {
            (numerator / denominator).coerceIn(-1.0, 1.0)
        }
    }
}