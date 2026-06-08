package com.yunfan.douyincontrol.ui.screen

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.TimerView
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
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

    // 执行扣分和退出（在协程中执行）
    LaunchedEffect(shouldExit) {
        if (!shouldExit) return@LaunchedEffect
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

    // 启动：检查积分但不扣
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

    // 倒计时
    LaunchedEffect(remainingSeconds, isReady) {
        if (!isReady || remainingSeconds <= 0) return@LaunchedEffect
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        // 时间到了，触发退出扣分
        timeUpExit = true
        dialogMessage = "时间到！去做题赚积分吧！"
        showDialog = true
    }

    // 弹窗
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight)
        ) {
            if (isReady) {
                TimerView(
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载中...", fontSize = 16.sp, color = TextSecondary)
                }
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // 强制硬件加速
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            val isHuawei = android.os.Build.MANUFACTURER.lowercase().contains("huawei") ||
                                    android.os.Build.BRAND.lowercase().contains("huawei")
                            // 所有设备统一用桌面版UA，华为WebView对桌面版兼容更好
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            if (isHuawei) {
                                // 华为浏览器引擎兼容
                                layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
                            }
                            cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                            mediaPlaybackRequiresUserGesture = false
                            builtInZoomControls = false
                            displayZoomControls = false
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NARROW_COLUMNS
                            blockNetworkLoads = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return if (url.startsWith("http")) { false } else { true }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // 页面加载完成后修正viewport，确保内容适配屏幕
                                view?.evaluateJavascript("""
                                    (function(){
                                        var meta = document.querySelector('meta[name="viewport"]');
                                        if (!meta) {
                                            meta = document.createElement('meta');
                                            meta.name = 'viewport';
                                            document.head.appendChild(meta);
                                        }
                                        meta.content = 'width=device-width, initial-scale=0.5, maximum-scale=5.0';
                                        document.body.style.zoom = '0.8';
                                        document.documentElement.style.zoom = '0.8';
                                    })();
                                """.trimIndent(), null)
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl("https://www.douyin.com")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
