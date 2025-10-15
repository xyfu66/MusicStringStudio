# 迭代2开发计划 - 跟音核心功能

## 🎯 目标
实现完整的跟音练习功能：曲谱解析、同步、音准比对、实时反馈

## 📋 当前状态
- ✅ 迭代1完成：音频采集、音高检测、基础UI
- ✅ 可以实时检测音高并显示
- 🔜 迭代2：实现跟随示范音频练习并评分

## 🗂️ 开发任务（按优先级）

### Phase 1: 数据模型与曲谱解析（第1周）

#### Task 2.1: 完善数据模型
- [ ] 扩展 `Note.kt`
  - 添加音符类型（四分音符、二分音符等）
  - 添加演奏结果字段
  
- [ ] 扩展 `Measure.kt`
  - 添加拍号信息
  - 添加速度信息
  
- [ ] 扩展 `Song.kt`
  - 添加曲目元数据
  - 添加音频URL
  
- [ ] 创建 `PracticeSession.kt`
  ```kotlin
  data class PracticeSession(
      val id: String,
      val songId: String,
      val userId: String,
      val startTime: Long,
      val endTime: Long?,
      val noteResults: List<NoteResult>,
      val score: Int = 0,
      val accuracyRate: Float = 0f
  )
  ```
  
- [ ] 创建 `NoteResult.kt`
  ```kotlin
  data class NoteResult(
      val targetNote: Note,
      val detectedFrequency: Float,
      val deviation: Float,  // cents
      val isHit: Boolean,
      val timestamp: Long
  )
  ```

#### Task 2.2: 曲谱JSON格式
- [ ] 创建示例曲谱文件
  - `res/raw/song_little_star.json`
  - `res/raw/song_simple_scale.json`
  
- [ ] JSON Schema 设计
  ```json
  {
    "id": "song_001",
    "title": "小星星",
    "composer": "传统",
    "tempo": 120,
    "timeSignature": "4/4",
    "key": "C",
    "audioUrl": "file:///android_asset/audio/little_star.mp3",
    "measures": [
      {
        "number": 1,
        "notes": [
          {
            "pitch": "C5",
            "frequency": 523.25,
            "startTime": 0,
            "duration": 500,
            "notation": "quarter"
          }
        ]
      }
    ]
  }
  ```

#### Task 2.3: 曲谱解析器
- [ ] 创建 `ScoreParser.kt`
  ```kotlin
  class ScoreParser {
      fun parseFromJson(json: String): Song
      fun parseFromResource(context: Context, @RawRes resId: Int): Song
      fun validateScore(song: Song): Boolean
  }
  ```
  
- [ ] 单元测试
  - 测试JSON解析
  - 测试错误处理
  - 测试数据验证

### Phase 2: 示范音频播放（第1-2周）

#### Task 2.4: AudioPlaybackManager
- [ ] 创建 `AudioPlaybackManager.kt`
  ```kotlin
  class AudioPlaybackManager(context: Context) {
      fun loadAudio(url: String)
      fun play()
      fun pause()
      fun stop()
      fun seekTo(positionMs: Long)
      fun getCurrentPosition(): Long
      fun setSpeed(speed: Float) // 0.5x - 1.5x
      fun setLoopRange(startMs: Long, endMs: Long)
      
      interface PlaybackListener {
          fun onPositionChanged(positionMs: Long)
          fun onPlaybackStateChanged(isPlaying: Boolean)
          fun onCompleted()
      }
  }
  ```
  
- [ ] 集成 ExoPlayer
- [ ] 实现播放控制
- [ ] 实现变速播放
- [ ] 实现循环播放
- [ ] 单元测试

#### Task 2.5: 准备示范音频
- [ ] 录制或下载示范音频
  - 小星星.mp3
  - C大调音阶.mp3
  
- [ ] 放置到 `assets/audio/` 目录
- [ ] 测试音频加载和播放

### Phase 3: 曲谱同步（第2周）

#### Task 2.6: ScoreFollower
- [ ] 创建 `ScoreFollower.kt`
  ```kotlin
  class ScoreFollower(private val song: Song) {
      fun getMeasureAtTime(timeMs: Long): Measure?
      fun getNoteAtTime(timeMs: Long): Note?
      fun getNextNote(timeMs: Long): Note?
      fun getCurrentNoteIndex(timeMs: Long): Int
      
      // 提前加载（预读）
      fun preloadNote(timeMs: Long, lookAheadMs: Long = 100): Note?
  }
  ```
  
- [ ] 实现时间定位算法
- [ ] 实现预读机制
- [ ] 单元测试

#### Task 2.7: 同步协调器
- [ ] 创建 `SyncCoordinator.kt`
  ```kotlin
  class SyncCoordinator(
      private val playbackManager: AudioPlaybackManager,
      private val scoreFollower: ScoreFollower
  ) {
      fun start()
      fun stop()
      
      interface SyncListener {
          fun onCurrentNoteChanged(note: Note, index: Int)
          fun onNextNotePreload(note: Note)
      }
  }
  ```
  
- [ ] 实现60fps更新
- [ ] 实现音符切换事件
- [ ] 处理播放状态变化

### Phase 4: 音准比对与评分（第2-3周）

#### Task 2.8: PitchComparator
- [ ] 创建 `PitchComparator.kt`
  ```kotlin
  class PitchComparator {
      fun compare(
          targetNote: Note,
          detectedFrequency: Float,
          timestamp: Long
      ): ComparisonResult
      
      data class ComparisonResult(
          val deviation: Float,  // cents
          val accuracyLevel: AccuracyLevel,
          val isInTimeWindow: Boolean,
          val isHit: Boolean
      )
      
      enum class AccuracyLevel {
          PERFECT,  // < 10 cents
          GOOD,     // 10-25 cents
          FAIR,     // 25-50 cents
          POOR      // > 50 cents
      }
  }
  ```
  
- [ ] 实现偏差计算
- [ ] 实现时间窗口判定
- [ ] 实现准确度评级
- [ ] 单元测试

#### Task 2.9: 评分系统
- [ ] 创建 `ScoreCalculator.kt`
  ```kotlin
  class ScoreCalculator {
      fun calculateSessionScore(results: List<NoteResult>): SessionScore
      
      data class SessionScore(
          val totalScore: Int,        // 0-100
          val accuracyRate: Float,    // 0-1
          val perfectCount: Int,
          val goodCount: Int,
          val fairCount: Int,
          val poorCount: Int,
          val missCount: Int,
          val averageDeviation: Float
      )
  }
  ```
  
- [ ] 实现评分算法
- [ ] 实现统计计算
- [ ] 单元测试

### Phase 5: 练习引擎整合（第3周）

#### Task 2.10: PracticeEngine
- [ ] 创建 `PracticeEngine.kt`
  ```kotlin
  class PracticeEngine(
      private val audioCapture: AudioCaptureManager,
      private val pitchDetector: PitchDetector,
      private val playbackManager: AudioPlaybackManager,
      private val scoreFollower: ScoreFollower,
      private val pitchComparator: PitchComparator
  ) {
      fun startPractice(song: Song)
      fun pausePractice()
      fun resumePractice()
      fun stopPractice(): PracticeSession
      
      interface PracticeListener {
          fun onNoteDetected(frequency: Float, note: String)
          fun onNoteResult(result: NoteResult)
          fun onCurrentNoteChanged(note: Note)
          fun onProgress(currentTime: Long, totalTime: Long)
      }
  }
  ```
  
- [ ] 实现数据处理管道
  ```
  [音频采集] → [音高检测] → [音准比对] → [结果收集]
       ↓
  [播放器] → [曲谱定位] → [目标音符]
  ```
  
- [ ] 实现状态管理
- [ ] 实现错误处理
- [ ] 集成测试

#### Task 2.11: PracticeViewModel 增强
- [ ] 扩展 `PracticeViewModel.kt`
  - 添加曲谱加载
  - 添加播放控制
  - 添加练习状态管理
  - 添加结果收集
  
- [ ] 状态管理
  ```kotlin
  sealed class PracticeState {
      object Idle : PracticeState()
      object Loading : PracticeState()
      object Ready : PracticeState()
      object Playing : PracticeState()
      object Paused : PracticeState()
      data class Completed(val session: PracticeSession) : PracticeState()
      data class Error(val message: String) : PracticeState()
  }
  ```

### Phase 6: UI实现（第3-4周）

#### Task 2.12: 练习界面重构
- [ ] 重构 `PracticeScreen.kt`
  - 添加曲目选择
  - 添加曲谱显示（简化版）
  - 添加进度条
  - 添加播放控制
  - 添加实时反馈
  
- [ ] 创建 `SongSelectionScreen.kt`
  - 显示曲目列表
  - 选择曲目
  - 预览曲目信息

#### Task 2.13: 简化曲谱显示
- [ ] 创建 `SimpleScoreDisplay.kt`
  ```kotlin
  @Composable
  fun SimpleScoreDisplay(
      currentNote: Note?,
      noteResults: Map<Int, NoteResult>,
      modifier: Modifier = Modifier
  ) {
      // 显示当前音符
      // 显示音符序列（横向滚动）
      // 用颜色标记已演奏音符的准确度
  }
  ```
  
- [ ] 音符可视化
  - 当前音符高亮
  - 已演奏音符色标
  - 未演奏音符灰显

#### Task 2.14: 播放控制UI
- [ ] 创建 `PlaybackControls.kt`
  ```kotlin
  @Composable
  fun PlaybackControls(
      isPlaying: Boolean,
      onPlayPause: () -> Unit,
      onStop: () -> Unit,
      progress: Float,
      onSeek: (Float) -> Unit,
      speed: Float,
      onSpeedChange: (Float) -> Unit
  )
  ```
  
- [ ] 播放/暂停按钮
- [ ] 停止按钮
- [ ] 进度条
- [ ] 速度调节

#### Task 2.15: 练习报告界面
- [ ] 创建 `PracticeReportScreen.kt`
  ```kotlin
  @Composable
  fun PracticeReportScreen(
      session: PracticeSession,
      score: SessionScore,
      onReplay: () -> Unit,
      onBackToList: () -> Unit
  )
  ```
  
- [ ] 显示总分
- [ ] 显示统计数据
- [ ] 显示准确率分布
- [ ] 重新练习/返回按钮

### Phase 7: 数据持久化（第4周）

#### Task 2.16: Room数据库
- [ ] 创建数据库实体
  ```kotlin
  @Entity(tableName = "practice_sessions")
  data class PracticeSessionEntity(
      @PrimaryKey val id: String,
      val songId: String,
      val startTime: Long,
      val endTime: Long,
      val score: Int,
      val accuracyRate: Float
  )
  
  @Entity(tableName = "note_results")
  data class NoteResultEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val sessionId: String,
      val noteIndex: Int,
      val targetPitch: String,
      val targetFrequency: Float,
      val detectedFrequency: Float,
      val deviation: Float,
      val isHit: Boolean
  )
  ```
  
- [ ] 创建 DAO
  ```kotlin
  @Dao
  interface PracticeSessionDao {
      @Insert
      suspend fun insertSession(session: PracticeSessionEntity)
      
      @Query("SELECT * FROM practice_sessions ORDER BY startTime DESC")
      fun getAllSessions(): Flow<List<PracticeSessionEntity>>
      
      @Query("SELECT * FROM practice_sessions WHERE id = :id")
      suspend fun getSessionById(id: String): PracticeSessionEntity?
  }
  ```
  
- [ ] 创建数据库
  ```kotlin
  @Database(
      entities = [PracticeSessionEntity::class, NoteResultEntity::class],
      version = 1
  )
  abstract class AppDatabase : RoomDatabase() {
      abstract fun practiceSessionDao(): PracticeSessionDao
      abstract fun noteResultDao(): NoteResultDao
  }
  ```

#### Task 2.17: Repository层
- [ ] 创建 `PracticeRepository.kt`
  ```kotlin
  class PracticeRepository(
      private val sessionDao: PracticeSessionDao,
      private val noteResultDao: NoteResultDao
  ) {
      suspend fun saveSession(session: PracticeSession)
      fun getAllSessions(): Flow<List<PracticeSession>>
      suspend fun getSessionById(id: String): PracticeSession?
      suspend fun deleteSession(id: String)
  }
  ```

### Phase 8: 测试与优化（第4周）

#### Task 2.18: 集成测试
- [ ] 端到端测试
  - 加载曲谱
  - 播放音频
  - 模拟演奏
  - 生成报告
  
- [ ] 性能测试
  - 延迟测试（< 100ms）
  - CPU占用测试
  - 内存占用测试
  
- [ ] 兼容性测试
  - 不同Android版本
  - 不同设备

#### Task 2.19: 优化
- [ ] 降低延迟
- [ ] 优化内存使用
- [ ] 优化UI流畅度
- [ ] 错误处理完善

## 📊 开发时间表

| 周次 | 主要任务 | 产出 |
|------|---------|------|
| 第1周 | Phase 1-2 | 数据模型、解析器、播放器 |
| 第2周 | Phase 3-4 | 同步、比对、评分 |
| 第3周 | Phase 5-6 | 引擎整合、UI实现 |
| 第4周 | Phase 7-8 | 数据库、测试、优化 |

## ✅ 完成标准

- [ ] 可以加载和播放示范音频
- [ ] 可以实时跟随曲谱定位
- [ ] 可以实时比对用户演奏
- [ ] 可以显示实时反馈（色标）
- [ ] 可以生成练习报告
- [ ] 可以保存练习记录
- [ ] 延迟 < 100ms
- [ ] 无明显卡顿
- [ ] 至少2首测试曲目可用

## 🚀 开始 Phase 1！

准备开始第一个任务：扩展数据模型
