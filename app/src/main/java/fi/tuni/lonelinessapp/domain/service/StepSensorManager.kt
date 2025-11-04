package fi.tuni.lonelinessapp.domain.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast

class StepSensorManager(
    private val context: Context,
    private val onStepsUpdated: (Int) -> Unit
) : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialSteps: Float? = null
    private var tracking = false


    fun startTracking() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(context, "Step sensor not available", Toast.LENGTH_LONG).show()
            return
        }

        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
        tracking = true

    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
        tracking = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(!tracking) return
        val steps = event.values[0]

        if (initialSteps == null) {
            initialSteps = steps
        }
        val stepsToday = (steps - (initialSteps ?: 0f)).toInt()
        onStepsUpdated(stepsToday)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}