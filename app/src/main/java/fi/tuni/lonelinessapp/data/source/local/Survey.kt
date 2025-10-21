package fi.tuni.lonelinessapp.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Survey(
    @PrimaryKey
    val date: Long,
    val score: Int
)
