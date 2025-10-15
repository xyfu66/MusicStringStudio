package com.example.musicstringstudioapp.domain.practice

import android.Manifest
import androidx.annotation.RequiresPermission
import com.example.musicstringstudioapp.data.model.Note
import com.example.musicstringstudioapp.data.model.Song
import com.example.musicstringstudioapp.domain.audio.AudioCaptureManager
import com.example.musicstringstudioapp.domain.audio.PitchDetector
import com.example.musicstringstudioapp.domain.pitch.PitchComparator
import com.example.musicstringstudioapp.domain.pitch.PitchStatistics
import com.example.musicstringstudioapp.domain.pitch.ScoreCalculator
import com.example.musicstringstudioapp.domain.score.ScoreFollower
import com.example.musicstringstudioapp.domain.score.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 练习引擎
 * 
 * 整合音频采集、音高检测、音准比对、曲谱同步等功能
 * 提供完整的跟音练习体验
 */
class PracticeEngine(
    private val song: Song,
    private val syncCoordinator: SyncCoordinator,
    private val audioCaptureManager: AudioCaptureManager,
    private val pitchDetector: PitchDetector
) {
    
    // 核心组件
    private val scoreFollower: ScoreFollower = ScoreFollower(song)
    private val pitchComparator: PitchComparator = PitchComparator()
    private val pitchStatistics: PitchStatistics = PitchStatistics()
    private val scoreCalculator: ScoreCalculator = ScoreCalculator()
    
    // 协程作用域
    private val practiceScope = CoroutineScope(Dispatchers.Default)
    private var practiceJob: Job? = null
    
    // 监听器
    private val listeners = mutableListOf<PracticeListener>()
    
    // 状态
    private var isPracticing = false
    private val comparisonResults = mutableListOf<PitchComparator.ComparisonResult>()
    
    /**
     * 练习监听器
     */
    interface PracticeListener {
        /**
         * 实时反馈
         * @param currentNote 当前目标音符
         * @param detectedPitch 检测到的音高（Hz），null 表示未检测到
         * @param comparisonResult 比对结果，null 表示未比对
         */
        fun onRealtimeFeedback(
            currentNote: Note?,
            detectedPitch: Float?,
            comparisonResult: PitchComparator.ComparisonResult?
        ) {}
        
        /**
         * 音符命中
         * @param result 比对结果
         */
        fun onNoteHit(result: PitchComparator.ComparisonResult) {}
        
        /**
         * 音符错过
         * @param missedNote 错过的音符
         */
        fun onNoteMissed(missedNote: Note) {}
        
        /**
         * 练习完成
         * @param score 最终得分
         */
        fun onPracticeCompleted(score: ScoreCalculator.PracticeScore) {}
        
        /**
         * 错误发生
         * @param error 错误信息
         */
        fun onError(error: String) {}
    }
    
    /**
     * 开始练习
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startPractice() {
        if (isPracticing) {
            Timber.w("练习已在进行中")
            return
        }
        
        Timber.d("开始练习")
        isPracticing = true
        
        // 清空之前的结果
        comparisonResults.clear()
        pitchStatistics.clear()
        
        // 启动音频采集
        audioCaptureManager.startCapture(audioDataCallback)
        
        // 启动练习循环
        startPracticeLoop()
        
        // 启动播放
        syncCoordinator.play()
    }
    
    /**
     * 停止练习
     */
    fun stopPractice() {
        if (!isPracticing) {
            return
        }
        
        Timber.d("停止练习")
        isPracticing = false
        
        // 停止音频采集
        audioCaptureManager.stopCapture()
        
        // 停止练习循环
        practiceJob?.cancel()
        practiceJob = null
        
        // 停止播放
        syncCoordinator.stop()
    }
    
    /**
     * 暂停练习
     */
    fun pausePractice() {
        if (!isPracticing) return
        
        syncCoordinator.pause()
        audioCaptureManager.stopCapture()
    }
    
    /**
     * 恢复练习
     */
    fun resumePractice() {
        if (!isPracticing) return
        
        syncCoordinator.play()
        audioCaptureManager.startCapture(audioDataCallback)
    }
    
    /**
     * 完成练习并生成报告
     */
    fun completePractice() {
        stopPractice()
        
        // 计算最终得分
        val finalScore = scoreCalculator.calculateScore(
            comparisonResults,
            song.getAllNotes().size
        )
        
        Timber.d("练习完成: 总分=${finalScore.totalScore}, 评级=${finalScore.grade}")
        
        // 通知监听器
        notifyPracticeCompleted(finalScore)
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: PracticeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: PracticeListener) {
        listeners.remove(listener)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopPractice()
        practiceScope.cancel()
        audioCaptureManager.release()
        syncCoordinator.release()
        
        Timber.d("练习引擎资源已释放")
    }
    
    /**
     * 启动练习循环
     */
    private fun startPracticeLoop() {
        practiceJob = practiceScope.launch {
            var lastProcessedNoteTime = 0L
            
            while (isActive && isPracticing) {
                try {
                    // 获取当前播放位置
                    val currentPosition = syncCoordinator.getCurrentPosition()
                    
                    // 更新曲谱跟随器
                    scoreFollower.updatePosition(currentPosition)
                    
                    // 获取当前目标音符
                    val currentNote = scoreFollower.getCurrentNote()
                    
                    // 获取检测到的音高
                    val detectedPitch = pitchDetector.getLatestPitch()
                    
                    // 进行音准比对
                    val comparisonResult = if (currentNote != null && detectedPitch != null) {
                        val result = pitchComparator.compare(
                            currentNote,
                            detectedPitch,
                            currentPosition
                        )
                        
                        // 如果这是该音符的首次比对，记录结果
                        if (currentNote.startTime != lastProcessedNoteTime) {
                            comparisonResults.add(result)
                            pitchStatistics.addResult(result)
                            lastProcessedNoteTime = currentNote.startTime
                            
                            // 判断是否命中
                            if (result.isInTimeWindow && 
                                result.accuracy != PitchComparator.PitchAccuracy.POOR) {
                                notifyNoteHit(result)
                            }
                        }
                        
                        result
                    } else {
                        null
                    }
                    
                    // 发送实时反馈
                    notifyRealtimeFeedback(currentNote, detectedPitch, comparisonResult)
                    
                    // 延迟以控制更新频率（约30fps）
                    kotlinx.coroutines.delay(33)
                    
                } catch (e: Exception) {
                    Timber.e(e, "练习循环错误")
                    notifyError("练习错误: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 音频数据回调
     */
    private val audioDataCallback = object : AudioCaptureManager.AudioDataCallback {
        override fun onAudioData(audioData: ShortArray, sampleRate: Int) {
            // 将音频数据传递给音高检测器
            pitchDetector.processAudioData(audioData, sampleRate)
        }
    }
    
    /**
     * 通知实时反馈
     */
    private fun notifyRealtimeFeedback(
        currentNote: Note?,
        detectedPitch: Float?,
        comparisonResult: PitchComparator.ComparisonResult?
    ) {
        listeners.forEach {
            it.onRealtimeFeedback(currentNote, detectedPitch, comparisonResult)
        }
    }
    
    /**
     * 通知音符命中
     */
    private fun notifyNoteHit(result: PitchComparator.ComparisonResult) {
        listeners.forEach { it.onNoteHit(result) }
    }
    
    /**
     * 通知音符错过
     */
    private fun notifyNoteMissed(note: Note) {
        listeners.forEach { it.onNoteMissed(note) }
    }
    
    /**
     * 通知练习完成
     */
    private fun notifyPracticeCompleted(score: ScoreCalculator.PracticeScore) {
        listeners.forEach { it.onPracticeCompleted(score) }
    }
    
    /**
     * 通知错误
     */
    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
}

/**
 * PracticeEngine 工厂
 */
object PracticeEngineFactory {
    
    /**
     * 创建练习引擎
     * 
     * @param song 曲谱
     * @param syncCoordinator 同步协调器
     * @param audioCaptureManager 音频采集管理器
     * @param pitchDetector 音高检测器
     * @return 练习引擎实例
     */
    fun create(
        song: Song,
        syncCoordinator: SyncCoordinator,
        audioCaptureManager: AudioCaptureManager,
        pitchDetector: PitchDetector
    ): PracticeEngine {
        return PracticeEngine(
            song,
            syncCoordinator,
            audioCaptureManager,
            pitchDetector
        )
    }
}
