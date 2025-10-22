package fi.tuni.lonelinessapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AllQuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestionPoint(question: AllQuestionEntity)

    @Query("SELECT * FROM allQuestion")
    fun getAllQuestionPoints(): Flow<List<AllQuestionEntity>>

    @Query("SELECT * FROM allQuestion WHERE date = :date")
    fun getQuestionPointsByDate(date: LocalDate): Flow<AllQuestionEntity?>
}