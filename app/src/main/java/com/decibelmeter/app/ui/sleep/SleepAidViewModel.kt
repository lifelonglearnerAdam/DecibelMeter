package com.decibelmeter.app.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decibelmeter.app.data.local.SleepAudioPlayer
import com.decibelmeter.app.data.repository.SleepRepository
import com.decibelmeter.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SleepUiState(
    val sounds: List<SleepSound> = BuiltInSounds.ALL,
    val activeSoundIds: Set<String> = emptySet(),
    val isAnyPlaying: Boolean = false,
    val timerState: SleepAudioPlayer.TimerState = SleepAudioPlayer.TimerState.Idle,
    val presets: List<SleepPreset> = emptyList(),
    val selectedCategory: SoundCategory? = null,
    val showTimerDialog: Boolean = false,
    val showPresetDialog: Boolean = false
)

@HiltViewModel
class SleepAidViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        // 监听播放状态
        viewModelScope.launch {
            repository.activeSounds.collect { active ->
                _uiState.update {
                    it.copy(
                        activeSoundIds = active.keys,
                        isAnyPlaying = active.isNotEmpty()
                    )
                }
            }
        }
        // 监听定时器
        viewModelScope.launch {
            repository.timerState.collect { timerState ->
                _uiState.update { it.copy(timerState = timerState) }
            }
        }
    }

    fun toggleSound(sound: SleepSound, rawResId: Int) {
        if (_uiState.value.activeSoundIds.contains(sound.id)) {
            repository.stopSound(sound.id)
        } else {
            repository.playSound(sound, rawResId)
        }
    }

    fun setVolume(soundId: String, volume: Float) {
        repository.setVolume(soundId, volume)
    }

    fun stopAll() {
        repository.stopAll()
    }

    fun selectCategory(category: SoundCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun showTimerDialog() {
        _uiState.update { it.copy(showTimerDialog = true) }
    }

    fun hideTimerDialog() {
        _uiState.update { it.copy(showTimerDialog = false) }
    }

    fun startTimer(minutes: Int) {
        repository.startTimer(minutes)
        _uiState.update { it.copy(showTimerDialog = false) }
    }

    fun cancelTimer() {
        repository.cancelTimer()
    }

    fun showPresetDialog() {
        _uiState.update { it.copy(showPresetDialog = true) }
    }

    fun hidePresetDialog() {
        _uiState.update { it.copy(showPresetDialog = false) }
    }

    fun savePreset(name: String) {
        val active = _uiState.value.activeSoundIds
        if (active.isEmpty()) return

        val preset = SleepPreset(
            id = System.currentTimeMillis().toString(),
            name = name,
            sounds = active.map { soundId ->
                PresetSound(soundId = soundId, volume = 0.5f)
            }
        )
        repository.savePreset(preset)
        _uiState.update {
            it.copy(
                presets = repository.getPresets(),
                showPresetDialog = false
            )
        }
    }

    fun loadPreset(preset: SleepPreset) {
        stopAll()
        preset.sounds.forEach { ps ->
            val sound = repository.allSounds.find { it.id == ps.soundId }
            if (sound != null) {
                // 需要 raw resource mapping, 这里简化为直接播放
                // 实际项目中需要维护 soundId → rawResId 映射
            }
        }
    }

    /** 获取分类后的声音列表 */
    fun getFilteredSounds(): Map<SoundCategory, List<SleepSound>> {
        val category = _uiState.value.selectedCategory
        val sounds = if (category != null) {
            repository.allSounds.filter { it.category == category }
        } else {
            repository.allSounds
        }
        return sounds.groupBy { it.category }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
