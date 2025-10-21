package fi.tuni.lonelinessapp.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.util.concurrent.Executors

private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

fun ioThread(f : () -> Unit) {
    IO_EXECUTOR.execute(f)
}

@Database(entities = [Survey::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "database")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        ioThread {
                            val surveyDao = getInstance(context).surveyDao()
                            PREPOPULATE_DATA.forEach {e -> surveyDao.insert(e)}
                        }
                    }
                })
                .build()

        private val PREPOPULATE_DATA = listOf(
            Survey(LocalDate.of(2025, 10, 17).toEpochDay(), 6),
            Survey(LocalDate.of(2025, 10, 18).toEpochDay(), 7),
            Survey(LocalDate.of(2025, 10, 19).toEpochDay(), 8)
        )
    }
}
