package fi.tuni.lonelinessapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "dayTable")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val loneliness: Int?,
    val nightMinutes: Int?,
    val dayMinutes: Int?,
    val steps: Int,
    val whatApps: Int?,
    val messages: Int?,
    val calls: Int,
    val signal: Int?,
    val telegram: Int?
)


