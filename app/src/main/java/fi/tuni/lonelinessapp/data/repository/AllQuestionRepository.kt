package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.datasource.AllQuestionDataSource
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AllQuestionRepository (
    private val allQuestionDataSource: AllQuestionDataSource
)  {
    suspend fun addQuestion(allQuestion: AllQuestionEntity) =
        allQuestionDataSource.addQuestion(allQuestion)

    fun getAllQuestionPoints(): Flow<List<AllQuestionEntity>> =
        allQuestionDataSource.getAllQuestionPoints()

    fun getQuestionPointsByDate(date: LocalDate): Flow<AllQuestionEntity?> =
        allQuestionDataSource.getQuestionPointsByDate(date)

    fun getQuestionPointsFromDate(startDate: LocalDate): Flow<List<AllQuestionEntity>> =
        allQuestionDataSource.getQuestionPointsFromDate(startDate)
}