# MusicXML 集成设计文档

## 📋 概述

MusicXML 是音乐记谱的国际标准格式，被广泛的音乐软件支持（如 MuseScore, Finale, Sibelius 等）。采用 MusicXML 作为曲谱基础格式有以下优势：

### ✅ 优势
1. **标准化** - 国际通用格式，兼容性强
2. **丰富性** - 支持完整的音乐记谱信息
3. **可编辑** - 用户可用专业软件编辑曲谱
4. **资源丰富** - 网上有大量现成的 MusicXML 曲谱
5. **专业性** - 支持复杂的音乐记谱需求

## 🏗️ 架构设计

### 数据流
```
MusicXML文件 → MusicXMLParser → 内部数据模型(Song) → 业务逻辑
```

### 核心组件

#### 1. MusicXMLParser
负责解析 MusicXML 文件并转换为内部数据模型

#### 2. 内部数据模型
保持现有的 Note, Measure, Song 等模型不变，作为业务逻辑层使用

#### 3. 解析策略
- 支持 MusicXML 3.1+ 格式
- 提取必要信息：音高、时值、速度、拍号等
- 忽略不需要的信息：排版、字体等

## 📄 MusicXML 基础结构

### 简化的 MusicXML 示例
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.1 Partwise//EN" 
  "http://www.musicxml.org/dtds/partwise.dtd">
<score-partwise version="3.1">
  <work>
    <work-title>小星星</work-title>
  </work>
  <identification>
    <creator type="composer">传统</creator>
  </identification>
  
  <part-list>
    <score-part id="P1">
      <part-name>小提琴</part-name>
    </score-part>
  </part-list>
  
  <part id="P1">
    <!-- 第1小节 -->
    <measure number="1">
      <attributes>
        <divisions>1</divisions>
        <key>
          <fifths>0</fifths>  <!-- C大调 -->
        </key>
        <time>
          <beats>4</beats>
          <beat-type>4</beat-type>
        </time>
        <clef>
          <sign>G</sign>
          <line>2</line>
        </clef>
      </attributes>
      <direction placement="above">
        <direction-type>
          <metronome>
            <beat-unit>quarter</beat-unit>
            <per-minute>120</per-minute>
          </metronome>
        </direction-type>
      </direction>
      
      <!-- 第1个音符: C4 -->
      <note>
        <pitch>
          <step>C</step>
          <octave>4</octave>
        </pitch>
        <duration>1</duration>
        <type>quarter</type>
      </note>
      
      <!-- 第2个音符: C4 -->
      <note>
        <pitch>
          <step>C</step>
          <octave>4</octave>
        </pitch>
        <duration>1</duration>
        <type>quarter</type>
      </note>
    </measure>
  </part>
</score-partwise>
```

## 🔧 实现计划

### Phase 1: 解析器核心
- [ ] 创建 MusicXMLParser.kt
- [ ] 实现基本XML解析
- [ ] 提取关键信息（音高、时值、速度）
- [ ] 转换为内部数据模型

### Phase 2: 示例曲谱
- [ ] 创建 little_star.musicxml
- [ ] 创建 simple_scale.musicxml
- [ ] 使用 MuseScore 验证

### Phase 3: 高级功能
- [ ] 支持多声部
- [ ] 支持表情记号
- [ ] 支持力度标记
- [ ] 时间计算优化

## 📚 需要解析的关键元素

### 必须支持
- `<work-title>` - 曲名
- `<creator>` - 作曲者
- `<key>` - 调性
- `<time>` - 拍号
- `<metronome>` - 速度
- `<note>` - 音符
  - `<pitch>` - 音高
  - `<duration>` - 时值
  - `<type>` - 音符类型

### 可选支持
- `<dynamics>` - 力度
- `<articulations>` - 奏法
- `<tempo>` - 速度标记
- `<lyric>` - 歌词

## 🛠️ 技术选型

### XML 解析库
推荐使用 Android 内置的 XmlPullParser：
- 轻量级
- 高性能
- 无需额外依赖

备选方案：
- DOM4J（需要添加依赖）
- SimpleXML（需要添加依赖）

## 📐 时间计算

### Duration 单位
MusicXML 中的 `duration` 是相对单位，需要通过 `divisions` 转换：

```kotlin
实际时长(ms) = (duration / divisions) * (60000 / BPM)
```

例如：
- divisions = 1
- duration = 1（四分音符）
- BPM = 120
- 实际时长 = (1/1) * (60000/120) = 500ms

### 累积时间
每个音符的 startTime 需要累加前面所有音符的时长。

## 🎯 兼容性策略

### JSON 格式过渡
1. 保留 JSON 解析器作为备用
2. 优先使用 MusicXML
3. 提供格式转换工具（可选）

### 向后兼容
现有的内部数据模型保持不变，只改变输入源。

## 📝 开发任务

### 立即执行
1. 创建 MusicXMLParser.kt
2. 实现基础解析逻辑
3. 创建示例 MusicXML 文件
4. 单元测试

### 后续优化
1. 性能优化
2. 错误处理
3. 复杂记谱支持
4. 用户自定义曲谱导入

---

**下一步**: 实现 MusicXMLParser.kt
