package com.yunfan.douyincontrol.engine

import com.yunfan.douyincontrol.api.AiRepository
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.util.SimilarityUtil

class QuizEngine(
    private val questionRepository: QuestionRepository,
    private val aiRepository: AiRepository? = null
) {

    suspend fun getQuestionsForStudy(subject: String, grade: String, count: Int = 10): List<QuestionEntity> {
        // 只取没做过的题
        val unanswered = questionRepository.getUnansweredQuestions(subject, grade, count)
        if (unanswered.isNotEmpty()) return unanswered.shuffled().take(count)

        // 没做过的题不够，需要 AI 补充
        if (aiRepository == null) {
            throw UnavailableException("AI 未配置，请先在家长设置中配置 DeepSeek API")
        }

        try {
            aiRepository.generateAndSaveQuestions(subject, grade, count + 3)
            val newUnanswered = questionRepository.getUnansweredQuestions(subject, grade, count)
            if (newUnanswered.isNotEmpty()) return newUnanswered.shuffled().take(count)
            throw UnavailableException("AI 出题失败，生成的题目与已有题目重复，请稍后再试")
        } catch (e: UnavailableException) {
            throw e
        } catch (e: Exception) {
            throw UnavailableException("AI 出题失败，请检查家长设置中的 API 配置是否正确")
        }
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

class UnavailableException(message: String) : Exception(message)
