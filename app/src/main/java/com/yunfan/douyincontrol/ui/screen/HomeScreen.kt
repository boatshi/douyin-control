package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.dataStore
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(navController: NavController, app: App) {
    val viewModel = remember {
        HomeViewModel(app.pointsRepository, app.studyRepository, app.dataStore)
    }
    val balance by viewModel.balance.collectAsState()

    var isLongPressing by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableStateOf(0f) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
    }

    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            longPressProgress = 0f
            while (longPressProgress < 1f) {
                delay(100)
                longPressProgress += 0.02f
            }
            showPasswordDialog = true
            isLongPressing = false
            longPressProgress = 0f
        } else {
            longPressProgress = 0f
        }
    }

    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        val lockedUntil by viewModel.lockedUntil.collectAsState()

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; password = "" },
            title = { Text("家长验证", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (System.currentTimeMillis() < lockedUntil) {
                        Text("密码错误次数过多，请等待30秒", color = WrongRed)
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                if (it.length <= 4) password = it; errorMsg = ""
                            },
                            label = { Text("请输入4位数字密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = WrongRed, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (System.currentTimeMillis() < lockedUntil) {
                    TextButton(onClick = {}) { Text("等待中...") }
                } else {
                    TextButton(onClick = {
                        if (viewModel.verifyPassword(password)) {
                            showPasswordDialog = false; password = ""
                            navController.navigate(Screen.Settings.route)
                        } else {
                            errorMsg = "密码错误"
                        }
                    }) { Text("确认") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; password = "" }) { Text("取消") }
            }
        )
    }

    // 选择科目弹窗
    if (showSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text("选择科目", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showSubjectDialog = false; navController.navigate(Screen.Study.createRoute("math")) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("➕ 数学", fontSize = 20.sp)
                    }
                    Button(
                        onClick = { showSubjectDialog = false; navController.navigate(Screen.Study.createRoute("chinese")) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleEnd)
                    ) {
                        Text("📝 语文", fontSize = 20.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSubjectDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(PurpleStart, PurpleEnd)))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧸", fontSize = 48.sp)
            Text(
                "欢迎回来，宝贝！",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ 积分：$balance", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundLight)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeButton(emoji = "📚", label = "去做题", onClick = {
                        showSubjectDialog = true
                    }, modifier = Modifier.weight(1f))
                    HomeButton(emoji = "📱", label = "看抖音", onClick = {
                        navController.navigate(Screen.Douyin.route)
                    }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeButton(emoji = "❌", label = "错题本", onClick = {
                        navController.navigate(Screen.WrongBook.route)
                    }, modifier = Modifier.weight(1f))
                    HomeButton(emoji = "🏆", label = "学习统计", onClick = {
                        navController.navigate(Screen.Stats.route)
                    }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "[ 长按进入家长设置 ]",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { isLongPressing = true },
                                onPress = {
                                    try { awaitRelease() } finally { isLongPressing = false }
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun HomeButton(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
