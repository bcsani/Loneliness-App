package fi.tuni.lonelinessapp.data.repository

import kotlin.time.Duration

interface AllQuestionRepositoryInterface {

    fun fetchPoint(timestamp: Duration): Int

    fun fetchWeekPoints(timestamp: Duration): Int

    fun fetchTimeStamp(userID: Int, questionID: Int): Duration
}