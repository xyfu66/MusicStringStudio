# 示范音频准备指南

## 📋 概述

为了使跟音练习功能正常工作，需要为每首 MusicXML 曲谱准备对应的示范音频文件。

## 📁 音频文件位置

音频文件应放置在以下位置：
```
AndroidApp/app/src/main/assets/audio/
```

## 🎵 当前需要的音频文件

### 1. little_star.mp3
- **曲目**: 小星星
- **对应曲谱**: `res/raw/little_star.musicxml`
- **要求**:
  - 速度: 120 BPM
  - 拍号: 4/4
  - 总时长: 约 8 秒（4小节）
  - 格式: MP3
  - 音质: 至少 128 kbps

### 2. simple_scale.mp3
- **曲目**: C大调音阶
- **对应曲谱**: `res/raw/simple_scale.musicxml`
- **要求**:
  - 速度: 100 BPM
  - 拍号: 4/4
  - 总时长: 约 9.6 秒（4小节）
  - 格式: MP3
  - 音质: 至少 128 kbps

## 🛠️ 准备方法

### 方法1: 使用 MuseScore 导出音频 (推荐)

1. **安装 MuseScore**
   - 下载: https://musescore.org/
   - 免费开源音乐记谱软件

2. **导出音频**
   ```
   1. 用 MuseScore 打开 MusicXML 文件
   2. 菜单: File → Export
   3. 选择格式: MP3
   4. 设置音质: 至少 128 kbps
   5. 保存到 assets/audio/ 目录
   ```

3. **验证音频**
   - 播放音频确认音高正确
   - 检查速度是否匹配曲谱标记
   - 确认总时长合理

### 方法2: 录制真实演奏

1. **录音设备**
   - 使用专业录音设备
   - 或使用手机高质量录音 app

2. **录制要求**
   - 安静环境，无背景噪音
   - 使用节拍器保持准确节奏
   - 音高准确（可用调音器辅助）

3. **后期处理**
   - 裁剪音频起止点
   - 调整音量（标准化）
   - 转换为 MP3 格式

### 方法3: 使用音频合成软件

可使用以下软件从 MIDI 合成音频：
- Synthesia
- FL Studio
- GarageBand (Mac)

## 📐 音频规格要求

### 必须满足
- ✅ 格式: MP3
- ✅ 采样率: 44100 Hz
- ✅ 比特率: ≥ 128 kbps
- ✅ 声道: 立体声或单声道
- ✅ 音量: 标准化处理

### 推荐
- 🎯 比特率: 192 kbps
- 🎯 采样率: 44100 Hz
- 🎯 格式: MP3 (VBR)

## 🔗 MusicXML 与音频关联

在 MusicXML 文件中，可以指定音频 URL：

```xml
<!-- 在 Song 模型中 -->
<audioUrl>file:///android_asset/audio/little_star.mp3</audioUrl>
```

或在代码中设置：

```kotlin
val song = musicXMLParser.parseFromResource(context, R.raw.little_star)
val audioUrl = "file:///android_asset/audio/little_star.mp3"
audioPlaybackManager.loadAudio(audioUrl)
```

## 📝 文件命名规范

建议使用以下命名格式：
```
{song_id}.mp3
```

示例：
- `little_star.mp3`
- `simple_scale.mp3`
- `violin_etude_01.mp3`

## ✅ 验证清单

音频准备完成后，检查以下项目：

- [ ] 音频文件放置在 `assets/audio/` 目录
- [ ] 文件名与曲谱 ID 匹配
- [ ] 音频格式为 MP3
- [ ] 播放音频验证音高正确
- [ ] 速度与曲谱标记匹配
- [ ] 音量标准化，不会过大或过小
- [ ] 没有明显的背景噪音
- [ ] 总时长与曲谱匹配

## 🚀 快速开始

### 临时测试方案

如果暂时没有真实音频，可以：

1. **使用 MuseScore 快速生成**
   ```bash
   # 下载 MuseScore
   # 打开 little_star.musicxml
   # File → Export → MP3
   ```

2. **从网上下载**
   - 搜索 "Twinkle Twinkle Little Star MIDI"
   - 转换 MIDI 为 MP3
   - 确保速度和音高正确

3. **使用在线工具**
   - https://onlinesequencer.net/
   - https://musiclab.chromeexperiments.com/

## 📚 资源链接

- **MuseScore 官网**: https://musescore.org/
- **MIDI 转 MP3 工具**: https://www.zamzar.com/convert/midi-to-mp3/
- **音频编辑器**: https://www.audacityteam.org/

---

**注意**: 音频文件是项目运行的必要条件，必须准备好才能测试跟音练习功能！
