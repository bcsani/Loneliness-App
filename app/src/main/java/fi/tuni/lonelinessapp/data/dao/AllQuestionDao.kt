package fi.tuni.lonelinessapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity

@Dao
interface AllQuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestionPoint(question: AllQuestionEntity)

    @Query("SELECT * FROM allQuestion")
    fun getAllQuestionPoints(): List<AllQuestionEntity>
}