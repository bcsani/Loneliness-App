package fi.tuni.lonelinessapp.domain.model

data class AppUsageData(
    val telegramUsageTime: Long = 0,
    val whatsappUsageTime: Long = 0,
    val lastUpdated: Long = 0
)
