package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.dataStore
import com.yunfan.douyincontrol.ui.component.QuestionCard
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(navController: NavController, app: App, subject: String) {
    val gson = remember { Gson() }
    val viewModel = remember {
        StudyViewModel(app.questionRepository, app.pointsRepository, app.studyRepository, app.dataStore)
    }
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val totalQuestions by viewModel.totalQuestions.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val score by viewModel.score.collectAsState()
    val earning by viewModel.earningThisSession.collectAsState()
    val finished by viewModel.finished.collectAsState()

    val subjectName = if (subject == "math") "数学" else "语文"

    LaunchedEffect(showResult) {
        if (showResult) {
            delay(1500)
            viewModel.nextQuestion()
        }
    }

    // 读取家长设置的年级（只读一次）
    LaunchedEffect(subject) {
        val prefs = app.dataStore.data.first()
        val grade = prefs[androidx.datastore.preferences.core.stringPreferencesKey("current_grade")] ?: "grade1"
        viewModel.startStudy(subject, grade)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${subjectName}挑战") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 退出", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            if (finished) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("全部完成！", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("答对 $score / $totalQuestions 题", fontSize = 20.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("获得 $earning 积分", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("返回首页", fontSize = 18.sp) }
                }
            } else if (currentQuestion != null) {
                val question = currentQuestion!!
                val options = try {
                    gson.fromJson(question.options, object : TypeToken<List<String>>() {}.type) as? List<String>
                        ?: listOf("A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4")
                } catch (e: Exception) {
                    listOf("A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4")
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("第 ${currentIndex + 1}/$totalQuestions 题", fontSize = 16.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    val progressFloat = remember(currentIndex, totalQuestions) {
                        (currentIndex + 1).toFloat() / totalQuestions
                    }
                    LinearProgressIndicator(
                        progress = progressFloat,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = PurpleStart,
                        trackColor = Color.LightGray,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    QuestionCard(
                        question = question.question,
                        options = options,
                        selectedAnswer = selectedAnswer,
                        correctAnswer = if (showResult) question.answer else null,
                        showResult = showResult,
                        onOptionSelected = { viewModel.selectAnswer(it) }
                    )

                    if (showResult) {
                        val (emoji, msg, color) = if (isCorrect == true) {
                            Triple("🎉", "答对了！积分 +${if (isCorrect == true) earning else 0}", CorrectGreen)
                        } else {
                            Triple("😊", "正确答案：${question.answer}", WrongRed)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("$emoji  $msg", fontSize = 18.sp, color = color, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "本局获得 $earning 积分",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(modifier = Modifier.width(120.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载题目中...", fontSize = 18.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
