package com.decibelmeter.app.ui.hardware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decibelmeter.app.data.repository.HardwareRepository
import com.decibelmeter.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HardwareUiState(
    val hardwareInfo: HardwareInfo = HardwareInfo(),
    val isTesting: Boolean = false,
    val testProgress: String = "",
    val selectedTest: String? = null
)

@HiltViewModel
class HardwareViewModel @Inject constructor(
    private val repository: HardwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HardwareUiState())
    val uiState: StateFlow<HardwareUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.hardwareInfo.collect { info ->
                _uiState.update { it.copy(hardwareInfo = info) }
            }
        }
    }

    /** 运行所有检测 */
    fun runAllTests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testProgress = "正在检测...") }

            repository.runAllTests()

            _uiState.update {
                it.copy(isTesting = false, testProgress = "检测完成")
            }
        }
    }

    /** 单独检测麦克风 */
    fun testMicrophone() {
        _uiState.update { it.copy(selectedTest = "mic") }
        repository.testMicrophone()
    }

    /** 单独检测扬声器 + 播放测试音 */
    fun testSpeaker() {
        _uiState.update { it.copy(selectedTest = "speaker") }
        repository.testSpeaker()
        repository.playTestTone()
    }

    /** 检测传感器 */
    fun testSensors() {
        repository.detectSensors()
    }

    /** 检测电池 */
    fun testBattery() {
        repository.detectBattery()
    }

    /** 检测存储 */
    fun testStorage() {
        repository.detectStorage()
    }

    /** 检测网络 */
    fun testNetwork() {
        repository.detectNetwork()
    }

    /** 检测屏幕 */
    fun testDisplay() {
        repository.detectDisplay()
    }

    /** 检测设备信息 */
    fun testDevice() {
        repository.detectDevice()
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
