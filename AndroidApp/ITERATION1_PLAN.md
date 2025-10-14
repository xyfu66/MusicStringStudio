# 迭代1开发计划 - MVP (最小可行产品)

## 🎯 目标
实现音频采集 + 实时音高检测 + 简单曲谱同步 + 基础UI

## 📋 任务清单

### 1. 音频采集模块 (AudioCapture)
- [ ] 创建 AudioCaptureManager
  - 使用 AudioRecord 实时采集音频
  - 处理录音权限
  - 音频缓冲区管理
  - 采样率: 44100Hz
  - 通道: 单声道
  - 编码: PCM 16bit

### 2. 音高检测模块 (PitchDetection)
- [ ] 研究并实现音高检测算法
  - 方案A: 寻找 TarsosDSP 的正确依赖
  - 方案B: 实现简化版 YIN 算法
  - 方案C: 使用 FFT + 自相关
- [ ] 创建 PitchDetector
  - 输入: PCM音频样本
  - 输出: 频率(Hz)、音符名称、置信度
- [ ] 创建 FrequencyConverter
  - 频率 → MIDI音符号
  - 频率 → 音符名称(C, D, E, F, G, A, B)
  - 频率 → 音分偏差(cents)

### 3. 数据模型 (Data Models)
- [ ] 创建 Note 数据类
  ```kotlin
  data class Note(
      val pitch: String,        // "A4"
      val frequency: Float,     // 440.0
      val startTime: Long,      // ms
      val duration: Long,       // ms
      val position: Int         // 在五线谱上的位置
  )
  ```
- [ ] 创建 Measure 数据类
- [ ] 创建 Song 数据类
- [ ] 创建简单的JSON解析器

### 4. 基础UI (Practice Screen)
- [ ] 创建 PracticeScreen Composable
  - 顶部: 当前检测到的音高显示
  - 中部: 音准指示器（类似调音器）
  - 底部: 控制按钮（开始/停止录音）
- [ ] 创建 PitchIndicator Composable
  - 显示当前频率
  - 显示音符名称
  - 显示音分偏差（±cents）
  - 颜色指示准确度
- [ ] 创建 PracticeViewModel
  - 管理音频采集状态
  - 处理音高检测结果
  - UI状态管理

### 5. 权限处理
- [ ] 请求并处理录音权限
- [ ] 权限拒绝时的提示

### 6. 测试
- [ ] 单元测试
  - FrequencyConverter 测试
  - Note/Measure 数据模型测试
- [ ] 集成测试
  - 音频采集测试
  - 音高检测准确性测试
- [ ] UI测试
  - 基础UI交互测试

## 🗂️ 文件结构

```
app/src/main/java/com/example/musicstringstudioapp/
├── data/
│   ├── model/
│   │   ├── Note.kt
│   │   ├── Measure.kt
│   │   └── Song.kt
│   └── parser/
│       └── SimpleScoreParser.kt
├── domain/
│   ├── audio/
│   │   ├── AudioCaptureManager.kt
│   │   └── PitchDetector.kt
│   └── converter/
│       └── FrequencyConverter.kt
├── ui/
│   ├── practice/
│   │   ├── PracticeScreen.kt
│   │   ├── PracticeViewModel.kt
│   │   └── components/
│   │       ├── PitchIndicator.kt
│   │       └── ControlButtons.kt
│   └── theme/
│       └── (已存在)
└── utils/
    ├── audio/ (已存在)
    └── music/ (已存在)
```

## 📊 开发优先级

**P0 (必须):**
1. 音频采集基础功能
2. 简单的音高检测（即使不够精确）
3. 基础UI显示

**P1 (重要):**
4. 音高检测优化
5. 音准指示器
6. 权限处理

**P2 (可选):**
7. 曲谱数据结构
8. 完整的单元测试

## 🚀 开发步骤

### Step 1: 数据模型 (30分钟)
创建 Note, Measure, Song 数据类

### Step 2: 音频采集 (1小时)
实现 AudioCaptureManager，处理权限

### Step 3: 音高检测 (2小时)
研究并实现音高检测算法

### Step 4: 频率转换 (30分钟)
实现 FrequencyConverter

### Step 5: 基础UI (1.5小时)
创建 PracticeScreen 和基础组件

### Step 6: ViewModel (1小时)
实现 PracticeViewModel，连接各模块

### Step 7: 测试与调优 (1小时)
测试整体功能，调整参数

**预计总时间: 7-8小时**

## 📝 注意事项

1. **音高检测算法选择**
   - 优先尝试找到 TarsosDSP 正确依赖
   - 如果不行，实现简化版算法
   - 先保证功能可用，精度可后续优化

2. **性能考虑**
   - 音频处理在后台线程
   - UI更新频率控制（避免过于频繁）
   - 内存管理（音频缓冲区复用）

3. **用户体验**
   - 清晰的权限请求说明
   - 实时反馈（不能有明显延迟）
   - 错误处理和提示

## ✅ 完成标准

迭代1完成的标准：
- [ ] 可以成功采集音频
- [ ] 可以检测出基本音高（误差在合理范围内）
- [ ] UI可以实时显示检测结果
- [ ] 用户可以开始/停止录音
- [ ] 权限处理完善
- [ ] 代码有基本注释
- [ ] 核心功能有单元测试

---

**准备开始Step 1！** 🎻
