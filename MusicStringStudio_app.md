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

说明：本节将定义练习主界面布局与交互行为

布局分区（从上到下/从左到右，响应式适配竖屏与横屏）
- 顶部导航栏（Header）
  - 内容：返回按钮、【示范，跟音，测评】tab
  - 行为：返回确认（若练习未保存，弹出提示），更多菜单含分享/保存/上传/报告入口

- 主区域（按屏幕横纵比自适应）
  - 五线谱显示区，占主视觉位置

- 底部控制栏（ControlBar）
  - 元素（从左到右或自适应排列）：节拍器开/关、播放/暂停、分段（设定一段区间，区间内循环）、更多（速度控制（0.5x-1.5x），耳返开关，导出五线谱或者简谱）。

控件与交互行为（按《一起练琴》交互细节）
- 跟随逻辑
  - 示范音频（示范轨）与学生声音同时播放/监听，播放进度以示范音频为基准（master clock）
  - ScoreFollower 根据示范进度定位当前目标音符并触发 UI 高亮（音符后的背景）
  - PitchDetector 实时输出频率与可靠性标识；PitchComparator 在目标音符的时窗内计算偏差与命中结果

- 播放控制
  - 播放按钮：单次点击切换播放/暂停；长按可进入逐帧（逐音）播放模式（按音符跳转）
  - 速度控制：支持预设值与细粒度（滑块或 +/-）；变速不改变音高（使用 ExoPlayer 变速不移位或合成器）
  - 循环控制：支持 Loop 全曲 / Loop 片段（用户在谱面上或进度条上设置 loopStart/loopEnd）；循环时自动以 loopStart 为下一次播放起点


- 耳返（监听）与混音
  - 耳返开关：开启后学生可听到自己音频（可延迟补偿）；推荐仅在耳机使用时打开

视觉反馈细节
- 当前音符后的背景高亮
- 音准色标规则：
  - 完美: 偏差 < 10 cents → 黑色
  - 偏低: <-20 cents → 绿色
  - 偏高: >20 cents → 红色
- 实时偏差数字：同时显示数值（+11 cents）
- 时间命中提示：当音符在目标时间窗口内被正确演奏，谱面上该音符加上打勾标识（已演奏样式）

错误处理与边界情况
- 静音/噪声：若输入能量低于静音阈值，显示“未检测到声音”，并暂停评分计数（但不暂停示范播放）
- 多音/泛音干扰：当PitchDetector发生不稳定检测（isReliable=false）时，UI显示“检测不可靠”并提示靠近麦克风或降低背景噪音
- 串流/解码延迟：若示范音频解码导致进度偏移超过阈值，允许用户手动调整同步偏移（ms 级），并在设置中保存偏移值

可配置项（在设置中提供）
- 音准敏感度（阈值/时窗）
- 显示模式：仅指针 / 指针+数值 / 仅数值
- 自动建议：开启后当检测到连续错误会自动建议慢速或循环练习

可访问性与本地化
- 色盲模式：用图标/纹理代替颜色作为附加标识
- 放大模式：支持 UI 元素放大和高对比主题
- 文本与语音本地化：支持多语言（简体/繁体/英文），教师注释支持语音留言

开发实现要点（工程约束，便于还原《一起练琴》体验）
- 精准时钟：以示范音频播放时间为 master clock，所有定位与高亮都以该时间计算，避免使用系统时间或 UI 帧时间作为基准
- 音频 IO：录音使用高优先级线程并尽量避免在 UI 线程中做音频相关阻塞操作；播放使用 ExoPlayer/Oboe 保证低延迟
- 缓存与回放：实时显示最近 10-20 秒的检测数据用于绘图和回放，避免频繁请求后端
- 测试：需要在真机（有线耳机/蓝牙耳机/内置扬声器）多场景下测试延迟与口感一致性


### 6.2 练习报告界面

```
┌─────────────────────────────────────┐
│          练习完成!                   │
│                                     │
│         总分: 87 分                  │
│                                     │
│  准确率: 87% (78/90)                │
│  平均偏差: ±12 cents                │
│  练习时长: 15分32秒                  │
│                                     │
│  [音准分布图表]                      │
│  完美: ██████████ 50%              │
│  偏高: ███████ 32%                 │
│  偏低: ████ 18%                    │
│                                     │
│  [查看详细报告][重新练习] [保存录音]  │
│                     [分享]          │
└─────────────────────────────────────┘

说明：练习报告界面，点击 [查看详细报告] 按钮后，跳转到详情页面。每个音符上面都有高亮显示，同时每个音符上有偏差。

```

---

## 七、开发实现步骤

### 阶段1: 基础音频功能
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

### 阶段2: 音高检测
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

### 阶段3: 曲谱解析与同步
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

### 阶段4: 音准比对与反馈
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

### 阶段5: 五线谱显示
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

### 阶段6: 完善与优化
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

---

## 十四、补充模块与功能清单（根据用户请求完善）

本节把需求细化为可实现的子模块、数据结构与接口，并补充与《一起练琴》跟音联系一致的界面/交互行为。

### 14.1 总览（新增/重点）
- 用户管理（学生/教师/管理员）与角色分离
- 调音器（Tuner）模块，包含精确校音及音色检测建议
- 节拍器（Metronome）模块，支持复杂节拍、分轨音量与可视化
- 示范演奏（机械演奏/自动跟随）：从五线谱或简谱生成示范声部并可变速/循环
- 测评（Practice Evaluation）：每首曲子结束后生成详尽报告（分音符、节拍、力度、节奏）
- 曲目库（曲单、专辑、收藏、用户上传、审核与标签）
- 曲子分类（多标签/多分类支持）
- 后台统计与管理员功能（使用统计、练习热榜、问题曲目管理）

### 14.2 优先级建议
- P0（必须）：实时音高检测、曲谱同步（跟音联系）、用户管理、练习测评报告
- P1（重要）：调音器、节拍器、示范演奏变速/循环、曲目库（浏览/收藏/上传）
- P2（可选/后期）：多声部支持、AI教学建议、在线PK、复杂后台BI报表

---

## 十五、用户管理（Student / Teacher / Admin）

目标：严格分离权限与视图，支持教师布置作业、查看学生成绩与批改，管理员管理内容与统计。

### 15.1 角色与权限（最小权限原则）
- 学生（student）
  - 权限: 进行练习、上传音频/曲谱、查看个人练习报告、收藏曲目、编辑个人资料
  - 视图: 学生练习UI、成绩历史、课程与作业列表
- 教师（teacher）
  - 权限: 查看所属学生练习报告、发放/指派练习任务、对学生演奏逐音点评（注释/打分）、上传/推荐曲目
  - 视图: 学生管理面板、批改界面、教学统计
- 管理员（admin）
  - 权限: 用户管理（禁用/升级角色）、曲谱/曲目审核、平台统计、导出数据、处理投诉
  - 视图: 后台管理控制台、内容审核、系统设置

### 15.2 用户表 / 数据模型 (示例)
```json
{
  "User": {
    "id": "uuid",
    "username": "string",
    "displayName": "string",
    "email": "string",
    "role": "student|teacher|admin",
    "createdAt": 0,
    "profile": { "avatarUrl": "", "bio": "" }
  }
}
```

### 15.3 教师-学生关系
- 支持绑定/班级概念：教师可以管理多个学生，教师查看权限仅限所属学生或所教班级
- 支持家长/监护人只读查看（可作为扩展）

---

## 十六、调音器（Tuner）模块

目标：提供一个专业的调音器工具，支持实时音高检测、弦名建议、误差可视化、历史记录与自动校准。

功能要点：
- 实时频率显示与音符映射（Hz → MIDI → 音名）
- 精度显示（cents）与误差色标
- 自动检测静音/噪音并提示
- 参考音（A4 = 440Hz 可调）与基准音播放
- 支持弦选择（G D A E）并给出目标频率提示

数据/接口示例：
- 输入: PCM buffer, sampleRate
- 输出: { frequency, midiNote, noteName, centsDeviation, isReliable }

UI建议：内嵌到练习与单独工具页面，支持耳返（监听）与麦克风增益提示

---

## 十七、节拍器（Metronome）模块

目标：提供可视化且精确的节拍器，便于学生跟随与分段训练。

核心功能：
- 支持设定 BPM、拍号（如 4/4、3/4、6/8）、小节划分
- 支持点击音/高低音标识强拍、可调节声音（点击音、口哨声、鼓声）
- 分轨功能：示范音轨和节拍器音轨可单独调节音量
- 视觉节拍（闪烁/指示器）与倒计时
- 支持节拍群组（复合拍号、切分节拍）和变速（缓慢到正常）
- 支持导出节拍设置并在练习任务中绑定

实现细节：
- 使用高精度计时器（避免UI主线程阻塞），优先考虑AudioTrack/Oboe播放节拍声

---

## 十八、示范演奏（机械演奏 / 自动跟随）

目标：从曲谱生成示范声部供学生跟随，支持变速、循环片段、单声部与多声部切换。

功能要点：
- 支持输入格式：MusicXML / 自定义 JSON（lines already defined） / 简谱
- 将曲谱映射为时间序列（Note.startTime, duration）并驱动AudioTrack/ExoPlayer播放合成/采样音色
- 机械演奏（示范）支持音量、表情（弱/强）、音色选择（小提琴采样、弦乐合成）
- 支持分声部播放：隐藏/显示声部、solo某声部
- 支持乐句循环（标记loopStart/loopEnd并在UI暴露）
- 支持MIDI输出/导入（可对接外部合成器）

实现建议：
- 对于高质量示范，使用采样音色或SoundFont/MIDI合成；低延迟优先使用Oboe或AudioTrack

---

## 十九、测评（Practice Evaluation & Reporting）

目标：在练习完成后，生成一个结构化的报告，覆盖音准、节奏、时值、力度（可选）、整体分数与分项细节，支持教师逐音注释。

报告内容（分层）：
- 总体评分：0-100
- 准确率（按音符计）与节奏准确率
- 平均偏差（cents）与偏差分布图
- 每个音符的详情（目标音、实际频率、偏差、是否命中时间窗口、教师备注）
- 节拍/时值命中统计（早/准/晚的次数与比例）
- 动态地图（可选）：显示强弱动态与参考轨对比
- 建议练习片段（基于错误密集区自动推荐Loop区间）

数据结构示例（PracticeReport）：
```json
{
  "songId": "violin_001",
  "sessionId": "uuid",
  "score": 87,
  "accuracyRate": 0.87,
  "averageDeviation": 12.3,
  "noteResults": [ { "noteId": 1, "targetPitch": "C5", "actualFreq": 520.1, "deviation": -3.2, "timingErrorMs": -12, "accuracy": "good" } ]
}
```

教师批改：
- 教师可以在每个NoteResult上添加文字或语音点评，并重新给出人工评分（覆盖或并列自动评分）

---

## 二十、曲目库（Repertoire）

目标：实现一个完整的曲目管理系统，支持单曲/专辑/收藏、用户上传与审核、音频与曲谱关联。

功能点：
- 曲目实体支持多音轨、音频示范、曲谱文件（MusicXML/JSON）、标签、难度等级、推荐语
- 专辑概念：专辑包含多首曲目并支持封面/作者信息
- 收藏/播放列表：用户可以收藏单曲或建立练习歌单
- 上传与审核：用户上传曲谱/音频→管理员或自动审核（格式/版权检测）→上架
- 搜索与筛选：按标签、难度、作曲者、类别、教师推荐等多维度筛选

数据模型（简要）:
- Song { id, title, composer, difficulty, tags[], audioUrl, scoreJsonUrl, uploaderId, visibility }
- Album { id, title, coverUrl, songIds[] }

---

## 二十一、曲子分类与标签体系

目标：支持一首曲子多分类、多标签，用户上传时选择标签并可申请更多细分分类。

示例分类维度：
- 风格: 流行, 古典, 中国风, 民谣
- 场景: 练习曲, 表演曲, 考级曲, 教学示范
- 难度: Beginner, Intermediate, Advanced
- 其他: 适合小提琴技巧（双弦、拨弦、颤音等）

实现细节：
- 标签采用可扩展的多对多关系, 支持管理员审核新增标签

---

## 二十二、后台管理与统计（Admin / BI）

目标：提供管理员面板以管理用户、曲目、审核任务与平台统计。

关键功能：
- 用户管理: 查询/禁用/角色变更/导出
- 内容审核: 待审核曲谱/音频列表、批量通过/拒绝、原因记录
- 练习统计: 日活/周活、练习时长分布、热门曲目、错误聚集区域
- 报表导出: CSV/Excel、按班级/教师/曲目导出练习成绩
- 系统监控: 音频队列长度、转码任务状态、存储使用量

实现注意：
- 数据量大时需使用异步任务（消息队列）、按天表汇总以优化查询

---

## 二十三、后端 API 概览（RESTful / GraphQL 可选）

以下为最小可行API集合（示例路径）：

- Auth
  - POST /api/v1/auth/register
  - POST /api/v1/auth/login
  - POST /api/v1/auth/refresh

- Users
  - GET /api/v1/users/{id} (role-based)
  - PUT /api/v1/users/{id}
  - GET /api/v1/teachers/{teacherId}/students

- Songs / Library
  - GET /api/v1/songs?tag=&difficulty=&q=
  - POST /api/v1/songs (uploader)
  - GET /api/v1/songs/{id}
  - POST /api/v1/songs/{id}/upload-score (MusicXML/JSON)

- Practice Sessions
  - POST /api/v1/sessions (开始练习, 返回sessionId)
  - POST /api/v1/sessions/{id}/events (上传分析事件/音符结果)
  - POST /api/v1/sessions/{id}/complete (触发最终评估, 返回报告)
  - GET /api/v1/sessions/{id}/report

- Reports
  - GET /api/v1/reports/{sessionId}
  - GET /api/v1/users/{id}/reports

- Admin / Stats
  - GET /api/v1/admin/stats/overview
  - GET /api/v1/admin/audit/pending

权限要点：所有写操作必须带JWT并按角色校验，教师/管理员可访问跨用户报告或进行管理操作。

---

## 二十四、界面与交互设计（参考《一起练琴》的跟音联系）

目标：在交互逻辑与视觉反馈上尽量与《一起练琴》保持一致，降低用户学习成本。核心要点：实时同步、明确的误差提示、教师标注与片段循环

核心交互元素：
- 分屏/层叠布局：顶部或左侧显示五线谱（高亮当前音符），底部显示控制栏（播放/速度/循环/节拍器），右侧显示实时音准指示器与评分（或可折叠）
- 实时高亮与平滑滚动：当前目标音符高亮并显示一个短暂的动画; 当学生偏离目标时，相应音符以颜色/标记提示（与文档中色标一致）
- 时间轴精确控制：支持拖动进度条、设定loop区间并即时生效（loop区间在曲谱上有可视化标记）
- 试听与耳返：学生可开启耳返模式，通过耳机听到自己的声音与示范音的混合（独立调节音量分比）
- 教师批改界面：播放学生录音的同时在五线谱上逐音显示教师注释/评分，支持跳转到错误片段并快速循环
- 对标《一起练琴》的要点：
  - 强调“跟随示范音频实时对比”的交互：示范音频与学生声音同时可听、进度严格对齐
  - 策略：当检测到学生连续多次错过某一区间，弹出提示并建议切换到慢速或循环练习该片段

视觉和可访问性：
- 保持色彩对比度，避免仅用颜色传达重要信息（同时使用符号/提示文本）
- 支持大字号和高对比主题以适配教学场景

---

## 二十五、安全、隐私与版权

关键点：
- 用户数据保护：遵循最小权限与数据加密传输（HTTPS/TLS）与存储（敏感字段加密）
- 隐私：练习录音为个人数据，默认私有；分享/上传需明确授权
- 曲谱/音频版权：用户上传需签署声明并进入审核流程；对受版权保护资源提供上架/下架记录

---

## 二十六、测试用例与验收标准

功能测试举例：
- 实时音高检测：在静音环境下，针对标准音（A4）检测偏差 < 10 cents
- 跟音同步：示范音频与目标音符高亮在播放延迟 < 100ms 的体验范围
- 权限验证：教师只能访问所属学生的报告

性能测试：
- 延迟：端到端音频采集→分析→显示延迟 < 100ms
- CPU / 内存：60秒高负载练习时 CPU < 40%（中端设备）

自动化测试建议：
- 单元测试：PitchDetector 算法、频率到音符映射、偏差计算
- 集成测试：AudioRecord 模拟输入流与 PitchDetector 的端到端验证
- E2E 测试：使用模拟音频文件验证从播放->采集->评分的完整链路

---

## 二十七、开发路线图（按迭代）

迭代 0（准备）: 环境搭建、库选型、创建五线谱（可以基于：https://github.com/musescore/MuseScore?tab=readme-ov-file 实现五线谱的显示和创建）

迭代 1（MVP）: 音频采集 + 实时音高检测 + 简单曲谱同步 + 基础UI 

迭代 2（增强）: 调音器、节拍器、示范演奏（变速/循环）+ 基础报告

迭代 3（用户体系）: 用户管理（学生/教师/管理员）、曲目库、上传审核 

迭代 4（教师功能）: 批改界面、教师点评、班级管理 

迭代 5（后台与优化）: 管理后台、统计报表、性能优化与兼容性

---

## 二十八、附录：示例 JSON Schema（简要）

Song（简化）:
```json
{
  "id": "violin_001",
  "title": "小星星",
  "composer": "传统",
  "difficulty": "Beginner",
  "tags": ["练习曲","儿童"],
  "audioUrl": "https://...",
  "score": { "format": "musicxml", "url": "https://..." }
}
```

PracticeSession（简化）:
```json
{
  "sessionId": "uuid",
  "userId": "uuid",
  "songId": "violin_001",
  "startedAt": 0,
  "completedAt": 0,
  "events": [ { "time": 12345, "type": "pitch", "frequency": 440.1 } ],
  "reportUrl": "/api/v1/reports/...."
}
```