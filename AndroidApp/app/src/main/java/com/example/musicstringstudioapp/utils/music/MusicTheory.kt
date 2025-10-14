package com.example.musicstringstudioapp.utils.music

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * 音乐理论工具类
 * 提供音高、音符、频率等转换功能
 */
object MusicTheory {
    
    // 标准音 A4 的频率
    const val A4_FREQUENCY = 440.0f
    
    // A4 的 MIDI 音符号
    const val A4_MIDI_NOTE = 69
    
    // 音符名称（从 C 开始）
    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", 
        "F#", "G", "G#", "A", "A#", "B"
    )
    
    // 小提琴空弦音符（从低到高：G3, D4, A4, E5）
    val VIOLIN_OPEN_STRINGS = mapOf(
        "G3" to 196.0f,
        "D4" to 293.66f,
        "A4" to 440.0f,
        "E5" to 659.25f
    )
    
    /**
     * 频率转换为 MIDI 音符号
     * @param frequency 频率 (Hz)
     * @return MIDI 音符号 (0-127)
     */
    fun frequencyToMidiNote(frequency: Float): Int {
        if (frequency <= 0) return 0
        return round(69 + 12 * ln(frequency / A4_FREQUENCY) / ln(2.0)).toInt()
    }
    
    /**
     * MIDI 音符号转换为频率
     * @param midiNote MIDI 音符号
     * @return 频率 (Hz)
     */
    fun midiNoteToFrequency(midiNote: Int): Float {
        return A4_FREQUENCY * 2.0.pow((midiNote - A4_MIDI_NOTE) / 12.0).toFloat()
    }
    
    /**
     * MIDI 音符号转换为音符名称
     * @param midiNote MIDI 音符号
     * @return 音符名称（如 "C4", "A#5"）
     */
    fun midiNoteToNoteName(midiNote: Int): String {
        val octave = (midiNote / 12) - 1
        val noteIndex = midiNote % 12
        return "${NOTE_NAMES[noteIndex]}$octave"
    }
    
    /**
     * 计算两个频率之间的音分偏差
     * @param frequency1 实际频率
     * @param frequency2 目标频率
     * @return 音分偏差（正值表示偏高，负值表示偏低）
     */
    fun calculateCentsDeviation(frequency1: Float, frequency2: Float): Float {
        if (frequency1 <= 0 || frequency2 <= 0) return 0f
        return (1200 * ln(frequency1 / frequency2) / ln(2.0)).toFloat()
    }
    
    /**
     * 频率直接转换为音符名称
     * @param frequency 频率 (Hz)
     * @return 音符名称
     */
    fun frequencyToNoteName(frequency: Float): String {
        val midiNote = frequencyToMidiNote(frequency)
        return midiNoteToNoteName(midiNote)
    }
    
    /**
     * 判定音准等级
     * @param centsDeviation 音分偏差
     * @return 准确度等级
     */
    fun judgeAccuracy(centsDeviation: Float): AccuracyLevel {
        val absCents = abs(centsDeviation)
        return when {
            absCents < 10 -> AccuracyLevel.PERFECT
            absCents < 25 -> AccuracyLevel.GOOD
            absCents < 50 -> AccuracyLevel.FAIR
            else -> AccuracyLevel.POOR
        }
    }
}

/**
 * 音准等级枚举
 */
enum class AccuracyLevel {
    PERFECT,  // 完美 (< 10 cents)
    GOOD,     // 良好 (10-25 cents)
    FAIR,     // 一般 (25-50 cents)
    POOR      // 较差 (> 50 cents)
}
