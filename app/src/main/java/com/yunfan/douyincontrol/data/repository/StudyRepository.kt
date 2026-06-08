package com.yunfan.douyincontrol.data.repository

import com.yunfan.douyincontrol.data.database.dao.StudyLogDao
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.database.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyLogDao: StudyLogDao) {

    suspend fun logQuestion(questionId: Long, subject: String, result: String) {
        studyLogDao.insert(
            StudyLogEntity(type = "question", subject = subject, questionId = questionId, result = result)
        )
    }

    suspend fun logVideo(durationMinutes: Int) {
        studyLogDao.insert(
            StudyLogEntity(type = "video", result = "watched", detail = durationMinutes.toString())
        )
    }

    fun getWrongQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> =
        studyLogDao.getWrongQuestionsBySubject(subject)

    suspend fun getQuestionCountToday(since: Long): Int =
        studyLogDao.getQuestionCountToday(since)

    suspend fun getCorrectCountToday(since: Long): Int =
        studyLogDao.getCorrectCountToday(since)

    suspend fun isInWrongBook(questionId: Long): Boolean =
        studyLogDao.isInWrongBook(questionId) > 0
}
