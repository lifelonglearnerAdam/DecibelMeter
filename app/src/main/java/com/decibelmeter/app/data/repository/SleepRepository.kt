package com.decibelmeter.app.data.repository

import com.decibelmeter.app.data.local.SleepAudioPlayer
import com.decibelmeter.app.domain.model.*
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 助眠音乐数据仓库
 * 参考: Tosencen/XMSLEEP 的数据管理架构
 */
@Singleton
class SleepRepository @Inject constructor(
    private val audioPlayer: SleepAudioPlayer
) {
    /** 所有可用的助眠声音 */
    val allSounds: List<SleepSound> = BuiltInSounds.ALL

    /** 当前激活的声音 */
    val activeSounds: StateFlow<Map<String, SleepSound>> = audioPlayer.activeSounds

    /** 是否有声音在播放 */
    val isAnyPlaying: StateFlow<Boolean> = audioPlayer.isAnyPlaying

    /** 定时器状态 */
    val timerState: StateFlow<SleepAudioPlayer.TimerState> = audioPlayer.timerState

    /** 已保存的预设 */
    private val _presets = mutableListOf<SleepPreset>()
    private val maxPresets = 3

    /** 播放声音 */
    fun playSound(sound: SleepSound, rawResId: Int) {
        audioPlayer.play(sound, rawResId)
    }

    /** 停止指定声音 */
    fun stopSound(soundId: String) {
        audioPlayer.stop(soundId)
    }

    /** 停止所有声音 */
    fun stopAll() {
        audioPlayer.stopAll()
    }

    /** 调节声音音量 */
    fun setVolume(soundId: String, volume: Float) {
        audioPlayer.setVolume(soundId, volume.coerceIn(0f, 1f))
    }

    /** 启动定时器 */
    fun startTimer(minutes: Int) {
        audioPlayer.startTimer(minutes)
    }

    /** 取消定时器 */
    fun cancelTimer() {
        audioPlayer.cancelTimer()
    }

    /** 保存预设 */
    fun savePreset(preset: SleepPreset) {
        _presets.removeAll { it.id == preset.id }
        if (_presets.size >= maxPresets) {
            _presets.removeAt(0) // 移除最旧的
        }
        _presets.add(preset)
    }

    /** 获取所有预设 */
    fun getPresets(): List<SleepPreset> = _presets.toList()

    /** 释放资源 */
    fun release() {
        audioPlayer.release()
    }
}
