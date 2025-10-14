# MusicStringStudio 快速开始指南

## 立即开始：迭代0 - 环境准备

本指南将帮助您快速开始第一个迭代的开发工作。

---

## 步骤 1: 配置依赖库 (30分钟)

### 1.1 修改项目级 build.gradle.kts

在 `AndroidApp/build.gradle.kts` 中添加：

```kotlin
// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 1.2 修改 gradle/libs.versions.toml

添加版本管理（如果文件存在）或在 app/build.gradle.kts 中直接使用版本号。

### 1.3 修改应用级 build.gradle.kts

在 `AndroidApp/app/build.gradle.kts` 中添加以下内容：

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.musicstringstudioapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.musicstringstudioapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 添加 Hilt 测试支持
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === 核心依赖 ===
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
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
    implementation("com.github.JorenSix:TarsosDSP:v2.5")
    
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
    
    // === 依赖注入 Hilt ===
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

### 1.4 配置 settings.gradle.kts

确保包含 jitpack 仓库：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MusicStringStudioApp"
include(":app")
```

---

## 步骤 2: 配置权限 (5分钟)

修改 `AndroidApp/app/src/main/AndroidManifest.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 录音权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 存储权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32"
        tools:ignore="ScopedStorage" />
    
    <!-- Android 13+ 新权限 -->
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    
    <!-- 麦克风特性声明 -->
    <uses-feature
        android:name="android.hardware.microphone"
        android:required="true" />

    <application
        android:name=".MusicStringStudioApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MusicStringStudioApp"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MusicStringStudioApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 步骤 3: 创建项目架构 (15分钟)

### 3.1 创建包结构

在 `app/src/main/java/com/example/musicstringstudioapp/` 下创建以下目录：

```
com.example.musicstringstudioapp/
├── MusicStringStudioApp.kt        # Application 类
├── MainActivity.kt                 # 主Activity（已存在）
├── audio/                          # 音频处理模块
│   ├── capture/
│   ├── playback/
│   └── pitch/
├── score/                          # 曲谱相关
│   ├── model/
│   ├── parser/
│   └── follower/
├── practice/                       # 练习功能
│   ├── ui/
│   ├── viewmodel/
│   └── engine/
├── tuner/                          # 调音器
│   ├── ui/
│   └── viewmodel/
├── metronome/                      # 节拍器
│   ├── ui/
│   └── engine/
├── user/                           # 用户管理
│   ├── data/
│   ├── ui/
│   └── viewmodel/
├── data/                           # 数据层
│   ├── local/
│   ├── remote/
│   └── repository/
├── network/                        # 网络层
│   └── api/
├── ui/                             # UI组件
│   ├── components/
│   ├── navigation/
│   └── theme/
└── utils/                          # 工具类
    ├── audio/
    └── music/
```

### 3.2 创建 Application 类

创建 `app/src/main/java/com/example/musicstringstudioapp/MusicStringStudioApp.kt`：

```kotlin
package com.example.musicstringstudioapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MusicStringStudioApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("MusicStringStudio App 已启动")
    }
}
```

### 3.3 修改 MainActivity

更新 `MainActivity.kt` 支持 Hilt：

```kotlin
package com.example.musicstringstudioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.musicstringstudioapp.ui.theme.MusicStringStudioAppTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity 启动")
        
        enableEdgeToEdge()
        setContent {
            MusicStringStudioAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Greeting(
                            name = "MusicStringStudio",
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "欢迎使用 $name!",
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicStringStudioAppTheme {
        Greeting("MusicStringStudio")
    }
}
```

---

## 步骤 4: 同步并验证 (5分钟)

1. 点击 Android Studio 的 "Sync Now" 同步 Gradle
2. 等待依赖下载完成（首次可能需要几分钟）
3. 解决可能出现的版本冲突
4. 编译项目：`Build > Make Project`
5. 运行应用到模拟器或真机

如果编译成功，恭喜！环境准备完成✅

---

## 步骤 5: 创建基础工具类 (20分钟)

### 5.1 音乐理论工具类

创建 `app/src/main/java/com/example/musicstringstudioapp/utils/music/MusicTheory.kt`：

```kotlin
package com.example.musicstringstudioapp.utils.music

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * 音乐理论工具类
 * 提供音高、音符、频率等转换功能
 */
object MusicTheory {
    
    // 标准音 A4 的频率
    const val A4_FREQUENCY = 440.0f
    
    // A4 的 MIDI 音符号
    const val A4_MIDI_NOTE = 69
    
    // 音符名称（从 C 开始）
    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", 
        "F#", "G", "G#", "A", "A#", "B"
    )
    
    // 小提琴空弦音符（从低到高：G3, D4, A4, E5）
    val VIOLIN_OPEN_STRINGS = mapOf(
        "G3" to 196.0f,
        "D4" to 293.66f,
        "A4" to 440.0f,
        "E5" to 659.25f
    )
    
    /**
     * 频率转换为 MIDI 音符号
     * @param frequency 频率 (Hz)
     * @return MIDI 音符号 (0-127)
     */
    fun frequencyToMidiNote(frequency: Float): Int {
        if (frequency <= 0) return 0
        return round(69 + 12 * ln(frequency / A4_FREQUENCY) / ln(2.0)).toInt()
    }
    
    /**
     * MIDI 音符号转换为频率
     * @param midiNote MIDI 音符号
     * @return 频率 (Hz)
     */
    fun midiNoteToFrequency(midiNote: Int): Float {
        return A4_FREQUENCY * 2.0.pow((midiNote - A4_MIDI_NOTE) / 12.0).toFloat()
    }
    
    /**
     * MIDI 音符号转换为音符名称
     * @param midiNote MIDI 音符号
     * @return 音符名称（如 "C4", "A#5"）
     */
    fun midiNoteToNoteName(midiNote: Int): String {
        val octave = (midiNote / 12) - 1
        val noteIndex = midiNote % 12
        return "${NOTE_NAMES[noteIndex]}$octave"
    }
    
    /**
     * 计算两个频率之间的音分偏差
     * @param frequency1 实际频率
     * @param frequency2 目标频率
     * @return 音分偏差（正值表示偏高，负值表示偏低）
     */
    fun calculateCentsDeviation(frequency1: Float, frequency2: Float): Float {
        if (frequency1 <= 0 || frequency2 <= 0) return 0f
        return (1200 * ln(frequency1 / frequency2) / ln(2.0)).toFloat()
    }
    
    /**
     * 频率直接转换为音符名称
     * @param frequency 频率 (Hz)
     * @return 音符名称
     */
    fun frequencyToNoteName(frequency: Float): String {
        val midiNote = frequencyToMidiNote(frequency)
        return midiNoteToNoteName(midiNote)
    }
    
    /**
     * 判定音准等级
     * @param centsDeviation 音分偏差
     * @return 准确度等级
     */
    fun judgeAccuracy(centsDeviation: Float): AccuracyLevel {
        val absCents = kotlin.math.abs(centsDeviation)
        return when {
            absCents < 10 -> AccuracyLevel.PERFECT
            absCents < 25 -> AccuracyLevel.GOOD
            absCents < 50 -> AccuracyLevel.FAIR
            else -> AccuracyLevel.POOR
        }
    }
}

/**
 * 音准等级枚举
 */
enum class AccuracyLevel {
    PERFECT,  // 完美 (< 10 cents)
    GOOD,     // 良好 (10-25 cents)
    FAIR,     // 一般 (25-50 cents)
    POOR      // 较差 (> 50 cents)
}
```

### 5.2 音频工具类

创建 `app/src/main/java/com/example/musicstringstudioapp/utils/audio/AudioUtils.kt`：

```kotlin
package com.example.musicstringstudioapp.utils.audio

import kotlin.math.sqrt

/**
 * 音频处理工具类
 */
object AudioUtils {
    
    /**
     * 计算音频缓冲区的 RMS（均方根）能量
     * 用于检测音量和静音
     * @param buffer 音频样本数组
     * @return RMS 值
     */
    fun calculateRMS(buffer: FloatArray): Float {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }
        return sqrt(sum / buffer.size).toFloat()
    }
    
    /**
     * 检测是否为静音
     * @param buffer 音频样本数组
     * @param threshold 静音阈值（默认 0.01）
     * @return true 如果是静音
     */
    fun isSilence(buffer: FloatArray, threshold: Float = 0.01f): Boolean {
        val rms = calculateRMS(buffer)
        return rms < threshold
    }
    
    /**
     * 转换 short 数组为 float 数组
     * AudioRecord 返回 short 数据，需要归一化为 float
     * @param shortArray short 数组
     * @return float 数组（范围 -1.0 到 1.0）
     */
    fun shortArrayToFloatArray(shortArray: ShortArray): FloatArray {
        val floatArray = FloatArray(shortArray.size)
        for (i in shortArray.indices) {
            floatArray[i] = shortArray[i] / 32768.0f
        }
        return floatArray
    }
    
    /**
     * 计算音频分贝值
     * @param rms RMS 值
     * @return 分贝值 (dB)
     */
    fun rmsToDb(rms: Float): Float {
        if (rms <= 0) return -100f
        return 20 * kotlin.math.log10(rms)
    }
}
```

---

## 步骤 6: 创建测试用例模板 (10分钟)

### 6.1 音乐理论单元测试

创建 `app/src/test/java/com/example/musicstringstudioapp/utils/music/MusicTheoryTest.kt`：

```kotlin
package com.example.musicstringstudioapp.utils.music

import org.junit.Assert.*
import org.junit.Test

class MusicTheoryTest {
    
    @Test
    fun `A4 频率应该转换为 MIDI 69`() {
        val midiNote = MusicTheory.frequencyToMidiNote(440f)
        assertEquals(69, midiNote)
    }
    
    @Test
    fun `MIDI 69 应该转换为 440Hz`() {
        val frequency = MusicTheory.midiNoteToFrequency(69)
        assertEquals(440f, frequency, 0.1f)
    }
    
    @Test
    fun `MIDI 69 应该转换为 A4`() {
        val noteName = MusicTheory.midiNoteToNoteName(69)
        assertEquals("A4", noteName)
    }
    
    @Test
    fun `音分偏差计算应该正确`() {
        // 440Hz 到 442Hz 约 +7.85 cents
        val cents = MusicTheory.calculateCentsDeviation(442f, 440f)
        assertTrue(cents > 7f && cents < 8f)
    }
    
    @Test
    fun `音准判定应该正确`() {
        assertEquals(AccuracyLevel.PERFECT, MusicTheory.judgeAccuracy(5f))
        assertEquals(AccuracyLevel.GOOD, MusicTheory.judgeAccuracy(15f))
        assertEquals(AccuracyLevel.FAIR, MusicTheory.judgeAccuracy(30f))
        assertEquals(AccuracyLevel.POOR, MusicTheory.judgeAccuracy(60f))
    }
}
```

---

## 步骤 7: 运行测试 (5分钟)

在 Android Studio 中：

1. 右键点击测试文件
2. 选择 "Run 'MusicTheoryTest'"
3. 查看测试结果

所有测试都应该通过 ✅

---

## 下一步

恭喜！您已经完成了**迭代0：环境准备**。

现在可以开始 **迭代1：音频基础功能** 的开发：

1. 实现音频采集模块（AudioCaptureManager）
2. 集成 TarsosDSP 实现音高检测
3. 创建简单的测试UI

详细步骤请参考 `ROADMAP.md` 的迭代1部分。

---

## 常见问题

### Q: Gradle 同步失败怎么办？
A: 
1. 检查网络连接
2. 使用代理或镜像（如阿里云 Maven）
3. 清理并重新构建：`Build > Clean Project` 然后 `Build > Rebuild Project`

### Q: TarsosDSP 依赖无法下载？
A: 确保 settings.gradle.kts 中包含了 jitpack 仓库

### Q: Hilt 注解处理器报错？
A: 确保正确配置了 KSP 插件

### Q: 最低支持 Android 版本？
A: Android 8.0 (API 26)，但推荐 Android 9.0 (API 28) 以获得更好的低延迟音频支持

---

## 参考资源

- [Android 官方文档](https://developer.android.com/)
- [Jetpack Compose 教程](https://developer.android.com/jetpack/compose)
- [TarsosDSP GitHub](https://github.com/JorenSix/TarsosDSP)
- [ExoPlayer 文档](https://exoplayer.dev/)
- [Hilt 依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)
