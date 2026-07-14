package com.decibelmeter.app.data.local

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 音频采集引擎 - 分贝计算核心
 *
 * 参考:
 * - iama2z/Glyph-Decibel-Meter: AudioRecord → RMS → dBFS pipeline
 * - albertopasqualetto/SoundMeterESP: 实时 dB 采集架构
 *
 * 技术路线:
 * 1. AudioRecord 采集 16-bit PCM 原始音频
 * 2. 计算 RMS (Root Mean Square) 振幅
 * 3. 转换为 dBFS (Decibels relative to Full Scale)
 * 4. 应用校准偏移量得到近似 SPL (Sound Pressure Level)
 *
 * 注意: 手机麦克风未经校准，得到的 dB 值是近似参考值。
 * 可通过与标准声级计对比来设置校准偏移量。
 */
@Singleton
class AudioCaptureEngine @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 44100          // 采样率 Hz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2   // 缓冲区倍增因子
        private const val REFERENCE_AMPLITUDE = 32768.0 // 16-bit PCM 最大振幅 (2^15)
    }

    private var audioRecord: AudioRecord? = null
    private var bufferSize: Int = 0
    // 默认偏移量 90dB，将 dBFS (负值) 转换为近似 dB SPL (正值)
    // 例如: -40 dBFS + 90 = 50 dB (正常室内环境)
    // 用户可通过校准界面微调此偏移量
    private var calibrationOffset: Float = 90f  // dB 校准偏移

    /**
     * 设置校准偏移量
     * 用户可通过与标准声级计对比进行校准
     */
    fun setCalibrationOffset(offsetDb: Float) {
        calibrationOffset = offsetDb
    }

    fun getCalibrationOffset(): Float = calibrationOffset

    /**
     * 开始采集音频并发射实时 dB 值
     * 返回 Flow<Float>，每轮采集发射一次当前 dB 值
     *
     * 参考 Glyph-Decibel-Meter 的 Kotlin Flow 设计
     */
    fun startCapturing(): Flow<Float> = flow {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        this@AudioCaptureEngine.bufferSize = bufferSize

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("无法获取有效的缓冲区大小")
        }

        val audioRecord = withContext(Dispatchers.IO) {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * BUFFER_SIZE_MULTIPLIER
            )
        }

        this@AudioCaptureEngine.audioRecord = audioRecord

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord 初始化失败")
        }

        val audioData = ShortArray(bufferSize)

        try {
            audioRecord.startRecording()

            while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord.read(audioData, 0, bufferSize)

                if (bytesRead > 0) {
                    val rms = calculateRMS(audioData, bytesRead)
                    var db = rmsToDb(rms)

                    // 限制最小值，避免 -Infinity
                    if (db.isInfinite() || db.isNaN()) {
                        db = 0f
                    }

                    // 应用校准偏移
                    db += calibrationOffset

                    emit(db)
                }
            }
        } catch (e: Exception) {
            // AudioRecord 被中断
            throw e
        } finally {
            audioRecord.stop()
            audioRecord.release()
            this@AudioCaptureEngine.audioRecord = null
        }
    }

    /** 停止采集 */
    fun stopCapturing() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }
    }

    /**
     * 计算 RMS (Root Mean Square)
     * RMS = √(Σ(xi²) / N)
     * 其中 xi 为每个采样值，N 为采样数
     */
    private fun calculateRMS(audioData: ShortArray, readSize: Int): Double {
        var sum = 0.0
        for (i in 0 until readSize) {
            val sample = audioData[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / readSize)
    }

    /**
     * RMS 转 dBFS
     * dBFS = 20 * log₁₀(rms / reference)
     * reference = 32768 (2^15, 16-bit PCM 最大值)
     *
     * 参考: Glyph-Decibel-Meter 的 DbCalculator.rmsToDb()
     */
    private fun rmsToDb(rms: Double): Float {
        // 防止 log10(0) → -Infinity
        val normalized = if (rms <= 0.0) {
            (1.0 / REFERENCE_AMPLITUDE)
        } else {
            rms / REFERENCE_AMPLITUDE
        }
        return (20.0 * log10(normalized)).toFloat()
    }
}
