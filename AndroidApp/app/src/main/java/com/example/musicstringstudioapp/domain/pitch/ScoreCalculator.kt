package com.example.musicstringstudioapp.domain.pitch

import com.example.musicstringstudioapp.data.model.Note
import timber.log.Timber
import kotlin.math.min

/**
 * 评分计算器
 * 
 * 根据音准比对结果计算练习得分
 * 提供多维度的评分和建议
 */
class ScoreCalculator {
    
    companion object {
        // 评分权重
        private const val PITCH_ACCURACY_WEIGHT = 0.6f    // 音准权重 60%
        private const val TIMING_ACCURACY_WEIGHT = 0.3f   // 节奏权重 30%
        private const val COMPLETION_WEIGHT = 0.1f        // 完成度权重 10%
        
        // 各等级分值
        private const val PERFECT_SCORE = 100f
        private const val GOOD_SCORE = 80f
        private const val FAIR_SCORE = 60f
        private const val POOR_SCORE = 40f
        private const val MISS_SCORE = 0f
    }
    
    /**
     * 练习评分结果
     */
    data class PracticeScore(
        val totalScore: Int,                        // 总分（0-100）
        val pitchAccuracyScore: Float,              // 音准得分
        val timingAccuracyScore: Float,             // 节奏得分
        val completionScore: Float,                 // 完成度得分
        val grade: ScoreGrade,                      // 评级
        val statistics: PitchStatistics.StatisticsReport,  // 统计数据
        val strengths: List<String>,                // 优点
        val weaknesses: List<String>,               // 需要改进的地方
        val suggestions: List<String>               // 建议
    )
    
    /**
     * 评级
     */
    enum class ScoreGrade {
        S,      // 90-100
        A,      // 80-89
        B,      // 70-79
        C,      // 60-69
        D       // < 60
    }
    
    /**
     * 计算练习得分
     * 
     * @param results 比对结果列表
     * @param totalExpectedNotes 预期演奏的总音符数
     * @return 练习评分结果
     */
    fun calculateScore(
        results: List<PitchComparator.ComparisonResult>,
        totalExpectedNotes: Int
    ): PracticeScore {
        
        Timber.d("开始计算得分: 演奏 ${results.size} 个音符，预期 $totalExpectedNotes 个")
        
        // 1. 创建统计器并生成报告
        val statistics = PitchStatistics().apply {
            results.forEach { addResult(it) }
        }.generateReport()
        
        // 2. 计算音准得分
        val pitchScore = calculatePitchAccuracyScore(results)
        
        // 3. 计算节奏得分
        val timingScore = calculateTimingAccuracyScore(results)
        
        // 4. 计算完成度得分
        val completionScore = calculateCompletionScore(results.size, totalExpectedNotes)
        
        // 5. 计算总分
        val totalScore = (
            pitchScore * PITCH_ACCURACY_WEIGHT +
            timingScore * TIMING_ACCURACY_WEIGHT +
            completionScore * COMPLETION_WEIGHT
        ).toInt().coerceIn(0, 100)
        
        // 6. 确定评级
        val grade = determineGrade(totalScore)
        
        // 7. 分析优点和缺点
        val strengths = analyzeStrengths(statistics, totalScore)
        val weaknesses = analyzeWeaknesses(statistics, totalScore)
        val suggestions = generateSuggestions(statistics, weaknesses)
        
        Timber.d("得分计算完成: 总分=$totalScore, 评级=$grade")
        
        return PracticeScore(
            totalScore = totalScore,
            pitchAccuracyScore = pitchScore,
            timingAccuracyScore = timingScore,
            completionScore = completionScore,
            grade = grade,
            statistics = statistics,
            strengths = strengths,
            weaknesses = weaknesses,
            suggestions = suggestions
        )
    }
    
    /**
     * 计算音准得分
     */
    private fun calculatePitchAccuracyScore(
        results: List<PitchComparator.ComparisonResult>
    ): Float {
        if (results.isEmpty()) return 0f
        
        val totalScore = results.sumOf { result ->
            when (result.accuracy) {
                PitchComparator.PitchAccuracy.PERFECT -> PERFECT_SCORE.toDouble()
                PitchComparator.PitchAccuracy.GOOD -> GOOD_SCORE.toDouble()
                PitchComparator.PitchAccuracy.FAIR -> FAIR_SCORE.toDouble()
                PitchComparator.PitchAccuracy.POOR -> POOR_SCORE.toDouble()
                PitchComparator.PitchAccuracy.MISS -> MISS_SCORE.toDouble()
            }
        }
        
        return (totalScore / results.size).toFloat()
    }
    
    /**
     * 计算节奏得分
     */
    private fun calculateTimingAccuracyScore(
        results: List<PitchComparator.ComparisonResult>
    ): Float {
        if (results.isEmpty()) return 0f
        
        val inTimeWindow = results.count { it.isInTimeWindow }
        val baseScore = (inTimeWindow.toFloat() / results.size) * 100
        
        // 考虑时间误差的大小
        val avgTimingError = results.map { kotlin.math.abs(it.timingErrorMs) }.average()
        val timingPenalty = when {
            avgTimingError < 50 -> 0f
            avgTimingError < 100 -> 5f
            avgTimingError < 150 -> 10f
            else -> 15f
        }
        
        return (baseScore - timingPenalty).coerceAtLeast(0f)
    }
    
    /**
     * 计算完成度得分
     */
    private fun calculateCompletionScore(
        playedNotes: Int,
        totalExpectedNotes: Int
    ): Float {
        if (totalExpectedNotes == 0) return 100f
        
        val completionRate = min(playedNotes.toFloat() / totalExpectedNotes, 1f)
        return completionRate * 100
    }
    
    /**
     * 确定评级
     */
    private fun determineGrade(totalScore: Int): ScoreGrade {
        return when {
            totalScore >= 90 -> ScoreGrade.S
            totalScore >= 80 -> ScoreGrade.A
            totalScore >= 70 -> ScoreGrade.B
            totalScore >= 60 -> ScoreGrade.C
            else -> ScoreGrade.D
        }
    }
    
    /**
     * 分析优点
     */
    private fun analyzeStrengths(
        statistics: PitchStatistics.StatisticsReport,
        totalScore: Int
    ): List<String> {
        val strengths = mutableListOf<String>()
        
        if (totalScore >= 90) {
            strengths.add("整体表现优秀")
        }
        
        if (statistics.accuracyRate >= 0.9f) {
            strengths.add("音准控制出色")
        }
        
        if (statistics.averageDeviation < 15f) {
            strengths.add("音高稳定性良好")
        }
        
        if (statistics.perfectCount >= statistics.totalNotes * 0.5) {
            strengths.add("完美音符比例高")
        }
        
        if (strengths.isEmpty()) {
            strengths.add("继续保持练习")
        }
        
        return strengths
    }
    
    /**
     * 分析缺点
     */
    private fun analyzeWeaknesses(
        statistics: PitchStatistics.StatisticsReport,
        totalScore: Int
    ): List<String> {
        val weaknesses = mutableListOf<String>()
        
        if (statistics.accuracyRate < 0.7f) {
            weaknesses.add("音准准确率偏低")
        }
        
        if (statistics.averageDeviation > 25f) {
            weaknesses.add("音高偏差较大")
        }
        
        if (statistics.sharpRate > 0.5f) {
            weaknesses.add("音高整体偏高")
        } else if (statistics.flatRate > 0.5f) {
            weaknesses.add("音高整体偏低")
        }
        
        if (statistics.poorCount >= statistics.totalNotes * 0.3) {
            weaknesses.add("较差音符比例过高")
        }
        
        return weaknesses
    }
    
    /**
     * 生成建议
     */
    private fun generateSuggestions(
        statistics: PitchStatistics.StatisticsReport,
        weaknesses: List<String>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (weaknesses.contains("音准准确率偏低")) {
            suggestions.add("使用慢速模式练习，专注于音准")
            suggestions.add("可以先练习音阶，熟悉音高")
        }
        
        if (weaknesses.contains("音高整体偏高")) {
            suggestions.add("注意降低音高，可能手指位置偏前")
        } else if (weaknesses.contains("音高整体偏低")) {
            suggestions.add("注意提高音高，可能手指位置偏后")
        }
        
        if (weaknesses.contains("音高偏差较大")) {
            suggestions.add("使用调音器进行校准练习")
            suggestions.add("循环练习单个小节，提高稳定性")
        }
        
        if (statistics.averageDeviation > 30f) {
            suggestions.add("建议降低速度至0.5x-0.75x")
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("继续保持，尝试提高练习速度")
            suggestions.add("可以挑战更难的曲目")
        }
        
        return suggestions
    }
    
    /**
     * 获取评级文本
     */
    fun getGradeText(grade: ScoreGrade): String {
        return when (grade) {
            ScoreGrade.S -> "S - 卓越"
            ScoreGrade.A -> "A - 优秀"
            ScoreGrade.B -> "B - 良好"
            ScoreGrade.C -> "C - 及格"
            ScoreGrade.D -> "D - 需要加强"
        }
    }
    
    /**
     * 获取鼓励语
     */
    fun getEncouragement(grade: ScoreGrade): String {
        return when (grade) {
            ScoreGrade.S -> "太棒了！你已经掌握得非常好！"
            ScoreGrade.A -> "做得很好！继续保持！"
            ScoreGrade.B -> "不错！还有进步的空间。"
            ScoreGrade.C -> "继续努力，你能做得更好！"
            ScoreGrade.D -> "不要气馁，多练习就会进步！"
        }
    }
}

/**
 * 进度跟踪器
 * 
 * 跟踪用户的练习进度和改进
 */
class ProgressTracker {
    
    private val historyScores = mutableListOf<ScoreCalculator.PracticeScore>()
    
    /**
     * 添加练习记录
     */
    fun addScore(score: ScoreCalculator.PracticeScore) {
        historyScores.add(score)
    }
    
    /**
     * 获取历史平均分
     */
    fun getAverageScore(): Float {
        if (historyScores.isEmpty()) return 0f
        return historyScores.map { it.totalScore }.average().toFloat()
    }
    
    /**
     * 获取最高分
     */
    fun getHighestScore(): Int {
        return historyScores.maxOfOrNull { it.totalScore } ?: 0
    }
    
    /**
     * 获取进步趋势
     * 
     * @return 正值表示进步，负值表示退步
     */
    fun getProgressTrend(): Float {
        if (historyScores.size < 2) return 0f
        
        val recent = historyScores.takeLast(5)
        val earlier = historyScores.dropLast(5).takeLast(5)
        
        if (earlier.isEmpty()) return 0f
        
        val recentAvg = recent.map { it.totalScore }.average()
        val earlierAvg = earlier.map { it.totalScore }.average()
        
        return (recentAvg - earlierAvg).toFloat()
    }
    
    /**
     * 生成进度报告
     */
    fun generateProgressReport(): ProgressReport {
        return ProgressReport(
            totalPractices = historyScores.size,
            averageScore = getAverageScore(),
            highestScore = getHighestScore(),
            progressTrend = getProgressTrend(),
            recentScores = historyScores.takeLast(10).map { it.totalScore }
        )
    }
    
    data class ProgressReport(
        val totalPractices: Int,
        val averageScore: Float,
        val highestScore: Int,
        val progressTrend: Float,
        val recentScores: List<Int>
    )
}
