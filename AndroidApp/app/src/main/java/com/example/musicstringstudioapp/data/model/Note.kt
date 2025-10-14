package com.example.musicstringstudioapp.data.model

/**
 * 音符数据模型
 * 
 * @property pitch 音高名称，如 "A4", "C5"
 * @property frequency 频率(Hz)，如 440.0 表示 A4
 * @property startTime 音符开始时间（毫秒）
 * @property duration 音符持续时间（毫秒）
 * @property position 在五线谱上的位置（用于绘制）
 * @property notation 音符类型，如 "quarter"(四分音符), "half"(二分音符)
 */
data class Note(
    val pitch: String,
    val frequency: Float,
    val startTime: Long,
    val duration: Long,
    val position: Int,
    val notation: String = "quarter"
) {
    companion object {
        /**
         * 从MIDI音符号创建Note
         * @param midiNote MIDI音符号 (0-127, A4=69)
         * @param startTime 开始时间
         * @param duration 持续时间
         */
        fun fromMidi(
            midiNote: Int,
            startTime: Long,
            duration: Long
        ): Note {
            val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val octave = (midiNote / 12) - 1
            val noteName = noteNames[midiNote % 12]
            val pitch = "$noteName$octave"
            
            // A4 (MIDI 69) = 440 Hz
            val frequency = 440.0f * Math.pow(2.0, (midiNote - 69) / 12.0).toFloat()
            
            // 简单的位置计算（可以后续优化）
            val position = midiNote - 60 // 以C4为基准
            
            return Note(
                pitch = pitch,
                frequency = frequency,
                startTime = startTime,
                duration = duration,
                position = position
            )
        }
    }
    
    /**
     * 获取MIDI音符号
     */
    fun toMidi(): Int {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        
        // 解析音符名称和八度
        val noteMatch = Regex("([A-G]#?)(-?\\d+)").find(pitch)
        if (noteMatch != null) {
            val (noteName, octaveStr) = noteMatch.destructured
            val octave = octaveStr.toInt()
            val noteIndex = noteNames.indexOf(noteName)
            
            if (noteIndex >= 0) {
                return (octave + 1) * 12 + noteIndex
            }
        }
        
        return 69 // 默认返回A4
    }
}
