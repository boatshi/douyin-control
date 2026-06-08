package com.yunfan.douyincontrol.data.seed

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.data.database.AppDatabase
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class SeedQuestions(private val context: Context, private val database: AppDatabase) {

    data class SeedQuestion(
        val subject: String,
        val grade: String,
        val question: String,
        val options: List<String>,
        val answer: String
    )

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val count = database.questionDao().countBySubjectAndGrade("math", "bigclass")
        if (count > 0) return@withContext

        try {
            val inputStream = context.assets.open("seed_questions.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val type = object : TypeToken<List<SeedQuestion>>() {}.type
            val seeds: List<SeedQuestion> = gson.fromJson(reader, type)

            val entities = seeds.map { seed ->
                QuestionEntity(
                    subject = seed.subject,
                    grade = seed.grade,
                    question = seed.question,
                    options = gson.toJson(seed.options),
                    answer = seed.answer,
                    source = "seed"
                )
            }
            database.questionDao().insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
