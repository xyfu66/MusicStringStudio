# 音频生成使用指南

## 📋 概述

本项目提供自动化脚本，使用 MuseScore 命令行工具从 MusicXML 文件批量生成 MP3 音频。

## 🎯 前提条件

### 1. 安装 MuseScore 4

**Windows**
- 下载: https://musescore.org/
- 默认安装路径: `C:\Program Files\MuseScore 4\bin\MuseScore4.exe`

**Linux**
```bash
# Ubuntu/Debian
sudo add-apt-repository ppa:musescore/musescore4
sudo apt update
sudo apt install musescore4

# Fedora
sudo dnf install musescore
```

**macOS**
```bash
# 使用 Homebrew
brew install --cask musescore

# 或下载 DMG 安装包
# https://musescore.org/
```

## 🚀 使用方法

### Windows (PowerShell)

**基本使用**
```powershell
cd AndroidApp
.\generate_audio.ps1
```

**指定 MuseScore 路径**
```powershell
.\generate_audio.ps1 -MuseScorePath "D:\MuseScore\MuseScore4.exe"
```

**指定音频比特率**
```powershell
.\generate_audio.ps1 -BitRate 256
```

### Linux/macOS (Bash)

**基本使用**
```bash
cd AndroidApp
chmod +x generate_audio.sh
./generate_audio.sh
```

**指定 MuseScore 路径**
```bash
./generate_audio.sh /usr/local/bin/musescore4
```

**使用环境变量**
```bash
export MUSESCORE_PATH="/usr/local/bin/musescore4"
./generate_audio.sh
```

## 📁 文件结构

脚本会自动处理以下目录：

```
AndroidApp/
├── app/src/main/res/raw/       # MusicXML 输入文件
│   ├── little_star.musicxml
│   └── simple_scale.musicxml
└── app/src/main/assets/audio/  # MP3 输出文件（自动创建）
    ├── little_star.mp3
    └── simple_scale.mp3
```

## ✅ 验证音频文件

生成后验证：

1. **检查文件存在**
   ```bash
   ls app/src/main/assets/audio/
   ```

2. **播放验证**
   - 使用媒体播放器打开 MP3 文件
   - 验证音高和节奏正确
   - 确认音质满意

3. **Android 项目中使用**
   ```kotlin
   val audioUrl = "file:///android_asset/audio/little_star.mp3"
   audioPlaybackManager.loadAudio(audioUrl)
   ```

## 🔧 故障排除

### 问题1: 找不到 MuseScore

**症状**: 脚本提示 "找不到 MuseScore!"

**解决方案**:
1. 确认 MuseScore 已安装
2. 找到 MuseScore 可执行文件路径
3. 使用 `-MuseScorePath` 参数指定路径

### 问题2: 转换失败

**症状**: 显示 "✗ 失败"

**可能原因**:
- MusicXML 文件格式错误
- MuseScore 版本不兼容
- 文件权限问题

**解决方案**:
1. 用 MuseScore 图形界面打开 MusicXML 验证
2. 检查文件是否损坏
3. 确保有写入 `assets/audio/` 的权限

### 问题3: 音频质量不佳

**解决方案**:
```powershell
# 提高比特率
.\generate_audio.ps1 -BitRate 320
```

### 问题4: PowerShell 执行策略错误

**症状**: "无法加载，因为在此系统上禁止运行脚本"

**解决方案**:
```powershell
# 临时允许执行脚本（当前会话）
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# 然后运行脚本
.\generate_audio.ps1
```

## 📊 输出示例

成功运行后，你会看到类似输出：

```
=========================================
  MusicXML 转 MP3 自动化工具
=========================================

MuseScore 路径: C:\Program Files\MuseScore 4\bin\MuseScore4.exe

找到 2 个 MusicXML 文件

转换: little_star.musicxml
  -> little_star.mp3
  ✓ 成功 (45.23 KB)

转换: simple_scale.musicxml
  -> simple_scale.mp3
  ✓ 成功 (52.18 KB)

=========================================
  转换完成
=========================================
成功: 2
失败: 0
总计: 2

输出目录: app/src/main/assets/audio

音频文件已生成！
可以在 Android 项目中使用这些文件了。
```

## 🎵 添加新曲谱

1. **创建 MusicXML 文件**
   - 使用 MuseScore 图形界面创建
   - 或从网上下载
   - 放置到 `app/src/main/res/raw/`

2. **运行生成脚本**
   ```bash
   ./generate_audio.sh
   ```

3. **在代码中使用**
   ```kotlin
   val song = MusicXMLParser().parseFromResource(
       context, 
       R.raw.your_song
   )
   val audioUrl = "file:///android_asset/audio/your_song.mp3"
   ```

## 🔄 CI/CD 集成

可以将脚本集成到构建流程：

### GitHub Actions 示例
```yaml
name: Generate Audio

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Install MuseScore
        run: |
          sudo add-apt-repository ppa:musescore/musescore4
          sudo apt update
          sudo apt install musescore4
      
      - name: Generate Audio
        run: |
          cd AndroidApp
          chmod +x generate_audio.sh
          ./generate_audio.sh
      
      - name: Commit Audio Files
        run: |
          git config --global user.name 'GitHub Actions'
          git config --global user.email 'actions@github.com'
          git add app/src/main/assets/audio/*.mp3
          git commit -m "Auto-generate audio files" || echo "No changes"
          git push
```

## 📚 参考资料

- **MuseScore 手册**: https://musescore.org/en/handbook/4
- **命令行选项**: https://musescore.org/en/handbook/4/command-line-options
- **MusicXML 规范**: https://www.musicxml.com/

## 💡 最佳实践

1. **版本控制**
   - 将 MusicXML 文件纳入 Git
   - 音频文件可选择性加入（因为可重新生成）

2. **音质设置**
   - 开发: 128 kbps（快速）
   - 生产: 192-320 kbps（高质量）

3. **自动化**
   - 在 Gradle 构建前自动运行脚本
   - 或使用 Git hook 在提交时自动生成

## ❓ 常见问题

**Q: 必须安装 MuseScore 吗？**  
A: 是的，脚本需要 MuseScore 命令行工具。但只需要在开发机器上安装，最终用户不需要。

**Q: 可以生成其他格式吗？**  
A: 可以，修改脚本中的输出格式（.mp3 → .wav, .ogg 等）

**Q: 音频文件很大，如何优化？**  
A: 降低比特率或使用 Ogg Vorbis 格式（更小的文件）

**Q: 可以在 Android 应用中直接生成吗？**  
A: 理论可行，但需要实现 MIDI 合成器。见 `MUSICXML_TO_AUDIO_SOLUTION.md` 方案1

---

**提示**: 首次使用前，请先安装 MuseScore 并测试脚本！
