# MusicXML 自动生成音频方案

## 📋 问题分析

MuseScore 是一个完整的桌面应用（C++ 编写），直接集成到 Android 应用中有以下挑战：
1. 代码库巨大（几百MB）
2. 依赖复杂（Qt framework 等）
3. 需要跨平台编译（C++ to Android NDK）
4. 包体积会显著增加

## 🎯 推荐方案

### 方案1: 使用 Android MIDI 合成器 ⭐ 推荐

**优势**
- ✅ 纯 Android 原生
- ✅ 无需外部依赖
- ✅ 包体积小
- ✅ 实时生成音频

**实现步骤**

#### 1. MusicXML → MIDI 转换
创建一个转换器，将 MusicXML 转换为 MIDI 序列

#### 2. 使用 Android MediaPlayer/SoundPool
Android 内置了 MIDI 播放支持

#### 3. 或使用 Sonivox EAS 合成器
Android 系统自带的 MIDI 合成引擎

**代码示例**
```kotlin
class MusicXMLToMidiConverter {
    fun convertToMidi(song: Song): ByteArray {
        // 将 Song 对象转换为 MIDI 字节流
        // 使用 javax.sound.midi (需要添加库)
    }
}

class MidiAudioGenerator {
    fun generateAudio(midiData: ByteArray): String {
        // 使用 Android MediaPlayer 播放 MIDI
        // 或使用 Sonivox 合成音频
    }
}
```

### 方案2: 使用 MuseScore 命令行工具（开发时）

在开发阶段，使用 MuseScore 命令行批量生成音频

**创建自动化脚本**

#### Windows PowerShell 脚本
```powershell
# generate_audio.ps1

# MuseScore 安装路径
$musescorePath = "C:\Program Files\MuseScore 4\bin\MuseScore4.exe"

# MusicXML 文件目录
$xmlDir = "app\src\main\res\raw"

# 输出音频目录
$audioDir = "app\src\main\assets\audio"

# 确保输出目录存在
New-Item -ItemType Directory -Force -Path $audioDir

# 遍历所有 .musicxml 文件
Get-ChildItem $xmlDir -Filter *.musicxml | ForEach-Object {
    $inputFile = $_.FullName
    $outputFile = Join-Path $audioDir ($_.BaseName + ".mp3")
    
    Write-Host "转换: $($_.Name) -> $($_.BaseName).mp3"
    
    # 使用 MuseScore 命令行转换
    & $musescorePath $inputFile -o $outputFile
}

Write-Host "完成！音频文件已生成到 $audioDir"
```

#### Linux/Mac Bash 脚本
```bash
#!/bin/bash
# generate_audio.sh

MUSESCORE_PATH="/usr/bin/musescore"
XML_DIR="app/src/main/res/raw"
AUDIO_DIR="app/src/main/assets/audio"

mkdir -p "$AUDIO_DIR"

for xml_file in "$XML_DIR"/*.musicxml; do
    filename=$(basename "$xml_file" .musicxml)
    output_file="$AUDIO_DIR/$filename.mp3"
    
    echo "转换: $filename.musicxml -> $filename.mp3"
    
    "$MUSESCORE_PATH" "$xml_file" -o "$output_file"
done

echo "完成！音频文件已生成到 $AUDIO_DIR"
```

### 方案3: 使用在线 API（需要网络）

调用在线 MusicXML 转音频服务

**优点**: 无需本地处理  
**缺点**: 需要网络连接，可能有成本

### 方案4: 预先生成（最简单）⭐ 开发初期推荐

使用 MuseScore 桌面应用手动或自动批量生成音频文件，打包到 APK 中

## 🚀 推荐实施路线

### 阶段1: 快速开发（当前）
使用**方案4（预先生成）** + **方案2（自动化脚本）**

1. 创建自动化脚本
2. 开发时运行脚本批量生成音频
3. 将音频文件打包到 assets 目录

### 阶段2: 功能增强（后期）
实现**方案1（MIDI 合成）**

1. 实现 MusicXML → MIDI 转换器
2. 集成 Android MIDI 播放
3. 用户可导入自己的 MusicXML 并即时生成音频

## 📦 实现方案1的依赖

如果选择实现 MIDI 合成方案，需要添加：

```kotlin
// build.gradle.kts
dependencies {
    // MIDI 处理库
    implementation("com.github.philburk:jsyn:20171016")
    
    // 或使用 Android 内置的 MIDI API
    // 无需额外依赖
}
```

## 💡 最佳实践建议

### 当前阶段（MVP）
1. ✅ 使用自动化脚本生成音频
2. ✅ 音频文件打包到 APK
3. ✅ 专注于核心功能开发

### 未来扩展
1. 🔄 实现 MIDI 合成功能
2. 🔄 支持用户导入 MusicXML
3. 🔄 云端音频生成服务

## 🛠️ 立即可用的脚本

### 创建 generate_audio.ps1（Windows）
```powershell
# 见上面的 PowerShell 脚本
```

### 使用方法
```bash
# Windows
cd AndroidApp
.\generate_audio.ps1

# Linux/Mac
cd AndroidApp
chmod +x generate_audio.sh
./generate_audio.sh
```

## ⚠️ 注意事项

### MuseScore 命令行参数
```bash
# 基本转换
musescore input.musicxml -o output.mp3

# 设置音质
musescore input.musicxml -o output.mp3 -r 192

# 设置格式
musescore input.musicxml -o output.wav
musescore input.musicxml -o output.mp3
musescore input.musicxml -o output.ogg
```

### Android MIDI 限制
- Android 6.0+ 才完全支持 MIDI API
- 需要确保设备支持
- 音色库可能因设备而异

## 📚 参考资源

- **MuseScore 命令行**: https://musescore.org/en/handbook/4/command-line-options
- **Android MIDI API**: https://developer.android.com/reference/android/media/midi/package-summary
- **JSyn (Java 合成器)**: https://github.com/philburk/jsyn

---

## 🎯 结论

**当前推荐**: 使用**自动化脚本 + 预先生成**的方案

**理由**:
1. ✅ 简单、可靠、无依赖
2. ✅ 音频质量可控
3. ✅ 不增加 APK 复杂度
4. ✅ 快速迭代开发

**未来可选**: 实现**MIDI 实时合成**功能，支持用户导入曲谱
