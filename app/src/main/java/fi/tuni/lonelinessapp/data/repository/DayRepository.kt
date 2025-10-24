package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.domain.repository.DayRepositoryInterface
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DayRepository  (
    private val dayDataSource: DayDataSource
): DayRepositoryInterface {
    override fun insertDay(day: DayEntity) =
        dayDataSource.insertDay(day)
    override suspend fun saveLoneliness(date: LocalDate, loneliness: Int) =
        dayDataSource.saveLoneliness(date, loneliness)
    override suspend fun saveNightMinutes(date: LocalDate, nightMinutes: Int) =
        dayDataSource.saveNightMinutes(date, nightMinutes)
    override suspend fun saveDayMinutes(date: LocalDate, dayMinutes: Int) =
        dayDataSource.saveDayMinutes(date, dayMinutes)
    override suspend fun saveSteps(date: LocalDate, steps: Int) =
        dayDataSource.saveSteps(date, steps)
    override fun getAllDays(): Flow<List<DayEntity>> =
        dayDataSource.getAllDays()
    override fun getDayByDate(date: LocalDate): Flow<DayEntity?> =
        dayDataSource.getDayByDate(date)
    override fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?> =
        dayDataSource.getDaysFromDate(startDate)
}