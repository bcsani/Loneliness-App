package fi.tuni.lonelinessapp.data.repository

import kotlin.time.Duration

class QuestionRepository (
    private val questionDataSource: QuestionDataSource
) : QuestionRepositoryInterface {
    override fun fetchQuestion(questionID: Int): Int {
        return questionDataSource.fetchQuestion(questionID)
    }

    override fun fetchPoint(userID: Int, questionID: Int): Int {
        return questionDataSource.fetchPoint(userID, questionID)
    }

    override fun fetchTimeStamp(userID: Int, questionID: Int): Duration {
        return questionDataSource.fetchTimeStamp(userID, questionID)
    }

}