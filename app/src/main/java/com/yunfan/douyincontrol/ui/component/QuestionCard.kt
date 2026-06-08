package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.*

@Composable
fun QuestionCard(
    question: String,
    options: List<String>,
    selectedAnswer: String?,
    correctAnswer: String?,
    showResult: Boolean,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            options.forEach { option ->
                val isSelected = selectedAnswer == option
                val isCorrect = correctAnswer == option
                val bgColor = when {
                    showResult && isCorrect -> CorrectGreen.copy(alpha = 0.15f)
                    showResult && isSelected && !isCorrect -> WrongRed.copy(alpha = 0.15f)
                    isSelected -> PurpleStart.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    showResult && isCorrect -> CorrectGreen
                    showResult && isSelected && !isCorrect -> WrongRed
                    isSelected -> PurpleStart
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                OutlinedButton(
                    onClick = { if (!showResult) onOptionSelected(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor),
                    border = BorderStroke(1.dp, SolidColor(borderColor))
                ) {
                    Text(
                        text = option,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
