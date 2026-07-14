package com.decibelmeter.app.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decibelmeter.app.data.repository.DecibelRepository
import com.decibelmeter.app.domain.model.DecibelLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeterUiState(
    val decibelLevel: DecibelLevel = DecibelLevel(),
    val isMeasuring: Boolean = false,
    val history: List<Float> = emptyList(),
    val calibration: Float = 0f,
    val showCalibrationDialog: Boolean = false
)

@HiltViewModel
class DecibelMeterViewModel @Inject constructor(
    private val repository: DecibelRepository
) : ViewModel() {

    val uiState: StateFlow<MeterUiState> = combine(
        repository.currentLevel,
        repository.isMeasuring,
        repository.history
    ) { level, measuring, history ->
        MeterUiState(
            decibelLevel = level,
            isMeasuring = measuring,
            history = history,
            calibration = repository.getCalibration()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeterUiState()
    )

    fun toggleMeasuring() {
        if (uiState.value.isMeasuring) {
            repository.stopMeasuring()
        } else {
            repository.startMeasuring()
        }
    }

    fun reset() {
        repository.resetStats()
    }

    fun clearHistory() {
        repository.clearHistory()
    }

    fun showCalibrationDialog() {
        viewModelScope.launch {
            // 通过更新状态显示对话框
        }
    }

    fun setCalibration(offsetDb: Float) {
        repository.setCalibration(offsetDb)
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
