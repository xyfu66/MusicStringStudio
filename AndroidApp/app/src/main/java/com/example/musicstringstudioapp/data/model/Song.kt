package com.example.musicstringstudioapp.data.model

/**
 * 歌曲/曲谱数据模型
 * 
 * @property id 曲谱ID
 * @property title 曲名
 * @property composer 作曲者
 * @property tempo 默认速度（BPM）
 * @property timeSignature 默认拍号
 * @property key 调性，如 "C", "G", "D"
 * @property measures 所有小节列表
 * @property audioUrl 示范音频URL（可选）
 * @property difficulty 难度等级，如 "Beginner", "Intermediate", "Advanced"
 * @property tags 标签列表，如 ["练习曲", "古典"]
 */
data class Song(
    val id: String,
    val title: String,
    val composer: String = "Unknown",
    val tempo: Int = 120,
    val timeSignature: String = "4/4",
    val key: String = "C",
    val measures: List<Measure>,
    val audioUrl: String? = null,
    val difficulty: String = "Beginner",
    val tags: List<String> = emptyList()
) {
    /**
     * 获取曲谱总时长（毫秒）
     */
    fun getTotalDuration(): Long {
        if (measures.isEmpty()) return 0
        return measures.last().getEndTime()
    }
    
    /**
     * 获取总音符数
     */
    fun getTotalNoteCount(): Int {
        return measures.sumOf { it.notes.size }
    }
    
    /**
     * 根据时间获取当前应该在哪个小节
     */
    fun getMeasureAtTime(timeMs: Long): Measure? {
        return measures.find { it.containsTime(timeMs) }
    }
    
    /**
     * 根据时间获取当前应该演奏的音符
     */
    fun getNoteAtTime(timeMs: Long): Note? {
        val measure = getMeasureAtTime(timeMs) ?: return null
        return measure.getNoteAtTime(timeMs)
    }
    
    /**
     * 获取所有音符的扁平列表
     */
    fun getAllNotes(): List<Note> {
        return measures.flatMap { it.notes }
    }
    
    /**
     * 获取指定小节号的小节
     */
    fun getMeasure(measureNumber: Int): Measure? {
        return measures.find { it.measureNumber == measureNumber }
    }
    
    companion object {
        /**
         * 创建一个简单的示例曲谱（用于测试）
         * 示例: 小星星前4小节
         */
        fun createSampleSong(): Song {
            // 小星星: C C G G A A G
            val measure1Notes = listOf(
                Note.fromMidi(60, 0, 500),      // C4
                Note.fromMidi(60, 500, 500)     // C4
            )
            
            val measure2Notes = listOf(
                Note.fromMidi(67, 1000, 500),   // G4
                Note.fromMidi(67, 1500, 500)    // G4
            )
            
            val measure3Notes = listOf(
                Note.fromMidi(69, 2000, 500),   // A4
                Note.fromMidi(69, 2500, 500)    // A4
            )
            
            val measure4Notes = listOf(
                Note.fromMidi(67, 3000, 1000)   // G4 (二分音符)
            )
            
            val measures = listOf(
                Measure(1, measure1Notes),
                Measure(2, measure2Notes),
                Measure(3, measure3Notes),
                Measure(4, measure4Notes)
            )
            
            return Song(
                id = "sample_001",
                title = "小星星",
                composer = "传统",
                tempo = 120,
                timeSignature = "4/4",
                key = "C",
                measures = measures,
                difficulty = "Beginner",
                tags = listOf("儿童", "练习曲")
            )
        }
    }
}
