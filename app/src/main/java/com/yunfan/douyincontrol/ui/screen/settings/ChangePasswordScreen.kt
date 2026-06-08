package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
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
import com.yunfan.douyincontrol.util.PasswordUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController, app: App) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改密码") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("旧密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码（4位数字）") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认新密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (newPassword.length != 4 || newPassword.any { !it.isDigit() }) {
                        message = "新密码必须为4位数字"; isError = true; return@Button
                    }
                    if (newPassword != confirmPassword) {
                        message = "两次密码不一致"; isError = true; return@Button
                    }
                    scope.launch {
                        val storedHash = app.dataStore.data.first()[stringPreferencesKey("parent_password_hash")]
                        if (storedHash != null && !PasswordUtil.verify(oldPassword, storedHash)) {
                            message = "旧密码错误"; isError = true; return@launch
                        }
                        app.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey("parent_password_hash")] = PasswordUtil.hash(newPassword)
                        }
                        message = "密码修改成功！"; isError = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("确认修改") }

            if (message.isNotEmpty()) {
                Text(message, color = if (isError) WrongRed else CorrectGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
