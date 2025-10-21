package fi.tuni.lonelinessapp.data.source.local

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query

@Dao
interface SurveyDao {
    @Upsert
    fun insert(survey: Survey)

    @Query("SELECT * FROM survey ORDER BY date")
    fun getData(): List<Survey>
}
