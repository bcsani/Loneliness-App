package fi.tuni.lonelinessapp.data.datasource

import fi.tuni.lonelinessapp.data.dao.AllQuestionDao
import fi.tuni.lonelinessapp.data.entity.AllQuestionEntity
import kotlin.time.Duration

class AllQuestionDataSource (
    private val allQuestionDao: AllQuestionDao
) {
    suspend fun addQuestion(allQuestion: AllQuestionEntity) = allQuestionDao.insertAllQuestionPoint(allQuestion)
    fun getAllQuestionPoints(): List<AllQuestionEntity> = allQuestionDao.getAllQuestionPoints()
}