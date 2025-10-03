package fi.tuni.lonelinessapp.data.repository

import kotlin.time.Duration

interface QuestionRepositoryInterface {

    fun fetchQuestion(questionID: Int): Int

    fun fetchPoint(userID: Int, questionID: Int): Int

    fun fetchTimeStamp(userID: Int, questionID: Int): Duration
}