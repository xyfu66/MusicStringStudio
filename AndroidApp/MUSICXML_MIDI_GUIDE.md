# MusicXML → MIDI 使用指南

## 📋 概述

本项目实现了完整的 MusicXML → MIDI 转换功能，无需外部依赖，纯 Kotlin 实现。

## 🎯 核心组件

### 1. MusicXMLToMidiConverter
将 Song 对象转换为标准 MIDI 文件

### 2. AudioGenerationService  
统一的音频生成服务，提供便捷的 API

### 3. AudioPlaybackManager
使用 ExoPlayer 播放 MIDI 文件（Android 原生支持）

## 🚀 快速开始

### 基本使用示例

```kotlin
class MainActivity : ComponentActivity() {
    
    private lateinit var audioGenerationService: AudioGenerationService
    private lateinit var audioPlaybackManager: AudioPlaybackManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化服务
        audioGenerationService = AudioGenerationService(this)
        audioPlaybackManager = AudioPlaybackManager(this)
        
        // 加载并播放 MusicXML
        lifecycleScope.launch {
            loadAndPlayMusicXML(R.raw.little_star)
        }
    }
    
    private suspend fun loadAndPlayMusicXML(resourceId: Int) {
        // 1. 生成 MIDI
        val result = audioGenerationService.generateMidiFromResource(resourceId)
        
        result.onSuccess { midiFile ->
            Log.d("MIDI", "生成成功: ${midiFile.absolutePath}")
            
            // 2. 播放 MIDI
            val url = audioGenerationService.getMidiPlaybackUrl(midiFile)
            audioPlaybackManager.loadAudio(url)
            audioPlaybackManager.play()
            
        }.onFailure { error ->
            Log.e("MIDI", "生成失败", error)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioPlaybackManager.release()
    }
}
```

## 📦 API 参考

### AudioGenerationService

#### 从资源文件生成
```kotlin
suspend fun generateMidiFromResource(resourceId: Int): Result<File>
```

**使用场景**: 从 app 内置的 MusicXML 资源生成 MIDI

**示例**:
```kotlin
val result = audioGenerationService.generateMidiFromResource(R.raw.little_star)
```

#### 从文件生成
```kotlin
suspend fun generateMidiFromFile(musicXMLFile: File): Result<File>
```

**使用场景**: 从用户导入的 MusicXML 文件生成 MIDI

**示例**:
```kotlin
val xmlFile = File("/path/to/user_song.musicxml")
val result = audioGenerationService.generateMidiFromFile(xmlFile)
```

#### 从 Song 对象生成
```kotlin
suspend fun generateMidiFromSong(song: Song): Result<File>
```

**使用场景**: 当你已经有 Song 对象时

**示例**:
```kotlin
val song = musicXMLParser.parseFromResource(context, R.raw.little_star)
val result = audioGenerationService.generateMidiFromSong(song)
```

#### 批量生成
```kotlin
suspend fun batchGenerate(
    musicXMLFiles: List<File>,
    outputDir: File
): Map<File, Result<File>>
```

**使用场景**: 批量处理多个 MusicXML 文件

### AudioPlaybackManager

所有现有的播放功能都可以用于 MIDI 文件：

```kotlin
// 加载
audioPlaybackManager.loadAudio(midiFile.absolutePath)

// 播放控制
audioPlaybackManager.play()
audioPlaybackManager.pause()
audioPlaybackManager.stop()

// 跳转
audioPlaybackManager.seekTo(2000) // 2秒位置

// 变速（保持音高）
audioPlaybackManager.setSpeed(0.5f) // 慢速
audioPlaybackManager.setSpeed(1.5f) // 快速

// 循环播放
audioPlaybackManager.setLoopRange(1000, 5000) // 循环 1-5 秒
audioPlaybackManager.clearLoop()

// 监听事件
audioPlaybackManager.addListener(object : AudioPlaybackManager.PlaybackListener {
    override fun onPositionChanged(positionMs: Long) {
        // 进度更新
    }
    
    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        // 播放状态变化
    }
    
    override fun onCompleted() {
        // 播放完成
    }
})
```

## 🔄 完整工作流程

### 场景1: 播放内置曲谱

```kotlin
lifecycleScope.launch {
    // 1. 生成 MIDI
    val midiResult = audioGenerationService.generateMidiFromResource(
        R.raw.little_star
    )
    
    // 2. 加载并播放
    midiResult.onSuccess { midiFile ->
        audioPlaybackManager.loadAudio(midiFile.absolutePath)
        audioPlaybackManager.play()
    }
}
```

### 场景2: 用户导入 MusicXML

```kotlin
// 用户选择文件
val xmlFile = getUserSelectedFile()

lifecycleScope.launch {
    // 1. 生成 MIDI
    val midiResult = audioGenerationService.generateMidiFromFile(xmlFile)
    
    // 2. 加载并播放
    midiResult.onSuccess { midiFile ->
        audioPlaybackManager.loadAudio(midiFile.absolutePath)
        audioPlaybackManager.play()
    }
}
```

### 场景3: 动态修改曲谱

```kotlin
lifecycleScope.launch {
    // 1. 解析 MusicXML
    val song = MusicXMLParser().parseFromResource(context, R.raw.little_star)
    
    // 2. 修改 Song 对象（例如改变速度）
    val modifiedSong = song.copy(tempo = 80) // 减慢速度
    
    // 3. 生成新的 MIDI
    val midiResult = audioGenerationService.generateMidiFromSong(modifiedSong)
    
    // 4. 播放
    midiResult.onSuccess { midiFile ->
        audioPlaybackManager.loadAudio(midiFile.absolutePath)
        audioPlaybackManager.play()
    }
}
```

## 💾 文件管理

### MIDI 文件位置
生成的 MIDI 文件保存在应用缓存目录：
```kotlin
val cacheDir = context.cacheDir
// MIDI 文件路径: cacheDir/<song_id>.mid
```

### 清理缓存
```kotlin
// 清理所有缓存的 MIDI 文件
audioGenerationService.clearCache()
```

## 📐 MIDI 技术细节

### 文件格式
- **格式**: 标准 MIDI 文件（Format 1）
- **分辨率**: 480 ticks per quarter note
- **乐器**: General MIDI #40 (Violin)

### 支持的功能
- ✅ 音符（Note On/Off）
- ✅ 速度（Tempo）
- ✅ 拍号（Time Signature）
- ✅ 乐器选择（Program Change）
- ✅ 音符力度（Velocity）

### 文件大小
- 通常 1-10 KB（非常小）
- 比 MP3 小 100-1000 倍

## ⚠️ 注意事项

### Android MIDI 播放
- ✅ Android 原生支持 MIDI 播放
- ⚠️ 音色取决于设备的 MIDI 合成器
- ⚠️ 不同设备音色可能略有差异

### 推荐实践
1. **开发测试**: 在多个设备上测试音色
2. **音质**: MIDI 适合示范，但音色不如 MP3 丰富
3. **文件管理**: 定期清理缓存

## 🎯 优势

✅ **零外部依赖** - 纯 Kotlin 实现  
✅ **文件小巧** - 几 KB vs 几 MB  
✅ **即时生成** - 无需预先准备音频  
✅ **灵活性高** - 可动态修改曲谱  
✅ **Android 原生支持** - 无需第三方播放器

## 📚 扩展阅读

- **MIDI 规范**: https://www.midi.org/specifications
- **Android MediaPlayer**: https://developer.android.com/reference/android/media/MediaPlayer
- **General MIDI**: https://en.wikipedia.org/wiki/General_MIDI

---

## 🎊 总结

现在你的应用完全支持：
1. ✅ 解析 MusicXML
2. ✅ 转换为 MIDI
3. ✅ 播放 MIDI
4. ✅ 用户导入 MusicXML

**无需任何外部工具或依赖！** 🎻🎵
