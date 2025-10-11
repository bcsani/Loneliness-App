package fi.tuni.lonelinessapp.data.datasource

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepCounterDataSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _steps = MutableStateFlow(0)
    val steps = _steps.asStateFlow()

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        println("Start listening")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        println("Stop listening")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            _steps.value = it.values[0].toInt()
        }
        println("Steps" + _steps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}