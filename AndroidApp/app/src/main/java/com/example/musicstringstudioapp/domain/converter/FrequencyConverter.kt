package com.example.musicstringstudioapp.domain.converter

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 频率转换工具类
 * 
 * 提供频率和音符之间的转换功能
 */
object FrequencyConverter {
    
    // A4 的标准频率 (440 Hz)
    private const val A4_FREQUENCY = 440.0f
    
    // A4 的 MIDI 音符号
    private const val A4_MIDI = 69
    
    // 音符名称数组
    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", 
        "F#", "G", "G#", "A", "A#", "B"
    )
    
    /**
     * 将频率转换为 MIDI 音符号
     * 
     * @param frequency 频率 (Hz)
     * @return MIDI 音符号 (0-127)
     */
    fun frequencyToMidi(frequency: Float): Int {
        if (frequency <= 0) return 0
        
        // MIDI = 69 + 12 * log2(frequency / 440)
        val midiFloat = A4_MIDI + 12 * log2(frequency / A4_FREQUENCY)
        return midiFloat.roundToInt().coerceIn(0, 127)
    }
    
    /**
     * 将 MIDI 音符号转换为频率
     * 
     * @param midi MIDI 音符号 (0-127)
     * @return 频率 (Hz)
     */
    fun midiToFrequency(midi: Int): Float {
        // frequency = 440 * 2^((midi - 69) / 12)
        return A4_FREQUENCY * 2.0f.pow((midi - A4_MIDI) / 12.0f)
    }
    
    /**
     * 将频率转换为音符名称（带八度）
     * 
     * @param frequency 频率 (Hz)
     * @return 音符名称，如 "A4", "C5"
     */
    fun frequencyToNoteName(frequency: Float): String {
        val midi = frequencyToMidi(frequency)
        return midiToNoteName(midi)
    }
    
    /**
     * 将 MIDI 音符号转换为音符名称（带八度）
     * 
     * @param midi MIDI 音符号 (0-127)
     * @return 音符名称，如 "A4", "C5"
     */
    fun midiToNoteName(midi: Int): String {
        val octave = (midi / 12) - 1
        val noteIndex = midi % 12
        val noteName = NOTE_NAMES[noteIndex]
        return "$noteName$octave"
    }
    
    /**
     * 计算频率偏差（音分 cents）
     * 
     * 音分是音程的对数单位，1个半音 = 100 cents
     * 
     * @param actualFrequency 实际频率
     * @param targetFrequency 目标频率
     * @return 偏差值（cents），正值表示偏高，负值表示偏低
     */
    fun calculateCentsDeviation(
        actualFrequency: Float,
        targetFrequency: Float
    ): Float {
        if (actualFrequency <= 0 || targetFrequency <= 0) return 0f
        
        // cents = 1200 * log2(actual / target)
        return 1200 * log2(actualFrequency / targetFrequency)
    }
    
    /**
     * 计算频率与最近音符的偏差（音分 cents）
     * 
     * @param frequency 频率 (Hz)
     * @return 偏差值（cents）
     */
    fun calculateCentsFromNearestNote(frequency: Float): Float {
        if (frequency <= 0) return 0f
        
        val midi = frequencyToMidi(frequency)
        val targetFrequency = midiToFrequency(midi)
        return calculateCentsDeviation(frequency, targetFrequency)
    }
    
    /**
     * 判断音准准确度等级
     * 
     * @param centsDeviation 音分偏差
     * @return 准确度等级: "perfect", "good", "fair", "poor"
     */
    fun getAccuracyLevel(centsDeviation: Float): String {
        val absCents = kotlin.math.abs(centsDeviation)
        return when {
            absCents < 10 -> "perfect"  // 完美: < 10 cents
            absCents < 25 -> "good"     // 良好: 10-25 cents
            absCents < 50 -> "fair"     // 偏差: 25-50 cents
            else -> "poor"               // 较差: > 50 cents
        }
    }
    
    /**
     * 数据类：频率分析结果
     */
    data class FrequencyAnalysis(
        val frequency: Float,
        val midiNote: Int,
        val noteName: String,
        val centsDeviation: Float,
        val accuracyLevel: String
    )
    
    /**
     * 完整分析频率
     * 
     * @param frequency 频率 (Hz)
     * @return 频率分析结果
     */
    fun analyzeFrequency(frequency: Float): FrequencyAnalysis {
        val midi = frequencyToMidi(frequency)
        val noteName = midiToNoteName(midi)
        val cents = calculateCentsFromNearestNote(frequency)
        val accuracy = getAccuracyLevel(cents)
        
        return FrequencyAnalysis(
            frequency = frequency,
            midiNote = midi,
            noteName = noteName,
            centsDeviation = cents,
            accuracyLevel = accuracy
        )
    }
}
