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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController, app: App) {
    val viewModel: StatsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return StatsViewModel(app.pointsRepository, app.studyRepository) as T
            }
        }
    )

    val balance by viewModel.balance.collectAsState()
    val todayCorrect by viewModel.todayCorrect.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20
    val totalItems = logs.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedLogs = logs.drop(currentPage * pageSize).take(pageSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 学习统计") },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("⭐ 积分", "$balance")
                        StatItem("📝 今日做题", "$todayTotal")
                        StatItem("✅ 正确率", if (todayTotal > 0) "${(todayCorrect * 100 / todayTotal)}%" else "0%")
                    }
                }
            }

            item {
                Text("积分流水", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            if (pagedLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无记录", fontSize = 16.sp, color = TextSecondary)
                    }
                }
            } else {
                itemsIndexed(pagedLogs) { _, log ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.sourceDetail, fontSize = 14.sp, color = TextPrimary)
                                val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.CHINA)
                                Text(
                                    sdf.format(Date(log.createdAt)),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "${if (log.type == "earn") "+" else "-"}${log.amount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.type == "earn") CorrectGreen else WrongRed
                            )
                        }
                    }
                }
            }

            if (totalPages > 1) {
                item {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        totalItems = totalItems,
                        pageSize = pageSize,
                        onPageChange = { currentPage = it }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PurpleStart)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 14.sp, color = TextSecondary)
    }
}
