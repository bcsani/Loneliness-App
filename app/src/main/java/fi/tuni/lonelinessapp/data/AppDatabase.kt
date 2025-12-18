package fi.tuni.lonelinessapp.data

import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fi.tuni.lonelinessapp.data.dao.DayDao
import fi.tuni.lonelinessapp.data.entity.DayEntity
import fi.tuni.lonelinessapp.data.utils.Converters
import fi.tuni.lonelinessapp.data.utils.PrepopulateDataGenerator
import java.util.concurrent.Executors

@Database(entities= [DayEntity::class], version = 3)
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

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create temporary table with new schema
                db.execSQL("""
                    CREATE TABLE dayTable_new (
                        date TEXT PRIMARY KEY NOT NULL,
                        loneliness INTEGER,
                        nightMinutes INTEGER,
                        dayMinutes INTEGER,
                        steps INTEGER NOT NULL,
                        whatApps INTEGER,
                        messages INTEGER,
                        calls INTEGER NOT NULL,
                        signal INTEGER,
                        telegram INTEGER
                    )
                    """.trimIndent())

                // Copy data from old table to new table
                db.execSQL("""
                        INSERT INTO dayTable_new (date, loneliness, nightMinutes, dayMinutes, steps, 
                                     whatApps, messages, calls, signal, telegram)
                        SELECT date, loneliness, nightMinutes, dayMinutes, steps, 
                            whatApps, messages, calls, signal, telegram
                        FROM dayTable
                    """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE dayTable")

                // Rename new table to original name
                db.execSQL("ALTER TABLE dayTable_new RENAME TO dayTable")
            }
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
                        ioExecutor.execute { getInstance(context).dayDao().insertAllDays(PREPOPULATE_DATA) }
                        ioExecutor.shutdown()
                    }
                })
                .addMigrations(MIGRATION_2_3)
                .build()
    }
}