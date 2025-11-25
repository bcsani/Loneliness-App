package fi.tuni.lonelinessapp.domain.service

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
import fi.tuni.lonelinessapp.data.repository.DayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
import androidx.core.app.NotificationCompat

class StepSensorManager() : Service(), SensorEventListener {

    // Constant values to store the steps
    companion object {
        private const val TAG = "StepTrackingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "step_tracking_channel"
        private const val STEP_COUNT_PREF_KEY = "step_count"
        private const val STEP_OFFSET_PREF_KEY = "step_offset"
        private const val TRACKING_DATE_PREF_KEY = "tracking_date"
    }

    private val binder = StepTrackingBinder()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0
    private var stepOffset = 0
    private var isTracking = false
    private var currentTrackingDate = LocalDate.now()

    // Database operations
    private var dayRepository: DayRepository? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Notification manager
    private lateinit var notificationManager: NotificationManager

    // This binder will create stepSensorManager
    inner class StepTrackingBinder : Binder() {
        fun getService(): StepSensorManager = this@StepSensorManager
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Get the step sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Loading the current step count, step offset and current date
        loadStepData()

        if (stepSensor == null) {
            Log.w(TAG, "Step counter sensor not available, using simulation")
        } else {
            Log.d(TAG, "Step counter sensor available")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        this.dayRepository = repository
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

        // Start tracking step if there is a step sensor
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
        // If sensor changed then start calculating the step
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                // Get the cumulative current step that is stored in the database.
                // The step is cumulated through the phone lifecycle
                val currentSteps = it.values[0].toInt()
                checkAndResetForNewDay(currentSteps)

                // Set stepOffset
                if (stepOffset == 0) {
                    // First reading, set offset
                    stepOffset = currentSteps
                }

                val newStepCount = currentSteps - stepOffset
                if (newStepCount > stepCount) {
                    stepCount = newStepCount
                    saveStepData()
                    updateDatabase()
                }
            }
        }

        println("Step Count" + stepCount)
    }

    private fun checkAndResetForNewDay(currentSteps: Int) {
        val today = LocalDate.now()

        // If it's a new day, reset the step counting
        if (today.isAfter(currentTrackingDate)) {

            // Reset for new day
            stepOffset = currentSteps
            stepCount = 0
            currentTrackingDate = today

            // Save the reset state
            saveStepData()
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun updateDatabase() {
        serviceScope.launch {
            try {
                val today = LocalDate.now()
                // Update the steps to the database through repository
                dayRepository?.saveSteps(today, stepCount)
            } catch (e: Exception) {
                // Handle database error
                e.printStackTrace()
            }
        }
    }

    private fun saveStepData() {
        // Save all step data through SharedPreferences "step_prefs"
        val prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(STEP_COUNT_PREF_KEY, stepCount)
            .putInt(STEP_OFFSET_PREF_KEY, stepOffset)
            .putString(TRACKING_DATE_PREF_KEY, currentTrackingDate.toString())
            .apply()
    }

    private fun loadStepData() {
        // Load all data using SharedPreferences "step_prefs
        val prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        stepCount = prefs.getInt(STEP_COUNT_PREF_KEY, 0)
        stepOffset = prefs.getInt(STEP_OFFSET_PREF_KEY, 0)

        val savedDate = prefs.getString(TRACKING_DATE_PREF_KEY, null)
        currentTrackingDate = if (savedDate != null) {
            LocalDate.parse(savedDate)
        } else {
            LocalDate.now()
        }

        // Check if need to reset on app start
        val today = LocalDate.now()
        if (today.isAfter(currentTrackingDate)) {
            // reset when get the first sensor reading
            stepOffset = 0  // Force reset on next sensor reading
        }
    }
}