package com.decibelmeter.app.data.repository

import com.decibelmeter.app.data.local.AudioCaptureEngine
import com.decibelmeter.app.domain.model.DecibelLevel
import com.decibelmeter.app.domain.model.NoiseCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分贝测量数据仓库
 * 参考: iama2z/Glyph-Decibel-Meter 的数据管道设计
 */
@Singleton
class DecibelRepository @Inject constructor(
    private val audioCaptureEngine: AudioCaptureEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 首页数据状态
    private val _currentLevel = MutableStateFlow(DecibelLevel())
    val currentLevel: StateFlow<DecibelLevel> = _currentLevel.asStateFlow()

    // 测量进行中
    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    // 历史数据 (最近 100 个采样点)
    private val _history = MutableStateFlow<List<Float>>(emptyList())
    val history: StateFlow<List<Float>> = _history.asStateFlow()

    private var captureJob: Job? = null
    private var sumDb: Float = 0f
    private var maxDb: Float = Float.MIN_VALUE
    private var minDb: Float = Float.MAX_VALUE
    private var count: Long = 0
    private val historyBuffer = mutableListOf<Float>()

    /** 开始测量 */
    fun startMeasuring() {
        if (_isMeasuring.value) return

        // 重置统计值
        resetStats()
        _isMeasuring.value = true

        captureJob = scope.launch {
            try {
                audioCaptureEngine.startCapturing().collect { db ->
                    // 更新统计数据
                    sumDb += db
                    maxDb = maxOf(maxDb, db)
                    minDb = minOf(minDb, db)
                    count++

                    // 添加到历史缓冲区
                    historyBuffer.add(db)
                    if (historyBuffer.size > 100) {
                        historyBuffer.removeAt(0)
                    }
                    _history.value = historyBuffer.toList()

                    // 发射更新
                    _currentLevel.value = DecibelLevel(
                        currentDb = db,
                        maxDb = if (maxDb == Float.MIN_VALUE) db else maxDb,
                        minDb = if (minDb == Float.MAX_VALUE) db else minDb,
                        avgDb = if (count > 0) sumDb / count else db,
                        sampleCount = count
                    )
                }
            } catch (_: Exception) {
                // 采集被中断
            } finally {
                _isMeasuring.value = false
            }
        }
    }

    /** 停止测量 */
    fun stopMeasuring() {
        captureJob?.cancel()
        captureJob = null
        audioCaptureEngine.stopCapturing()
        _isMeasuring.value = false
    }

    /** 重置统计数据，保留历史 */
    fun resetStats() {
        sumDb = 0f
        maxDb = Float.MIN_VALUE
        minDb = Float.MAX_VALUE
        count = 0
        _currentLevel.value = DecibelLevel()
    }

    /** 清除历史 */
    fun clearHistory() {
        historyBuffer.clear()
        _history.value = emptyList()
    }

    /** 设置校准偏移 */
    fun setCalibration(offsetDb: Float) {
        audioCaptureEngine.setCalibrationOffset(offsetDb)
    }

    fun getCalibration(): Float = audioCaptureEngine.getCalibrationOffset()

    /** 释放资源 */
    fun release() {
        stopMeasuring()
        scope.cancel()
    }
}
