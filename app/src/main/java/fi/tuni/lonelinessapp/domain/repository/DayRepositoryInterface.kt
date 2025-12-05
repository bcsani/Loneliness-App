package fi.tuni.lonelinessapp.domain.repository

import fi.tuni.lonelinessapp.data.entity.DayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DayRepositoryInterface {
    suspend fun insertDay(day: DayEntity)

    suspend fun saveLoneliness(date: LocalDate = LocalDate.now(), loneliness: Int)

    suspend fun saveNightMinutes(date: LocalDate = LocalDate.now(), nightMinutes: Int)

    suspend fun saveDayMinutes(date: LocalDate = LocalDate.now(), dayMinutes: Int)

    suspend fun saveSteps(date: LocalDate = LocalDate.now(), steps: Int)

    fun getAllDays(): Flow<List<DayEntity>>

    fun getDayByDate(date: LocalDate): Flow<DayEntity?>

    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?>
}