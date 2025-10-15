package com.example.musicstringstudioapp.domain.audio

import com.example.musicstringstudioapp.data.model.Song
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * MusicXML 转 MIDI 转换器
 * 
 * 将 Song 对象（从 MusicXML 解析）转换为标准 MIDI 文件
 * 使用纯 Kotlin 实现，无需 javax.sound.midi（Android 不完全支持）
 */
class MusicXMLToMidiConverter {
    
    companion object {
        // MIDI 文件头标识
        private const val MIDI_HEADER = "MThd"
        private const val MIDI_TRACK = "MTrk"
        
        // MIDI 消息类型
        private const val NOTE_OFF = 0x80
        private const val NOTE_ON = 0x90
        private const val PROGRAM_CHANGE = 0xC0
        private const val META_EVENT = 0xFF
        
        // 默认参数
        private const val TICKS_PER_QUARTER = 480
        private const val VELOCITY = 100
        private const val VIOLIN_PROGRAM = 40 // General MIDI: Violin
    }
    
    /**
     * 将 Song 对象转换为 MIDI 文件
     * 
     * @param song Song 对象（从 MusicXML 解析）
     * @param outputFile 输出 MIDI 文件路径
     */
    fun convertToMidi(song: Song, outputFile: File) {
        Timber.d("开始转换 ${song.title} 到 MIDI")
        
        try {
            val midiData = generateMidiData(song)
            
            FileOutputStream(outputFile).use { fos ->
                fos.write(midiData)
            }
            
            Timber.d("MIDI 文件生成成功: ${outputFile.absolutePath} (${midiData.size} bytes)")
        } catch (e: Exception) {
            Timber.e(e, "MIDI 转换失败")
            throw e
        }
    }
    
    /**
     * 将 Song 对象转换为 MIDI 字节数组
     */
    fun convertToMidiBytes(song: Song): ByteArray {
        return generateMidiData(song)
    }
    
    /**
     * 生成 MIDI 数据
     */
    private fun generateMidiData(song: Song): ByteArray {
        val trackData = generateTrackData(song)
        val headerData = generateHeader(1, TICKS_PER_QUARTER)
        
        return headerData + trackData
    }
    
    /**
     * 生成 MIDI 文件头
     * 
     * 格式: MThd <length> <format> <tracks> <division>
     */
    private fun generateHeader(numTracks: Int, division: Int): ByteArray {
        val buffer = ByteBuffer.allocate(14)
        
        // 文件头标识 "MThd"
        buffer.put('M'.code.toByte())
        buffer.put('T'.code.toByte())
        buffer.put('h'.code.toByte())
        buffer.put('d'.code.toByte())
        
        // 头长度: 6 字节
        buffer.putInt(6)
        
        // 格式: 0 = 单音轨, 1 = 多音轨同步
        buffer.putShort(1)
        
        // 音轨数量
        buffer.putShort(numTracks.toShort())
        
        // Division (ticks per quarter note)
        buffer.putShort(division.toShort())
        
        return buffer.array()
    }
    
    /**
     * 生成音轨数据
     */
    private fun generateTrackData(song: Song): ByteArray {
        val events = mutableListOf<Byte>()
        
        // 1. 音轨名称
        events.addAll(createMetaEvent(0, 0x03, song.title.toByteArray()))
        
        // 2. 设置速度 (Tempo)
        val microsecondsPerQuarter = 60_000_000 / song.tempo
        val tempoBytes = byteArrayOf(
            (microsecondsPerQuarter shr 16).toByte(),
            (microsecondsPerQuarter shr 8).toByte(),
            microsecondsPerQuarter.toByte()
        )
        events.addAll(createMetaEvent(0, 0x51, tempoBytes))
        
        // 3. 设置拍号
        val (beats, beatType) = parseTimeSignature(song.timeSignature)
        val timeSignatureBytes = byteArrayOf(
            beats.toByte(),
            log2(beatType).toByte(),
            24, // MIDI clocks per metronome click
            8   // 32nd notes per quarter note
        )
        events.addAll(createMetaEvent(0, 0x58, timeSignatureBytes))
        
        // 4. 设置乐器 (Program Change)
        events.addAll(createProgramChange(0, VIOLIN_PROGRAM))
        
        // 5. 添加所有音符事件
        val noteEvents = generateNoteEvents(song)
        events.addAll(noteEvents)
        
        // 6. 音轨结束标记
        events.addAll(createMetaEvent(0, 0x2F, byteArrayOf()))
        
        // 生成音轨头
        val trackHeader = ByteBuffer.allocate(8).apply {
            put('M'.code.toByte())
            put('T'.code.toByte())
            put('r'.code.toByte())
            put('k'.code.toByte())
            putInt(events.size)
        }.array()
        
        return trackHeader + events.toByteArray()
    }
    
    /**
     * 生成所有音符事件
     */
    private fun generateNoteEvents(song: Song): List<Byte> {
        val events = mutableListOf<Byte>()
        val allNotes = song.getAllNotes().sortedBy { it.startTime }
        
        var lastTick = 0L
        
        for (note in allNotes) {
            // 计算音符开始的 tick
            val startTick = timeToTicks(note.startTime, song.tempo)
            val durationTicks = timeToTicks(note.duration, song.tempo)
            
            // Delta time (与上一个事件的时间差)
            val deltaTime = startTick - lastTick
            
            // Note On
            events.addAll(createNoteOn(deltaTime, note.toMidi(), VELOCITY))
            
            // Note Off (在持续时间后)
            events.addAll(createNoteOff(durationTicks, note.toMidi()))
            
            lastTick = startTick + durationTicks
        }
        
        return events
    }
    
    /**
     * 创建 Meta 事件
     */
    private fun createMetaEvent(deltaTime: Long, type: Int, data: ByteArray): List<Byte> {
        val result = mutableListOf<Byte>()
        
        // Delta time
        result.addAll(encodeVariableLength(deltaTime))
        
        // Meta event 标识
        result.add(META_EVENT.toByte())
        
        // Event type
        result.add(type.toByte())
        
        // Data length
        result.addAll(encodeVariableLength(data.size.toLong()))
        
        // Data
        result.addAll(data.toList())
        
        return result
    }
    
    /**
     * 创建 Program Change 事件
     */
    private fun createProgramChange(deltaTime: Long, program: Int): List<Byte> {
        val result = mutableListOf<Byte>()
        
        // Delta time
        result.addAll(encodeVariableLength(deltaTime))
        
        // Program Change 消息 (channel 0)
        result.add(PROGRAM_CHANGE.toByte())
        
        // Program number
        result.add(program.toByte())
        
        return result
    }
    
    /**
     * 创建 Note On 事件
     */
    private fun createNoteOn(deltaTime: Long, noteNumber: Int, velocity: Int): List<Byte> {
        val result = mutableListOf<Byte>()
        
        // Delta time
        result.addAll(encodeVariableLength(deltaTime))
        
        // Note On 消息 (channel 0)
        result.add(NOTE_ON.toByte())
        
        // Note number
        result.add(noteNumber.toByte())
        
        // Velocity
        result.add(velocity.toByte())
        
        return result
    }
    
    /**
     * 创建 Note Off 事件
     */
    private fun createNoteOff(deltaTime: Long, noteNumber: Int): List<Byte> {
        val result = mutableListOf<Byte>()
        
        // Delta time
        result.addAll(encodeVariableLength(deltaTime))
        
        // Note Off 消息 (channel 0)
        result.add(NOTE_OFF.toByte())
        
        // Note number
        result.add(noteNumber.toByte())
        
        // Velocity (0)
        result.add(0)
        
        return result
    }
    
    /**
     * 将时间（毫秒）转换为 MIDI ticks
     */
    private fun timeToTicks(timeMs: Long, tempo: Int): Long {
        // ticks = (timeMs / 1000) * (tempo / 60) * TICKS_PER_QUARTER
        return (timeMs * tempo * TICKS_PER_QUARTER) / 60000
    }
    
    /**
     * 编码可变长度值 (MIDI Variable-Length Quantity)
     * 
     * MIDI 使用特殊的可变长度编码来节省空间
     */
    private fun encodeVariableLength(value: Long): List<Byte> {
        val result = mutableListOf<Byte>()
        var v = value
        
        // 提取7位组
        var buffer = (v and 0x7F).toInt()
        v = v shr 7
        
        while (v > 0) {
            buffer = buffer shl 8
            buffer = buffer or ((v and 0x7F).toInt() or 0x80)
            v = v shr 7
        }
        
        // 写入字节
        while (true) {
            result.add(buffer.toByte())
            if (buffer and 0x80 == 0) break
            buffer = buffer shr 8
        }
        
        return result
    }
    
    /**
     * 解析拍号字符串 (如 "4/4", "3/4", "6/8")
     */
    private fun parseTimeSignature(timeSignature: String): Pair<Int, Int> {
        val parts = timeSignature.split("/")
        return if (parts.size == 2) {
            Pair(parts[0].toIntOrNull() ?: 4, parts[1].toIntOrNull() ?: 4)
        } else {
            Pair(4, 4) // 默认 4/4
        }
    }
    
    /**
     * 计算 log2
     */
    private fun log2(n: Int): Int {
        var result = 0
        var value = n
        while (value > 1) {
            value = value shr 1
            result++
        }
        return result
    }
}
