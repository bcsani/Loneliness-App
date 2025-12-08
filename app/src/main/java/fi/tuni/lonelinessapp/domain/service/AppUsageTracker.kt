package fi.tuni.lonelinessapp.domain.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class AppUsageTracker(
    private val context: Context,
    private val dataSource: DayDataSource
) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val telegramPackages = listOf(
        "org.telegram.messenger",
    )

    private val whatsappPackages = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.whatsapp.business"
    )

    fun isUsageStatsPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startTracking() {
        if (!isUsageStatsPermissionGranted()) {
            return
        }

    }

    private fun getMinsFromInterval(beg: Long, end: Long): List<Pair<String, Long>> {
        val usageEvents = usageStatsManager.queryEvents(beg, end)
        val foregroundEvents: MutableList<UsageEvents.Event> = mutableListOf()
        val appUsage = HashMap<String, Long>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                foregroundEvents.add(event)
                if (event.packageName !in appUsage) {
                    appUsage[event.packageName] = 0
                }
            }
        }

        for (i in 0..<foregroundEvents.size - 1) {
            val e0 = foregroundEvents[i]
            val e1 = foregroundEvents[i + 1]

            if (e0.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                e1.eventType == UsageEvents.Event.ACTIVITY_PAUSED &&
                e0.packageName == e1.packageName
            ) {
                val diff = e1.timeStamp - e0.timeStamp
                appUsage[e0.packageName]?.let { appUsage[e0.packageName] = it + diff }
            }
        }

        return appUsage.toList().map { (key, value) ->
                Pair(key, TimeUnit.MILLISECONDS.toMinutes(value))
            }
    }

    private fun getAppMins(usageStats: List<Pair<String, Long>>, packages: List<String>): Int {
        return usageStats
            .filter { (packageName, _) -> packageName in packages }
            .sumOf { (_, minutes) -> minutes }
            .toInt()
    }

    suspend fun updateDate(date: LocalDate) {
        val off = ZoneId.systemDefault().rules.getOffset(Instant.now())

        val dateBeg = date.atStartOfDay().toInstant(off).toEpochMilli()
        val dateEnd = date.atStartOfDay().plus(24, ChronoUnit.HOURS).toInstant(off).toEpochMilli()

        val usageStats = getMinsFromInterval(dateBeg, dateEnd)

        val telegramMinutes = getAppMins(usageStats, telegramPackages)
        val whatsAppMinutes = getAppMins(usageStats, whatsappPackages)

        dataSource.saveTelegram(date, telegramMinutes)
        dataSource.saveWhatApp(date, whatsAppMinutes)

        val nighttimeBegin = date.atStartOfDay().minus(2, ChronoUnit.HOURS).toInstant(off).toEpochMilli()
        val transition     = date.atStartOfDay().plus(6, ChronoUnit.HOURS).toInstant(off).toEpochMilli()
        val daytimeEnd     = date.atStartOfDay().plus(22, ChronoUnit.HOURS).toInstant(off).toEpochMilli()

        val nightMinutes = getMinsFromInterval(nighttimeBegin, transition)
            .sumOf { (_, mins) -> mins}.toInt()

        val dayMinutes = getMinsFromInterval(transition, daytimeEnd)
            .sumOf { (_, mins) -> mins}.toInt()

        dataSource.saveNightMinutes(date, nightMinutes)
        dataSource.saveDayMinutes(date, dayMinutes)
    }

    suspend fun update() {
        var date = LocalDate.now().minusDays(6)

        while (date != LocalDate.now().plusDays(1)) {
            updateDate(date)
            date = date.plusDays(1)
        }
    }
}