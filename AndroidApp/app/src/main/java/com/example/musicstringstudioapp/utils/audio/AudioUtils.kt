package com.example.musicstringstudioapp.utils.audio

import kotlin.math.log10
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
        return 20 * log10(rms)
    }
}
