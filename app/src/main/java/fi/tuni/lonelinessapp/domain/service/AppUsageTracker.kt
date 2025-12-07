package fi.tuni.lonelinessapp.domain.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import fi.tuni.lonelinessapp.domain.model.AppUsageData
import java.util.*
import java.util.concurrent.TimeUnit

class AppUsageTracker(
    private val context: Context
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

    fun getAppUsageStats(daysBack: Int = 1): AppUsageData {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_WEEK, -daysBack)
        }

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            calendar.timeInMillis,
            System.currentTimeMillis()
        ) ?: return AppUsageData()

        return processUsageStats(usageStats)
    }

    private fun processUsageStats(usageStats: List<UsageStats>): AppUsageData {
        var telegramTime = 0L
        var whatsappTime = 0L

        println("=== PROCESSING USAGE STATS ===")

        // Sum usage across all package variations
        usageStats.forEach { stats ->
            val packageName = stats.packageName
            val usageTime = stats.totalTimeInForeground

            // Only process if there's actual usage time
            if (usageTime > 0) {
                // println("Processing: $packageName - ${usageTime}ms")

                when {
                    telegramPackages.any { it == packageName } -> {
                        // println("✓ FOUND TELEGRAM: $packageName - ${usageTime}ms")
                        telegramTime += usageTime
                    }
                    whatsappPackages.any { it == packageName } -> {
                        // println("✓ FOUND WHATSAPP: $packageName - ${usageTime}ms")
                        whatsappTime += usageTime
                    }
                    else -> {
                        // Debug: print other apps with significant usage
                        if (usageTime > 60000) { // More than 1 minute
                            println("  Other app: $packageName - ${usageTime}ms")
                        }
                    }
                }
            }
        }


        /*
        println("FINAL RESULTS:")
        println("Telegram time: ${telegramTime}ms (${TimeUnit.MILLISECONDS.toMinutes(telegramTime)} minutes)")
        println("WhatsApp time: ${whatsappTime}ms (${TimeUnit.MILLISECONDS.toMinutes(whatsappTime)} minutes)")
        println("=== END PROCESSING ===")
         */

        // Convert milliseconds to minutes
        val telegramMinutes = TimeUnit.MILLISECONDS.toMinutes(telegramTime)
        val whatsappMinutes = TimeUnit.MILLISECONDS.toMinutes(whatsappTime)

        return AppUsageData(
            telegramUsageTime = telegramMinutes,
            whatsappUsageTime = whatsappMinutes,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun getCurrentUsage(): AppUsageData {
        return getAppUsageStats(1) // Last 24 hours
    }
}