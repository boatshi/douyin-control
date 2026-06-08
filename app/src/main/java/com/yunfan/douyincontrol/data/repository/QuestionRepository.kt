package com.yunfan.douyincontrol.data.repository

import com.yunfan.douyincontrol.data.database.dao.QuestionDao
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

class QuestionRepository(private val questionDao: QuestionDao) {

    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> =
        questionDao.getAllBySubject(subject)

    fun getAllQuestions(): Flow<List<QuestionEntity>> =
        questionDao.getAllQuestions()

    suspend fun getQuestionsForStudy(subject: String, grade: String, limit: Int = 20): List<QuestionEntity> =
        questionDao.getQuestionsBySubjectAndGradeLimit(subject, grade, limit)

    suspend fun getUnansweredQuestions(subject: String, grade: String, limit: Int = 20): List<QuestionEntity> =
        questionDao.getUnansweredQuestions(subject, grade, limit)

    suspend fun countBySubjectAndGrade(subject: String, grade: String): Int =
        questionDao.countBySubjectAndGrade(subject, grade)

    suspend fun findByQuestionText(text: String): QuestionEntity? =
        questionDao.findByQuestionText(text)

    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long> =
        questionDao.insertAll(questions)

    suspend fun insertQuestion(question: QuestionEntity): Long =
        questionDao.insert(question)

    suspend fun softDelete(id: Long) =
        questionDao.softDelete(id)
}
