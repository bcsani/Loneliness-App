package fi.tuni.lonelinessapp.data.utils

import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.LocalDate
import kotlin.random.Random

object PrepopulateDataGenerator {
    /*
    This object is used to generate data DayEntity within a year to drawn in charts.
    This will be used to prepopulate the day_db when AppDatabase is initialized
     */
    fun generateData(): List<DayEntity> {
        val data = mutableListOf<DayEntity>()
        val year = LocalDate.now().year
        val month = LocalDate.now().month
        val dayOfMonth = LocalDate.now().dayOfMonth

        var currentDate = LocalDate.of( year - 1, month, dayOfMonth)

        while (currentDate != LocalDate.now().plusDays(1)) {
            data.add(
                DayEntity(
                    date = currentDate,
                    loneliness = if (currentDate != LocalDate.now().minusDays(2) && currentDate != LocalDate.now()) Random.nextInt(3, 10) else null,
                    //loneliness = Random.nextInt(3, 10),
                    nightMinutes = Random.nextInt(16, 31),
                    dayMinutes = Random.nextInt(27, 420),
                    steps = Random.nextInt(500, 8000),
                    whatApps = Random.nextInt(1, 120),
                    messages = Random.nextInt(1,120),
                    calls = Random.nextInt(1,60),
                    signal = Random.nextInt(1, 120),
                    telegram = Random.nextInt(1,120)
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        return data
    }
}