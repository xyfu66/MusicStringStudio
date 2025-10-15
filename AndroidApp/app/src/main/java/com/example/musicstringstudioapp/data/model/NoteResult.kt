package com.example.musicstringstudioapp.data.model

/**
 * 音符演奏结果
 * 
 * 记录用户演奏某个音符的实际表现
 * 
 * @property targetNote 目标音符
 * @property detectedFrequency 检测到的频率 (Hz)
 * @property deviation 音分偏差 (cents)
 * @property isHit 是否命中（在时间窗口内且音准合格）
 * @property timestamp 检测时间戳 (毫秒)
 * @property accuracyLevel 准确度等级
 */
data class NoteResult(
    val targetNote: Note,
    val detectedFrequency: Float,
    val deviation: Float,
    val isHit: Boolean,
    val timestamp: Long,
    val accuracyLevel: AccuracyLevel = AccuracyLevel.POOR
) {
    /**
     * 准确度等级
     */
    enum class AccuracyLevel {
        PERFECT,  // 完美: < 10 cents
        GOOD,     // 良好: 10-25 cents
        FAIR,     // 偏差: 25-50 cents
        POOR,     // 较差: > 50 cents
        MISS      // 未演奏或严重偏差
    }
    
    /**
     * 获取色标颜色（用于UI显示）
     */
    fun getColorIndicator(): ColorIndicator {
        return when (accuracyLevel) {
            AccuracyLevel.PERFECT -> ColorIndicator.GREEN
            AccuracyLevel.GOOD -> ColorIndicator.YELLOW
            AccuracyLevel.FAIR -> ColorIndicator.ORANGE
            AccuracyLevel.POOR, AccuracyLevel.MISS -> ColorIndicator.RED
        }
    }
    
    enum class ColorIndicator {
        GREEN, YELLOW, ORANGE, RED
    }
    
    companion object {
        /**
         * 根据偏差计算准确度等级
         */
        fun calculateAccuracyLevel(deviation: Float): AccuracyLevel {
            val absCents = kotlin.math.abs(deviation)
            return when {
                absCents < 10 -> AccuracyLevel.PERFECT
                absCents < 25 -> AccuracyLevel.GOOD
                absCents < 50 -> AccuracyLevel.FAIR
                else -> AccuracyLevel.POOR
            }
        }
    }
}
