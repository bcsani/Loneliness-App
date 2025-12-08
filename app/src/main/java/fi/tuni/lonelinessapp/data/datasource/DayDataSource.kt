package fi.tuni.lonelinessapp.data.datasource

import fi.tuni.lonelinessapp.data.dao.DayDao
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.utils.CallDurationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DayDataSource (
    private val dayDao: DayDao,
    private val callDurationHelper: CallDurationHelper
) {
    suspend fun insertDay(day: DayEntity) =
        dayDao.insertDay(day)
    fun getAllDays(): Flow<List<DayEntity>> =
        dayDao.getAllDays()
    fun getDayByDate(date: LocalDate): Flow<DayEntity?> =
        dayDao.getDayByDate(date)
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?> =
        dayDao.getDaysFromDate(startDate)

    fun getSignalByDate(date: LocalDate): Flow<Int?> =
        dayDao.getSignalByDate(date)
    fun getSignalAmountByDate(date: LocalDate): Flow<Int?> =
        dayDao.getSignalAmountByDate(date)
    fun getCallsByDate(date: LocalDate): Flow<Int?> =
        dayDao.getCallsByDate(date)
    fun getWhatAppsByDate(date: LocalDate): Flow<Int?> =
        dayDao.getWhatAppsByDate(date)
    fun getTelegramByDate(date: LocalDate): Flow<Int?> =
        dayDao.getTelegramByDate(date)
    fun addSignalByDate(date: LocalDate, signal: Int) {
        dayDao.addSignalByDate(date, signal)
    }
    fun addSignalAmountByDate(date: LocalDate, signalAmount: Int) {
        dayDao.addSignalAmountByDate(date, signalAmount)
    }

    suspend fun updateCallDurationToday() {
        val duration = callDurationHelper.getTotalCallDurationToday()
        val today = LocalDate.now()
        dayDao.updateCalls(today, duration.toInt())
    }

    suspend fun saveLoneliness(date: LocalDate = LocalDate.now(), loneliness: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, loneliness = loneliness)
        } else {
            dayDao.updateLoneliness(date, loneliness)
        }
    }

    suspend fun saveNightMinutes(date: LocalDate = LocalDate.now(), nightMinutes: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, nightMinutes = nightMinutes)
        } else {
            dayDao.updateNightMinutes(date, nightMinutes)
        }
    }

    suspend fun saveDayMinutes(date: LocalDate = LocalDate.now(), dayMinutes: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, dayMinutes = dayMinutes)
        } else {
            dayDao.updateDayMinutes(date, dayMinutes)
        }
    }

    suspend fun saveSteps(date: LocalDate = LocalDate.now(), steps: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, steps = steps)
        } else {
            dayDao.updateSteps(date, steps)
        }
    }

    suspend fun saveWhatApp(date: LocalDate = LocalDate.now(), whatApps: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, whatApps = whatApps)
        } else {
            dayDao.updateWhatApps(date, whatApps)
        }
    }

    suspend fun saveMessages(date: LocalDate = LocalDate.now(), messages: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, messages = messages)
        } else {
            dayDao.updateMessages(date, messages)
        }
    }

    suspend fun saveCalls(date: LocalDate = LocalDate.now(), calls: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, calls = calls)
        } else {
            dayDao.updateCalls(date, calls)
        }
    }

    suspend fun saveSignal(date: LocalDate = LocalDate.now(), signal: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, signal = signal)
        } else {
            dayDao.updateSignal(date, signal)
        }
    }

    suspend fun saveSignalAmount(date: LocalDate = LocalDate.now(), signalAmount: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, signalAmount = signalAmount)
        } else {
            dayDao.updateSignalAmount(date, signalAmount)
        }
    }

    suspend fun saveTelegram(date: LocalDate = LocalDate.now(), telegram: Int) {
        val existingDay = dayDao.getDayByDate(date).first()

        // Check if the day exists or not, if it doesn't exist then create a new day
        if (existingDay == null) {
            insertDayWithData(date = date, telegram = telegram)
        } else {
            dayDao.updateTelegram(date, telegram)
        }
    }

    suspend fun insertDayWithData(
        date: LocalDate,
        loneliness: Int? = null,
        nightMinutes: Int? = null,
        dayMinutes: Int? = null,
        steps: Int? = null,
        whatApps: Int? = null,
        messages: Int? = null,
        calls: Int? = null,
        signal: Int? = null,
        telegram: Int? = null,
        signalAmount: Int? = null,
    ) {
        dayDao.insertDay(
            DayEntity(
                date = date,
                loneliness = loneliness,
                nightMinutes = nightMinutes,
                dayMinutes = dayMinutes,
                steps = steps,
                whatApps = whatApps,
                messages = messages,
                calls = calls,
                signal = signal,
                telegram = telegram,
                signalAmount = signalAmount
            )
        )
    }

    suspend fun resetData() {
        dayDao.resetData()
    }

    fun isResponded(): Flow<Boolean> = dayDao.lastResponse().map { it == LocalDate.now() }

    fun getStreak(): Flow<Int> {
        return flow {
            dayDao.firstDate()
                .combine(dayDao.lastResponse()) { i, j -> Pair(i, j) }
                .combine(dayDao.lastNonStreak(LocalDate.now())) { (i, j), k -> Triple(i, j, k) }
                .collect { (firstDate, lastResponse, lastNonStreak) ->
                    emit(
                        if (firstDate == null || lastResponse == null) {
                            0
                        } else if (lastNonStreak == null) {
                            lastResponse.toEpochDay().toInt() - firstDate.toEpochDay()
                                .toInt() + 1
                        } else if (lastResponse < LocalDate.now().minusDays(1)) {
                            0
                        } else {
                            lastResponse.toEpochDay().toInt() - lastNonStreak.toEpochDay()
                                .toInt()
                        }
                    )
                }
        }
    }
}