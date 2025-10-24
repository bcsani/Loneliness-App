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

private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

fun ioThread(f : () -> Unit) {
    IO_EXECUTOR.execute(f)
}

@Database(entities= [DayEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao

    companion object {
        private val PREPOPULATE_DATA: List<DayEntity> by lazy {
            PrepopulateDataGenerator.generateData()
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "day6_db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        ioThread {
                            val dayDao = getInstance(context).dayDao()
                            dayDao.insertAllDays(PREPOPULATE_DATA)
                        }
                    }
                })
                .build()
    }
}