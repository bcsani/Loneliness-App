package fi.tuni.lonelinessapp.domain.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH

class StepSensorManager() : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepTrackingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "step_tracking_channel"
        private const val STEP_COUNT_PREF_KEY = "step_count"
        private const val STEP_OFFSET_PREF_KEY = "step_offset"
    }

    private val binder = StepTrackingBinder()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0
    private var stepOffset = 0
    private var isTracking = false

    // Database operations
    private var dayRepository: DayRepository? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)


    private lateinit var notificationManager: NotificationManager

    interface StepTrackingCallback {
        fun onStepsUpdated(steps: Int)
        fun onTrackingStateChanged(isTracking: Boolean)
    }

    private var callback: StepTrackingCallback? = null

    inner class StepTrackingBinder : Binder() {
        fun getService(): StepSensorManager = this@StepSensorManager
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        println("onCreate")
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        loadStepData()

        if (stepSensor == null) {
            Log.w(TAG, "Step counter sensor not available, using simulation")
        } else {
            Log.d(TAG, "Step counter sensor available")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand")
        startForegroundService()
        startStepTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStepTracking()
        saveStepData()
    }

    fun setDayRepository(repository: DayRepository) {
        println("setDayRepository")

        this.dayRepository = repository
        if(this.dayRepository == null) {
            println("dayRepository is null during setday")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your steps in the background"
        }
        notificationManager.createNotificationChannel(channel)

    }


    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_HEALTH)

    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Tracker")
            .setContentText("Tracking your steps: $stepCount")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun startStepTracking() {
        if (isTracking) return

        stepSensor?.let { sensor ->
            val success = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (success) {
                isTracking = true
                Log.d(TAG, "Step tracking started with hardware sensor")
            } else {
                Log.e(TAG, "Failed to register step sensor listener")
            }
        }

    }

    fun stopStepTracking() {
        if (!isTracking) return

        sensorManager.unregisterListener(this)
        isTracking = false
        saveStepData()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        println("onSensorChanged")
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = it.values[0].toInt()

                if (stepOffset == 0) {
                    // First reading, set offset
                    stepOffset = currentSteps
                }

                val newStepCount = currentSteps - stepOffset
                if (newStepCount > stepCount) {
                    println("saveStepData")
                    println("UpdateDatabase")
                    stepCount = newStepCount
                    saveStepData()
                    updateDatabase()
                }
            }
        }
        println("Step Count: " + stepCount)
        println("Step Offset: " + stepOffset)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun updateDatabase() {
        serviceScope.launch {
            try {
                val today = LocalDate.now()
                println("Step count in updateDatabase: " + stepCount)
                if (dayRepository == null) {
                    println("DayRepository is null")
                }
                println("today" + today)
                dayRepository?.saveSteps(today, stepCount)
            } catch (e: Exception) {
                println("Error in updatedabase")
                // Handle database error
                e.printStackTrace()
            }
        }
    }

    fun getStepCount(): Int = stepCount

    fun resetStepCount() {
        stepCount = 0
        stepOffset = 0
        saveStepData()
        callback?.onStepsUpdated(stepCount)
        updateNotification()
    }

    fun isTracking(): Boolean = isTracking

    fun setCallback(callback: StepTrackingCallback?) {
        this.callback = callback
    }

    private fun saveStepData() {
        val prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(STEP_COUNT_PREF_KEY, stepCount)
            .putInt(STEP_OFFSET_PREF_KEY, stepOffset)
            .apply()
    }

    private fun loadStepData() {
        val prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        stepCount = prefs.getInt(STEP_COUNT_PREF_KEY, 0)
        stepOffset = prefs.getInt(STEP_OFFSET_PREF_KEY, 0)
    }

//    fun startTracking() {
//        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
//
//        if (stepSensor == null) {
//            Toast.makeText(context, "Step sensor not available", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
//        tracking = true
//
//    }
//
//    fun stopTracking() {
//        sensorManager?.unregisterListener(this)
//        tracking = false
//    }
//
//    override fun onSensorChanged(event: SensorEvent) {
//        if(!tracking) return
//        val steps = event.values[0]
//
//        if (initialSteps == null) {
//            initialSteps = steps
//        }
//        val stepsToday = (steps - (initialSteps ?: 0f)).toInt()
//        onStepsUpdated(stepsToday)
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}