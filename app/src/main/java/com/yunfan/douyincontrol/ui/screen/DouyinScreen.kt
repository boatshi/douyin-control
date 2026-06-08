package com.yunfan.douyincontrol.ui.screen

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.TimerView
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DouyinScreen(navController: NavController, app: App) {
    val pointsRepository = app.pointsRepository
    var remainingSeconds by remember { mutableIntStateOf(-1) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var enterTime by remember { mutableLongStateOf(0L) }
    var costPerMinute by remember { mutableIntStateOf(10) }
    var shouldExit by remember { mutableStateOf(false) }
    var timeUpExit by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    // 优先使用预加载的 Session，没有才新建
    val isPreloaded = remember { mutableStateOf(false) }
    val geckoSession = remember {
        app.takePreloadedSession()?.also {
            Log.i("GeckoView", "⚡ 使用预加载的 Session，秒开！")
            isPreloaded.value = true
        } ?: run {
            Log.i("GeckoView", "⏳ 没有预加载，新建 Session")
            GeckoSession()
        }
    }

    // 页面销毁时强制关闭 Session，停掉声音
    DisposableEffect(Unit) {
        onDispose { geckoSession.close() }
    }

    LaunchedEffect(shouldExit) {
        if (!shouldExit) return@LaunchedEffect
        geckoSession.close()
        if (enterTime > 0 && totalSeconds > 0) {
            val elapsedMs = System.currentTimeMillis() - enterTime
            val elapsedMinutes = (elapsedMs / 60000).toInt().coerceAtLeast(1)
            val deductAmount = elapsedMinutes * costPerMinute
            val currentBalance = pointsRepository.getBalance()
            val actualDeduct = minOf(deductAmount, currentBalance)
            if (actualDeduct > 0) {
                pointsRepository.spendPoints(
                    amount = actualDeduct,
                    source = "video_cost",
                    detail = "看抖音${elapsedMinutes}分钟${if (timeUpExit) "(完整看完)" else ""}"
                )
            }
        }
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        val balance = pointsRepository.getBalance()
        val rule = pointsRepository.getRule("cost_per_minute")
        val threshold = pointsRepository.getRule("min_cost_threshold")
        costPerMinute = rule?.value ?: 10
        val minThreshold = threshold?.value ?: 10

        if (balance < minThreshold) {
            dialogMessage = "积分不足，去做题赚积分吧！"
            showDialog = true
            return@LaunchedEffect
        }

        val maxMinutes = balance / costPerMinute
        val minutes = minOf(maxMinutes, 30)
        totalSeconds = minutes * 60
        remainingSeconds = totalSeconds
        enterTime = System.currentTimeMillis()
        isReady = true
    }

    LaunchedEffect(remainingSeconds, isReady) {
        if (!isReady || remainingSeconds <= 0) return@LaunchedEffect
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        timeUpExit = true
        dialogMessage = "时间到！去做题赚积分吧！"
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { shouldExit = true },
            title = { Text(if (dialogMessage.contains("不足")) "积分不足" else "时间到！") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { shouldExit = true }) { Text("去做题") }
            },
            dismissButton = {
                TextButton(onClick = { shouldExit = true }) { Text("返回首页") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📱 抖音") },
                navigationIcon = {
                    TextButton(onClick = { shouldExit = true }) {
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
            modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight)
        ) {
            if (isReady) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TimerView(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        modifier = Modifier.padding(16.dp)
                    )

                    AndroidView(
                        factory = { context ->
                            org.mozilla.geckoview.GeckoView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                if (isPreloaded.value) {
                                    Log.i("GeckoView", "⚡ Session 已预加载，直接显示")
                                    isLoading = false
                                } else {
                                    Log.i("GeckoView", "⏳ 非预加载 Session，开始加载")
                                    geckoSession.open(app.geckoRuntime)
                                    geckoSession.settings.userAgentOverride = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                    geckoSession.progressDelegate = MyProgressDelegate { isLoading = it }
                                    geckoSession.loadUri("https://www.douyin.com")
                                    isLoading = true
                                }
                                setSession(geckoSession)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isLoading && isReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏳", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在加载抖音...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("GeckoView 首次加载较慢", fontSize = 14.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = PurpleStart
                            )
                        }
                    }
                }
            }

            if (!isReady && !showDialog) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("检查积分中...", fontSize = 16.sp, color = TextSecondary)
                }
            }
        }
    }
}

private class MyProgressDelegate(private val onLoadingChange: (Boolean) -> Unit) : ProgressDelegate {
    override fun onPageStop(session: GeckoSession, success: Boolean) { onLoadingChange(false) }
    override fun onPageStart(session: GeckoSession, uri: String) { onLoadingChange(true) }
    override fun onProgressChange(session: GeckoSession, progress: Int) {}
    override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {}
    override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) {}
}
