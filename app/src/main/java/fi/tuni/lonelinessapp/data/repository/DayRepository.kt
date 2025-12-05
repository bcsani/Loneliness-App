package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.domain.repository.DayRepositoryInterface
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DayRepository  (
    private val dayDataSource: DayDataSource
): DayRepositoryInterface {
    private val today = LocalDate.now()
    override suspend fun insertDay(day: DayEntity) =
        dayDataSource.insertDay(day)
    override suspend fun saveLoneliness(date: LocalDate, loneliness: Int) =
        dayDataSource.saveLoneliness(date, loneliness)
    override suspend fun saveNightMinutes(date: LocalDate, nightMinutes: Int) =
        dayDataSource.saveNightMinutes(date, nightMinutes)
    override suspend fun saveDayMinutes(date: LocalDate, dayMinutes: Int) =
        dayDataSource.saveDayMinutes(date, dayMinutes)
    override suspend fun saveSteps(date: LocalDate, steps: Int) =
        dayDataSource.saveSteps(date, steps)
    suspend fun saveWhatApp(date: LocalDate = today, whatApps: Int) =
        dayDataSource.saveWhatApp(date, whatApps)
    suspend fun saveMessages(date: LocalDate, messages: Int) =
        dayDataSource.saveMessages(date, messages)
    suspend fun saveCalls(date: LocalDate = today, calls: Int) =
        dayDataSource.saveCalls(date, calls)
    suspend fun saveSignal(date: LocalDate = today, signal: Int) =
        dayDataSource.saveSignal(date, signal)
    suspend fun saveTelegram(date: LocalDate = today, telegram: Int) =
        dayDataSource.saveTelegram(date, telegram)
    override fun getAllDays(): Flow<List<DayEntity>> =
        dayDataSource.getAllDays()
    override fun getDayByDate(date: LocalDate): Flow<DayEntity?> =
        dayDataSource.getDayByDate(date)
    override fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?> =
        dayDataSource.getDaysFromDate(startDate)
    fun getSignalToday(): Flow<Int?> =
        dayDataSource.getSignalByDate(today)
    fun getCallsToday(): Flow<Int?> =
        dayDataSource.getCallsByDate(today)
    fun getWhatAppsToday(): Flow<Int?> =
        dayDataSource.getWhatAppsByDate(today)
    fun getTelegramToday(): Flow<Int?> =
        dayDataSource.getTelegramByDate(today)
    suspend fun resetData() =
        dayDataSource.resetData()
    suspend fun updateCallDurationToday() =
        dayDataSource.updateCallDurationToday()
}