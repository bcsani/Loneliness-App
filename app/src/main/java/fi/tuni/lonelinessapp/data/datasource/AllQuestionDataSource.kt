package fi.tuni.lonelinessapp.data.datasource

import fi.tuni.lonelinessapp.data.dao.AllQuestionDao
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.time.Duration

class AllQuestionDataSource (
    private val allQuestionDao: AllQuestionDao
) {
    suspend fun addQuestion(allQuestion: AllQuestionEntity) = allQuestionDao.insertAllQuestionPoint(allQuestion)
    fun getAllQuestionPoints(): Flow<List<AllQuestionEntity>> = allQuestionDao.getAllQuestionPoints()

    fun getQuestionPointsByDate(date: LocalDate): Flow<AllQuestionEntity?> = allQuestionDao.getQuestionPointsByDate(date)
}