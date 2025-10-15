package com.example.musicstringstudioapp.data.parser

import android.content.Context
import androidx.annotation.RawRes
import com.example.musicstringstudioapp.data.model.Measure
import com.example.musicstringstudioapp.data.model.Note
import com.example.musicstringstudioapp.data.model.Song
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.pow

/**
 * MusicXML 解析器
 * 
 * 解析 MusicXML 格式的曲谱文件并转换为内部数据模型
 * 支持 MusicXML 3.1+ 格式
 */
class MusicXMLParser {
    
    /**
     * 从 InputStream 解析 MusicXML
     */
    fun parse(inputStream: InputStream): Song {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(inputStream))
        
        return parseScorePartwise(parser)
    }
    
    /**
     * 从资源文件解析 MusicXML
     */
    fun parseFromResource(context: Context, @RawRes resId: Int): Song {
        val inputStream = context.resources.openRawResource(resId)
        return parse(inputStream).also {
            inputStream.close()
        }
    }
    
    /**
     * 从文件路径解析 MusicXML
     */
    fun parseFromFile(filePath: String): Song {
        val inputStream = java.io.FileInputStream(filePath)
        return parse(inputStream).also {
            inputStream.close()
        }
    }
    
    /**
     * 解析 score-partwise 结构
     */
    private fun parseScorePartwise(parser: XmlPullParser): Song {
        var title = "Untitled"
        var composer = "Unknown"
        var currentTempo = 120
        var currentTimeSignature = "4/4"
        var currentKey = "C"
        val measures = mutableListOf<Measure>()
        
        var currentDivisions = 1 // MusicXML divisions 单位
        var currentTime = 0L // 累积时间（毫秒）
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "work-title" -> {
                            title = parser.nextText().trim()
                            Timber.d("解析曲名: $title")
                        }
                        "creator" -> {
                            if (parser.getAttributeValue(null, "type") == "composer") {
                                composer = parser.nextText().trim()
                                Timber.d("解析作曲者: $composer")
                            }
                        }
                        "measure" -> {
                            val measureNumber = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: 1
                            val measure = parseMeasure(
                                parser,
                                measureNumber,
                                currentDivisions,
                                currentTempo,
                                currentTime
                            )
                            
                            // 更新状态
                            if (measure.attributes.divisions != null) {
                                currentDivisions = measure.attributes.divisions!!
                            }
                            if (measure.attributes.tempo != null) {
                                currentTempo = measure.attributes.tempo!!
                            }
                            if (measure.attributes.timeSignature != null) {
                                currentTimeSignature = measure.attributes.timeSignature!!
                            }
                            if (measure.attributes.key != null) {
                                currentKey = measure.attributes.key!!
                            }
                            
                            // 创建 Measure 对象
                            val measureObj = Measure(
                                measureNumber = measureNumber,
                                notes = measure.notes,
                                timeSignature = currentTimeSignature,
                                tempo = currentTempo
                            )
                            measures.add(measureObj)
                            
                            // 更新累积时间
                            if (measure.notes.isNotEmpty()) {
                                val lastNote = measure.notes.maxByOrNull { it.startTime + it.duration }
                                if (lastNote != null) {
                                    currentTime = lastNote.startTime + lastNote.duration
                                }
                            }
                            
                            Timber.d("解析小节 $measureNumber, 音符数: ${measure.notes.size}")
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        
        val song = Song(
            id = "musicxml_${System.currentTimeMillis()}",
            title = title,
            composer = composer,
            tempo = currentTempo,
            timeSignature = currentTimeSignature,
            key = currentKey,
            measures = measures,
            difficulty = "Beginner",
            tags = listOf("MusicXML")
        )
        
        Timber.d("MusicXML 解析完成: $title, ${measures.size} 小节")
        return song
    }
    
    /**
     * 解析单个小节
     */
    private fun parseMeasure(
        parser: XmlPullParser,
        measureNumber: Int,
        divisions: Int,
        tempo: Int,
        startTime: Long
    ): MeasureData {
        val notes = mutableListOf<Note>()
        var currentTime = startTime
        
        var currentDivisions = divisions
        var currentTempo = tempo
        var currentTimeSignature: String? = null
        var currentKey: String? = null
        
        var depth = 1 // 跟踪嵌套深度
        var eventType = parser.next()
        
        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "attributes" -> {
                            val attrs = parseAttributes(parser)
                            if (attrs.divisions != null) currentDivisions = attrs.divisions!!
                            if (attrs.tempo != null) currentTempo = attrs.tempo!!
                            if (attrs.timeSignature != null) currentTimeSignature = attrs.timeSignature
                            if (attrs.key != null) currentKey = attrs.key
                        }
                        "direction" -> {
                            val t = parseDirection(parser)
                            if (t != null) currentTempo = t
                        }
                        "note" -> {
                            val note = parseNote(parser, currentDivisions, currentTempo, currentTime)
                            notes.add(note)
                            currentTime += note.duration
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                    if (depth == 0 && parser.name == "measure") {
                        break
                    }
                }
            }
            eventType = parser.next()
        }
        
        return MeasureData(
            notes = notes,
            attributes = MeasureAttributes(
                divisions = currentDivisions,
                tempo = currentTempo,
                timeSignature = currentTimeSignature,
                key = currentKey
            )
        )
    }
    
    /**
     * 解析 attributes 元素
     */
    private fun parseAttributes(parser: XmlPullParser): MeasureAttributes {
        var divisions: Int? = null
        var timeSignature: String? = null
        var key: String? = null
        
        var depth = 1
        var eventType = parser.next()
        
        var beats: Int? = null
        var beatType: Int? = null
        
        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "divisions" -> divisions = parser.nextText().toIntOrNull()
                        "beats" -> beats = parser.nextText().toIntOrNull()
                        "beat-type" -> beatType = parser.nextText().toIntOrNull()
                        "fifths" -> {
                            val fifths = parser.nextText().toIntOrNull() ?: 0
                            key = fifthsToKey(fifths)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
            }
            eventType = parser.next()
        }
        
        if (beats != null && beatType != null) {
            timeSignature = "$beats/$beatType"
        }
        
        return MeasureAttributes(divisions, null, timeSignature, key)
    }
    
    /**
     * 解析 direction 元素（速度标记）
     */
    private fun parseDirection(parser: XmlPullParser): Int? {
        var tempo: Int? = null
        var depth = 1
        var eventType = parser.next()
        
        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (parser.name == "per-minute") {
                        tempo = parser.nextText().toIntOrNull()
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
            }
            eventType = parser.next()
        }
        
        return tempo
    }
    
    /**
     * 解析音符
     */
    private fun parseNote(
        parser: XmlPullParser,
        divisions: Int,
        tempo: Int,
        startTime: Long
    ): Note {
        var step: String? = null
        var octave: Int? = null
        var duration: Int? = null
        var noteType: String? = null
        var isRest = false
        
        var depth = 1
        var eventType = parser.next()
        
        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "rest" -> isRest = true
                        "step" -> step = parser.nextText()
                        "octave" -> octave = parser.nextText().toIntOrNull()
                        "duration" -> duration = parser.nextText().toIntOrNull()
                        "type" -> noteType = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
            }
            eventType = parser.next()
        }
        
        // 如果是休止符，使用默认值
        if (isRest) {
            step = "C"
            octave = 4
        }
        
        // 计算实际时长（毫秒）
        val durationMs = if (duration != null && divisions > 0) {
            ((duration.toFloat() / divisions) * (60000f / tempo)).toLong()
        } else {
            500 // 默认500ms
        }
        
        // 构建音符名称
        val pitch = "$step$octave"
        
        // 计算频率
        val frequency = calculateFrequency(step ?: "C", octave ?: 4)
        
        // 计算位置（简化）
        val position = calculatePosition(step ?: "C", octave ?: 4)
        
        return Note(
            pitch = pitch,
            frequency = frequency,
            startTime = startTime,
            duration = durationMs,
            position = position,
            notation = noteType ?: "quarter"
        )
    }
    
    /**
     * 根据 fifths 值转换为调性
     */
    private fun fifthsToKey(fifths: Int): String {
        return when (fifths) {
            -7 -> "Cb"
            -6 -> "Gb"
            -5 -> "Db"
            -4 -> "Ab"
            -3 -> "Eb"
            -2 -> "Bb"
            -1 -> "F"
            0 -> "C"
            1 -> "G"
            2 -> "D"
            3 -> "A"
            4 -> "E"
            5 -> "B"
            6 -> "F#"
            7 -> "C#"
            else -> "C"
        }
    }
    
    /**
     * 计算音符频率
     */
    private fun calculateFrequency(step: String, octave: Int): Float {
        val noteMap = mapOf(
            "C" to 0, "D" to 2, "E" to 4, "F" to 5,
            "G" to 7, "A" to 9, "B" to 11
        )
        
        val semitone = noteMap[step] ?: 0
        val midiNote = (octave + 1) * 12 + semitone
        
        // A4 (MIDI 69) = 440 Hz
        return 440f * 2f.pow((midiNote - 69) / 12f)
    }
    
    /**
     * 计算音符在五线谱上的位置
     */
    private fun calculatePosition(step: String, octave: Int): Int {
        val noteMap = mapOf(
            "C" to 0, "D" to 1, "E" to 2, "F" to 3,
            "G" to 4, "A" to 5, "B" to 6
        )
        
        val basePosition = noteMap[step] ?: 0
        return (octave - 4) * 7 + basePosition
    }
    
    /**
     * 小节数据（临时结构）
     */
    private data class MeasureData(
        val notes: List<Note>,
        val attributes: MeasureAttributes
    )
    
    /**
     * 小节属性
     */
    private data class MeasureAttributes(
        val divisions: Int?,
        val tempo: Int?,
        val timeSignature: String?,
        val key: String?
    )
}
