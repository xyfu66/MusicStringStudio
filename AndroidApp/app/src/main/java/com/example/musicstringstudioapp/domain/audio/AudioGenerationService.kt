package com.example.musicstringstudioapp.domain.audio

import android.content.Context
import com.example.musicstringstudioapp.data.model.Song
import com.example.musicstringstudioapp.data.parser.MusicXMLParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * 音频生成服务
 * 
 * 将 MusicXML 转换为 MIDI 文件，供播放使用
 */
class AudioGenerationService(
    private val context: Context
) {
    
    private val musicXMLParser = MusicXMLParser()
    private val midiConverter = MusicXMLToMidiConverter()
    
    /**
     * 从 MusicXML 资源生成 MIDI 文件
     * 
     * @param resourceId MusicXML 资源 ID (R.raw.xxx)
     * @return MIDI 文件路径，失败返回 null
     */
    suspend fun generateMidiFromResource(
        resourceId: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. 解析 MusicXML
            val song = musicXMLParser.parseFromResource(context, resourceId)
            Timber.d("解析 MusicXML: ${song.title}")
            
            // 2. 生成 MIDI 文件
            val midiFile = File(context.cacheDir, "${song.id}.mid")
            midiConverter.convertToMidi(song, midiFile)
            
            Timber.d("MIDI 生成成功: ${midiFile.absolutePath}")
            Result.success(midiFile)
            
        } catch (e: Exception) {
            Timber.e(e, "MIDI 生成失败")
            Result.failure(e)
        }
    }
    
    /**
     * 从 MusicXML 文件生成 MIDI 文件
     * 
     * @param musicXMLFile MusicXML 文件
     * @return MIDI 文件路径，失败返回 null
     */
    suspend fun generateMidiFromFile(
        musicXMLFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始转换: ${musicXMLFile.name}")
            
            // 1. 解析 MusicXML
            val song = musicXMLParser.parseFromFile(musicXMLFile.absolutePath)
            Timber.d("MusicXML 解析完成: ${song.title}")
            
            // 2. 生成 MIDI 文件
            val midiFile = File(context.cacheDir, "${song.id}.mid")
            midiConverter.convertToMidi(song, midiFile)
            
            Timber.d("MIDI 文件生成: ${midiFile.absolutePath}")
            Result.success(midiFile)
            
        } catch (e: Exception) {
            Timber.e(e, "音频生成失败")
            Result.failure(e)
        }
    }
    
    /**
     * 从 Song 对象生成 MIDI 文件
     * 
     * @param song Song 对象
     * @return MIDI 文件路径
     */
    suspend fun generateMidiFromSong(
        song: Song
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val midiFile = File(context.cacheDir, "${song.id}.mid")
            midiConverter.convertToMidi(song, midiFile)
            
            Timber.d("MIDI 生成成功: ${midiFile.absolutePath}")
            Result.success(midiFile)
            
        } catch (e: Exception) {
            Timber.e(e, "MIDI 生成失败")
            Result.failure(e)
        }
    }
    
    /**
     * 批量生成 MIDI 文件
     * 
     * @param musicXMLFiles MusicXML 文件列表
     * @param outputDir 输出目录
     * @return 转换结果映射
     */
    suspend fun batchGenerate(
        musicXMLFiles: List<File>,
        outputDir: File
    ): Map<File, Result<File>> = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        
        musicXMLFiles.associateWith { xmlFile ->
            try {
                val song = musicXMLParser.parseFromFile(xmlFile.absolutePath)
                val midiFile = File(outputDir, "${xmlFile.nameWithoutExtension}.mid")
                midiConverter.convertToMidi(song, midiFile)
                Result.success(midiFile)
            } catch (e: Exception) {
                Timber.e(e, "批量转换失败: ${xmlFile.name}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取 MIDI 文件的播放 URL
     * 
     * @param midiFile MIDI 文件
     * @return 可用于 AudioPlaybackManager 的 URL
     */
    fun getMidiPlaybackUrl(midiFile: File): String {
        return midiFile.absolutePath
    }
    
    /**
     * 清理缓存的 MIDI 文件
     */
    fun clearCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.extension == "mid") {
                    file.delete()
                    Timber.d("删除缓存 MIDI 文件: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理缓存失败")
        }
    }
}
