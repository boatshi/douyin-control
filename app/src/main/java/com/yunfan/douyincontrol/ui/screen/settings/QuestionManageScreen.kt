package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.api.AiRepository
import com.yunfan.douyincontrol.api.AiService
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.dataStore
import com.yunfan.douyincontrol.engine.QuizEngine
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.screen.settings.ApiConfigKeys
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuestionManageViewModel(private val app: App) : ViewModel() {
    private val questionRepo = app.questionRepository
    private val quizEngine = QuizEngine(questionRepo)
    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val questions: StateFlow<List<QuestionEntity>> = _questions.asStateFlow()
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    init { loadQuestions() }

    private fun loadQuestions() {
        viewModelScope.launch {
            questionRepo.getAllQuestions().collect { list ->
                _questions.value = list
            }
        }
    }

    fun deleteQuestion(id: Long) {
        viewModelScope.launch { questionRepo.softDelete(id) }
    }

    fun generateQuestions(subject: String, grade: String, count: Int = 5) {
        viewModelScope.launch {
            _isGenerating.value = true
            _message.value = "正在生成题目..."
            try {
                // 从 DataStore 读取 API 配置
                val prefs = app.dataStore.data.first()
                val apiUrl = prefs[ApiConfigKeys.API_URL]
                    ?: "https://api.your-proxy.com/v1/chat/completions"
                val apiKey = prefs[ApiConfigKeys.API_KEY] ?: ""
                val aiService = AiService(apiUrl = apiUrl, apiKey = apiKey)
                val aiRepository = AiRepository(aiService, quizEngine)

                val saved = aiRepository.generateAndSaveQuestions(subject, grade, count)
                _message.value = "成功生成 ${saved.size} 道新题目"
            } catch (e: Exception) {
                _message.value = "生成失败：${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionManageScreen(navController: NavController, app: App) {
    val viewModel: QuestionManageViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuestionManageViewModel(app) as T
            }
        }
    )
    val questions by viewModel.questions.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val message by viewModel.message.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20
    val totalItems = questions.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedQuestions = questions.drop(currentPage * pageSize).take(pageSize)

    var showGenerateDialog by remember { mutableStateOf(false) }
    var generateSubject by remember { mutableStateOf("math") }
    var generateGrade by remember { mutableStateOf("grade1") }
    var generateCount by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 题目管理") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { showGenerateDialog = true }) {
                        Text("AI出题", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (message.isNotEmpty()) {
                Text(
                    message,
                    fontSize = 14.sp,
                    color = if (message.contains("失败")) WrongRed else CorrectGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (pagedQuestions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无题目，点击右上角 AI出题 生成", fontSize = 16.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pagedQuestions) { index, question ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${currentPage * pageSize + index + 1}. ${question.question}",
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${if (question.subject == "math") "数学" else "语文"} | ${question.grade}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteQuestion(question.id) }) {
                                    Text("🗑️")
                                }
                            }
                        }
                    }
                }

                if (totalPages > 1) {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        totalItems = totalItems,
                        pageSize = pageSize,
                        onPageChange = { currentPage = it },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (showGenerateDialog) {
            AlertDialog(
                onDismissRequest = { showGenerateDialog = false },
                title = { Text("AI 出题") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("科目：")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = generateSubject == "math",
                                onClick = { generateSubject = "math" },
                                label = { Text("数学") }
                            )
                            FilterChip(
                                selected = generateSubject == "chinese",
                                onClick = { generateSubject = "chinese" },
                                label = { Text("语文") }
                            )
                        }
                        Text("年级：")
                        grades.forEach { (key, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = generateGrade == key,
                                    onClick = { generateGrade = key }
                                )
                                Text(name, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        OutlinedTextField(
                            value = generateCount,
                            onValueChange = { generateCount = it.filter { c -> c.isDigit() } },
                            label = { Text("生成数量（1-10）") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showGenerateDialog = false
                        val count = generateCount.toIntOrNull() ?: 5
                        viewModel.generateQuestions(generateSubject, generateGrade, count.coerceIn(1, 10))
                    }) { Text("开始生成") }
                },
                dismissButton = {
                    TextButton(onClick = { showGenerateDialog = false }) { Text("取消") }
                }
            )
        }
    }
}
