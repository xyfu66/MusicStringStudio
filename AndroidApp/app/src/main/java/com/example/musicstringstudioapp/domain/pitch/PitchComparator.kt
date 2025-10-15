package com.example.musicstringstudioapp.domain.pitch

import com.example.musicstringstudioapp.data.model.Note
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

/**
 * 音准比对器
 * 
 * 比对用户演奏的音高与目标音高，计算偏差
 * 提供音准评级和实时反馈
 */
class PitchComparator {
    
    companion object {
        // 音准评级阈值（cents）
        const val PERFECT_THRESHOLD = 10f    // 完美：< 10 cents
        const val GOOD_THRESHOLD = 25f       // 良好：10-25 cents
        const val FAIR_THRESHOLD = 50f       // 一般：25-50 cents
        // > 50 cents = 较差
        
        // 时间窗口阈值（毫秒）
        const val TIME_WINDOW_EARLY = -100L  // 提前 100ms 内算命中
        const val TIME_WINDOW_LATE = 200L    // 延后 200ms 内算命中
    }
    
    /**
     * 音准评级
     */
    enum class PitchAccuracy {
        PERFECT,  // 完美
        GOOD,     // 良好
        FAIR,     // 一般
        POOR,     // 较差
        MISS      // 未演奏/错过
    }
    
    /**
     * 比对结果
     */
    data class ComparisonResult(
        val targetNote: Note,              // 目标音符
        val detectedFrequency: Float,      // 检测到的频率（Hz）
        val deviationCents: Float,         // 偏差（cents）
        val accuracy: PitchAccuracy,       // 音准评级
        val timingErrorMs: Long,           // 时间误差（毫秒）
        val isInTimeWindow: Boolean        // 是否在时间窗口内
    )
    
    /**
     * 比对用户演奏音高与目标音符
     * 
     * @param targetNote 目标音符
     * @param detectedFrequency 检测到的频率（Hz）
     * @param currentTimeMs 当前时间（毫秒）
     * @return 比对结果
     */
    fun compare(
        targetNote: Note,
        detectedFrequency: Float,
        currentTimeMs: Long
    ): ComparisonResult {
        
        // 1. 计算音高偏差（cents）
        val deviationCents = calculateDeviationCents(
            targetNote.frequency,
            detectedFrequency
        )
        
        // 2. 评估音准等级
        val accuracy = evaluateAccuracy(deviationCents)
        
        // 3. 计算时间误差
        val timingErrorMs = calculateTimingError(targetNote, currentTimeMs)
        
        // 4. 判断是否在时间窗口内
        val isInTimeWindow = isInTimeWindow(timingErrorMs)
        
        val result = ComparisonResult(
            targetNote = targetNote,
            detectedFrequency = detectedFrequency,
            deviationCents = deviationCents,
            accuracy = accuracy,
            timingErrorMs = timingErrorMs,
            isInTimeWindow = isInTimeWindow
        )
        
        Timber.d("音准比对: 目标=${targetNote.pitch}(${targetNote.frequency}Hz), " +
                "检测=${detectedFrequency}Hz, 偏差=${deviationCents}cents, " +
                "评级=$accuracy, 时间误差=${timingErrorMs}ms")
        
        return result
    }
    
    /**
     * 快速判断音高是否接近目标
     * 
     * @param targetFrequency 目标频率（Hz）
     * @param detectedFrequency 检测到的频率（Hz）
     * @param thresholdCents 阈值（cents）
     * @return 是否接近
     */
    fun isCloseToTarget(
        targetFrequency: Float,
        detectedFrequency: Float,
        thresholdCents: Float = GOOD_THRESHOLD
    ): Boolean {
        val deviation = abs(calculateDeviationCents(targetFrequency, detectedFrequency))
        return deviation <= thresholdCents
    }
    
    /**
     * 计算两个频率之间的偏差（cents）
     * 
     * 1个半音 = 100 cents
     * Cents = 1200 * log2(f2 / f1)
     * 
     * @param targetFrequency 目标频率（Hz）
     * @param detectedFrequency 检测到的频率（Hz）
     * @return 偏差（cents），正值表示偏高，负值表示偏低
     */
    fun calculateDeviationCents(
        targetFrequency: Float,
        detectedFrequency: Float
    ): Float {
        if (targetFrequency <= 0 || detectedFrequency <= 0) {
            return 0f
        }
        
        return (1200 * log2(detectedFrequency / targetFrequency))
    }
    
    /**
     * 评估音准等级
     * 
     * @param deviationCents 偏差（cents）
     * @return 音准评级
     */
    fun evaluateAccuracy(deviationCents: Float): PitchAccuracy {
        val absDeviation = abs(deviationCents)
        
        return when {
            absDeviation < PERFECT_THRESHOLD -> PitchAccuracy.PERFECT
            absDeviation < GOOD_THRESHOLD -> PitchAccuracy.GOOD
            absDeviation < FAIR_THRESHOLD -> PitchAccuracy.FAIR
            else -> PitchAccuracy.POOR
        }
    }
    
    /**
     * 计算时间误差
     * 
     * @param targetNote 目标音符
     * @param currentTimeMs 当前时间（毫秒）
     * @return 时间误差（毫秒），负值表示提前，正值表示延后
     */
    private fun calculateTimingError(
        targetNote: Note,
        currentTimeMs: Long
    ): Long {
        val noteStartTime = targetNote.startTime
        return currentTimeMs - noteStartTime
    }
    
    /**
     * 判断是否在时间窗口内
     * 
     * @param timingErrorMs 时间误差（毫秒）
     * @return 是否在时间窗口内
     */
    private fun isInTimeWindow(timingErrorMs: Long): Boolean {
        return timingErrorMs >= TIME_WINDOW_EARLY && 
               timingErrorMs <= TIME_WINDOW_LATE
    }
    
    /**
     * 获取音准反馈文本
     */
    fun getAccuracyFeedback(accuracy: PitchAccuracy): String {
        return when (accuracy) {
            PitchAccuracy.PERFECT -> "完美"
            PitchAccuracy.GOOD -> "良好"
            PitchAccuracy.FAIR -> "一般"
            PitchAccuracy.POOR -> "较差"
            PitchAccuracy.MISS -> "错过"
        }
    }
    
    /**
     * 获取偏差方向文本
     */
    fun getDeviationDirection(deviationCents: Float): String {
        return when {
            deviationCents > 5 -> "偏高"
            deviationCents < -5 -> "偏低"
            else -> "准确"
        }
    }
    
    /**
     * 获取建议文本
     */
    fun getSuggestion(result: ComparisonResult): String {
        return when {
            result.accuracy == PitchAccuracy.PERFECT -> {
                "很好！继续保持。"
            }
            abs(result.deviationCents) > 50 -> {
                if (result.deviationCents > 0) {
                    "音高偏高，尝试降低一些。"
                } else {
                    "音高偏低，尝试提高一些。"
                }
            }
            abs(result.deviationCents) > 25 -> {
                "音准需要微调。"
            }
            !result.isInTimeWindow -> {
                if (result.timingErrorMs < 0) {
                    "演奏过早，注意节奏。"
                } else {
                    "演奏过晚，跟上节奏。"
                }
            }
            else -> {
                "不错，继续练习。"
            }
        }
    }
}

/**
 * 音准统计器
 * 
 * 统计一段时间内的音准数据
 */
class PitchStatistics {
    
    private val results = mutableListOf<PitchComparator.ComparisonResult>()
    
    /**
     * 添加比对结果
     */
    fun addResult(result: PitchComparator.ComparisonResult) {
        results.add(result)
    }
    
    /**
     * 清空统计
     */
    fun clear() {
        results.clear()
    }
    
    /**
     * 获取总音符数
     */
    fun getTotalNotes(): Int = results.size
    
    /**
     * 获取各等级音符数
     */
    fun getAccuracyCount(accuracy: PitchComparator.PitchAccuracy): Int {
        return results.count { it.accuracy == accuracy }
    }
    
    /**
     * 获取准确率（PERFECT + GOOD）
     */
    fun getAccuracyRate(): Float {
        if (results.isEmpty()) return 0f
        
        val accurateCount = getAccuracyCount(PitchComparator.PitchAccuracy.PERFECT) +
                           getAccuracyCount(PitchComparator.PitchAccuracy.GOOD)
        
        return accurateCount.toFloat() / results.size
    }
    
    /**
     * 获取平均偏差（绝对值）
     */
    fun getAverageDeviation(): Float {
        if (results.isEmpty()) return 0f
        
        val sum = results.sumOf { abs(it.deviationCents).toDouble() }
        return (sum / results.size).toFloat()
    }
    
    /**
     * 获取最大偏差
     */
    fun getMaxDeviation(): Float {
        if (results.isEmpty()) return 0f
        
        return results.maxOf { abs(it.deviationCents) }
    }
    
    /**
     * 获取偏高音符比例
     */
    fun getSharpRate(): Float {
        if (results.isEmpty()) return 0f
        
        val sharpCount = results.count { it.deviationCents > 5 }
        return sharpCount.toFloat() / results.size
    }
    
    /**
     * 获取偏低音符比例
     */
    fun getFlatRate(): Float {
        if (results.isEmpty()) return 0f
        
        val flatCount = results.count { it.deviationCents < -5 }
        return flatCount.toFloat() / results.size
    }
    
    /**
     * 生成统计报告
     */
    fun generateReport(): StatisticsReport {
        return StatisticsReport(
            totalNotes = getTotalNotes(),
            perfectCount = getAccuracyCount(PitchComparator.PitchAccuracy.PERFECT),
            goodCount = getAccuracyCount(PitchComparator.PitchAccuracy.GOOD),
            fairCount = getAccuracyCount(PitchComparator.PitchAccuracy.FAIR),
            poorCount = getAccuracyCount(PitchComparator.PitchAccuracy.POOR),
            accuracyRate = getAccuracyRate(),
            averageDeviation = getAverageDeviation(),
            maxDeviation = getMaxDeviation(),
            sharpRate = getSharpRate(),
            flatRate = getFlatRate()
        )
    }
    
    /**
     * 统计报告
     */
    data class StatisticsReport(
        val totalNotes: Int,
        val perfectCount: Int,
        val goodCount: Int,
        val fairCount: Int,
        val poorCount: Int,
        val accuracyRate: Float,
        val averageDeviation: Float,
        val maxDeviation: Float,
        val sharpRate: Float,
        val flatRate: Float
    )
}
