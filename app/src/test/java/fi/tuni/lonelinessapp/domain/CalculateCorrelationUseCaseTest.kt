package fi.tuni.lonelinessapp.domain

import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.repository.DayRepository
import fi.tuni.lonelinessapp.domain.model.CorrelationResult
import fi.tuni.lonelinessapp.domain.usecase.CalculateCorrelationUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CalculateCorrelationUseCaseTest {
    private lateinit var dayRepository: DayRepository
    private lateinit var calculateCorrelationUseCase: CalculateCorrelationUseCase
    private val exampleTestDays = listOf(
        DayEntity(
            date = LocalDate.now(),
            loneliness = 3,
            nightMinutes = 40,
            dayMinutes = 480,
            steps = 8000,
            whatApps = 50,
            messages = 30,
            calls = 5,
            signal = 75,
            telegram = 20
        ),
        DayEntity(
            date = LocalDate.now().plusDays(1),
            loneliness = 5,
            nightMinutes = 60,
            dayMinutes = 600,
            steps = 12000,
            whatApps = 80,
            messages = 45,
            calls = 8,
            signal = 60,
            telegram = 35
        ),
        DayEntity(
            date = LocalDate.now().plusDays(2),
            loneliness = 7,
            nightMinutes = 80,
            dayMinutes = 420,
            steps = 10000,
            whatApps = 30,
            messages = 20,
            calls = 3,
            signal = 85,
            telegram = 15
        )
    )

    private val randomTestDays = listOf(
        DayEntity(
            date = LocalDate.now(),
            loneliness = 3,
            nightMinutes = 28,
            dayMinutes = 81,
            steps = 1049,
            whatApps = 74,
            messages = 86,
            calls = 15,
            signal = 45,
            telegram = 10
        ),
        DayEntity(
            date = LocalDate.now().plusDays(1),
            loneliness = 7,
            nightMinutes = 27,
            dayMinutes = 74,
            steps = 1112,
            whatApps = 46,
            messages = 88,
            calls = 38,
            signal = 88,
            telegram = 93
        ),
        DayEntity(
            date = LocalDate.now().plusDays(2),
            loneliness = 3,
            nightMinutes = 18,
            dayMinutes = 278,
            steps = 918,
            whatApps = 17,
            messages = 23,
            calls = 3,
            signal = 108,
            telegram = 104
        ),
    )

    @Before
    fun setup() {
        dayRepository = mockk()
        calculateCorrelationUseCase = CalculateCorrelationUseCase(dayRepository)
    }

    @Test
    fun `Calculate correlation should return correlation results for multiple days with valid data`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(exampleTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        assertEquals(8, results.size)
        assertTrue(results.all { it.correlationValue >= -1.0 && it.correlationValue <= 1.0 })

        // Check all variable names are present
        val variableNames = results.map { it.variableName }
        assertTrue(variableNames.contains("Night Minutes"))
        assertTrue(variableNames.contains("Day Minutes"))
        assertTrue(variableNames.contains("Steps"))
        assertTrue(variableNames.contains("WhatApps"))
        assertTrue(variableNames.contains("Messages"))
        assertTrue(variableNames.contains("Calls"))
        assertTrue(variableNames.contains("Signal"))
        assertTrue(variableNames.contains("Telegram"))
    }

    @Test
    fun `Calculate correlation should return correct correlation results with valid data`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(exampleTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.05
        for (result in results) {
            when (result.variableName) {
                "Night Minutes" -> {
                    assertEquals(1.00000, result.correlationValue, delta)
                }
                "Day Minutes" -> {
                    assertEquals(-0.34183, result.correlationValue, delta)
                }
                "Steps" -> {
                    assertEquals(0.52250, result.correlationValue, delta)
                }
                "WhatApps" -> {
                    assertEquals(-0.41502, result.correlationValue, delta)
                }
                "Messages" -> {
                    assertEquals(-0.41502	, result.correlationValue, delta)
                }
                "Calls" -> {
                    assertEquals(-0.41502, result.correlationValue, delta)
                }
                "Signal" -> {
                    assertEquals(0.41502, result.correlationValue, delta)
                }
                "Telegram" -> {
                    assertEquals(-0.25081, result.correlationValue, delta)
                }
            }
        }

    }

    @Test
    fun `Calculate correlation should return correct correlation results with random data`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(randomTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.05
        for (result in results) {
            when (result.variableName) {
                "Night Minutes" -> {
                    assertEquals(1.00000, result.correlationValue, delta)
                }
                "Day Minutes" -> {
                    assertEquals(-0.34183, result.correlationValue, delta)
                }
                "Steps" -> {
                    assertEquals(0.52250, result.correlationValue, delta)
                }
                "WhatApps" -> {
                    assertEquals(-0.41502, result.correlationValue, delta)
                }
                "Messages" -> {
                    assertEquals(-0.41502	, result.correlationValue, delta)
                }
                "Calls" -> {
                    assertEquals(-0.41502, result.correlationValue, delta)
                }
                "Signal" -> {
                    assertEquals(0.41502, result.correlationValue, delta)
                }
                "Telegram" -> {
                    assertEquals(-0.25081, result.correlationValue, delta)
                }
            }
        }
    }


    fun `Calculate correlation should return correct Pearson correlation results with valid data`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(exampleTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.05
        for (result in results) {
            when (result.variableName) {
                "Night Minutes" -> {
                    assertEquals(1.00000, result.correlationValue, delta)
                }
                "Day Minutes" -> {
                    assertEquals(-0.32733, result.correlationValue, delta)
                }
                "Steps" -> {
                    assertEquals(0.50000, result.correlationValue, delta)
                }
                "WhatApps" -> {
                    assertEquals(-0.39736, result.correlationValue, delta)
                }
                "Messages" -> {
                    assertEquals(-0.39736	, result.correlationValue, delta)
                }
                "Calls" -> {
                    assertEquals(-0.39736, result.correlationValue, delta)
                }
                "Signal" -> {
                    assertEquals(0.39736, result.correlationValue, delta)
                }
                "Telegram" -> {
                    assertEquals(-0.24019, result.correlationValue, delta)
                }
            }
        }

    }


    fun `Calculate correlation should return correct Pearson correlation results with random data`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(randomTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.05
        for (result in results) {
            when (result.variableName) {
                "Night Minutes" -> {
                    assertEquals(0.41931, result.correlationValue, delta)
                }
                "Day Minutes" -> {
                    assertEquals(-0.52594, result.correlationValue, delta)
                }
                "Steps" -> {
                    assertEquals(0.74964, result.correlationValue, delta)
                }
                "WhatApps" -> {
                    assertEquals(0.01013, result.correlationValue, delta)
                }
                "Messages" -> {
                    assertEquals(0.52325	, result.correlationValue, delta)
                }
                "Calls" -> {
                    assertEquals(0.94138, result.correlationValue, delta)
                }
                "Signal" -> {
                    assertEquals(0.20625, result.correlationValue, delta)
                }
                "Telegram" -> {
                    assertEquals(0.40444, result.correlationValue, delta)
                }
            }
        }

    }

    @Test
    fun `Calculate correlation should handle perfect positive correlation`() = runTest {
        val testDays = (1..10).map { dayNumber ->
            DayEntity(
                date = LocalDate.now().plusDays(dayNumber.toLong()),
                loneliness = dayNumber,
                nightMinutes = dayNumber,
                dayMinutes = dayNumber,
                steps = dayNumber * 1000,
                whatApps = dayNumber,
                messages = dayNumber,
                calls = dayNumber,
                signal = dayNumber,
                telegram = dayNumber
            )
        }

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.07

        // Assert - All correlations should be close to 1.0
        results.forEach { correlationResult ->
            assertEquals(1.0, correlationResult.correlationValue, delta)
        }
    }

    @Test
    fun `Calculate correlation should handle perfect negative correlation`() = runTest {
        val testDays = (1..10).map { dayNumber ->
            DayEntity(
                date = LocalDate.now().plusDays(dayNumber.toLong()),
                loneliness = dayNumber,
                nightMinutes = (11 - dayNumber),
                dayMinutes = (11 - dayNumber),
                steps = (11 - dayNumber) * 1000,
                whatApps = (11 - dayNumber),
                messages = (11 - dayNumber),
                calls = (11 - dayNumber),
                signal = (11 - dayNumber),
                telegram = (11 - dayNumber)
            )
        }

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.07

        // Assert - All correlations should be close to -1.0
        results.forEach { correlationResult ->
            assertEquals(-1.0, correlationResult.correlationValue, delta)
        }
    }

    @Test
    fun `Calculate correlation should handle no correlation`() = runTest {
        val testDays = (1..10).map { dayNumber ->
            DayEntity(
                date = LocalDate.now().plusDays(dayNumber.toLong()),
                loneliness = 3,
                nightMinutes = (11 - dayNumber),
                dayMinutes = (11 - dayNumber),
                steps = (11 - dayNumber) * 1000,
                whatApps = (11 - dayNumber),
                messages = (11 - dayNumber),
                calls = (11 - dayNumber),
                signal = (11 - dayNumber),
                telegram = (11 - dayNumber)
            )
        }

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.001

        // Assert - All correlations should be close to 1.0
        results.forEach { correlationResult ->
            assertEquals(0.0, correlationResult.correlationValue, delta)
        }
    }

    @Test
    fun `Calculate correlation should handle minimum (2) data points`() = runTest {
        val testDays = listOf(
            DayEntity(
                date = LocalDate.now(),
                loneliness = 3,
                nightMinutes = 40,
                dayMinutes = 480,
                steps = 8000,
                whatApps = 50,
                messages = 30,
                calls = 5,
                signal = 75,
                telegram = 20
            ),
            DayEntity(
                date = LocalDate.now().plusDays(1),
                loneliness = 5,
                nightMinutes = 60,
                dayMinutes = 600,
                steps = 12000,
                whatApps = 80,
                messages = 45,
                calls = 8,
                signal = 60,
                telegram = 35
            )
        )

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        assertEquals(8, results.size)
        assertTrue(results.all { it.correlationValue >= -1.0 && it.correlationValue <= 1.0 })
    }


    @Test
    fun `Calculate correlation should handle many data points`() = runTest {
        val testDays = (1..100).map { dayNumber ->
            DayEntity(
                date = LocalDate.now().plusDays(dayNumber.toLong()),
                loneliness = (dayNumber % 10),
                nightMinutes = (dayNumber * 5),
                dayMinutes = (dayNumber * 4),
                steps = dayNumber * 100,
                whatApps = (dayNumber * 2),
                messages = (dayNumber * 3),
                calls = dayNumber % 5,
                signal = (dayNumber % 100),
                telegram = (dayNumber % 20)
            )
        }

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        assertEquals(8, results.size)
        assertTrue(results.all { it.correlationValue >= -1.0 && it.correlationValue <= 1.0 })

        results.forEach { correlationResult ->
            val value = correlationResult.correlationValue
            assertTrue("Correlation value $value out of range", value >= -1.0 && value <= 1.0)
        }
    }

    @Test
    fun `Calculate correlation should handle zero variance in ordinal data`() = runTest {
        val testDays = (1..5).map { dayNumber ->
            DayEntity(
                date = LocalDate.now().plusDays(dayNumber.toLong()),
                loneliness = dayNumber,
                nightMinutes = 400,
                dayMinutes = 400,
                steps = 5000,
                whatApps = 50,
                messages = 30,
                calls = 4,
                signal = 75,
                telegram = 75
            )
        }

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.001

        results.forEach { correlationResult ->
            assertEquals(0.0, correlationResult.correlationValue, delta)
        }
    }

    @Test
    fun `Calculate correlation should return results in expected order`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(exampleTestDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        val expectedOrder = listOf(
            "Night Minutes",
            "Day Minutes",
            "Steps",
            "WhatApps",
            "Messages",
            "Calls",
            "Signal",
            "Telegram"
        )

        assertEquals(expectedOrder, results.map { it.variableName })
    }

    @Test
    fun `Calculate correlation should handle boundary values`() = runTest {
        val testDays = listOf(
            DayEntity(
                date = LocalDate.now(),
                loneliness = 0,
                nightMinutes = 0,
                dayMinutes = 0,
                steps = 0,
                whatApps = 0,
                messages = 0,
                calls = 0,
                signal = 0,
                telegram = 0
            ),
            DayEntity(
                date = LocalDate.now().plusDays(1),
                loneliness = 10,
                nightMinutes = 1440,
                dayMinutes = 1440,
                steps = 50000,
                whatApps = 1000,
                messages = 1000,
                calls = 100,
                signal = 100,
                telegram = 100
            )
        )

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()
        val delta = 0.07

        assertEquals(8, results.size)
        assertTrue(results.all { it.correlationValue >= -1.0 && it.correlationValue <= 1.0 })

        results.forEach { correlationResult ->
            assertEquals(1.0, correlationResult.correlationValue, delta)
        }
    }

    @Test
    fun `Calculate correlation should handle empty days list gracefully`() = runTest {
        coEvery { dayRepository.getAllDays() } returns flowOf(emptyList())

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `Calculate correlation should handle few null loneliness points`() = runTest {
        val testDays = listOf(
            DayEntity(
                date = LocalDate.now(),
                loneliness = 3,
                nightMinutes = 40,
                dayMinutes = 480,
                steps = 8000,
                whatApps = 50,
                messages = 30,
                calls = 5,
                signal = 75,
                telegram = 20
            ),
            DayEntity(
                date = LocalDate.now().plusDays(1),
                loneliness = 5,
                nightMinutes = 60,
                dayMinutes = 600,
                steps = 12000,
                whatApps = 80,
                messages = 45,
                calls = 8,
                signal = 60,
                telegram = 35
            ),
            DayEntity(
                date = LocalDate.now().plusDays(2),
                loneliness = null,
                nightMinutes = 80,
                dayMinutes = 420,
                steps = 10000,
                whatApps = 30,
                messages = 20,
                calls = 3,
                signal = 85,
                telegram = 15
            ),
            DayEntity(
                date = LocalDate.now().plusDays(3),
                loneliness = null,
                nightMinutes = 28,
                dayMinutes = 81,
                steps = 1049,
                whatApps = 74,
                messages = 86,
                calls = 15,
                signal = 45,
                telegram = 10
            ),
        )

        coEvery { dayRepository.getAllDays() } returns flowOf(testDays)

        val results: List<CorrelationResult> = calculateCorrelationUseCase()

        assertEquals(8, results.size)
        assertTrue(results.all { it.correlationValue >= -1.0 && it.correlationValue <= 1.0 })
    }
}