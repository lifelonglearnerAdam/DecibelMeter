package com.decibelmeter.app.data.local

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.decibelmeter.app.domain.model.SleepSound
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 助眠音频播放器
 *
 * 参考: Tosencen/XMSLEEP ⭐1.3k 的 Media3/ExoPlayer 多音轨架构
 *
 * 使用 Media3 ExoPlayer 实现:
 * - 多音轨独立播放和混音
 * - 无缝循环播放
 * - 独立音量控制
 * - 睡眠定时器
 */
@Singleton
class SleepAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 每个声音独立一个 ExoPlayer 实例以实现多轨混音 + 独立音量
    private val players = mutableMapOf<String, ExoPlayer>()
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _activeSounds = MutableStateFlow<Map<String, SleepSound>>(emptyMap())
    val activeSounds: StateFlow<Map<String, SleepSound>> = _activeSounds.asStateFlow()

    private val _isAnyPlaying = MutableStateFlow(false)
    val isAnyPlaying: StateFlow<Boolean> = _isAnyPlaying.asStateFlow()

    private var timerJob: Job? = null
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    sealed class TimerState {
        data object Idle : TimerState()
        data class Running(val remainingMillis: Long, val totalMillis: Long) : TimerState()
        data class Finished(val reason: String) : TimerState()
    }

    /**
     * 播放一个助眠声音
     * 可以同时播放多个声音（多轨混音）
     */
    fun play(sound: SleepSound, rawResId: Int) {
        if (players.containsKey(sound.id)) return // 已在播放

        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("asset:///android_asset/raw/${sound.id}.mp3")
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL   // 无缝循环 (XMSLEEP 核心特性)
            volume = sound.volume
            prepare()
            playWhenReady = true
        }

        players[sound.id] = player
        updateActiveSounds()
        _isAnyPlaying.value = true
    }

    /**
     * 停止播放指定声音
     */
    fun stop(soundId: String) {
        players[soundId]?.let { player ->
            player.stop()
            player.release()
        }
        players.remove(soundId)
        updateActiveSounds()

        if (players.isEmpty()) {
            _isAnyPlaying.value = false
        }
    }

    /** 停止所有声音 */
    fun stopAll() {
        players.values.forEach { player ->
            player.stop()
            player.release()
        }
        players.clear()
        _isAnyPlaying.value = false
        updateActiveSounds()
        cancelTimer()
    }

    /** 设置指定声音的音量 (0.0 - 1.0) */
    fun setVolume(soundId: String, volume: Float) {
        players[soundId]?.volume = volume
    }

    /** 设置总音量 */
    fun setMasterVolume(volume: Float) {
        players.values.forEach { it.volume = volume }
    }

    /**
     * 启动睡眠定时器
     * 参考 XMSLEEP 的倒计时定时器设计
     */
    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalMillis = minutes * 60 * 1000L

        timerJob = playerScope.launch {
            var remaining = totalMillis
            while (remaining > 0) {
                _timerState.value = TimerState.Running(remaining, totalMillis)
                delay(1000L)
                remaining -= 1000L
            }
            // 时间到，停止所有播放
            stopAll()
            _timerState.value = TimerState.Finished("定时结束，已自动停止播放")
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerState.Idle
    }

    private fun updateActiveSounds() {
        _activeSounds.value = players.keys.associateWith { id ->
            SleepSound(id = id, name = id, nameEn = id,
                category = com.decibelmeter.app.domain.model.SoundCategory.AMBIENT,
                iconName = "", isPlaying = true)
        }
    }

    fun release() {
        stopAll()
        playerScope.cancel()
    }
}
