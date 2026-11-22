package fi.tuni.lonelinessapp.domain.usecase

import fi.tuni.lonelinessapp.domain.model.CorrelationResult
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

class CalculateCorrelationUseCase (
    private val dayRepository: DayRepository
) {
    suspend operator fun invoke(): List<CorrelationResult> {
        // Get all days values from database
        val days = dayRepository.getAllDays().first()

        // Retrieve all data from days value
        val lonelinessData = days.map { it.loneliness?.toDouble() }
        val nightMinutesData = days.map { it.nightMinutes?.toDouble() }
        val dayMinutesData = days.map { it.dayMinutes?.toDouble() }
        val stepsData = days.map { it.steps?.toDouble() }
        val whatApps = days.map {it.whatApps?.toDouble() }
        val messages = days.map {it.messages?.toDouble() }
        val calls = days.map{it.calls?.toDouble() }
        val signal = days.map{it.signal?.toDouble() }
        val telegram = days.map{it.telegram?.toDouble() }

        // Calculate the correlations and return the results
        val variablePairs = listOf(
            "Night Minutes" to nightMinutesData,
            "Day Minutes" to dayMinutesData,
            "Steps" to stepsData,
            "WhatsApp" to whatApps,
            "Messages" to messages,
            "Calls" to calls,
            "Signal" to signal,
            "Telegram" to telegram
        )

        val results = variablePairs.mapNotNull { (variableName, variableData) ->
            val zipped = variableData.zip(lonelinessData).filter { (i, j) -> i != null && j != null }
            val independent = zipped.map { (i, _) -> i!! }
            val loneliness = zipped.map { (_, j) -> j!! }
            calculatePolyserialCorrelation(
                variableName = variableName,
                continuousData = independent,
                ordinalData = loneliness
            )
        }

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

    private fun calculatePolyserialCorrelation(
        variableName: String,
        continuousData: List<Double>, // Continuous variable
        ordinalData: List<Double>     // Ordinal variable (discrete values representing categories)
    ): CorrelationResult? {
        if (continuousData.size < 2) {
            return null
        }

        // Calculate polyserial correlation value
        val correlationValue = calculatePolyserialCoefficient(continuousData, ordinalData)

        return CorrelationResult(
            variableName = variableName,
            correlationValue = correlationValue
        )
    }

    private fun calculatePolyserialCoefficient(
        continuousData: List<Double>,
        ordinalData: List<Double>
    ): Double {
        require(continuousData.size == ordinalData.size) { "Datasets must have the same size" }
        require(continuousData.size >= 2) { "At least 2 data points required" }

        // Get unique ordinal values and sort them
        val uniqueOrdinalValues = ordinalData.distinct().sorted()
        val nCategories = uniqueOrdinalValues.size

        if (nCategories < 2 || continuousData.all { it == continuousData.first() }) {
            return 0.0 // No variation in one of the variables
        }

        // Calculate thresholds (z-scores for category boundaries)
        val thresholds = calculateThresholds(ordinalData, uniqueOrdinalValues)

        // Calculate polyserial correlation using maximum likelihood estimation
        return estimatePolyserialCorrelation(continuousData, ordinalData, thresholds, uniqueOrdinalValues)
    }

    private fun calculateThresholds(
        ordinalData: List<Double>,
        uniqueOrdinalValues: List<Double>
    ): List<Double> {
        val n = ordinalData.size.toDouble()
        val thresholds = mutableListOf<Double>()

        // Calculate cumulative proportions
        var cumulativeProportion = 0.0
        for (i in 0 until uniqueOrdinalValues.size - 1) {
            val category = uniqueOrdinalValues[i]
            val proportion = ordinalData.count { it == category } / n
            cumulativeProportion += proportion
            // Convert proportion to z-score using inverse normal CDF approximation
            thresholds.add(inverseNormalCDF(cumulativeProportion))
        }

        return thresholds
    }

    private fun estimatePolyserialCorrelation(
        continuousData: List<Double>,
        ordinalData: List<Double>,
        thresholds: List<Double>,
        uniqueOrdinalValues: List<Double>
    ): Double {
        val mean = continuousData.average()
        val std = sqrt(continuousData.variance())
        val standardized = continuousData.map { (it - mean) / std }
        val categories = ordinalData.map { uniqueOrdinalValues.indexOf(it) }

        fun logLikelihood(rho: Double): Double {
            val scale = sqrt(1 - rho * rho)
            return standardized.indices.sumOf { i ->
                val category = categories[i]
                val lower = if (category == 0) 0.0 else
                    normalCDF((thresholds[category - 1] - rho * standardized[i]) / scale)
                val upper = if (category == thresholds.size) 1.0 else
                    normalCDF((thresholds[category] - rho * standardized[i]) / scale)
                ln((upper - lower).coerceAtLeast(1e-15))
            }
        }

        // Bracket the best region before refining the bounded likelihood maximum.
        val minRho = -0.99
        val step = 1.98 / 100
        val bestIndex = (0..100).maxByOrNull { logLikelihood(minRho + it * step) }!!
        var left = minRho + (bestIndex - 1).coerceAtLeast(0) * step
        var right = minRho + (bestIndex + 1).coerceAtMost(100) * step
        val ratio = (sqrt(5.0) - 1) / 2
        var a = right - ratio * (right - left)
        var b = left + ratio * (right - left)
        var scoreA = logLikelihood(a)
        var scoreB = logLikelihood(b)
        repeat(40) {
            if (scoreA < scoreB) {
                left = a
                a = b
                scoreA = scoreB
                b = left + ratio * (right - left)
                scoreB = logLikelihood(b)
            } else {
                right = b
                b = a
                scoreB = scoreA
                a = right - ratio * (right - left)
                scoreA = logLikelihood(a)
            }
        }
        return (left + right) / 2
    }

    // Helper functions for normal distribution
    private fun normalCDF(x: Double): Double {
        // Approximation of normal CDF
        return 0.5 * (1 + erf(x / sqrt(2.0)))
    }

    private fun inverseNormalCDF(p: Double): Double {
        // Approximation of inverse normal CDF
        require(p in 0.0..1.0) { "Probability must be between 0 and 1" }
        if (p == 0.0) return Double.NEGATIVE_INFINITY
        if (p == 1.0) return Double.POSITIVE_INFINITY

        val a1 = -39.6968302866538
        val a2 = 220.946098424521
        val a3 = -275.928510446969
        val a4 = 138.357751867269
        val a5 = -30.6647980661472
        val a6 = 2.50662827745924

        val b1 = -54.4760987982241
        val b2 = 161.585836858041
        val b3 = -155.698979859887
        val b4 = 66.8013118877197
        val b5 = -13.2806815528857

        val c1 = -7.78489400243029E-03
        val c2 = -0.322396458041136
        val c3 = -2.40075827716184
        val c4 = -2.54973253934373
        val c5 = 4.37466414146497
        val c6 = 2.93816398269878

        val d1 = 7.78469570904146E-03
        val d2 = 0.32246712907004
        val d3 = 2.445134137143
        val d4 = 3.75440866190742

        val p_low = 0.02425
        val p_high = 1.0 - p_low

        return when {
            p < p_low -> {
                val q = sqrt(-2.0 * ln(p))
                (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                        ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0)
            }
            p <= p_high -> {
                val q = p - 0.5
                val r = q * q
                (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q /
                        (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - p))
                -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                        ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0)
            }
        }
    }

    // Extension function for variance
    private fun List<Double>.variance(): Double {
        val mean = this.average()
        return this.map { (it - mean) * (it - mean) }.average()
    }

    // Error function approximation
    private fun erf(x: Double): Double {
        // Approximation of error function
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1 else 1
        val absX = abs(x)

        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)

        return sign * y
    }
}