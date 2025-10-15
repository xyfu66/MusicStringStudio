# Phase 5: UI实现与完整集成指南

## 📋 概述

Phase 5 将所有核心功能整合到完整的用户界面中，提供完整的跟音练习体验。

## 🎯 架构总览

```
┌─────────────────────────────────────────────────┐
│                MainActivity                     │
│  ↓                                              │
│  PracticeScreen (Compose UI)                   │
│  ↓                                              │
│  PracticeViewModel                              │
│  ↓                                              │
│  ┌───────────────────────────────────────────┐ │
│  │         PracticeEngine                    │ │
│  │  ┌─────────────────────────────────────┐ │ │
│  │  │  AudioCaptureManager                 │ │ │
│  │  │  PitchDetector                       │ │ │
│  │  │  SyncCoordinator                     │ │ │
│  │  │    - ScoreFollower                   │ │ │
│  │  │    - AudioPlaybackManager            │ │ │
│  │  │  PitchComparator                     │ │ │
│  │  │  ScoreCalculator                     │ │ │
│  │  └─────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

## 🚀 完整集成步骤

### 步骤1: 更新 PracticeViewModel

```kotlin
class PracticeViewModel : ViewModel() {
    
    // 状态
    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Idle)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    
    // 实时反馈
    private val _realtimeFeedback = MutableStateFlow<RealtimeFeedback?>(null)
    val realtimeFeedback: StateFlow<RealtimeFeedback?> = _realtimeFeedback.asStateFlow()
    
    // 核心组件
    private var practiceEngine: PracticeEngine? = null
    private var song: Song? = null
    
    /**
     * 初始化练习
     */
    fun initializePractice(context: Context, songResourceId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = PracticeUiState.Loading
                
                // 1. 加载 MusicXML
                val musicXMLParser = MusicXMLParser()
                song = musicXMLParser.parseFromResource(context, songResourceId)
                
                // 2. 生成 MIDI
                val audioGenService = AudioGenerationService(context)
                val midiResult = audioGenService.generateMidiFromSong(song!!)
                
                val midiFile = midiResult.getOrThrow()
                
                // 3. 初始化播放器
                val audioPlayback = AudioPlaybackManager(context)
                audioPlayback.loadAudio(midiFile.absolutePath)
                
                // 4. 创建同步协调器
                val syncCoordinator = SyncCoordinatorFactory.create(song!!, audioPlayback)
                
                // 5. 初始化音频采集和音高检测
                val audioCapture = AudioCaptureManager(context)
                val pitchDetector = PitchDetector()
                
                // 6. 创建练习引擎
                practiceEngine = PracticeEngineFactory.create(
                    song!!,
                    syncCoordinator,
                    audioCapture,
                    pitchDetector
                )
                
                // 7. 添加监听器
                practiceEngine?.addListener(practiceListener)
                
                _uiState.value = PracticeUiState.Ready(song!!)
                
            } catch (e: Exception) {
                _uiState.value = PracticeUiState.Error(e.message ?: "初始化失败")
            }
        }
    }
    
    /**
     * 开始练习
     */
    fun startPractice() {
        practiceEngine?.startPractice()
        _uiState.value = PracticeUiState.Practicing(song!!)
    }
    
    /**
     * 停止练习
     */
    fun stopPractice() {
        practiceEngine?.completePractice()
    }
    
    /**
     * 暂停/恢复
     */
    fun togglePause() {
        // 实现暂停/恢复逻辑
    }
    
    /**
     * 练习监听器
     */
    private val practiceListener = object : PracticeEngine.PracticeListener {
        override fun onRealtimeFeedback(
            currentNote: Note?,
            detectedPitch: Float?,
            comparisonResult: PitchComparator.ComparisonResult?
        ) {
            _realtimeFeedback.value = RealtimeFeedback(
                currentNote,
                detectedPitch,
                comparisonResult
            )
        }
        
        override fun onPracticeCompleted(score: ScoreCalculator.PracticeScore) {
            _uiState.value = PracticeUiState.Completed(score)
        }
        
        override fun onError(error: String) {
            _uiState.value = PracticeUiState.Error(error)
        }
    }
    
    override fun onCleared() {
        practiceEngine?.release()
        super.onCleared()
    }
}

/**
 * UI 状态
 */
sealed class PracticeUiState {
    object Idle : PracticeUiState()
    object Loading : PracticeUiState()
    data class Ready(val song: Song) : PracticeUiState()
    data class Practicing(val song: Song) : PracticeUiState()
    data class Completed(val score: ScoreCalculator.PracticeScore) : PracticeUiState()
    data class Error(val message: String) : PracticeUiState()
}

/**
 * 实时反馈数据
 */
data class RealtimeFeedback(
    val currentNote: Note?,
    val detectedPitch: Float?,
    val comparisonResult: PitchComparator.ComparisonResult?
)
```

### 步骤2: 更新 PracticeScreen

```kotlin
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val realtimeFeedback by viewModel.realtimeFeedback.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.initializePractice(
            LocalContext.current,
            R.raw.little_star
        )
    }
    
    when (val state = uiState) {
        is PracticeUiState.Loading -> {
            LoadingScreen()
        }
        
        is PracticeUiState.Ready -> {
            ReadyScreen(
                song = state.song,
                onStartClick = { viewModel.startPractice() }
            )
        }
        
        is PracticeUiState.Practicing -> {
            PracticingScreen(
                song = state.song,
                realtimeFeedback = realtimeFeedback,
                onStopClick = { viewModel.stopPractice() }
            )
        }
        
        is PracticeUiState.Completed -> {
            ScoreReportScreen(score = state.score)
        }
        
        is PracticeUiState.Error -> {
            ErrorScreen(message = state.message)
        }
        
        else -> {
            // Idle state
        }
    }
}

@Composable
fun PracticingScreen(
    song: Song,
    realtimeFeedback: RealtimeFeedback?,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. 曲谱显示区
        ScoreView(
            song = song,
            currentNote = realtimeFeedback?.currentNote,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. 实时音准指示器
        PitchIndicator(
            targetNote = realtimeFeedback?.currentNote,
            detectedPitch = realtimeFeedback?.detectedPitch,
            comparisonResult = realtimeFeedback?.comparisonResult,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 3. 控制按钮
        Button(
            onClick = onStopClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("完成练习")
        }
    }
}

@Composable
fun ScoreReportScreen(score: ScoreCalculator.PracticeScore) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 总分显示
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "练习完成！",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${score.totalScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 72.sp
                )
                
                Text(
                    text = ScoreCalculator().getGradeText(score.grade),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = ScoreCalculator().getEncouragement(score.grade),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 统计数据
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "详细数据",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatRow("准确率", "${(score.statistics.accuracyRate * 100).toInt()}%")
                StatRow("平均偏差", "${score.statistics.averageDeviation.toInt()} cents")
                StatRow("完美音符", "${score.statistics.perfectCount}/${score.statistics.totalNotes}")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 优点
        if (score.strengths.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("优点", style = MaterialTheme.typography.titleMedium)
                    score.strengths.forEach { strength ->
                        Text("• $strength", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 建议
        if (score.suggestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("建议", style = MaterialTheme.typography.titleMedium)
                    score.suggestions.forEach { suggestion ->
                        Text("• $suggestion", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
```

## 📱 完整应用流程

```
1. 用户启动应用
   ↓
2. 选择曲目
   ↓
3. PracticeViewModel.initializePractice()
   - 加载 MusicXML
   - 生成 MIDI
   - 初始化所有组件
   ↓
4. 显示 ReadyScreen
   - 显示曲目信息
   - [开始练习] 按钮
   ↓
5. 点击开始 → startPractice()
   ↓
6. PracticingScreen
   - 五线谱显示
   - 实时音准指示器
   - 当前音符高亮
   - 偏差显示
   ↓
7. 练习过程中
   - PracticeEngine 实时处理
   - UI 30fps 更新
   - 视觉反馈
   ↓
8. 点击完成 → completePractice()
   ↓
9. ScoreReportScreen
   - 总分和评级
   - 详细统计
   - 优点和建议
   - [重新练习] / [返回] 按钮
```

## 🎨 UI组件清单

### 已实现
- ✅ PitchIndicator - 音准指示器（迭代1）
- ✅ PracticeScreen - 练习主界面（迭代1）
- ✅ PracticeViewModel - 视图模型（迭代1）

### 需要增强
- [ ] ScoreView - 五线谱显示（增强版）
- [ ] ScoreReportScreen - 得分报告（新增）
- [ ] 进度条组件
- [ ] 控制栏组件

## 🔧 关键集成点

### 1. 权限处理

```kotlin
// MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 请求录音权限
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO
        )
    }
}
```

### 2. 生命周期管理

```kotlin
@Composable
fun PracticeScreen() {
    val viewModel: PracticeViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pausePractice()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumePractice()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
```

## 📊 测试清单

- [ ] 完整练习流程测试
- [ ] 音频采集和播放同步测试
- [ ] UI 响应性能测试
- [ ] 不同设备适配测试
- [ ] 权限处理测试
- [ ] 生命周期测试

## 🎯 下一步优化

1. 五线谱高级渲染
2. 动画效果
3. 声音反馈
4. 数据持久化
5. 用户设置

---

## 🎊 总结

Phase 5 完成后，应用将具备：
- ✅ 完整的用户界面
- ✅ 流畅的练习体验
- ✅ 详细的得分报告
- ✅ 专业的反馈系统

**准备好发布第一个完整版本！** 🎻🎵✨
