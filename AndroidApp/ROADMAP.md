# MusicStringStudio Android App 开发路线图

## 项目概览

**项目名称**: MusicStringStudio - 小提琴智能陪练APP  
**当前状态**: 空项目（Kotlin + Jetpack Compose）  
**目标**: 构建一个功能完整的小提琴实时跟音练习与音准评测应用

---

## 技术栈确认

### 当前项目配置
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **最小SDK**: 26 (Android 8.0)
- **目标SDK**: 36
- **构建工具**: Gradle (Kotlin DSL)

### 需要集成的核心库
- TarsosDSP (音频处理与YIN算法)
- ExoPlayer (音频播放)
- Room (本地数据库)
- Retrofit (网络请求)
- MPAndroidChart (数据可视化)
- CameraX/Canvas (五线谱绘制)

---

## 开发路线图总览

```
迭代0: 环境准备 (1周)
    ↓
迭代1: 音频基础 (2-3周) ← MVP核心
    ↓
迭代2: 跟音核心功能 (3-4周) ← MVP核心
    ↓
迭代3: UI完善与调音器 (2周)
    ↓
迭代4: 用户系统与曲目库 (2-3周)
    ↓
迭代5: 教师功能 (2周)
    ↓
迭代6: 后台管理与优化 (2周)
```

**总预计时间**: 14-17周

---

## 迭代0: 环境准备与项目初始化 (1周)

### 目标
建立完整的开发环境，配置所有必要的依赖库，创建基础项目架构。

### 任务清单

#### 0.1 依赖库配置
- [ ] 配置 `build.gradle.kts` 添加所有必要依赖
- [ ] 配置 `AndroidManifest.xml` 添加权限声明
  - 录音权限 (RECORD_AUDIO)
  - 网络权限 (INTERNET)
  - 存储权限 (READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
- [ ] 配置 ProGuard 规则（针对音频库）
- [ ] 同步并验证所有依赖可用

#### 0.2 项目架构设置
- [ ] 创建包结构
  ```
  com.example.musicstringstudioapp/
  ├── audio/          # 音频处理模块
  ├── score/          # 曲谱相关
  ├── practice/       # 练习功能
  ├── tuner/          # 调音器
  ├── metronome/      # 节拍器
  ├── user/           # 用户管理
  ├── ui/             # UI组件
  ├── data/           # 数据层
  ├── network/        # 网络层
  └── utils/          # 工具类
  ```
- [ ] 设置依赖注入（推荐 Hilt）
- [ ] 配置导航（Jetpack Navigation Compose）
- [ ] 创建基础ViewModel和Repository模板

#### 0.3 测试环境
- [ ] 配置单元测试框架
- [ ] 配置UI测试框架
- [ ] 创建测试用例模板
- [ ] 准备测试音频文件（标准音A4, 简单旋律）

### 交付物
- 完整配置的项目
- 基础架构代码
- 可编译运行的空白APP

---

## 迭代1: 音频基础功能 (2-3周) - MVP阶段1

### 目标
实现音频采集、音高检测和基础播放功能。这是整个应用的核心基础。

### 任务清单

#### 1.1 音频采集模块
- [ ] 创建 `AudioCaptureManager.kt`
  - [ ] 初始化 AudioRecord
  - [ ] 配置采样率 44100Hz
  - [ ] 实现实时音频数据读取
  - [ ] 使用协程处理异步采集
  - [ ] 添加音量检测（静音判断）
- [ ] 创建 `AudioPermissionHandler.kt`
  - [ ] 处理录音权限请求
  - [ ] 权限拒绝后的引导处理
- [ ] 单元测试：验证音频采集正常

#### 1.2 音高检测模块
- [ ] 集成 TarsosDSP 库
- [ ] 创建 `PitchDetector.kt`
  - [ ] 封装 YIN 算法
  - [ ] 实现频率检测
  - [ ] 添加可靠性检测
  - [ ] 实现移动平均平滑算法
- [ ] 创建 `FrequencyUtils.kt`
  - [ ] 频率 → MIDI音符转换
  - [ ] 频率 → 音符名称转换
  - [ ] 计算音分偏差
- [ ] 创建 `MusicTheory.kt`
  - [ ] 音符理论常量
  - [ ] 音程计算
  - [ ] 音阶映射
- [ ] 单元测试：
  - [ ] 测试标准音A4 (440Hz)
  - [ ] 测试各八度音符识别
  - [ ] 测试偏差计算精度

#### 1.3 音频播放模块
- [ ] 集成 ExoPlayer
- [ ] 创建 `AudioPlaybackManager.kt`
  - [ ] 播放示范音频
  - [ ] 支持暂停/继续/停止
  - [ ] 获取当前播放进度（毫秒级）
  - [ ] 实现变速播放 (0.5x - 1.5x)
  - [ ] 循环播放指定段落
- [ ] 创建 `AudioSyncManager.kt`
  - [ ] 同步播放进度与UI
  - [ ] 延迟补偿
- [ ] 单元测试：验证播放控制和进度获取

#### 1.4 测试与验证
- [ ] 创建简单测试UI
  - [ ] 显示实时频率
  - [ ] 显示音符名称
  - [ ] 显示偏差值
  - [ ] 音频播放控制按钮
- [ ] 真机测试
  - [ ] 使用调音器APP对比精度
  - [ ] 测试不同环境噪音影响
  - [ ] 测量采集到显示的延迟

### 交付物
- 可工作的音频采集和检测系统
- 音频播放控制系统
- 测试UI界面
- 测试报告（精度、延迟数据）

### 关键代码示例

#### AudioCaptureManager.kt 伪代码
```kotlin
class AudioCaptureManager(context: Context) {
    private val SAMPLE_RATE = 44100
    private val BUFFER_SIZE = 4096
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    fun startCapture(onAudioData: (FloatArray) -> Unit) {
        // 初始化 AudioRecord
        // 在协程中循环读取音频数据
        // 回调数据给 PitchDetector
    }
    
    fun stopCapture() {
        // 停止录音并释放资源
    }
    
    fun checkSilence(buffer: FloatArray): Boolean {
        // 计算RMS能量判断是否静音
    }
}
```

---

## 迭代2: 跟音核心功能 (3-4周) - MVP阶段2

### 目标
实现完整的跟音练习功能：曲谱解析、同步、音准比对、实时反馈。

### 任务清单

#### 2.1 曲谱数据模型
- [ ] 创建数据模型
  - [ ] `Note.kt` - 音符实体
  - [ ] `Measure.kt` - 小节实体
  - [ ] `Song.kt` - 曲目实体
  - [ ] `PracticeSession.kt` - 练习会话
  - [ ] `NoteResult.kt` - 音符演奏结果
- [ ] 定义JSON格式规范（参考开发大纲）
- [ ] 创建示例曲谱JSON文件（小星星、简单练习曲）

#### 2.2 曲谱解析模块
- [ ] 创建 `ScoreParser.kt`
  - [ ] 解析JSON格式曲谱
  - [ ] 验证数据完整性
  - [ ] 处理解析异常
- [ ] 创建 `MusicXMLParser.kt`（可选）
  - [ ] 支持标准MusicXML格式
  - [ ] 转换为内部数据模型
- [ ] 单元测试：验证各种格式解析

#### 2.3 曲谱同步模块
- [ ] 创建 `ScoreFollower.kt`
  - [ ] 根据播放进度定位当前音符
  - [ ] 提前加载下一音符
  - [ ] 处理拍号变化
  - [ ] 处理速度变化
  - [ ] 实现自动翻页逻辑
- [ ] 创建 `TimeMapper.kt`
  - [ ] BPM到毫秒转换
  - [ ] 音符时值计算
  - [ ] 时间窗口计算
- [ ] 单元测试：验证定位精度

#### 2.4 音准比对模块
- [ ] 创建 `PitchComparator.kt`
  - [ ] 获取当前目标音符
  - [ ] 比对用户演奏音高
  - [ ] 计算音分偏差
  - [ ] 判定准确性等级（完美/良好/偏差/较差）
  - [ ] 检测时间窗口命中
  - [ ] 记录演奏结果
- [ ] 创建 `AccuracyCalculator.kt`
  - [ ] 计算准确率
  - [ ] 计算平均偏差
  - [ ] 生成评分（0-100）
- [ ] 单元测试：验证比对逻辑

#### 2.5 练习核心逻辑
- [ ] 创建 `PracticeViewModel.kt`
  - [ ] 管理练习状态（准备/进行中/暂停/完成）
  - [ ] 协调各模块工作
  - [ ] 处理用户交互
  - [ ] 收集练习数据
- [ ] 创建 `PracticeEngine.kt`
  - [ ] 启动练习流程
  - [ ] 实时数据处理管道
    ```
    音频采集 → 音高检测 → 音准比对 → UI更新
         ↓
    播放器进度 → 曲谱定位 → 目标音符
    ```
  - [ ] 异常处理（检测失败、音频中断等）

#### 2.6 数据持久化
- [ ] 配置 Room 数据库
- [ ] 创建数据库实体
  - [ ] PracticeSessionEntity
  - [ ] NoteResultEntity
  - [ ] SongEntity
- [ ] 创建 DAO 接口
- [ ] 创建 Repository 层
- [ ] 实现数据缓存策略

### 交付物
- 完整的跟音练习引擎
- 曲谱解析和同步系统
- 音准比对和评分系统
- 数据持久化方案
- 至少3首测试曲目

---

## 迭代3: UI完善与辅助工具 (2周)

### 目标
完善练习界面，实现五线谱显示、调音器和节拍器功能。

### 任务清单

#### 3.1 五线谱显示
- [ ] 创建 `StaffView.kt` (自定义Compose组件)
  - [ ] 绘制五线谱（高音谱号）
  - [ ] 绘制音符
  - [ ] 实现音符高亮
  - [ ] 颜色标注（绿/黄/橙/红）
  - [ ] 滚动和翻页
- [ ] 创建 `StaffRenderer.kt`
  - [ ] Canvas绘制逻辑
  - [ ] 音符位置计算
  - [ ] 动画效果
- [ ] 可选：集成开源五线谱库
  - [ ] 评估 AlphaTab 或其他Kotlin兼容库
  - [ ] 如不合适，使用自定义Canvas绘制

#### 3.2 实时反馈UI
- [ ] 创建 `TunerIndicatorView.kt`
  - [ ] 调音器样式的指针指示器
  - [ ] 显示偏差数值
  - [ ] 色标反馈
- [ ] 创建 `PitchWaveformView.kt`（可选）
  - [ ] 显示实时音高曲线
  - [ ] 对比目标音高
- [ ] 创建 `PracticeControlBar.kt`
  - [ ] 播放/暂停按钮
  - [ ] 速度控制
  - [ ] 循环控制
  - [ ] 节拍器开关

#### 3.3 练习主界面
- [ ] 创建 `PracticeScreen.kt`
  - [ ] 整合五线谱显示区
  - [ ] 整合实时反馈区
  - [ ] 整合控制栏
  - [ ] 响应式布局（竖屏/横屏）
- [ ] 实现交互逻辑
  - [ ] 播放控制
  - [ ] 拖动进度条
  - [ ] 设置循环区间
  - [ ] 速度调节

#### 3.4 调音器模块
- [ ] 创建 `TunerScreen.kt`
  - [ ] 独立调音器页面
  - [ ] 实时频率显示
  - [ ] 音符名称显示
  - [ ] 偏差指示器
  - [ ] 参考音播放
- [ ] 创建 `TunerViewModel.kt`
  - [ ] 复用 PitchDetector
  - [ ] 管理调音器状态
- [ ] 支持弦选择（G D A E）

#### 3.5 节拍器模块
- [ ] 创建 `MetronomeScreen.kt`
  - [ ] BPM设置
  - [ ] 拍号选择
  - [ ] 视觉节拍指示
  - [ ] 音效选择
- [ ] 创建 `MetronomeEngine.kt`
  - [ ] 精确定时器
  - [ ] 节拍音播放
  - [ ] 强弱拍区分
- [ ] 整合到练习界面

### 交付物
- 完整的练习UI界面
- 独立的调音器功能
- 独立的节拍器功能
- UI/UX流畅体验

---

## 迭代4: 用户系统与曲目库 (2-3周)

### 目标
实现用户管理、曲目浏览、上传和收藏功能。

### 任务清单

#### 4.1 用户认证
- [ ] 设计用户数据模型
- [ ] 创建 `AuthRepository.kt`
  - [ ] 注册
  - [ ] 登录
  - [ ] JWT token管理
- [ ] 创建 `AuthViewModel.kt`
- [ ] 创建认证UI
  - [ ] `LoginScreen.kt`
  - [ ] `RegisterScreen.kt`
  - [ ] `ProfileScreen.kt`

#### 4.2 角色管理
- [ ] 实现角色系统（学生/教师/管理员）
- [ ] 权限控制
- [ ] 角色切换UI

#### 4.3 曲目库
- [ ] 创建 `SongRepository.kt`
  - [ ] 获取曲目列表
  - [ ] 搜索和筛选
  - [ ] 下载曲谱和音频
- [ ] 创建 `SongLibraryScreen.kt`
  - [ ] 曲目列表
  - [ ] 分类筛选
  - [ ] 搜索功能
  - [ ] 曲目详情
- [ ] 创建 `SongDetailScreen.kt`
  - [ ] 曲目信息
  - [ ] 试听
  - [ ] 开始练习
  - [ ] 收藏

#### 4.4 上传功能
- [ ] 创建 `UploadScreen.kt`
  - [ ] 选择文件
  - [ ] 填写信息
  - [ ] 标签选择
  - [ ] 上传进度
- [ ] 创建 `UploadManager.kt`
  - [ ] 文件上传
  - [ ] 断点续传
  - [ ] 错误处理

#### 4.5 收藏与播放列表
- [ ] 本地收藏管理
- [ ] 创建自定义歌单
- [ ] 同步到云端

### 交付物
- 完整的用户系统
- 曲目库浏览和搜索
- 曲目上传功能
- 收藏和歌单管理

---

## 迭代5: 教师功能与报告系统 (2周)

### 目标
实现练习报告、教师批改和学生管理功能。

### 任务清单

#### 5.1 练习报告
- [ ] 创建 `PracticeReportScreen.kt`
  - [ ] 总分显示
  - [ ] 准确率统计
  - [ ] 偏差分布图表
  - [ ] 详细音符列表
- [ ] 创建 `ReportGenerator.kt`
  - [ ] 数据汇总
  - [ ] 图表生成（使用MPAndroidChart）
  - [ ] 建议生成
- [ ] 创建 `ReportDetailScreen.kt`
  - [ ] 逐音符查看
  - [ ] 回放演奏
  - [ ] 对比示范

#### 5.2 教师批改界面
- [ ] 创建 `TeacherReviewScreen.kt`
  - [ ] 学生列表
  - [ ] 练习记录列表
  - [ ] 批改界面
- [ ] 创建 `AnnotationTool.kt`
  - [ ] 文字点评
  - [ ] 语音点评（可选）
  - [ ] 分数修改

#### 5.3 学生管理
- [ ] 创建 `StudentManagementScreen.kt`
  - [ ] 学生列表
  - [ ] 绑定/解绑
  - [ ] 查看进度
- [ ] 创建 `AssignmentScreen.kt`
  - [ ] 布置作业
  - [ ] 设置截止日期
  - [ ] 推荐曲目

#### 5.4 统计分析
- [ ] 创建 `StatisticsScreen.kt`
  - [ ] 个人练习统计
  - [ ] 进度曲线
  - [ ] 薄弱点分析
- [ ] 数据可视化
  - [ ] 时间分布
  - [ ] 准确率趋势
  - [ ] 曲目掌握度

### 交付物
- 完整的练习报告系统
- 教师批改功能
- 学生管理面板
- 统计分析工具

---

## 迭代6: 后台管理与优化 (2周)

### 目标
实现管理员功能，优化性能，完善错误处理。

### 任务清单

#### 6.1 管理员后台
- [ ] 创建 `AdminDashboardScreen.kt`
  - [ ] 用户管理
  - [ ] 内容审核
  - [ ] 平台统计
- [ ] 创建审核工具
  - [ ] 曲谱审核
  - [ ] 举报处理

#### 6.2 性能优化
- [ ] 音频处理优化
  - [ ] 减少延迟
  - [ ] 降低CPU占用
  - [ ] 内存优化
- [ ] UI优化
  - [ ] 流畅度优化
  - [ ] 减少重组
  - [ ] LazyColumn优化
- [ ] 网络优化
  - [ ] 缓存策略
  - [ ] 图片压缩
  - [ ] 请求合并

#### 6.3 错误处理
- [ ] 统一错误处理机制
- [ ] 崩溃报告（Firebase Crashlytics）
- [ ] 用户友好的错误提示
- [ ] 降级方案

#### 6.4 测试完善
- [ ] 单元测试覆盖率 > 70%
- [ ] 集成测试
- [ ] UI测试
- [ ] 真机兼容性测试

#### 6.5 文档完善
- [ ] API文档
- [ ] 用户手册
- [ ] 教师使用指南
- [ ] 管理员文档

### 交付物
- 管理员后台
- 性能优化报告
- 完整测试报告
- 项目文档

---

## 关键技术实现指南

### 1. 降低音频延迟的策略

```kotlin
// 使用 Oboe 低延迟音频（Android 8.0+）
class LowLatencyAudioCapture {
    private var audioStream: AudioStream? = null
    
    fun initialize() {
        audioStream = AudioStreamBuilder()
            .setDirection(Direction.INPUT)
            .setSharingMode(SharingMode.EXCLUSIVE) // 独占模式
            .setPerformanceMode(PerformanceMode.LOW_LATENCY)
            .setFormat(AudioFormat.PCM_FLOAT)
            .setChannelCount(1)
            .setSampleRate(44100)
            .setCallback(object : AudioStreamCallback() {
                override fun onAudioReady(
                    audioStream: AudioStream,
                    audioData: Any,
                    numFrames: Int
                ): DataCallbackResult {
                    // 实时处理音频
                    return DataCallbackResult.CONTINUE
                }
            })
            .build()
    }
}
```

### 2. 精确的曲谱同步

```kotlin
class PrecisionSync(
    private val playbackManager: AudioPlaybackManager,
    private val scoreFollower: ScoreFollower
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun startSync() {
        scope.launch {
            while (isActive) {
                val currentPosition = playbackManager.getCurrentPosition()
                val currentNote = scoreFollower.getNoteAtTime(currentPosition)
                
                // 提前加载下一音符（预读100ms）
                val nextNote = scoreFollower.getNoteAtTime(currentPosition + 100)
                
                // UI更新
                updateUI(currentNote, nextNote)
                
                delay(16) // 60fps更新
            }
        }
    }
}
```

### 3. 音准判定优化

```kotlin
class AccuracyJudge {
    fun judge(
        targetFrequency: Float,
        actualFrequency: Float,
        isReliable: Boolean
    ): AccuracyLevel {
        if (!isReliable) return AccuracyLevel.UNRELIABLE
        
        val cents = 1200 * ln(actualFrequency / targetFrequency) / ln(2f)
        
        return when {
            abs(cents) < 10 -> AccuracyLevel.PERFECT
            abs(cents) < 25 -> AccuracyLevel.GOOD
            abs(cents) < 50 -> AccuracyLevel.FAIR
            else -> AccuracyLevel.POOR
        }
    }
}
```

---

## 依赖库配置清单

### build.gradle.kts (Project级别)

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### build.gradle.kts (Module级别)

```kotlin
dependencies {
    // === 核心依赖 ===
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // === Jetpack Compose ===
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // === Navigation ===
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // === 音频处理 ===
    implementation("be.tarsos.dsp:core:2.5")
    implementation("be.tarsos.dsp:jvm:2.5")
    implementation("com.google.oboe:oboe:1.8.0") // 低延迟音频
    
    // === 音视频播放 ===
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    
    // === 数据库 ===
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // === 网络 ===
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // === 图表 ===
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // === 权限 ===
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // === 依赖注入 ===
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // === 图片加载 ===
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // === 协程 ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // === 日志 ===
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // === 测试 ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### AndroidManifest.xml 权限配置

```xml
<!-- 录音权限 -->
<uses-permission android:name="android.permission.
