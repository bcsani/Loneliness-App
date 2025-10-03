package fi.tuni.lonelinessapp.data.datasource

import kotlin.time.Duration

class AllQuestionDataSource : AllQuestionDataSourceInterface {

    override fun fetchPoint(timestamp: Duration): Int {
        TODO("Not yet implemented")
    }

    override fun fetchTimeStamp(userID: Int, questionID: Int): Duration {
        TODO("Not yet implemented")
    }
}