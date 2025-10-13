# 小提琴智能陪练APP 《MusicStringStudio》 - 详细开发方案

## 项目概述
**核心功能**: 小提琴实时跟音练习与音准评测  
**平台**: Android  
**语言**: Java  
**技术难点**: 音频实时分析、音高检测、节拍同步

---

## 一、跟音练习核心功能

### 1.1 功能概述
用户跟随示范音频演奏小提琴,系统实时采集用户演奏音频,通过音高识别技术分析音准,并给出即时反馈和评分。

### 1.2 核心特性
- **实时音高检测**: 识别用户演奏的音高(A4=440Hz标准)
- **音准评估**: 计算与目标音高的偏差
- **视觉反馈**: 曲谱高亮显示当前音符,颜色标识音准状态
- **节奏跟随**: 根据乐曲进度自动推进
- **练习报告**: 生成音准准确率、节奏准确性等数据

---

## 二、技术架构设计

### 2.1 音频处理流程图
```
[示范音频播放] ────→ [音频输出]
                          ↓
[用户演奏] ────→ [麦克风采集] ────→ [音频缓冲区]
                                      ↓
                              [实时音频分析]
                                      ↓
                         ┌────────────┴────────────┐
                         ↓                         ↓
                    [音高检测]                [音量检测]
                         ↓                         ↓
                    [音准比对]                [静音判断]
                         ↓
                    [视觉反馈]
                         ↓
                    [评分统计]
```

### 2.2 核心技术选型

#### 音高检测算法
1. **YIN算法** (推荐)
   - 高精度音高检测
   - 适合单音乐器
   - 低延迟(< 50ms)

2. **FFT快速傅里叶变换**
   - 频域分析
   - 识别基频

3. **自相关算法**
   - 备选方案

#### Android音频库
- **AudioRecord**: 实时音频采集
- **AudioTrack**: 音频播放控制
- **TarsosDSP**: 音频处理库(包含YIN算法)
- **Oboe**: 低延迟音频(可选,适合高端机型)

---

## 三、详细模块设计

### 3.1 音频采集模块

#### AudioCaptureManager.java
```java
功能:
- 初始化AudioRecord
- 设置采样率: 44100Hz (CD音质)
- 设置缓冲区大小
- 实时读取音频数据
- 线程管理

关键参数:
- SAMPLE_RATE = 44100
- CHANNEL = AudioFormat.CHANNEL_IN_MONO (单声道)
- ENCODING = AudioFormat.ENCODING_PCM_16BIT
- BUFFER_SIZE = 4096 samples
```

#### 实现要点
- 使用独立线程进行音频采集
- 循环读取缓冲区数据
- 传递数据到分析模块
- 处理录音权限

### 3.2 音频播放模块

#### AudioPlaybackManager.java
```java
功能:
- 播放示范音频
- 支持暂停/继续/停止
- 获取当前播放进度(毫秒级)
- 速度调节(0.5x - 1.5x)
- 循环播放指定段落

使用ExoPlayer优势:
- 支持多种音频格式
- 精确的进度控制
- 低延迟
- 支持变速播放
```

### 3.3 音高检测模块

#### PitchDetector.java
```java
功能:
- 集成YIN算法
- 输入: 音频样本数组
- 输出: 频率值(Hz)
- 实时处理 (每50-100ms分析一次)

音高转换:
- 频率 → 音符名称 (A, B, C, D, E, F, G)
- 频率 → 音分值 (Cent, 音程单位)
- 计算与标准音的偏差

示例:
440Hz = A4 (标准音)
880Hz = A5 (高八度)
```

#### 音准判定标准
```
完美: 偏差 < 10 cents (绿色)
良好: 偏差 10-25 cents (黄色)
偏差: 偏差 25-50 cents (橙色)
较差: 偏差 > 50 cents (红色)

1个半音 = 100 cents
```

### 3.4 曲谱同步模块

#### ScoreFollower.java
```java
功能:
- 加载曲谱数据(MusicXML/自定义格式)
- 根据播放进度定位当前音符
- 音符高亮显示
- 自动翻页
- 标注用户演奏结果

数据结构:
class Note {
    String pitch;        // 音高 (如 "A4")
    float frequency;     // 频率 (如 440.0)
    long startTime;      // 起始时间(ms)
    long duration;       // 持续时间(ms)
    int position;        // 在五线谱上的位置
}
```

### 3.5 音准比对模块

#### PitchComparator.java
```java
功能:
- 获取当前应演奏的目标音符
- 获取用户实际演奏的音高
- 计算偏差(cents)
- 判定准确性等级
- 记录每个音符的演奏结果

比对逻辑:
1. 从ScoreFollower获取当前目标音符
2. 从PitchDetector获取用户演奏频率
3. 计算音分偏差
4. 更新UI反馈
5. 记录到统计数据
```

### 3.6 视觉反馈模块

#### PracticeView.java (自定义View)
```java
显示内容:
1. 五线谱曲谱
   - 当前音符高亮
   - 已演奏音符标注(颜色标识准确度)
   
2. 实时音准指示器
   - 类似调音器显示
   - 指针指示偏高/偏低
   - 刻度显示偏差cents
   
3. 音高波形图(可选)
   - 显示用户演奏的音高曲线
   - 对比目标音高曲线

4. 状态指示
   - 演奏中/暂停/停止
   - 当前小节号
   - 已练习时长
```

---

## 四、数据结构设计

### 4.1 曲谱数据格式(JSON)
```json
{
  "songId": "violin_001",
  "title": "小星星",
  "composer": "传统",
  "tempo": 120,
  "timeSignature": "4/4",
  "key": "C",
  "measures": [
    {
      "measureNumber": 1,
      "notes": [
        {
          "pitch": "C5",
          "frequency": 523.25,
          "duration": 500,
          "startTime": 0,
          "notation": "quarter"
        },
        {
          "pitch": "C5",
          "frequency": 523.25,
          "duration": 500,
          "startTime": 500,
          "notation": "quarter"
        }
      ]
    }
  ],
  "audioUrl": "https://example.com/demo.mp3"
}
```

### 4.2 练习记录数据
```java
class PracticeResult {
    String songId;
    long practiceDate;
    int totalNotes;              // 总音符数
    int accurateNotes;           // 准确音符数
    float accuracyRate;          // 准确率
    float averageDeviation;      // 平均偏差(cents)
    List<NoteResult> noteResults; // 每个音符的结果
    int score;                   // 总分(0-100)
}

class NoteResult {
    String targetPitch;
    float targetFrequency;
    float actualFrequency;
    float deviation;             // 偏差(cents)
    String accuracy;             // "perfect"/"good"/"fair"/"poor"
}
```

---

## 五、核心算法实现

### 5.1 YIN音高检测算法流程

```
输入: 音频样本buffer (2048 samples)

步骤1: 差分函数计算
for (tau = 0; tau < bufferSize/2; tau++)
    difference[tau] = sum((buffer[j] - buffer[j+tau])^2)

步骤2: 累积平均归一化
normalizedDifference[0] = 1
for (tau = 1; tau < bufferSize/2; tau++)
    normalizedDifference[tau] = difference[tau] / 
        ((1/tau) * sum(difference[0...tau]))

步骤3: 阈值检测
threshold = 0.1
找到第一个满足 normalizedDifference[tau] < threshold 的tau

步骤4: 抛物线插值
精确估计基频周期

步骤5: 频率计算
frequency = SAMPLE_RATE / tau

输出: 频率(Hz)
```

### 5.2 频率到音符转换

```java
// 计算音符号(A4 = 69)
int midiNote = (int) Math.round(69 + 12 * Math.log(frequency / 440.0) / Math.log(2));

// 计算偏差(cents)
float targetFrequency = 440 * Math.pow(2, (midiNote - 69) / 12.0);
float cents = 1200 * Math.log(frequency / targetFrequency) / Math.log(2);

// 音符名称映射
String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
String noteName = noteNames[midiNote % 12] + (midiNote / 12 - 1);
```

---

## 六、界面设计

### 6.1 练习主界面布局

```
┌─────────────────────────────────────┐
│  [返回] 小星星 - 小提琴   [设置]    │
├─────────────────────────────────────┤
│                                     │
│        [五线谱显示区域]              │
│     (高亮当前音符,颜色标注准确度)     │
│                                     │
├─────────────────────────────────────┤
│                                     │
│       [实时音准指示器]               │
│         ◄─────●─────►               │
│       -50   0   +50 (cents)         │
│                                     │
├─────────────────────────────────────┤
│  当前音符: A4 (440Hz)                │
│  你演奏的: A4 (445Hz) +11cents      │
│  评价: 良好 ⭐⭐⭐⭐                  │
├─────────────────────────────────────┤
│  [◀◀] [播放/暂停] [▶▶]   速度: 1.0x │
│  ────●────────────────  02:35/04:12 │
├─────────────────────────────────────┤
│  准确率: 87%  已练习: 15分钟          │
└─────────────────────────────────────┘
```

### 6.2 练习设置界面

```
- 节拍器开关
- 节拍器音量
- 示范音频音量
- 监听音量(耳返)
- 音准敏感度调节
- 显示选项(波形图/指示器)
```

### 6.3 练习报告界面

```
┌─────────────────────────────────────┐
│          练习完成!                   │
│                                     │
│         总分: 87分                   │
│         ⭐⭐⭐⭐☆                     │
│                                     │
│  准确率: 87% (78/90)                │
│  平均偏差: ±12 cents                │
│  练习时长: 15分32秒                  │
│                                     │
│  [音准分布图表]                      │
│  完美: ██████████ 45%              │
│  良好: ███████ 32%                 │
│  偏差: ████ 18%                    │
│  较差: ██ 5%                       │
│                                     │
│  [重新练习] [保存录音] [分享]        │
└─────────────────────────────────────┘
```

---

## 七、开发实现步骤

### 阶段1: 基础音频功能(1周)
```
任务:
□ 实现AudioRecord音频采集
□ 实现ExoPlayer音频播放
□ 测试采集与播放同步
□ 处理音频权限

AI提示词示例:
"创建Android AudioRecord工具类,实现44100Hz采样率的实时音频采集,
使用独立线程,提供开始、停止、数据回调接口"
```

### 阶段2: 音高检测(1周)
```
任务:
□ 集成TarsosDSP库
□ 实现YIN算法封装
□ 频率到音符转换
□ 音准偏差计算
□ 实时检测测试

AI提示词示例:
"使用TarsosDSP库的YIN算法,创建实时音高检测器,
输入音频样本,输出频率和音符名称,处理静音情况"
```

### 阶段3: 曲谱解析与同步(1周)
```
任务:
□ 设计曲谱JSON格式
□ 实现曲谱解析器
□ 播放进度与曲谱同步
□ 当前音符定位算法

AI提示词示例:
"创建曲谱同步类,根据音频播放进度(毫秒),
定位JSON格式曲谱中的当前音符,返回音符信息"
```

### 阶段4: 音准比对与反馈(1周)
```
任务:
□ 实现音准比对逻辑
□ 设计视觉反馈UI
□ 实时更新显示
□ 结果记录统计

AI提示词示例:
"创建自定义View显示实时音准指示器,
类似调音器,指针指示偏差,颜色标识准确度"
```

### 阶段5: 五线谱显示(1周)
```
任务:
□ 实现五线谱绘制
□ 音符高亮动画
□ 颜色标注反馈
□ 自动滚动

AI提示词示例:
"使用Canvas绘制小提琴五线谱(高音谱号),
根据Note对象绘制音符,支持高亮和颜色标注"
```

### 阶段6: 完善与优化(1周)
```
任务:
□ 练习报告生成
□ 数据持久化
□ 性能优化(降低延迟)
□ UI/UX优化
□ 异常处理
```

---

## 八、技术难点与解决方案

### 8.1 音频延迟问题
**问题**: 采集→处理→显示的延迟影响体验

**解决方案**:
- 使用低延迟AudioRecord配置
- 减小缓冲区大小(权衡采样精度)
- 高优先级线程处理
- 考虑使用Oboe库(Android 8.0+)
- 延迟补偿算法

### 8.2 音高检测准确性
**问题**: 背景噪音、泛音干扰

**解决方案**:
- 增加静音检测(音量阈值)
- YIN算法阈值调优
- 低通滤波器去除高频噪音
- 平滑算法(移动平均)
- 提示用户保持安静环境

### 8.3 曲谱同步精度
**问题**: 播放进度与曲谱不同步

**解决方案**:
- 精确获取ExoPlayer当前位置
- 时间戳精确到毫秒
- 提前加载下一个音符
- 考虑音频解码延迟
- 用户可手动微调同步偏移

### 8.4 性能优化
**问题**: CPU/内存/电量消耗

**解决方案**:
- 音频处理异步化
- 对象池复用音频缓冲区
- 降低UI刷新频率(30fps足够)
- 后台节能模式
- 音频数据压缩存储

---

## 九、依赖库清单

### build.gradle配置
```gradle
dependencies {
    // 音频处理
    implementation 'be.tarsos.dsp:core:2.5'
    implementation 'be.tarsos.dsp:jvm:2.5'
    
    // 音视频播放
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
    
    // UI组件
    implementation 'com.google.android.material:material:1.11.0'
    
    // 数据库
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    
    // 网络
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // 图表
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // 权限
    implementation 'com.guolindev.permissionx:permissionx:1.7.1'
    
    // 低延迟音频(可选)
    implementation 'com.google.oboe:oboe:1.8.0'
}
```

---

## 十、测试清单

### 功能测试
- [ ] 音频采集正常
- [ ] 音高检测准确(使用调音器对照)
- [ ] 曲谱同步精确
- [ ] 音准反馈及时
- [ ] 练习报告数据正确
- [ ] 暂停/继续功能正常

### 性能测试
- [ ] CPU占用率 < 30%
- [ ] 内存占用 < 200MB
- [ ] 延迟 < 100ms
- [ ] 1小时使用电量 < 20%

### 兼容性测试
- [ ] Android 5.0+ 各版本
- [ ] 不同屏幕尺寸
- [ ] 主流机型(小米、华为、OPPO等)
- [ ] 不同麦克风硬件

---

## 十一、示例代码结构

```
app/src/main/java/com/example/violinpractice/
│
├── audio/
│   ├── AudioCaptureManager.java      // 音频采集
│   ├── AudioPlaybackManager.java     // 音频播放
│   ├── PitchDetector.java            // 音高检测
│   └── AudioUtils.java               // 工具类
│
├── score/
│   ├── ScoreParser.java              // 曲谱解析
│   ├── ScoreFollower.java            // 曲谱同步
│   ├── Note.java                     // 音符实体
│   └── Measure.java                  // 小节实体
│
├── practice/
│   ├── PracticeActivity.java         // 主界面
│   ├── PitchComparator.java          // 音准比对
│   ├── PracticeViewModel.java        // 业务逻辑
│   └── PracticeResult.java           // 结果数据
│
├── ui/
│   ├── PracticeView.java             // 练习视图
│   ├── StaffView.java                // 五线谱视图
│   ├── TunerView.java                // 调音器视图
│   └── ReportView.java               // 报告视图
│
└── utils/
    ├── FrequencyUtils.java           // 频率转换
    ├── MusicTheory.java              // 音乐理论
    └── AudioPermission.java          // 权限处理
```

---

## 十二、后续扩展功能

### 已完成基础版后可添加:
1. **多声部练习**: 支持重奏
2. **AI智能陪练**: 根据错误提供建议
3. **录音回放分析**: 查看历史练习
4. **自定义曲谱**: 用户上传曲谱
5. **在线PK**: 与他人比拼准确率
6. **练习打卡**: 每日目标与提醒
7. **视频教程**: 演奏技巧讲解
8. **音色识别**: 识别演奏技法(颤音、滑音等)

---

## 十三、开发资源

### 参考资料
- TarsosDSP官方文档: https://github.com/JorenSix/TarsosDSP
- YIN算法论文: "YIN, a fundamental frequency estimator for speech and music"
- Android Audio开发指南: https://developer.android.com/guide/topics/media/
- ExoPlayer文档: https://exoplayer.dev/

### 调试工具
- Android Studio Profiler (监控CPU/内存)
- Audacity (音频波形分析)
- 吉他调音器APP (对照测试音高检测)

### AI开发提示
每个模块都可以单独请AI生成代码,按照以下模式:
1. 明确输入输出
2. 指定技术栈
3. 说明关键逻辑
4. 要求代码注释
5. 包含异常处理