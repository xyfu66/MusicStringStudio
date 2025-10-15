package com.example.musicstringstudioapp.domain.score

import com.example.musicstringstudioapp.data.model.Measure
import com.example.musicstringstudioapp.data.model.Note
import com.example.musicstringstudioapp.data.model.Song
import timber.log.Timber

/**
 * 曲谱跟随器
 * 
 * 根据播放进度跟踪当前演奏位置，定位目标音符
 * 负责曲谱的时间定位和自动翻页
 */
class ScoreFollower(
    private val song: Song
) {
    
    // 当前播放位置（毫秒）
    private var currentPositionMs: Long = 0
    
    // 当前小节索引
    private var currentMeasureIndex: Int = 0
    
    // 当前音符索引（在当前小节内）
    private var currentNoteIndexInMeasure: Int = 0
    
    // 状态监听器列表
    private val listeners = mutableListOf<ScoreFollowerListener>()
    
    /**
     * 曲谱跟随监听器
     */
    interface ScoreFollowerListener {
        /**
         * 当前音符变化
         * @param note 当前应该演奏的音符，null 表示没有音符
         * @param measureNumber 小节号
         */
        fun onCurrentNoteChanged(note: Note?, measureNumber: Int) {}
        
        /**
         * 小节变化
         * @param measure 当前小节
         * @param measureIndex 小节索引
         */
        fun onMeasureChanged(measure: Measure, measureIndex: Int) {}
        
        /**
         * 播放进度更新
         * @param positionMs 当前位置（毫秒）
         * @param totalMs 总时长（毫秒）
         * @param progress 进度（0.0 - 1.0）
         */
        fun onProgressUpdate(positionMs: Long, totalMs: Long, progress: Float) {}
    }
    
    /**
     * 更新播放位置
     * 
     * @param positionMs 当前播放位置（毫秒）
     */
    fun updatePosition(positionMs: Long) {
        if (currentPositionMs == positionMs) return
        
        currentPositionMs = positionMs
        
        // 定位当前小节
        updateCurrentMeasure()
        
        // 定位当前音符
        updateCurrentNote()
        
        // 通知进度更新
        notifyProgressUpdate()
    }
    
    /**
     * 获取当前应该演奏的音符
     */
    fun getCurrentNote(): Note? {
        if (currentMeasureIndex >= song.measures.size) {
            return null
        }
        
        val currentMeasure = song.measures[currentMeasureIndex]
        
        if (currentNoteIndexInMeasure >= currentMeasure.notes.size) {
            return null
        }
        
        val note = currentMeasure.notes[currentNoteIndexInMeasure]
        
        // 检查音符是否在当前时间窗口内
        if (currentPositionMs >= note.startTime && 
            currentPositionMs < note.startTime + note.duration) {
            return note
        }
        
        return null
    }
    
    /**
     * 获取当前小节
     */
    fun getCurrentMeasure(): Measure? {
        return if (currentMeasureIndex < song.measures.size) {
            song.measures[currentMeasureIndex]
        } else {
            null
        }
    }
    
    /**
     * 获取当前小节号
     */
    fun getCurrentMeasureNumber(): Int {
        return getCurrentMeasure()?.measureNumber ?: 0
    }
    
    /**
     * 获取下一个音符（预览）
     */
    fun getNextNote(): Note? {
        val currentMeasure = getCurrentMeasure() ?: return null
        
        // 检查当前小节是否还有下一个音符
        if (currentNoteIndexInMeasure + 1 < currentMeasure.notes.size) {
            return currentMeasure.notes[currentNoteIndexInMeasure + 1]
        }
        
        // 检查下一个小节
        if (currentMeasureIndex + 1 < song.measures.size) {
            val nextMeasure = song.measures[currentMeasureIndex + 1]
            return nextMeasure.notes.firstOrNull()
        }
        
        return null
    }
    
    /**
     * 获取指定时间窗口内的所有音符
     * 
     * @param startMs 开始时间（毫秒）
     * @param endMs 结束时间（毫秒）
     * @return 时间窗口内的音符列表
     */
    fun getNotesInRange(startMs: Long, endMs: Long): List<Note> {
        return song.getAllNotes().filter { note ->
            // 音符的时间范围与查询范围有重叠
            val noteEndTime = note.startTime + note.duration
            !(noteEndTime <= startMs || note.startTime >= endMs)
        }
    }
    
    /**
     * 跳转到指定位置
     * 
     * @param positionMs 目标位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        currentPositionMs = positionMs
        updateCurrentMeasure()
        updateCurrentNote()
        notifyProgressUpdate()
    }
    
    /**
     * 重置到开始位置
     */
    fun reset() {
        currentPositionMs = 0
        currentMeasureIndex = 0
        currentNoteIndexInMeasure = 0
        
        if (song.measures.isNotEmpty()) {
            notifyMeasureChanged(song.measures[0], 0)
        }
    }
    
    /**
     * 获取总时长
     */
    fun getTotalDuration(): Long {
        return song.getTotalDuration()
    }
    
    /**
     * 获取当前进度（0.0 - 1.0）
     */
    fun getProgress(): Float {
        val total = getTotalDuration()
        return if (total > 0) {
            (currentPositionMs.toFloat() / total).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: ScoreFollowerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: ScoreFollowerListener) {
        listeners.remove(listener)
    }
    
    /**
     * 更新当前小节
     */
    private fun updateCurrentMeasure() {
        val newMeasureIndex = findMeasureIndexAtTime(currentPositionMs)
        
        if (newMeasureIndex != currentMeasureIndex && newMeasureIndex >= 0) {
            currentMeasureIndex = newMeasureIndex
            currentNoteIndexInMeasure = 0
            
            if (currentMeasureIndex < song.measures.size) {
                notifyMeasureChanged(
                    song.measures[currentMeasureIndex],
                    currentMeasureIndex
                )
            }
        }
    }
    
    /**
     * 更新当前音符
     */
    private fun updateCurrentNote() {
        val currentMeasure = getCurrentMeasure() ?: return
        
        // 在当前小节中查找当前音符
        for (i in currentMeasure.notes.indices) {
            val note = currentMeasure.notes[i]
            
            if (currentPositionMs >= note.startTime && 
                currentPositionMs < note.startTime + note.duration) {
                
                if (currentNoteIndexInMeasure != i) {
                    currentNoteIndexInMeasure = i
                    notifyCurrentNoteChanged(note, currentMeasure.measureNumber)
                }
                return
            }
        }
        
        // 如果没有找到，可能在音符之间的间隙
        // 通知当前没有音符
        notifyCurrentNoteChanged(null, currentMeasure.measureNumber)
    }
    
    /**
     * 查找指定时间所在的小节索引
     */
    private fun findMeasureIndexAtTime(timeMs: Long): Int {
        for (i in song.measures.indices) {
            val measure = song.measures[i]
            if (measure.containsTime(timeMs)) {
                return i
            }
        }
        
        // 如果时间超出最后一个小节，返回最后一个小节
        return if (timeMs >= getTotalDuration() && song.measures.isNotEmpty()) {
            song.measures.size - 1
        } else {
            0
        }
    }
    
    /**
     * 通知当前音符变化
     */
    private fun notifyCurrentNoteChanged(note: Note?, measureNumber: Int) {
        Timber.d("当前音符变化: ${note?.pitch ?: "无"}, 小节: $measureNumber")
        listeners.forEach { it.onCurrentNoteChanged(note, measureNumber) }
    }
    
    /**
     * 通知小节变化
     */
    private fun notifyMeasureChanged(measure: Measure, measureIndex: Int) {
        Timber.d("小节变化: ${measure.measureNumber}")
        listeners.forEach { it.onMeasureChanged(measure, measureIndex) }
    }
    
    /**
     * 通知进度更新
     */
    private fun notifyProgressUpdate() {
        val total = getTotalDuration()
        val progress = getProgress()
        listeners.forEach { 
            it.onProgressUpdate(currentPositionMs, total, progress) 
        }
    }
}
