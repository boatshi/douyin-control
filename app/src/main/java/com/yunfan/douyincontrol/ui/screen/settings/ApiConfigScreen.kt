package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.dataStore
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.launch

object ApiConfigKeys {
    val API_URL = stringPreferencesKey("api_url")
    val API_KEY = stringPreferencesKey("api_key")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(navController: NavController, app: App) {
    val scope = rememberCoroutineScope()

    var apiUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        app.dataStore.data.collect { prefs ->
            apiUrl = prefs[ApiConfigKeys.API_URL] ?: "https://api.your-proxy.com/v1/chat/completions"
            apiKey = prefs[ApiConfigKeys.API_KEY] ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 配置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "配置 AI 出题的接口信息",
                fontSize = 16.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it; saved = false },
                label = { Text("API 地址") },
                placeholder = { Text("https://api.your-proxy.com/v1/chat/completions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        app.dataStore.edit { prefs ->
                            prefs[ApiConfigKeys.API_URL] = apiUrl
                            prefs[ApiConfigKeys.API_KEY] = apiKey
                        }
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存", fontSize = 18.sp)
            }

            if (saved) {
                Text(
                    "✅ 已保存",
                    color = CorrectGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 说明", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "AI出题使用 DeepSeek API（兼容 OpenAI 格式）。\n\n" +
                        "如果你有自己的 API 代理，填写代理地址；\n" +
                        "如果直接用 DeepSeek 官方，地址为：\n" +
                        "https://api.deepseek.com/v1/chat/completions",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
