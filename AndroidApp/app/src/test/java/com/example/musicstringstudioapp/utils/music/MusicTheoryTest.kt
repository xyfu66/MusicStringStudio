package com.example.musicstringstudioapp.utils.music

import org.junit.Assert.*
import org.junit.Test

class MusicTheoryTest {
    
    @Test
    fun `A4 频率应该转换为 MIDI 69`() {
        val midiNote = MusicTheory.frequencyToMidiNote(440f)
        assertEquals(69, midiNote)
    }
    
    @Test
    fun `MIDI 69 应该转换为 440Hz`() {
        val frequency = MusicTheory.midiNoteToFrequency(69)
        assertEquals(440f, frequency, 0.1f)
    }
    
    @Test
    fun `MIDI 69 应该转换为 A4`() {
        val noteName = MusicTheory.midiNoteToNoteName(69)
        assertEquals("A4", noteName)
    }
    
    @Test
    fun `音分偏差计算应该正确`() {
        // 440Hz 到 442Hz 约 +7.85 cents
        val cents = MusicTheory.calculateCentsDeviation(442f, 440f)
        assertTrue(cents > 7f && cents < 8f)
    }
    
    @Test
    fun `音准判定应该正确`() {
        assertEquals(AccuracyLevel.PERFECT, MusicTheory.judgeAccuracy(5f))
        assertEquals(AccuracyLevel.GOOD, MusicTheory.judgeAccuracy(15f))
        assertEquals(AccuracyLevel.FAIR, MusicTheory.judgeAccuracy(30f))
        assertEquals(AccuracyLevel.POOR, MusicTheory.judgeAccuracy(60f))
    }
    
    @Test
    fun `频率转换为音符名称应该正确`() {
        assertEquals("A4", MusicTheory.frequencyToNoteName(440f))
        assertEquals("C4", MusicTheory.frequencyToNoteName(261.63f))
    }
    
    @Test
    fun `小提琴空弦音符应该正确`() {
        assertEquals(196.0f, MusicTheory.VIOLIN_OPEN_STRINGS["G3"])
        assertEquals(293.66f, MusicTheory.VIOLIN_OPEN_STRINGS["D4"])
        assertEquals(440.0f, MusicTheory.VIOLIN_OPEN_STRINGS["A4"])
        assertEquals(659.25f, MusicTheory.VIOLIN_OPEN_STRINGS["E5"])
    }
}
