package fi.tuni.lonelinessapp.data.repository

import fi.tuni.lonelinessapp.data.source.local.Survey
import fi.tuni.lonelinessapp.data.source.local.SurveyDao
import java.time.LocalDate

class SurveyRepo(private val surveyDao: SurveyDao) {
    val readAllData: List<Survey> = surveyDao.getData()

    fun addSurveyResponse(score: Int) {
        surveyDao.insert(Survey(LocalDate.now().toEpochDay(), score))
    }
}
