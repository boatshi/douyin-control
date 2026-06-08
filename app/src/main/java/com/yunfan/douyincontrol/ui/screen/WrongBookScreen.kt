package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongBookScreen(navController: NavController, app: App) {
    val viewModel = remember { WrongBookViewModel(app.studyRepository) }
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val wrongQuestions by viewModel.wrongQuestions.collectAsState()
    val gson = remember { Gson() }

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10
    val totalItems = wrongQuestions.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedQuestions = wrongQuestions.drop(currentPage * pageSize).take(pageSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("❌ 错题本") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedSubject == "math",
                    onClick = { viewModel.selectSubject("math"); currentPage = 0 },
                    label = { Text("数学", fontSize = 16.sp) }
                )
                FilterChip(
                    selected = selectedSubject == "chinese",
                    onClick = { viewModel.selectSubject("chinese"); currentPage = 0 },
                    label = { Text("语文", fontSize = 16.sp) }
                )
            }

            if (wrongQuestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无错题，继续保持！🎉", fontSize = 18.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pagedQuestions) { index, question ->
                        val options = try {
                            gson.fromJson(question.options, object : TypeToken<List<String>>() {}.type) as? List<String>
                                ?: emptyList()
                        } catch (e: Exception) { emptyList() }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "${currentPage * pageSize + index + 1}. ${question.question}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "正确答案：${question.answer}",
                                    fontSize = 14.sp,
                                    color = CorrectGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Study.createRoute(question.subject))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleStart)
                                ) {
                                    Text("重新挑战", fontSize = 14.sp)
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
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
