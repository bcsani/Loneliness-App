package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.AllQuestionDataSource
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity

class AllQuestionRepository (
    private val allQuestionDataSource: AllQuestionDataSource
)  {
    suspend fun addQuestion(allQuestion: AllQuestionEntity) =
        allQuestionDataSource.addQuestion(allQuestion)

    fun getAllQuestionPoints(): List<AllQuestionEntity> =
        allQuestionDataSource.getAllQuestionPoints()
}