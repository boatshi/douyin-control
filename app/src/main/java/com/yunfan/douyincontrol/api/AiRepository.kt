package com.yunfan.douyincontrol.api

import com.google.gson.Gson
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.engine.QuizEngine

class AiRepository(
    private val aiService: AiService,
    private val quizEngine: QuizEngine
) {
    suspend fun generateAndSaveQuestions(
        subject: String,
        grade: String,
        count: Int = 5
    ): List<QuestionEntity> {
        val aiQuestions = aiService.generateQuestions(subject, grade, count)
        val entities = aiQuestions.map { aiq ->
            QuestionEntity(
                subject = subject,
                grade = grade,
                question = aiq.question,
                options = Gson().toJson(aiq.options),
                answer = aiq.answer,
                source = "ai"
            )
        }
        return quizEngine.deduplicateAndSave(entities)
    }
}
