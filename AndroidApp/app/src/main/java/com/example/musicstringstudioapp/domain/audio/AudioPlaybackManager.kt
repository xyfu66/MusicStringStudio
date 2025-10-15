package com.example.musicstringstudioapp.domain.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 音频播放管理器
 * 
 * 使用 ExoPlayer 播放示范音频
 * 支持播放控制、变速、循环等功能
 * 
 * @property context Android Context
 */
class AudioPlaybackManager(
    private val context: Context
) {
    
    // ExoPlayer 实例
    private var exoPlayer: ExoPlayer? = null
    
    // 播放状态监听器列表
    private val listeners = mutableListOf<PlaybackListener>()
    
    // 当前播放的音频 URL
    private var currentAudioUrl: String? = null
    
    // 循环播放范围
    private var loopStartMs: Long = 0
    private var loopEndMs: Long = 0
    private var isLooping: Boolean = false
    
    // 进度更新协程
    private var progressJob: Job? = null
    private val progressScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * 播放状态监听器
     */
    interface PlaybackListener {
        /**
         * 播放位置变化
         * @param positionMs 当前位置（毫秒）
         */
        fun onPositionChanged(positionMs: Long) {}
        
        /**
         * 播放状态变化
         * @param isPlaying 是否正在播放
         */
        fun onPlaybackStateChanged(isPlaying: Boolean) {}
        
        /**
         * 播放完成
         */
        fun onCompleted() {}
        
        /**
         * 错误发生
         * @param error 错误信息
         */
        fun onError(error: String) {}
    }
    
    /**
     * 初始化 ExoPlayer
     */
    private fun initializePlayer() {
        if (exoPlayer != null) {
            return
        }
        
        try {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                // 添加播放器监听器
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Timber.d("播放器准备就绪")
                            }
                            Player.STATE_ENDED -> {
                                Timber.d("播放完成")
                                notifyCompleted()
                                if (isLooping) {
                                    // 循环播放
                                    seekTo(loopStartMs)
                                    play()
                                }
                            }
                            Player.STATE_IDLE -> {
                                Timber.d("播放器空闲")
                            }
                            Player.STATE_BUFFERING -> {
                                Timber.d("播放器缓冲中")
                            }
                        }
                    }
                    
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Timber.d("播放状态变化: $isPlaying")
                        notifyPlaybackStateChanged(isPlaying)
                        
                        if (isPlaying) {
                            startProgressUpdate()
                        } else {
                            stopProgressUpdate()
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Timber.e(error, "播放器错误")
                        notifyError("播放错误: ${error.message}")
                    }
                })
            }
            
            Timber.d("ExoPlayer 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "初始化 ExoPlayer 失败")
            notifyError("初始化播放器失败: ${e.message}")
        }
    }
    
    /**
     * 加载音频文件
     * 
     * @param audioUrl 音频文件 URL（支持 file://, http://, https://, asset:// 等）
     */
    fun loadAudio(audioUrl: String) {
        Timber.d("加载音频: $audioUrl")
        
        try {
            initializePlayer()
            
            currentAudioUrl = audioUrl
            
            // 创建 MediaItem
            val mediaItem = MediaItem.fromUri(audioUrl)
            
            // 设置媒体项
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
            }
            
            Timber.d("音频加载成功")
        } catch (e: Exception) {
            Timber.e(e, "加载音频失败")
            notifyError("加载音频失败: ${e.message}")
        }
    }
    
    /**
     * 播放
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * 暂停
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * 停止
     */
    fun stop() {
        exoPlayer?.apply {
            stop()
            seekTo(0)
        }
        stopProgressUpdate()
    }
    
    /**
     * 跳转到指定位置
     * 
     * @param positionMs 位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * 获取当前播放位置
     * 
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }
    
    /**
     * 获取总时长
     * 
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }
    
    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
    
    /**
     * 设置播放速度
     * 
     * @param speed 播放速度（0.5 - 2.0）
     */
    fun setSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        
        exoPlayer?.playbackParameters = PlaybackParameters(clampedSpeed)
        
        Timber.d("播放速度设置为: $clampedSpeed")
    }
    
    /**
     * 设置循环播放范围
     * 
     * @param startMs 开始位置（毫秒）
     * @param endMs 结束位置（毫秒）
     */
    fun setLoopRange(startMs: Long, endMs: Long) {
        loopStartMs = startMs
        loopEndMs = endMs
        isLooping = true
        
        Timber.d("设置循环范围: $startMs - $endMs ms")
    }
    
    /**
     * 取消循环
     */
    fun clearLoop() {
        isLooping = false
        loopStartMs = 0
        loopEndMs = 0
        
        Timber.d("取消循环")
    }
    
    /**
     * 添加播放监听器
     */
    fun addListener(listener: PlaybackListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * 移除播放监听器
     */
    fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }
    
    /**
     * 开始进度更新
     */
    private fun startProgressUpdate() {
        stopProgressUpdate()
        
        progressJob = progressScope.launch {
            while (isActive) {
                val position = getCurrentPosition()
                
                // 通知位置变化
                notifyPositionChanged(position)
                
                // 检查循环播放
                if (isLooping && loopEndMs > 0 && position >= loopEndMs) {
                    seekTo(loopStartMs)
                }
                
                // 每 50ms 更新一次（20fps）
                delay(50)
            }
        }
    }
    
    /**
     * 停止进度更新
     */
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
    
    /**
     * 通知位置变化
     */
    private fun notifyPositionChanged(positionMs: Long) {
        listeners.forEach { it.onPositionChanged(positionMs) }
    }
    
    /**
     * 通知播放状态变化
     */
    private fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        listeners.forEach { it.onPlaybackStateChanged(isPlaying) }
    }
    
    /**
     * 通知播放完成
     */
    private fun notifyCompleted() {
        listeners.forEach { it.onCompleted() }
    }
    
    /**
     * 通知错误
     */
    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        Timber.d("释放 AudioPlaybackManager 资源")
        
        stopProgressUpdate()
        
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
        
        listeners.clear()
        currentAudioUrl = null
    }
}
