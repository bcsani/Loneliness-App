package fi.tuni.lonelinessapp.data

import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import fi.tuni.lonelinessapp.data.dao.DayDao
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.utils.Converters
import fi.tuni.lonelinessapp.data.utils.PrepopulateDataGenerator
import java.util.concurrent.Executors

@Database(entities= [DayEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao

    companion object {
        // PREPOPULATE_DATA contains all data in DayEntity within a year
        private val PREPOPULATE_DATA: List<DayEntity> by lazy {
            PrepopulateDataGenerator.generateData()
        }

        // Single source Instance of AppDatabase
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // This will return AppDatabase Instance if it already exists or build a new one
        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        /*
        The database will be built with name "day_db".
        All the data will be inserted immediately while creating the database
        */
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "day_db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val ioExecutor = Executors.newSingleThreadExecutor()
                        ioExecutor.execute({
                            getInstance(context).dayDao().insertAllDays(PREPOPULATE_DATA)
                        })
                        ioExecutor.shutdown()
                    }
                })
                .build()
    }
}