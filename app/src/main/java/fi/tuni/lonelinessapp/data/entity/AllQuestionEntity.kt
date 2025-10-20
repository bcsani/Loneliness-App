package fi.tuni.lonelinessapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "allQuestion")
data class AllQuestionEntity(
    @PrimaryKey val date: LocalDate,
    val totalPoints: Int,
)