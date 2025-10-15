package com.example.musicstringstudioapp.data.model

import java.util.UUID

/**
 * 练习会话
 * 
 * 记录一次完整的练习过程及结果
 * 
 * @property id 会话唯一标识
 * @property songId 曲目ID
 * @property userId 用户ID（可选，用于多用户系统）
 * @property startTime 开始时间（毫秒）
 * @property endTime 结束时间（毫秒，null表示未完成）
 * @property noteResults 所有音符的演奏结果
 * @property score 总分 (0-100)
 * @property accuracyRate 准确率 (0.0-1.0)
 */
data class PracticeSession(
    val id: String = UUID.randomUUID().toString(),
    val songId: String,
    val userId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val noteResults: List<NoteResult> = emptyList(),
    val score: Int = 0,
    val accuracyRate: Float = 0f
) {
    /**
     * 获取练习时长（秒）
     */
    fun getDurationSeconds(): Long {
        return if (endTime != null) {
            (endTime - startTime) / 1000
        } else {
            0
        }
    }
    
    /**
     * 获取总音符数
     */
    fun getTotalNotes(): Int = noteResults.size
    
    /**
     * 获取命中音符数
     */
    fun getHitNotes(): Int = noteResults.count { it.isHit }
    
    /**
     * 获取各准确度等级的数量
     */
    fun getAccuracyLevelCounts(): AccuracyLevelCounts {
        var perfect = 0
        var good = 0
        var fair = 0
        var poor = 0
        var miss = 0
        
        noteResults.forEach { result ->
            when (result.accuracyLevel) {
                NoteResult.AccuracyLevel.PERFECT -> perfect++
                NoteResult.AccuracyLevel.GOOD -> good++
                NoteResult.AccuracyLevel.FAIR -> fair++
                NoteResult.AccuracyLevel.POOR -> poor++
                NoteResult.AccuracyLevel.MISS -> miss++
            }
        }
        
        return AccuracyLevelCounts(
            perfect = perfect,
            good = good,
            fair = fair,
            poor = poor,
            miss = miss
        )
    }
    
    /**
     * 获取平均偏差（cents）
     */
    fun getAverageDeviation(): Float {
        if (noteResults.isEmpty()) return 0f
        
        val totalDeviation = noteResults.sumOf { kotlin.math.abs(it.deviation.toDouble()) }
        return (totalDeviation / noteResults.size).toFloat()
    }
    
    /**
     * 判断练习是否完成
     */
    fun isCompleted(): Boolean = endTime != null
    
    /**
     * 准确度等级计数
     */
    data class AccuracyLevelCounts(
        val perfect: Int,
        val good: Int,
        val fair: Int,
        val poor: Int,
        val miss: Int
    ) {
        /**
         * 获取总数
         */
        fun getTotal(): Int = perfect + good + fair + poor + miss
        
        /**
         * 获取完美率
         */
        fun getPerfectRate(): Float {
            val total = getTotal()
            return if (total > 0) perfect.toFloat() / total else 0f
        }
        
        /**
         * 获取良好及以上率
         */
        fun getGoodOrBetterRate(): Float {
            val total = getTotal()
            return if (total > 0) (perfect + good).toFloat() / total else 0f
        }
    }
    
    companion object {
        /**
         * 创建新的练习会话
         */
        fun create(songId: String, userId: String? = null): PracticeSession {
            return PracticeSession(
                songId = songId,
                userId = userId,
                startTime = System.currentTimeMillis()
            )
        }
    }
}
