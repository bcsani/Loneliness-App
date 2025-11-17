package fi.tuni.lonelinessapp.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import java.util.Calendar

class CallDurationHelper(private val context: Context) {
    fun getTotalCallDurationToday(): Long {
        var totalDuration: Long = 0

        // Check if we have permission
        if (!hasCallLogPermission()) {
            throw SecurityException("READ_CALL_LOG permission required")
        }

        val projection = arrayOf(
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val selection = "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} < ?"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val durationColumn = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val duration = it.getLong(durationColumn)
                totalDuration += duration
            }
        }

        return totalDuration
    }

    fun getTotalCallDurationTodayFormatted(): String {
        val totalSeconds = getTotalCallDurationToday()
        return formatDuration(totalSeconds)
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun hasCallLogPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED
    }
}