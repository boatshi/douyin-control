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
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, app: App) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 家长设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsItem("积分规则", "调整做题得分、抖音消耗积分") {
                navController.navigate(Screen.Rules.route)
            }
            SettingsItem("年级设置", "当前年级设置") {
                navController.navigate(Screen.Grade.route)
            }
            SettingsItem("题目管理", "查看/删除题目，AI出题") {
                navController.navigate(Screen.QuestionManage.route)
            }
            SettingsItem("学习数据", "查看学习统计") {
                navController.navigate(Screen.Stats.route)
            }
            SettingsItem("修改密码", "修改家长入口密码") {
                navController.navigate(Screen.ChangePassword.route)
            }
            SettingsItem("数据管理", "导出/重置数据") {
                navController.navigate(Screen.Data.route)
            }
            SettingsItem("API配置", "配置AI出题的接口地址和密钥") {
                navController.navigate(Screen.ApiConfig.route)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(title: String, desc: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(desc, fontSize = 14.sp, color = TextSecondary)
            }
            Text("→", fontSize = 20.sp, color = TextSecondary)
        }
    }
}
