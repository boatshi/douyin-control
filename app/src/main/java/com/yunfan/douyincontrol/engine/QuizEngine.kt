package com.yunfan.douyincontrol.engine

import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.util.SimilarityUtil

class QuizEngine(private val questionRepository: QuestionRepository) {

    suspend fun getQuestionsForStudy(subject: String, grade: String, count: Int = 10): List<QuestionEntity> {
        val questions = questionRepository.getQuestionsForStudy(subject, grade, count)
        return questions.shuffled().take(count)
    }

    suspend fun deduplicateAndSave(newQuestions: List<QuestionEntity>): List<QuestionEntity> {
        val saved = mutableListOf<QuestionEntity>()
        for (q in newQuestions) {
            val existing = questionRepository.findByQuestionText(q.question)
            if (existing != null) continue

            val allExisting = questionRepository.getQuestionsForStudy(q.subject, q.grade, 1000)
            val isDuplicate = allExisting.any { SimilarityUtil.isSimilar(it.question, q.question) }
            if (isDuplicate) continue

            val id = questionRepository.insertQuestion(q)
            if (id > 0) {
                saved.add(q.copy(id = id))
            }
        }
        return saved
    }
}
