package fi.tuni.lonelinessapp.data

import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import fi.tuni.lonelinessapp.data.dao.AllQuestionDao
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity
import fi.tuni.lonelinessapp.data.utils.Converters

@Database(entities= [AllQuestionEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun allQuestionDao(): AllQuestionDao
}