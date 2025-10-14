package com.example.musicstringstudioapp.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 音准指示器组件
 * 
 * 类似调音器，显示音高偏差
 * 
 * @param centsDeviation 音分偏差 (-50 到 +50)
 * @param accuracyLevel 准确度等级
 * @param isSilent 是否静音
 */
@Composable
fun PitchIndicator(
    centsDeviation: Float,
    accuracyLevel: String,
    isSilent: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "音准指示",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 音准指示器画布
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 32.dp)
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2
            
            // 颜色定义
            val perfectColor = Color(0xFF4CAF50)  // 绿色
            val goodColor = Color(0xFFFFEB3B)     // 黄色
            val fairColor = Color(0xFFFF9800)     // 橙色
            val poorColor = Color(0xFFF44336)     // 红色
            val neutralColor = Color(0xFF9E9E9E)  // 灰色
            
            // 绘制刻度线
            for (i in -5..5) {
                val x = centerX + (i * width / 12)
                val lineHeight = if (i == 0) 30f else if (i % 5 == 0) 20f else 10f
                val lineColor = if (i == 0) perfectColor else neutralColor
                val strokeWidth = if (i == 0) 3f else 1f
                
                drawLine(
                    color = lineColor,
                    start = Offset(x, centerY - lineHeight / 2),
                    end = Offset(x, centerY + lineHeight / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            
            // 绘制刻度标签
            // -50, -25, 0, +25, +50
            
            // 绘制指针（如果不是静音）
            if (!isSilent && abs(centsDeviation) <= 50) {
                // 将偏差映射到屏幕坐标
                // -50 cents -> 最左边, +50 cents -> 最右边
                val normalizedDeviation = (centsDeviation / 50).coerceIn(-1f, 1f)
                val pointerX = centerX + (normalizedDeviation * width / 2.4f)
                
                // 根据准确度选择颜色
                val pointerColor = when (accuracyLevel) {
                    "perfect" -> perfectColor
                    "good" -> goodColor
                    "fair" -> fairColor
                    "poor" -> poorColor
                    else -> neutralColor
                }
                
                // 绘制指针（三角形）
                val pointerSize = 20f
                drawLine(
                    color = pointerColor,
                    start = Offset(pointerX, centerY - 40),
                    end = Offset(pointerX, centerY + 40),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                
                // 绘制指针头部（圆圈）
                drawCircle(
                    color = pointerColor,
                    radius = 8f,
                    center = Offset(pointerX, centerY - 40)
                )
            }
            
            // 绘制中心线（0点）
            drawLine(
                color = perfectColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 2f,
                alpha = 0.3f
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 刻度标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-50¢",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "-25¢",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "0¢",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "+25¢",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+50¢",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态提示
        Text(
            text = when {
                isSilent -> "🎤 请演奏或唱出音符"
                accuracyLevel == "perfect" -> "✅ 完美！音准非常准确"
                accuracyLevel == "good" -> "👍 良好！继续保持"
                accuracyLevel == "fair" -> "⚠️ 偏差较大，需要调整"
                accuracyLevel == "poor" -> "❌ 偏差很大，请重新调整"
                else -> "等待检测..."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when (accuracyLevel) {
                "perfect" -> Color(0xFF4CAF50)
                "good" -> Color(0xFFFFEB3B)
                "fair" -> Color(0xFFFF9800)
                "poor" -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
