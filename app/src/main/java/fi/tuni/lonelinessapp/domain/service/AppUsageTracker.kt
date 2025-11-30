package fi.tuni.lonelinessapp.domain.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import java.util.*

class AppUsageTracker(
    private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val targetApps = listOf(
        "org.telegram.messenger",
        "com.whatsapp"
    )

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getAppUsageStats(days: Int = 1): Map<String, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val appUsage = mutableMapOf<String, Long>()

        usageStats?.forEach { stat ->
            if (targetApps.contains(stat.packageName) && stat.totalTimeInForeground > 0) {
                appUsage[stat.packageName] = (appUsage[stat.packageName] ?: 0) + stat.totalTimeInForeground
            }
        }

        return appUsage
    }

    fun getAppUsageStatsForToday(): Map<String, Long> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getAppUsageStats(1)
        } else {
            emptyMap()
        }
    }

    fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, secs)
            minutes > 0 -> String.format("%dm %02ds", minutes, secs)
            else -> String.format("%ds", secs)
        }
    }

    fun getAppName(packageName: String): String {
        return when (packageName) {
            "org.telegram.messenger" -> "Telegram"
            "com.whatsapp" -> "WhatsApp"
            else -> packageName
        }
    }
}