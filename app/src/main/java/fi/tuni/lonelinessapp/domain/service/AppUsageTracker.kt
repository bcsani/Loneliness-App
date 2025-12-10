package fi.tuni.lonelinessapp.domain.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents.Event
import android.app.usage.UsageStatsManager
import android.content.Context
import fi.tuni.lonelinessapp.data.datasource.DayDataSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

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

    private fun getMinsFromInterval(beg: Long, end: Long): List<Pair<String, Int>> {
        val usageEvents = usageStatsManager.queryEvents(beg, end)
        val foregroundEvents: MutableList<Event> = mutableListOf()

        while (usageEvents.hasNextEvent()) {
            val event = Event()
            usageEvents.getNextEvent(event)
            if (
                event.eventType == Event.ACTIVITY_RESUMED ||
                event.eventType == Event.ACTIVITY_PAUSED
            ) {
                foregroundEvents.add(event)
            }
        }

        val grouped = foregroundEvents.groupBy { it.packageName }
        val appUsage = HashMap<String, Long>()

        for (pkg in grouped.keys) {
            var mins = 0L

            if (grouped[pkg]!!.first().eventType == Event.ACTIVITY_PAUSED) {
                mins += grouped[pkg]!!.first().timeStamp - beg
            }

            for (i in 0..<grouped[pkg]!!.size - 1) {
                val e0 = grouped[pkg]!![i]
                val e1 = grouped[pkg]!![i + 1]

                if (
                    e0.eventType == Event.ACTIVITY_RESUMED &&
                    e1.eventType == Event.ACTIVITY_PAUSED
                ) {
                    mins += e1.timeStamp - e0.timeStamp
                }
            }

            if (grouped[pkg]!!.last().eventType == Event.ACTIVITY_RESUMED) {
                mins += end - grouped[pkg]!!.last().timeStamp
            }

            appUsage[pkg] = mins
        }

        return appUsage.toList().map { (key, value) ->
            Pair(key, TimeUnit.MILLISECONDS.toMinutes(value).toInt())
        }
    }

    private fun getAppMins(usageStats: List<Pair<String, Int>>, packages: List<String>): Int {
        return usageStats
            .filter { (packageName, _) -> packageName in packages }
            .sumOf { (_, minutes) -> minutes }
    }

    private suspend fun updateDate(date: LocalDate) {
        val off = ZoneId.systemDefault().rules.getOffset(Instant.now())

        val dateBeg = date.atStartOfDay().toInstant(off).toEpochMilli()
        val dateEnd = min(System.currentTimeMillis(), date.atStartOfDay().plusHours(24).toInstant(off).toEpochMilli())

        val usageStats = getMinsFromInterval(dateBeg, dateEnd)

        val telegramMinutes = getAppMins(usageStats, telegramPackages)
        val whatsAppMinutes = getAppMins(usageStats, whatsappPackages)

        dataSource.saveTelegram(date, telegramMinutes)
        dataSource.saveWhatApp(date, whatsAppMinutes)

        val nighttimeBegin = date.atStartOfDay().minusHours(2).toInstant(off).toEpochMilli()
        val transition     = min(System.currentTimeMillis(), date.atStartOfDay().plusHours(6).toInstant(off).toEpochMilli())
        val daytimeEnd     = min(System.currentTimeMillis(), date.atStartOfDay().plusHours(22).toInstant(off).toEpochMilli())

        val nightMinutes = getMinsFromInterval(nighttimeBegin, transition)
            .sumOf { (_, mins) -> mins}

        val dayMinutes = getMinsFromInterval(transition, daytimeEnd)
            .sumOf { (_, mins) -> mins}

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