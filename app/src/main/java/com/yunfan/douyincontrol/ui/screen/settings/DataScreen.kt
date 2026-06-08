package com.yunfan.douyincontrol.ui.screen.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(navController: NavController, app: App) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("← 返回", color = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleStart, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                onClick = { scope.launch { exportData(context, app) } },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📤 导出学习记录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("导出为CSV格式", fontSize = 14.sp, color = TextSecondary)
                }
            }

            Card(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🗑️ 重置所有数据", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WrongRed)
                    Text("清除所有数据，此操作不可恢复", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("确认重置？") },
                text = { Text("清除所有数据包括题目、积分、学习记录。此操作不可恢复！") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) { context.deleteDatabase("douyin_control.db") }
                            Toast.makeText(context, "数据已重置", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("确认重置", color = WrongRed) }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("取消") } }
            )
        }
    }
}

private suspend fun exportData(context: Context, app: App) = withContext(Dispatchers.IO) {
    try {
        val logs = app.database.studyLogDao().getPage(1000, 0)
        val file = File(context.getExternalFilesDir(null), "study_logs_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.write("ID,类型,科目,结果,详情,时间\n")
            logs.forEach { log ->
                writer.write("${log.id},${log.type},${log.subject},${log.result},${log.detail ?: ""},${log.createdAt}\n")
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "已导出到：${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
