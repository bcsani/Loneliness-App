package fi.tuni.lonelinessapp.data.datasource

import fi.tuni.lonelinessapp.data.dao.DayDao
import fi.tuni.lonelinessapp.data.entity.DayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DayDataSource (
    private val dayDao: DayDao
) {
    fun insertDay(day: DayEntity) =
        dayDao.insertDay(day)
    suspend fun updateLoneliness(date: LocalDate, loneliness: Int) =
        dayDao.updateLoneliness(date, loneliness)
    suspend fun updateNightMinutes(date: LocalDate, nightMinutes: Int) =
        dayDao.updateNightMinutes(date, nightMinutes)
    suspend fun updateDayMinutes(date: LocalDate, dayMinutes: Int) =
        dayDao.updateDayMinutes(date, dayMinutes)
    suspend fun updateSteps(date: LocalDate, steps: Int) =
        dayDao.updateSteps(date, steps)
    fun getAllDays(): Flow<List<DayEntity>> =
        dayDao.getAllDays()
    fun getDayByDate(date: LocalDate): Flow<DayEntity> =
        dayDao.getDayByDate(date)
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>> =
        dayDao.getDaysFromDate(startDate)
}