package fi.tuni.lonelinessapp.data.datasource

import fi.tuni.lonelinessapp.data.dao.DayDao
import fi.tuni.lonelinessapp.data.entity.DayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class DayDataSource (
    private val dayDao: DayDao
) {
    fun insertDay(day: DayEntity) =
        dayDao.insertDay(day)
    fun getAllDays(): Flow<List<DayEntity>> =
        dayDao.getAllDays()
    fun getDayByDate(date: LocalDate): Flow<DayEntity?> =
        dayDao.getDayByDate(date)
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?> =
        dayDao.getDaysFromDate(startDate)
    suspend fun saveLoneliness(date: LocalDate, loneliness: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date=date, loneliness=loneliness)
        } else {
            dayDao.updateLoneliness(date, loneliness)
        }
    }

    suspend fun saveNightMinutes(date: LocalDate, nightMinutes: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date=date, nightMinutes=nightMinutes)
        } else {
            dayDao.updateNightMinutes(date, nightMinutes)
        }
    }

    suspend fun saveDayMinutes(date: LocalDate, dayMinutes: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date=date, dayMinutes=dayMinutes)
        } else {
            dayDao.updateDayMinutes(date, dayMinutes)
        }
    }

    suspend fun saveSteps(date: LocalDate, steps: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date=date, steps=steps)
        } else {
            dayDao.updateSteps(date, steps)
        }
    }

    private fun insertDayWithData(
        date: LocalDate,
        loneliness: Int = 0,
        nightMinutes: Int = 0,
        dayMinutes: Int = 0,
        steps: Int = 0
    ) {
        dayDao.insertDay(DayEntity(
            date = date,
            loneliness = loneliness,
            nightMinutes = nightMinutes,
            dayMinutes = dayMinutes,
            steps = steps
        ))
    }
}