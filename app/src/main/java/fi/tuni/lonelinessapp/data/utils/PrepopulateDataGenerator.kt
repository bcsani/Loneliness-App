package fi.tuni.lonelinessapp.data.utils

import fi.tuni.lonelinessapp.data.entity.DayEntity
import java.time.LocalDate
import kotlin.random.Random

object PrepopulateDataGenerator {
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
                    loneliness = Random.nextInt(3, 10),
                    nightMinutes = Random.nextInt(16, 31),
                    dayMinutes = Random.nextInt(27, 420),
                    steps = Random.nextInt(500, 8000)
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        println("Prepopulate data generated: ${data.size} items")
        return data
    }
}