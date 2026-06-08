package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.Gold
import com.yunfan.douyincontrol.ui.theme.TextPrimary

@Composable
fun TimerView(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱️ ", fontSize = 20.sp)
            Text(
                text = "剩余 %02d:%02d".format(minutes, seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (remainingSeconds < 60) MaterialTheme.colorScheme.error else TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = Gold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
