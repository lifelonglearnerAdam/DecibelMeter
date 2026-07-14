package com.decibelmeter.app.data.repository

import com.decibelmeter.app.data.local.HardwareDetector
import com.decibelmeter.app.domain.model.HardwareInfo
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 硬件检测数据仓库
 * 参考: ahmmedrejowan/DeviceInfo 的数据架构
 */
@Singleton
class HardwareRepository @Inject constructor(
    private val detector: HardwareDetector
) {
    val hardwareInfo: StateFlow<HardwareInfo> = detector.hardwareInfo

    /** 运行全部硬件检测 */
    suspend fun runAllTests(): HardwareInfo = detector.runAllTests()

    /** 仅检测麦克风 */
    fun testMicrophone() = detector.detectMicrophone()

    /** 仅检测扬声器 */
    fun testSpeaker() = detector.detectSpeaker()

    /** 播放测试音 */
    fun playTestTone(durationMs: Int = 500) = detector.playTestTone(durationMs)

    /** 单独检测传感器 */
    fun detectSensors() = detector.detectSensors()

    /** 单独检测电池 */
    fun detectBattery() = detector.detectBattery()

    /** 单独检测存储 */
    fun detectStorage() = detector.detectStorage()

    /** 单独检测网络 */
    fun detectNetwork() = detector.detectNetwork()

    /** 单独检测屏幕 */
    fun detectDisplay() = detector.detectDisplay()

    /** 单独检测设备信息 */
    fun detectDevice() = detector.detectDevice()

    fun release() = detector.release()
}
