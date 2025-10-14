package com.example.musicstringstudioapp.domain.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 音高检测器（简化版）
 * 
 * 使用自相关算法检测音频中的基频
 * 注意：这是一个简化版实现，适用于MVP阶段
 * 后续可以替换为更精确的YIN算法或TarsosDSP
 * 
 * @property sampleRate 采样率 (Hz)
 */
class PitchDetector(
    private val sampleRate: Int = 44100
) {
    companion object {
        // 最小检测频率 (Hz) - 大约 C2
        private const val MIN_FREQUENCY = 65.0f
        
        // 最大检测频率 (Hz) - 大约 C8
        private const val MAX_FREQUENCY = 4186.0f
        
        // 静音阈值（能量）
        private const val SILENCE_THRESHOLD = 0.01f
        
        // 置信度阈值
        private const val CONFIDENCE_THRESHOLD = 0.3f
    }
    
    // 最小周期（样本数）
    private val minPeriod = (sampleRate / MAX_FREQUENCY).toInt()
    
    // 最大周期（样本数）
    private val maxPeriod = (sampleRate / MIN_FREQUENCY).toInt()
    
    /**
     * 音高检测结果
     */
    data class PitchResult(
        val frequency: Float,
        val confidence: Float,
        val isSilent: Boolean
    )
    
    /**
     * 检测音频样本中的音高
     * 
     * @param audioBuffer 音频样本数组（归一化到 -1.0 到 1.0）
     * @return 音高检测结果
     */
    fun detectPitch(audioBuffer: FloatArray): PitchResult {
        // 1. 检查是否为静音
        val energy = calculateEnergy(audioBuffer)
        if (energy < SILENCE_THRESHOLD) {
            return PitchResult(
                frequency = 0f,
                confidence = 0f,
                isSilent = true
            )
        }
        
        // 2. 使用自相关算法检测基频周期
        val (period, confidence) = detectPeriodByAutocorrelation(audioBuffer)
        
        // 3. 将周期转换为频率
        val frequency = if (period > 0 && confidence > CONFIDENCE_THRESHOLD) {
            sampleRate.toFloat() / period
        } else {
            0f
        }
        
        return PitchResult(
            frequency = frequency,
            confidence = confidence,
            isSilent = false
        )
    }
    
    /**
     * 计算音频能量
     */
    private fun calculateEnergy(buffer: FloatArray): Float {
        var sum = 0f
        for (sample in buffer) {
            sum += sample * sample
        }
        return sum / buffer.size
    }
    
    /**
     * 使用自相关算法检测周期
     * 
     * @param buffer 音频样本
     * @return Pair<周期, 置信度>
     */
    private fun detectPeriodByAutocorrelation(buffer: FloatArray): Pair<Int, Float> {
        val bufferSize = buffer.size
        
        // 计算自相关
        val autocorrelation = FloatArray(maxPeriod + 1)
        
        // 计算 r(0) - 用于归一化
        var r0 = 0f
        for (i in buffer.indices) {
            r0 += buffer[i] * buffer[i]
        }
        
        if (r0 == 0f) {
            return Pair(0, 0f)
        }
        
        // 计算自相关函数
        for (lag in minPeriod..maxPeriod.coerceAtMost(bufferSize - 1)) {
            var sum = 0f
            for (i in 0 until (bufferSize - lag)) {
                sum += buffer[i] * buffer[i + lag]
            }
            autocorrelation[lag] = sum / r0 // 归一化
        }
        
        // 寻找第一个峰值（周期）
        var maxCorrelation = 0f
        var bestLag = 0
        
        for (lag in minPeriod..maxPeriod.coerceAtMost(bufferSize - 1)) {
            if (autocorrelation[lag] > maxCorrelation) {
                maxCorrelation = autocorrelation[lag]
                bestLag = lag
            }
        }
        
        // 使用抛物线插值提高精度
        val refinedLag = if (bestLag > minPeriod && bestLag < maxPeriod) {
            parabolicInterpolation(
                autocorrelation[bestLag - 1],
                autocorrelation[bestLag],
                autocorrelation[bestLag + 1],
                bestLag
            )
        } else {
            bestLag.toFloat()
        }
        
        return Pair(refinedLag.toInt(), maxCorrelation)
    }
    
    /**
     * 抛物线插值以提高周期估计精度
     */
    private fun parabolicInterpolation(
        y1: Float,
        y2: Float,
        y3: Float,
        x: Int
    ): Float {
        val a = (y1 + y3 - 2 * y2) / 2
        val b = (y3 - y1) / 2
        
        return if (abs(a) > 1e-10) {
            x - b / (2 * a)
        } else {
            x.toFloat()
        }
    }
    
    /**
     * 平滑频率（使用移动平均）
     * 可用于减少检测结果的抖动
     */
    class FrequencySmoother(
        private val windowSize: Int = 5
    ) {
        private val frequencies = mutableListOf<Float>()
        
        fun addFrequency(frequency: Float): Float {
            if (frequency > 0) {
                frequencies.add(frequency)
                if (frequencies.size > windowSize) {
                    frequencies.removeAt(0)
                }
            }
            
            return if (frequencies.isNotEmpty()) {
                frequencies.average().toFloat()
            } else {
                0f
            }
        }
        
        fun reset() {
            frequencies.clear()
        }
    }
}
