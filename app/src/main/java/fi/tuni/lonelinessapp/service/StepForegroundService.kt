package fi.tuni.lonelinessapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fi.tuni.lonelinessapp.R
import fi.tuni.lonelinessapp.data.datasource.StepCounterDataSource
import fi.tuni.lonelinessapp.data.repository.StepRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest


class StepForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repository: StepRepository

    override fun onCreate() {
        super.onCreate()
        repository = StepRepository(StepCounterDataSource(applicationContext))
        repository.startTracking()
        createNotificationChannel()
        startForeground(1, buildNotification(0))

        serviceScope.launch {
            repository.steps.collectLatest { steps ->
                println("Steps:" + steps)
                val notification = buildNotification(steps)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(1, notification)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.stopTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(steps: Int): Notification {
        val notification: Notification = NotificationCompat.Builder(this, "step_channel")
            .setContentTitle("Step Tracker")
            .setContentText("Steps: $steps")
            .setSmallIcon(R.drawable.ic_step_icon)
            .setOngoing(true)
            .build()

        return notification
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("step_channel", "Step Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}