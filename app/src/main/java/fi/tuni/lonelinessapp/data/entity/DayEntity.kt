package fi.tuni.lonelinessapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "dayTable")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val loneliness: Int? = null,
    val nightMinutes: Int? = null,
    val dayMinutes: Int? = null,
    val steps: Int? = null,
    val whatApps: Int? = null,
    val messages: Int? = null,
    val calls: Int? = null,
    val signal: Int? = null,
    val telegram: Int? = null
)


