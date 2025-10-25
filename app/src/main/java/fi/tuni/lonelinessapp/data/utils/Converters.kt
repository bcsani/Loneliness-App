package fi.tuni.lonelinessapp.data.utils

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    /*
    This class is used to convert data in order to fit Room database's format
     */
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Convert LocalDate to string
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let {
            LocalDate.parse(it, formatter)
        }
    }

    // Convert String to LocalDate
    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.format(formatter)
    }
}