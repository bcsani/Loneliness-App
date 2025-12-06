package fi.tuni.lonelinessapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fi.tuni.lonelinessapp.data.entity.DayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate


@Dao
interface DayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllDays(days: List<DayEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: DayEntity)
    @Query("UPDATE dayTable SET loneliness = :loneliness WHERE date = :date")
    suspend fun updateLoneliness(date: LocalDate, loneliness: Int)
    @Query("UPDATE dayTable SET nightMinutes = :nightMinutes WHERE date = :date")
    suspend fun updateNightMinutes(date: LocalDate, nightMinutes: Int)

    @Query("UPDATE dayTable SET dayMinutes = :dayMinutes WHERE date = :date")
    suspend fun updateDayMinutes(date: LocalDate, dayMinutes: Int)

    @Query("UPDATE dayTable SET steps = :steps WHERE date = :date")
    suspend fun updateSteps(date: LocalDate, steps: Int)

    @Query("UPDATE dayTable SET whatApps = :whatApps WHERE date = :date")
    suspend fun updateWhatApps(date: LocalDate, whatApps: Int)
    @Query("UPDATE dayTable SET messages = :messages WHERE date = :date")
    suspend fun updateMessages(date: LocalDate, messages: Int)
    @Query("UPDATE dayTable SET calls = :calls WHERE date = :date")
    suspend fun updateCalls(date: LocalDate, calls: Int)
    @Query("UPDATE dayTable SET signal = :signal WHERE date = :date")
    suspend fun updateSignal(date: LocalDate, signal: Int)
    @Query("UPDATE dayTable SET telegram = :telegram WHERE date = :date")
    suspend fun updateTelegram(date: LocalDate, telegram: Int)

    @Query("SELECT * FROM dayTable ORDER BY date ASC")
    fun getAllDays(): Flow<List<DayEntity>>

    @Query("SELECT * FROM dayTable WHERE date = :date")
    fun getDayByDate(date: LocalDate): Flow<DayEntity?>

    @Query("SELECT * FROM daytable WHERE date >= :startDate ORDER BY date ASC")
    fun getDaysFromDate(startDate: LocalDate): Flow<List<DayEntity>?>

    @Query("SELECT signal FROM dayTable where date = :date")
    fun getSignalByDate(date: LocalDate): Flow<Int?>
    @Query("SELECT signal FROM dayTable WHERE date = :date LIMIT 1")
    fun getSignalByDateSingle(date: LocalDate): Int?
    @Query("UPDATE dayTable SET signal = COALESCE(signal, 0) + :signal WHERE date = :date")
    fun addSignalByDate(date: LocalDate, signal: Int)
    @Query("SELECT calls FROM dayTable where date = :date")
    fun getCallsByDate(date: LocalDate): Flow<Int?>
    @Query("SELECT whatApps FROM dayTable where date = :date")
    fun getWhatAppsByDate(date: LocalDate): Flow<Int?>
    @Query("SELECT telegram FROM dayTable where date = :date")
    fun getTelegramByDate(date: LocalDate): Flow<Int?>

    @Query("DELETE FROM dayTable")
    suspend fun resetData()
}