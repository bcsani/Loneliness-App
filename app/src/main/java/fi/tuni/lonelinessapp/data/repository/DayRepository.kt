package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import fi.tuni.lonelinessapp.data.entity.DayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DayRepository  (
    private val dayDataSource: DayDataSource
){
    fun insertDay(day: DayEntity) =
        dayDataSource.insertDay(day)
    suspend fun saveLoneliness(date: LocalDate, loneliness: Int) =
        dayDataSource.saveLoneliness(date, loneliness)
    suspend fun saveNightMinutes(date: LocalDate, nightMinutes: Int) =
        dayDataSource.saveNightMinutes(date, nightMinutes)
    suspend fun saveDayMinutes(date: LocalDate, dayMinutes: Int) =
        dayDataSource.saveDayMinutes(date, dayMinutes)
    suspend fun saveSteps(date: LocalDate, steps: Int) =
        dayDataSource.saveSteps(date, steps)
    fun getAllDays(): Flow<List<DayEntity>?> =
        dayDataSource.getAllDays()
    fun getDayByDate(date: LocalDate): Flow<DayEntity?> =
        dayDataSource.getDayByDate(date)
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?> =
        dayDataSource.getDaysFromDate(startDate)
}