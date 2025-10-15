package com.example.musicstringstudioapp.domain.score

import com.example.musicstringstudioapp.data.model.Song
import com.example.musicstringstudioapp.domain.audio.AudioPlaybackManager
import timber.log.Timber

/**
 * 同步协调器
 * 
 * 协调音频播放和曲谱跟随，确保两者同步
 * 作为音频播放器和曲谱跟随器之间的桥梁
 */
class SyncCoordinator(
    private val audioPlaybackManager: AudioPlaybackManager,
    private val scoreFollower: ScoreFollower
) {
    
    // 播放状态
    private var isPlaying = false
    
    // 同步偏移量（毫秒）- 用于手动调整同步
    private var syncOffsetMs: Long = 0
    
    /**
     * 初始化同步
     */
    fun initialize() {
        // 添加音频播放监听器
        audioPlaybackManager.addListener(audioPlaybackListener)
        
        Timber.d("同步协调器初始化完成")
    }
    
    /**
     * 播放
     */
    fun play() {
        audioPlaybackManager.play()
        isPlaying = true
        Timber.d("开始播放")
    }
    
    /**
     * 暂停
     */
    fun pause() {
        audioPlaybackManager.pause()
        isPlaying = false
        Timber.d("暂停播放")
    }
    
    /**
     * 停止
     */
    fun stop() {
        audioPlaybackManager.stop()
        scoreFollower.reset()
        isPlaying = false
        Timber.d("停止播放")
    }
    
    /**
     * 跳转到指定位置
     * 
     * @param positionMs 目标位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        val adjustedPosition = positionMs + syncOffsetMs
        audioPlaybackManager.seekTo(adjustedPosition)
        scoreFollower.seekTo(positionMs)
        
        Timber.d("跳转到: ${positionMs}ms (调整后: ${adjustedPosition}ms)")
    }
    
    /**
     * 设置播放速度
     * 
     * @param speed 播放速度（0.5 - 2.0）
     */
    fun setSpeed(speed: Float) {
        audioPlaybackManager.setSpeed(speed)
        Timber.d("设置速度: ${speed}x")
    }
    
    /**
     * 设置循环播放范围
     * 
     * @param startMs 开始位置（毫秒）
     * @param endMs 结束位置（毫秒）
     */
    fun setLoopRange(startMs: Long, endMs: Long) {
        audioPlaybackManager.setLoopRange(startMs, endMs)
        Timber.d("设置循环范围: $startMs - $endMs ms")
    }
    
    /**
     * 取消循环
     */
    fun clearLoop() {
        audioPlaybackManager.clearLoop()
        Timber.d("取消循环")
    }
    
    /**
     * 设置同步偏移
     * 
     * 用于手动调整音频和曲谱的同步
     * 正值：曲谱提前，负值：曲谱延后
     * 
     * @param offsetMs 偏移量（毫秒）
     */
    fun setSyncOffset(offsetMs: Long) {
        syncOffsetMs = offsetMs
        
        // 重新同步当前位置
        if (isPlaying) {
            val currentAudioPosition = audioPlaybackManager.getCurrentPosition()
            val scorePosition = currentAudioPosition - syncOffsetMs
            scoreFollower.updatePosition(scorePosition)
        }
        
        Timber.d("设置同步偏移: ${offsetMs}ms")
    }
    
    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Long {
        return audioPlaybackManager.getCurrentPosition()
    }
    
    /**
     * 获取总时长
     */
    fun getDuration(): Long {
        return audioPlaybackManager.getDuration()
    }
    
    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }
    
    /**
     * 释放资源
     */
    fun release() {
        audioPlaybackManager.removeListener(audioPlaybackListener)
        Timber.d("同步协调器资源释放")
    }
    
    /**
     * 音频播放监听器
     * 
     * 将音频播放位置同步到曲谱跟随器
     */
    private val audioPlaybackListener = object : AudioPlaybackManager.PlaybackListener {
        
        override fun onPositionChanged(positionMs: Long) {
            // 应用同步偏移
            val scorePosition = positionMs - syncOffsetMs
            
            // 更新曲谱跟随器位置
            scoreFollower.updatePosition(scorePosition)
        }
        
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            this@SyncCoordinator.isPlaying = isPlaying
            
            if (!isPlaying) {
                Timber.d("播放状态变化: 已暂停/停止")
            }
        }
        
        override fun onCompleted() {
            isPlaying = false
            Timber.d("播放完成")
            
            // 可选：重置到开始位置
            // scoreFollower.reset()
        }
        
        override fun onError(error: String) {
            isPlaying = false
            Timber.e("播放错误: $error")
        }
    }
}

/**
 * SyncCoordinator 工厂方法
 */
object SyncCoordinatorFactory {
    
    /**
     * 创建同步协调器
     * 
     * @param song 曲谱
     * @param audioPlaybackManager 音频播放管理器
     * @return 同步协调器实例
     */
    fun create(
        song: Song,
        audioPlaybackManager: AudioPlaybackManager
    ): SyncCoordinator {
        val scoreFollower = ScoreFollower(song)
        val coordinator = SyncCoordinator(audioPlaybackManager, scoreFollower)
        coordinator.initialize()
        return coordinator
    }
}
