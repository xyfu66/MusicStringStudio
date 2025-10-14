package com.example.musicstringstudioapp.data.model

/**
 * 小节数据模型
 * 
 * @property measureNumber 小节号（从1开始）
 * @property notes 该小节包含的音符列表
 * @property timeSignature 拍号，如 "4/4", "3/4"
 * @property tempo 速度（BPM - Beats Per Minute）
 */
data class Measure(
    val measureNumber: Int,
    val notes: List<Note>,
    val timeSignature: String = "4/4",
    val tempo: Int = 120
) {
    /**
     * 获取小节的总时长（毫秒）
     */
    fun getDuration(): Long {
        if (notes.isEmpty()) return 0
        val lastNote = notes.maxByOrNull { it.startTime + it.duration }
        return lastNote?.let { it.startTime + it.duration } ?: 0
    }
    
    /**
     * 获取小节的开始时间（毫秒）
     */
    fun getStartTime(): Long {
        return notes.minOfOrNull { it.startTime } ?: 0
    }
    
    /**
     * 获取小节的结束时间（毫秒）
     */
    fun getEndTime(): Long {
        if (notes.isEmpty()) return 0
        val lastNote = notes.maxByOrNull { it.startTime + it.duration }
        return lastNote?.let { it.startTime + it.duration } ?: 0
    }
    
    /**
     * 检查给定时间点是否在此小节内
     */
    fun containsTime(timeMs: Long): Boolean {
        return timeMs >= getStartTime() && timeMs <= getEndTime()
    }
    
    /**
     * 获取指定时间点应该演奏的音符
     */
    fun getNoteAtTime(timeMs: Long): Note? {
        return notes.find { note ->
            timeMs >= note.startTime && timeMs < (note.startTime + note.duration)
        }
    }
}
