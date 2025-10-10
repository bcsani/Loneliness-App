package fi.tuni.lonelinessapp.data.datasource

import kotlin.time.Duration

interface AllQuestionDataSourceInterface {
    fun fetchPoint(timestamp: Duration): Int

    fun fetchTimeStamp(userID: Int, questionID: Int): Duration
}