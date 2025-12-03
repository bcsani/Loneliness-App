package fi.tuni.lonelinessapp.domain.usecase

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import java.util.Calendar

class GetCallDurationUseCase(
    private val context: Context
) {
    operator fun invoke(): Long {
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

    private fun hasCallLogPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}