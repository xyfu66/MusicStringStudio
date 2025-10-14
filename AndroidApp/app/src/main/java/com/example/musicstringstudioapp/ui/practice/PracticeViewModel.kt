package com.example.musicstringstudioapp.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicstringstudioapp.domain.audio.AudioCaptureManager
import com.example.musicstringstudioapp.domain.audio.PitchDetector
import com.example.musicstringstudioapp.domain.converter.FrequencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 练习界面的 ViewModel
 * 
 * 管理音频采集、音高检测和UI状态
 */
class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    
    // 音频采集管理器
    private lateinit var audioCaptureManager: AudioCaptureManager
    
    // 音高检测器
    private val pitchDetector = PitchDetector(sampleRate = 44100)
    
    // 频率平滑器
    private val frequencySmoother = PitchDetector.FrequencySmoother(windowSize = 3)
    
    // UI状态
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    
    init {
        initializeAudioCapture()
    }
    
    /**
     * 初始化音频采集
     */
    private fun initializeAudioCapture() {
        audioCaptureManager = AudioCaptureManager(
            context = getApplication(),
            onAudioDataAvailable = { audioData ->
                processAudioData(audioData)
            },
            onError = { error ->
                Timber.e("Audio capture error: $error")
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error
                    )
                }
            }
        )
    }
    
    /**
     * 处理音频数据
     */
    private fun processAudioData(audioData: FloatArray) {
        viewModelScope.launch {
            // 检测音高
            val pitchResult = pitchDetector.detectPitch(audioData)
            
            if (pitchResult.isSilent || pitchResult.frequency <= 0) {
                // 静音或无法检测
                _uiState.value = _uiState.value.copy(
                    detectedFrequency = 0f,
                    detectedNote = "---",
                    centsDeviation = 0f,
                    confidence = 0f,
                    isSilent = true
                )
                frequencySmoother.reset()
            } else {
                // 平滑频率以减少抖动
                val smoothedFrequency = frequencySmoother.addFrequency(pitchResult.frequency)
                
                // 分析频率
                val analysis = FrequencyConverter.analyzeFrequency(smoothedFrequency)
                
                // 更新UI状态
                _uiState.value = _uiState.value.copy(
                    detectedFrequency = smoothedFrequency,
                    detectedNote = analysis.noteName,
                    centsDeviation = analysis.centsDeviation,
                    confidence = pitchResult.confidence,
                    accuracyLevel = analysis.accuracyLevel,
                    isSilent = false
                )
            }
        }
    }
    
    /**
     * 开始录音
     */
    fun startRecording() {
        if (!audioCaptureManager.hasRecordPermission()) {
            _uiState.value = _uiState.value.copy(
                needsPermission = true,
                errorMessage = "需要录音权限"
            )
            return
        }
        
        audioCaptureManager.startRecording()
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            errorMessage = null
        )
        Timber.d("Recording started")
    }
    
    /**
     * 停止录音
     */
    fun stopRecording() {
        audioCaptureManager.stopRecording()
        frequencySmoother.reset()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            detectedFrequency = 0f,
            detectedNote = "---",
            centsDeviation = 0f,
            confidence = 0f,
            isSilent = true
        )
        Timber.d("Recording stopped")
    }
    
    /**
     * 切换录音状态
     */
    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    /**
     * 检查是否有录音权限
     */
    fun hasRecordPermission(): Boolean {
        return audioCaptureManager.hasRecordPermission()
    }
    
    /**
     * 权限已授予
     */
    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            needsPermission = false
        )
        startRecording()
    }
    
    /**
     * 权限被拒绝
     */
    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            needsPermission = false,
            errorMessage = "没有录音权限，无法使用音高检测功能"
        )
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        audioCaptureManager.release()
    }
}

/**
 * 练习界面UI状态
 */
data class PracticeUiState(
    val isRecording: Boolean = false,
    val detectedFrequency: Float = 0f,
    val detectedNote: String = "---",
    val centsDeviation: Float = 0f,
    val confidence: Float = 0f,
    val accuracyLevel: String = "none",
    val isSilent: Boolean = true,
    val needsPermission: Boolean = false,
    val errorMessage: String? = null
)
