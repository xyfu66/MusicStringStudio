# Android 应用内 MusicXML 转音频方案

## 📋 目标

在 Android 应用内实现 MusicXML → MIDI → 音频的转换链路，无需依赖外部工具。

## 🎯 技术方案

### 方案架构
```
MusicXML → MIDI转换 → Android MIDI播放/录制 → MP3文件
```

## 🔧 实现步骤

### 步骤1: MusicXML 到 MIDI 转换

使用 Java MIDI API 转换 MusicXML 到 MIDI 格式

#### 添加依赖
```kotlin
// build.gradle.kts
dependencies {
    // 用于 MIDI 文件处理
    implementation("com.github.philburk:jsyn:20171016")
    
    // 或使用 Android 内置 MIDI API (Android 6.0+)
    // 无需额外依赖
}
```

#### 创建 MusicXMLToMidiConverter.kt
```kotlin
package com.example.musicstringstudioapp.domain.audio

import com.example.musicstringstudioapp.data.model.Note
import com.example.musicstringstudioapp.data.model.Song
import java.io.File
import java.io.FileOutputStream
import javax.sound.midi.*

/**
 * MusicXML 转 MIDI 转换器
 */
class MusicXMLToMidiConverter {
    
    /**
     * 将 Song 对象转换为 MIDI 文件
     * 
     * @param song Song 对象（从 MusicXML 解析）
     * @param outputFile 输出 MIDI 文件路径
     */
    fun convertToMidi(song: Song, outputFile: File) {
        // 创建 MIDI Sequence
        val sequence = Sequence(Sequence.PPQ, 480) // 480 ticks per quarter note
        
        // 创建音轨
        val track = sequence.createTrack()
        
        // 设置乐器（Violin = Program 40）
        val programChange = ShortMessage()
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 40, 0)
        track.add(MidiEvent(programChange, 0))
        
        // 设置速度
        val tempo = song.tempo
        val microsecondsPerQuarter = 60000000 / tempo
        val tempoMessage = MetaMessage()
        tempoMessage.setMessage(
            0x51, // Tempo message type
            byteArrayOf(
                (microsecondsPerQuarter shr 16).toByte(),
                (microsecondsPerQuarter shr 8).toByte(),
                microsecondsPerQuarter.toByte()
            ),
            3
        )
        track.add(MidiEvent(tempoMessage, 0))
        
        // 转换所有音符
        var currentTick = 0L
        
        for (note in song.getAllNotes()) {
            // 计算 tick 位置
            val startTick = (note.startTime * 480 / (60000 / tempo)).toLong()
            val durationTicks = (note.duration * 480 / (60000 / tempo)).toLong()
            
            // Note On
            val noteOn = ShortMessage()
            noteOn.setMessage(
                ShortMessage.NOTE_ON,
                0,
                note.toMidi(),
                100 // Velocity
            )
            track.add(MidiEvent(noteOn, startTick))
            
            // Note Off
            val noteOff = ShortMessage()
            noteOff.setMessage(
                ShortMessage.NOTE_OFF,
                0,
                note.toMidi(),
                0
            )
            track.add(MidiEvent(noteOff, startTick + durationTicks))
            
            currentTick = startTick + durationTicks
        }
        
        // 写入文件
        MidiSystem.write(sequence, 1, FileOutputStream(outputFile))
    }
    
    /**
     * 将 Song 对象转换为 MIDI 字节数组
     */
    fun convertToMidiBytes(song: Song): ByteArray {
        val tempFile = File.createTempFile("temp_midi", ".mid")
        try {
            convertToMidi(song, tempFile)
            return tempFile.readBytes()
        } finally {
            tempFile.delete()
        }
    }
}
```

### 步骤2: MIDI 音频合成

使用 Android 内置的 MIDI 合成器或第三方库

#### 方案A: 使用 Android MediaPlayer (推荐)
```kotlin
package com.example.musicstringstudioapp.domain.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * MIDI 音频生成器
 * 使用 Android 内置的 MIDI 播放功能
 */
class MidiAudioGenerator(
    private val context: Context
) {
    
    /**
     * 播放 MIDI 文件
     * 
     * Android 内置支持 MIDI 播放
     */
    fun playMidi(midiFile: File): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(midiFile.absolutePath)
            prepare()
            start()
        }
    }
    
    /**
     * 将 MIDI 转换为音频文件（需要录制）
     * 
     * 注意: Android 不直接支持 MIDI → MP3 转换
     * 需要播放 MIDI 并同时录制音频
     */
    suspend fun convertMidiToAudio(
        midiFile: File,
        outputFile: File
    ): Boolean {
        // 实现方案：
        // 1. 播放 MIDI
        // 2. 使用 AudioRecord 录制系统音频输出
        // 3. 保存为 MP3
        
        // 这需要 Android 10+ 的 MediaProjection API
        // 或使用第三方库进行合成
        
        return false // 待实现
    }
}
```

#### 方案B: 使用 JSyn 合成器 (完全自主)
```kotlin
package com.example.musicstringstudioapp.domain.audio

import com.jsyn.JSyn
import com.jsyn.Synthesizer
import com.jsyn.unitgen.LineOut
import com.jsyn.unitgen.SineOscillator
import com.example.musicstringstudioapp.data.model.Song
import java.io.File

/**
 * 使用 JSyn 合成音频
 */
class JSynAudioGenerator {
    
    private val synth: Synthesizer = JSyn.createSynthesizer()
    
    /**
     * 从 Song 生成音频
     */
    fun generateAudio(song: Song, outputFile: File) {
        synth.start()
        
        val lineOut = LineOut()
        synth.add(lineOut)
        
        // 为每个音符生成音频
        for (note in song.getAllNotes()) {
            val osc = SineOscillator()
            synth.add(osc)
            
            osc.frequency.set(note.frequency.toDouble())
            osc.amplitude.set(0.5)
            osc.output.connect(0, lineOut.input, 0)
            
            // 播放音符时长
            synth.sleepFor((note.duration / 1000.0))
            
            synth.remove(osc)
        }
        
        synth.stop()
    }
}
```

### 步骤3: 集成到应用

#### 创建统一的音频生成服务
```kotlin
package com.example.musicstringstudioapp.domain.audio

import android.content.Context
import com.example.musicstringstudioapp.data.model.Song
import com.example.musicstringstudioapp.data.parser.MusicXMLParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * 音频生成服务
 * 
 * 将 MusicXML 转换为可播放的音频文件
 */
class AudioGenerationService(
    private val context: Context
) {
    
    private val musicXMLParser = MusicXMLParser()
    private val midiConverter = MusicXMLToMidiConverter()
    private val midiPlayer = MidiAudioGenerator(context)
    
    /**
     * 从 MusicXML 生成音频文件
     * 
     * @param musicXMLFile MusicXML 文件
     * @param outputAudioFile 输出音频文件（MP3/MIDI）
     * @return 是否成功
     */
    suspend fun generateAudioFromMusicXML(
        musicXMLFile: File,
        outputAudioFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始转换: ${musicXMLFile.name}")
            
            // 1. 解析 MusicXML
            val song = musicXMLParser.parseFromFile(musicXMLFile.absolutePath)
            Timber.d("MusicXML 解析完成: ${song.title}")
            
            // 2. 转换为 MIDI
            val midiFile = File(context.cacheDir, "${song.id}.mid")
            midiConverter.convertToMidi(song, midiFile)
            Timber.d("MIDI 文件生成: ${midiFile.absolutePath}")
            
            // 3. 选项A: 直接使用 MIDI 文件
            // Android MediaPlayer 支持 MIDI 播放
            if (outputAudioFile.extension == "mid" || outputAudioFile.extension == "midi") {
                midiFile.copyTo(outputAudioFile, overwrite = true)
                Timber.d("MIDI 文件已复制到: ${outputAudioFile.absolutePath}")
                return@withContext Result.success(outputAudioFile)
            }
            
            // 3. 选项B: 转换为 MP3（需要额外实现）
            // TODO: 实现 MIDI → MP3 转换
            // 当前返回 MIDI 文件
            Timber.w("MP3 转换暂未实现，返回 MIDI 文件")
            
            Result.success(midiFile)
            
        } catch (e: Exception) {
            Timber.e(e, "音频生成失败")
            Result.failure(e)
        }
    }
    
    /**
     * 批量生成音频
     */
    suspend fun batchGenerate(
        musicXMLFiles: List<File>,
        outputDir: File
    ): Map<File, Result<File>> {
        outputDir.mkdirs()
        
        return musicXMLFiles.associateWith { xmlFile ->
            val outputFile = File(outputDir, "${xmlFile.nameWithoutExtension}.mid")
            generateAudioFromMusicXML(xmlFile, outputFile)
        }
    }
}
```

## 📦 完整实现示例

### 在 Activity 中使用
```kotlin
class MainActivity : ComponentActivity() {
    
    private lateinit var audioGenerationService: AudioGenerationService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        audioGenerationService = AudioGenerationService(this)
        
        // 示例：生成音频
        lifecycleScope.launch {
            val xmlFile = File(filesDir, "little_star.musicxml")
            val audioFile = File(cacheDir, "little_star.mid")
            
            val result = audioGenerationService.generateAudioFromMusicXML(
                xmlFile,
                audioFile
            )
            
            result.onSuccess { file ->
                Log.d("Audio", "生成成功: ${file.absolutePath}")
                // 播放音频
                playAudio(file)
            }.onFailure { error ->
                Log.e("Audio", "生成失败", error)
            }
        }
    }
    
    private fun playAudio(audioFile: File) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFile.absolutePath)
            prepare()
            start()
        }
    }
}
```

## ⚠️ 限制与注意事项

### Android MIDI 支持
- ✅ Android 原生支持 MIDI 播放（MediaPlayer）
- ❌ 不直接支持 MIDI → MP3 转换
- ✅ 可以使用 MIDI 文件作为示范音频

### 音质考虑
- MIDI 文件小巧（几 KB）
- 音质取决于设备的 MIDI 合成器
- 不同设备音色可能不同

### 推荐方案
1. **开发阶段**: 使用 MuseScore 命令行脚本生成高质量 MP3
2. **运行时**: 支持用户导入 MusicXML，应用内生成 MIDI
3. **未来**: 集成第三方音频合成库生成 MP3

## 🚀 后续优化

### 阶段1: MIDI 支持（当前可实现）
- ✅ MusicXML → MIDI 转换
- ✅ Android 播放 MIDI
- ✅ 用户可导入 MusicXML

### 阶段2: 音频合成（高级）
- 🔄 集成专业音频合成库
- 🔄 支持音色选择
- 🔄 生成高质量 MP3

### 阶段3: 云服务（可选）
- 🔄 上传 MusicXML 到服务器
- 🔄 服务器生成 MP3
- 🔄 下载到本地

## 📚 相关库

- **JSyn**: https://github.com/philburk/jsyn
- **Android MIDI API**: https://developer.android.com/reference/android/media/midi/package-summary
- **TarsosDSP**: https://github.com/JorenSix/TarsosDSP

---

## 🎯 结论

**当前最佳方案**:
1. 开发时使用 MuseScore 脚本生成 MP3（高质量）
2. 运行时支持 MusicXML → MIDI 转换（用户导入功能）
3. 两种方式结合，覆盖所有使用场景

这样既保证了音质，又支持了动态导入！
