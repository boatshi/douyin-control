package com.yunfan.douyincontrol.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AiRequest(
    val model: String = "deepseek-chat",
    val messages: List<AiMessage>,
    val temperature: Double = 0.8
)

data class AiMessage(
    val role: String,
    val content: String
)

data class AiResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: AiMessage?
)

data class AiQuestion(
    val question: String,
    val options: List<String>,
    val answer: String
)

class AiService(
    private val apiUrl: String = "https://api.your-proxy.com/v1/chat/completions",
    private val apiKey: String = ""
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateQuestions(
        subject: String,
        grade: String,
        count: Int = 5
    ): List<AiQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiUrl.contains("your-proxy")) {
            throw IOException("AI 未配置，请先在家长设置 → API配置 中填入正确的接口地址和密钥")
        }
        val gradeNames = mapOf(
            "bigclass" to "幼儿园大班",
            "grade1" to "小学一年级",
            "grade2" to "小学二年级",
            "grade3" to "小学三年级",
            "grade4" to "小学四年级",
            "grade5" to "小学五年级",
            "grade6" to "小学六年级"
        )
        val subjectNames = mapOf("math" to "数学", "chinese" to "语文")
        val gradeName = gradeNames[grade] ?: "小学一年级"
        val subjectName = subjectNames[subject] ?: "数学"

        val prompt = """
你是一位小学教育专家，请为${gradeName}学生出${count}道${subjectName}选择题。
每道题4个选项，仅一个正确答案，选项用A、B、C、D标记。
以JSON格式返回，不要返回其他内容：
[
  {
    "question": "题目内容",
    "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
    "answer": "A. 选项1"
  }
]
        """.trimIndent()

        val requestBody = AiRequest(
            messages = listOf(
                AiMessage(role = "user", content = prompt)
            )
        )

        val jsonBody = gson.toJson(requestBody)
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        val aiResponse = gson.fromJson(responseBody, AiResponse::class.java)
        val content = aiResponse.choices?.firstOrNull()?.message?.content
            ?: throw IOException("No content in response")

        val jsonStr = content
            .replace(Regex("""^```json\s*""", RegexOption.MULTILINE), "")
            .replace(Regex("""^```\s*""", RegexOption.MULTILINE), "")
            .trim()

        val type = object : TypeToken<List<AiQuestion>>() {}.type
        gson.fromJson(jsonStr, type) ?: emptyList()
    }
}
