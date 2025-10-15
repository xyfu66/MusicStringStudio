package com.example.musicstringstudioapp.domain.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 音频采集管理器
 * 
 * 负责实时采集音频数据并通过回调提供给音高检测模块
 * 
 * @property context Android Context
 */
class AudioCaptureManager(
    private val context: Context
) {
    
    /**
     * 音频数据回调接口
     */
    interface AudioDataCallback {
        fun onAudioData(audioData: ShortArray, sampleRate: Int)
    }
    
    companion object {
        // 音频采样率 - 44100Hz (CD音质)
        private const val SAMPLE_RATE = 44100
        
        // 音频通道配置 - 单声道
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        
        // 音频编码格式 - 16位PCM
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // 缓冲区大小 - 4096 samples (约93ms)
        private const val BUFFER_SIZE_IN_SAMPLES = 4096
        
        // 最小缓冲区大小
        private val MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
    }
    
    // AudioRecord 实例
    private var audioRecord: AudioRecord? = null
    
    // 录音任务的协程 Job
    private var recordingJob: Job? = null
    
    // 录音状态
    private var isRecording = false
    
    // 当前回调
    private var currentCallback: AudioDataCallback? = null
    
    // 短整型缓冲区（从 AudioRecord 读取）
    private val shortBuffer = ShortArray(BUFFER_SIZE_IN_SAMPLES)
    
    /**
     * 检查录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 初始化 AudioRecord
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initAudioRecord(): Boolean {
        if (audioRecord != null) {
            Timber.d("AudioRecord already initialized")
            return true
        }
        
        if (!hasRecordPermission()) {
            Timber.e("没有录音权限")
            return false
        }
        
        try {
            // 计算缓冲区大小（取较大值）
            val bufferSize = maxOf(MIN_BUFFER_SIZE, BUFFER_SIZE_IN_SAMPLES * 2)
            
            // 创建 AudioRecord 实例
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            // 检查状态
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord = null
                Timber.e("AudioRecord 初始化失败")
                return false
            }
            
            Timber.d("AudioRecord initialized successfully")
            Timber.d("Sample rate: $SAMPLE_RATE, Buffer size: $bufferSize")
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AudioRecord")
            return false
        }
    }
    
    /**
     * 开始采集（使用回调）
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCapture(callback: AudioDataCallback) {
        currentCallback = callback
        startRecording()
    }
    
    /**
     * 停止采集
     */
    fun stopCapture() {
        stopRecording()
        currentCallback = null
    }
    
    /**
     * 开始录音（内部方法）
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        if (isRecording) {
            Timber.w("Already recording")
            return
        }
        
        if (!hasRecordPermission()) {
            Timber.e("No record permission")
            return
        }
        
        if (!initAudioRecord()) {
            return
        }
        
        try {
            audioRecord?.startRecording()
            isRecording = true
            
            // 在后台协程中读取音频数据
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                recordAudio()
            }
            
            Timber.d("Recording started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            isRecording = false
        }
    }
    
    /**
     * 停止录音
     */
    private fun stopRecording() {
        if (!isRecording) {
            Timber.w("Not recording")
            return
        }
        
        isRecording = false
        
        // 取消录音协程
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
            Timber.d("Recording stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping recording")
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopRecording()
        
        try {
            audioRecord?.release()
            audioRecord = null
            Timber.d("AudioRecord released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioRecord")
        }
    }
    
    /**
     * 录音循环（在后台线程执行）
     */
    private suspend fun recordAudio() {
        while (isRecording) {
            try {
                // 读取音频数据到短整型缓冲区
                val bytesRead = audioRecord?.read(
                    shortBuffer,
                    0,
                    BUFFER_SIZE_IN_SAMPLES
                ) ?: 0
                
                if (bytesRead > 0) {
                    // 只传递有效数据
                    val validData = if (bytesRead < BUFFER_SIZE_IN_SAMPLES) {
                        shortBuffer.copyOf(bytesRead)
                    } else {
                        shortBuffer.copyOf()
                    }
                    
                    // 回调音频数据
                    currentCallback?.onAudioData(validData, SAMPLE_RATE)
                } else if (bytesRead < 0) {
                    // 读取出错
                    Timber.e("Error reading audio data: $bytesRead")
                    break
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in recording loop")
                break
            }
        }
    }
    
    /**
     * 获取当前录音状态
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 获取采样率
     */
    fun getSampleRate(): Int = SAMPLE_RATE
    
    /**
     * 获取缓冲区大小
     */
    fun getBufferSize(): Int = BUFFER_SIZE_IN_SAMPLES
}
