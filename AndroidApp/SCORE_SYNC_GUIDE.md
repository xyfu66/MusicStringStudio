# 曲谱同步使用指南

## 📋 概述

Phase 3 实现了音频播放与曲谱的精确同步，是跟音练习功能的核心。

## 🎯 核心组件

### 1. ScoreFollower（曲谱跟随器）
根据播放进度跟踪当前演奏位置，定位目标音符

### 2. SyncCoordinator（同步协调器）
协调音频播放和曲谱跟随，确保两者同步

## 🚀 快速开始

### 基本使用示例

```kotlin
class PracticeActivity : ComponentActivity() {
    
    private lateinit var syncCoordinator: SyncCoordinator
    private lateinit var scoreFollower: ScoreFollower
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            setupPractice()
        }
    }
    
    private suspend fun setupPractice() {
        // 1. 加载 MusicXML 和 MIDI
        val song = MusicXMLParser().parseFromResource(
            this,
            R.raw.little_star
        )
        
        val audioGenService = AudioGenerationService(this)
        val midiResult = audioGenService.generateMidiFromSong(song)
        
        midiResult.onSuccess { midiFile ->
            // 2. 初始化播放器
            val audioPlayback = AudioPlaybackManager(this)
            audioPlayback.loadAudio(midiFile.absolutePath)
            
            // 3. 创建同步协调器
            syncCoordinator = SyncCoordinatorFactory.create(song, audioPlayback)
            scoreFollower = ScoreFollower(song)
            
            // 4. 添加监听器
            setupListeners()
            
            // 5. 开始播放
            syncCoordinator.play()
        }
    }
    
    private fun setupListeners() {
        // 监听当前音符变化
        scoreFollower.addListener(object : ScoreFollower.ScoreFollowerListener {
            override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
                // 更新 UI，高亮当前音符
                if (note != null) {
                    Log.d("Score", "当前音符: ${note.pitch}, 小节: $measureNumber")
                    highlightNote(note)
                }
            }
            
            override fun onMeasureChanged(measure: Measure, measureIndex: Int) {
                // 自动翻页
                Log.d("Score", "小节变化: ${measure.measureNumber}")
                scrollToMeasure(measureIndex)
            }
            
            override fun onProgressUpdate(positionMs: Long, totalMs: Long, progress: Float) {
                // 更新进度条
                updateProgressBar(progress)
            }
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        syncCoordinator.release()
    }
}
```

## 📦 API 参考

### ScoreFollower

#### 核心方法

**更新位置**
```kotlin
fun updatePosition(positionMs: Long)
```
通常由 SyncCoordinator 自动调用，也可手动调用

**获取当前音符**
```kotlin
fun getCurrentNote(): Note?
```
返回当前应该演奏的音符

**获取当前小节**
```kotlin
fun getCurrentMeasure(): Measure?
```

**获取下一个音符**
```kotlin
fun getNextNote(): Note?
```
用于预览下一个要演奏的音符

**获取时间范围内的音符**
```kotlin
fun getNotesInRange(startMs: Long, endMs: Long): List<Note>
```
用于绘制可视化进度

**跳转**
```kotlin
fun seekTo(positionMs: Long)
fun reset()
```

#### 监听器

```kotlin
interface ScoreFollowerListener {
    // 当前音符变化
    fun onCurrentNoteChanged(note: Note?, measureNumber: Int)
    
    // 小节变化（用于自动翻页）
    fun onMeasureChanged(measure: Measure, measureIndex: Int)
    
    // 进度更新
    fun onProgressUpdate(positionMs: Long, totalMs: Long, progress: Float)
}
```

### SyncCoordinator

#### 播放控制

```kotlin
// 播放/暂停/停止
syncCoordinator.play()
syncCoordinator.pause()
syncCoordinator.stop()

// 跳转
syncCoordinator.seekTo(5000) // 跳转到 5 秒
```

#### 变速播放

```kotlin
syncCoordinator.setSpeed(0.5f)  // 慢速 0.5x
syncCoordinator.setSpeed(1.0f)  // 正常速度
syncCoordinator.setSpeed(1.5f)  // 快速 1.5x
```

#### 循环播放

```kotlin
// 设置循环范围（例如循环第2-3小节）
val measure2Start = song.getMeasure(2)?.getStartTime() ?: 0
val measure3End = song.getMeasure(3)?.getEndTime() ?: 0
syncCoordinator.setLoopRange(measure2Start, measure3End)

// 取消循环
syncCoordinator.clearLoop()
```

#### 同步调整

```kotlin
// 手动调整同步偏移（如果音频和曲谱不同步）
syncCoordinator.setSyncOffset(100)  // 曲谱提前 100ms
syncCoordinator.setSyncOffset(-100) // 曲谱延后 100ms
```

## 🔄 完整工作流程

### 场景1: 基础练习

```kotlin
lifecycleScope.launch {
    // 1. 准备
    val song = loadSong()
    val midiFile = generateMidi(song)
    
    // 2. 初始化
    val audioPlayback = AudioPlaybackManager(context)
    audioPlayback.loadAudio(midiFile.absolutePath)
    
    val syncCoordinator = SyncCoordinatorFactory.create(song, audioPlayback)
    
    // 3. 监听
    val scoreFollower = syncCoordinator.scoreFollower
    scoreFollower.addListener(object : ScoreFollowerListener {
        override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
            // UI: 高亮当前音符
            highlightCurrentNote(note)
        }
    })
    
    // 4. 播放
    syncCoordinator.play()
}
```

### 场景2: 循环练习某个片段

```kotlin
// 用户选择要练习的小节范围
val startMeasure = 2
val endMeasure = 4

// 获取时间范围
val startTime = song.getMeasure(startMeasure)?.getStartTime() ?: 0
val endTime = song.getMeasure(endMeasure)?.getEndTime() ?: 0

// 设置循环
syncCoordinator.setLoopRange(startTime, endTime)
syncCoordinator.play()
```

### 场景3: 慢速练习

```kotlin
// 以 0.75 倍速练习
syncCoordinator.setSpeed(0.75f)
syncCoordinator.play()

// 练习完成后恢复正常速度
syncCoordinator.setSpeed(1.0f)
```

### 场景4: 显示接下来的音符

```kotlin
scoreFollower.addListener(object : ScoreFollowerListener {
    override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
        // 当前音符
        highlightCurrentNote(note)
        
        // 下一个音符（预览）
        val nextNote = scoreFollower.getNextNote()
        showNextNote(nextNote)
    }
})
```

## 🎨 UI 集成示例

### 实时高亮音符

```kotlin
@Composable
fun ScoreView(
    song: Song,
    scoreFollower: ScoreFollower
) {
    var currentNote by remember { mutableStateOf<Note?>(null) }
    var currentMeasure by remember { mutableStateOf(0) }
    
    // 监听变化
    DisposableEffect(scoreFollower) {
        val listener = object : ScoreFollowerListener {
            override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
                currentNote = note
                currentMeasure = measureNumber
            }
        }
        
        scoreFollower.addListener(listener)
        
        onDispose {
            scoreFollower.removeListener(listener)
        }
    }
    
    // 渲染曲谱
    Column {
        song.measures.forEach { measure ->
            MeasureView(
                measure = measure,
                isCurrentMeasure = measure.measureNumber == currentMeasure,
                currentNote = if (measure.measureNumber == currentMeasure) currentNote else null
            )
        }
    }
}

@Composable
fun MeasureView(
    measure: Measure,
    isCurrentMeasure: Boolean,
    currentNote: Note?
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(if (isCurrentMeasure) Color.LightGray else Color.White)
    ) {
        measure.notes.forEach { note ->
            NoteView(
                note = note,
                isHighlighted = note == currentNote
            )
        }
    }
}

@Composable
fun NoteView(
    note: Note,
    isHighlighted: Boolean
) {
    Text(
        text = note.pitch,
        fontSize = 20.sp,
        color = if (isHighlighted) Color.Red else Color.Black,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.padding(4.dp)
    )
}
```

### 进度条

```kotlin
@Composable
fun ProgressBar(scoreFollower: ScoreFollower) {
    var progress by remember { mutableStateOf(0f) }
    var positionText by remember { mutableStateOf("00:00 / 00:00") }
    
    DisposableEffect(scoreFollower) {
        val listener = object : ScoreFollowerListener {
            override fun onProgressUpdate(positionMs: Long, totalMs: Long, progress: Float) {
                this@ProgressBar.progress = progress
                positionText = formatTime(positionMs, totalMs)
            }
        }
        
        scoreFollower.addListener(listener)
        onDispose { scoreFollower.removeListener(listener) }
    }
    
    Column {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
        
        Text(
            text = positionText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun formatTime(positionMs: Long, totalMs: Long): String {
    val posMin = positionMs / 60000
    val posSec = (positionMs % 60000) / 1000
    val totalMin = totalMs / 60000
    val totalSec = (totalMs % 60000) / 1000
    return String.format("%02d:%02d / %02d:%02d", posMin, posSec, totalMin, totalSec)
}
```

## ⚠️ 注意事项

### 同步精度
- ScoreFollower 更新频率: 20fps (每 50ms)
- 音符定位精度: ±25ms
- 对于大多数练习场景足够准确

### 性能优化
- ScoreFollower 使用高效的索引查找
- 避免在监听器中执行耗时操作
- UI 更新使用 Compose 的 mutableState

### 同步偏移
- 如果音频和曲谱不同步，使用 `setSyncOffset()`
- 正值：曲谱提前
- 负值：曲谱延后
- 通常偏移在 ±200ms 内

## 🎯 最佳实践

### 1. 生命周期管理
```kotlin
class PracticeViewModel : ViewModel() {
    private var syncCoordinator: SyncCoordinator? = null
    
    override fun onCleared() {
        syncCoordinator?.release()
        super.onCleared()
    }
}
```

### 2. 错误处理
```kotlin
scoreFollower.addListener(object : ScoreFollowerListener {
    override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
        try {
            updateUI(note, measureNumber)
        } catch (e: Exception) {
            Log.e("Score", "UI 更新失败", e)
        }
    }
})
```

### 3. 性能监控
```kotlin
scoreFollower.addListener(object : ScoreFollowerListener {
    private var lastUpdateTime = 0L
    
    override fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {
        val now = System.currentTimeMillis()
        val delta = now - lastUpdateTime
        
        if (delta > 100) {
            Log.w("Perf", "音符更新延迟: ${delta}ms")
        }
        
        lastUpdateTime = now
    }
})
```

## 📚 参考资料

- ScoreFollower.kt - 曲谱跟随器实现
- SyncCoordinator.kt - 同步协调器实现
- AudioPlaybackManager.kt - 音频播放管理器

---

## 🎊 总结

Phase 3 提供了完整的曲谱同步功能：

✅ 精确的时间定位（±25ms）  
✅ 自动音符跟踪  
✅ 小节自动翻页  
✅ 循环练习支持  
✅ 变速播放支持  
✅ 同步偏移调整  

**现在可以实现完整的跟音练习功能了！** 🎻✨
