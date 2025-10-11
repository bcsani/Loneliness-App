package fi.tuni.lonelinessapp.data.repository

import kotlinx.coroutines.flow.StateFlow
import fi.tuni.lonelinessapp.data.datasource.StepCounterDataSource

class StepRepository(private val dataSource: StepCounterDataSource) {
    val steps: StateFlow<Int> = dataSource.steps

    fun startTracking() = dataSource.startListening()
    fun stopTracking() = dataSource.stopListening()

}