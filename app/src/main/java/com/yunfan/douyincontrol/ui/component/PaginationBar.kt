package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.PurpleStart

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "显示 ${currentPage * pageSize + 1}-${minOf((currentPage + 1) * pageSize, totalItems)} / 共 $totalItems 条",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { if (currentPage > 0) onPageChange(currentPage - 1) },
                enabled = currentPage > 0
            ) {
                Text("← 上一页", fontSize = 14.sp)
            }

            val startPage = maxOf(0, currentPage - 2)
            val endPage = minOf(totalPages - 1, startPage + 4)
            for (i in startPage..endPage) {
                if (i == currentPage) {
                    TextButton(onClick = {}) {
                        Text(
                            "${i + 1}",
                            fontWeight = FontWeight.Bold,
                            color = PurpleStart,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    TextButton(onClick = { onPageChange(i) }) {
                        Text("${i + 1}", fontSize = 14.sp)
                    }
                }
            }

            TextButton(
                onClick = { if (currentPage < totalPages - 1) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages - 1
            ) {
                Text("下一页 →", fontSize = 14.sp)
            }
        }
    }
}
