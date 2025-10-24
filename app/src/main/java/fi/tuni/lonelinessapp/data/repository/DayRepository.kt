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
    suspend fun updateLoneliness(date: LocalDate, loneliness: Int) =
        dayDataSource.updateLoneliness(date, loneliness)
    suspend fun updateNightMinutes(date: LocalDate, nightMinutes: Int) =
        dayDataSource.updateNightMinutes(date, nightMinutes)
    suspend fun updateDayMinutes(date: LocalDate, dayMinutes: Int) =
        dayDataSource.updateDayMinutes(date, dayMinutes)
    suspend fun updateSteps(date: LocalDate, steps: Int) =
        dayDataSource.updateSteps(date, steps)
    fun getAllDays(): Flow<List<DayEntity>> =
        dayDataSource.getAllDays()
    fun getDayByDate(date: LocalDate): Flow<DayEntity> =
        dayDataSource.getDayByDate(date)
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>> =
        dayDataSource.getDaysFromDate(startDate)
}