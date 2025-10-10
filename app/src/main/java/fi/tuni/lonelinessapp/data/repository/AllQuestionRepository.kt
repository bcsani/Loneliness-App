package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.AllQuestionDataSource
import kotlin.time.Duration

class AllQuestionRepository (
    private val allQuestionDataSource: AllQuestionDataSource
) : AllQuestionRepositoryInterface {

    override fun fetchPoint(timestamp: Duration): Int {
        return allQuestionDataSource.fetchPoint(timestamp)
    }

    override fun fetchWeekPoints(timestamp: Duration): Int {
        TODO("Not yet implemented")
    }

    override fun fetchTimeStamp(userID: Int, questionID: Int): Duration {
        return allQuestionDataSource.fetchTimeStamp(userID, questionID)
    }

}