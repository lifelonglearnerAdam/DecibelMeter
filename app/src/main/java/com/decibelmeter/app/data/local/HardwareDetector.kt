package com.decibelmeter.app.data.local

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.decibelmeter.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 硬件检测器
 * 参考: ahmmedrejowan/DeviceInfo (高星) 的硬件检测架构
 * 参考: rohanagarwal94/Hardware-testing 的测试项设计
 */
@Singleton
class HardwareDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _hardwareInfo = MutableStateFlow(HardwareInfo())
    val hardwareInfo: StateFlow<HardwareInfo> = _hardwareInfo.asStateFlow()

    private val detectorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 运行全部硬件检测 */
    suspend fun runAllTests(): HardwareInfo = withContext(Dispatchers.IO) {
        detectMicrophone()
        detectSpeaker()
        detectSensors()
        detectBattery()
        detectStorage()
        detectNetwork()
        detectDisplay()
        detectDevice()

        _hardwareInfo.value
    }

    /**
     * 麦克风检测
     * 尝试初始化 AudioRecord 来判断麦克风是否可用
     */
    fun detectMicrophone() {
        _hardwareInfo.value = _hardwareInfo.value.copy(microphone = MicStatus.TESTING)

        val status = try {
            val bufferSize = AudioRecord.getMinBufferSize(
                44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                MicStatus.NOT_WORKING
            } else {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    MicStatus.WORKING
                } else {
                    recorder.release()
                    MicStatus.NOT_WORKING
                }
            }
        } catch (_: Exception) {
            MicStatus.NOT_WORKING
        }

        _hardwareInfo.value = _hardwareInfo.value.copy(microphone = status)
    }

    /**
     * 扬声器检测
     * 使用 AudioManager 和 ToneGenerator 检测扬声器
     */
    fun detectSpeaker() {
        _hardwareInfo.value = _hardwareInfo.value.copy(speaker = SpeakerStatus.TESTING)

        val status = try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            // 检查扬声器是否可用
            val hasSpeaker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                audioManager?.let { am ->
                    val hasBuiltInSpeaker = try {
                        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                            .any { device -> device.type == 2 } // TYPE_BUILTIN_SPEAKER = 2
                    } catch (_: Exception) { true }
                    hasBuiltInSpeaker
                } ?: true
            } else {
                true // API 23 以下默认认为有扬声器
            }

            if (hasSpeaker) SpeakerStatus.WORKING else SpeakerStatus.NOT_WORKING
        } catch (_: Exception) {
            SpeakerStatus.NOT_WORKING
        }

        _hardwareInfo.value = _hardwareInfo.value.copy(speaker = status)
    }

    /** 播放测试音 */
    fun playTestTone(durationMs: Int = 500) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, durationMs)
            // ToneGenerator 会在 durationMs 后自动停止
            detectorScope.launch {
                delay(durationMs + 200L)
                toneGen.release()
            }
        } catch (_: Exception) {
            // 扬声器不可用
        }
    }

    /** 检测所有传感器 */
    fun detectSensors() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL)?.map { sensor ->
            SensorItem(
                name = sensor.name,
                vendor = sensor.vendor,
                version = sensor.version.toString(),
                power = sensor.power,
                type = sensor.type
            )
        } ?: emptyList()

        _hardwareInfo.value = _hardwareInfo.value.copy(sensors = sensors)
    }

    /** 检测电池信息 - 通过 BatteryManager 获取 */
    @Suppress("DEPRECATION")
    fun detectBattery() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val info = if (batteryManager != null) {
            // BATTERY_PROPERTY_CAPACITY = 4
            val level = batteryManager.getIntProperty(4)
            // Get battery info from sticky intent (compatible approach)
            val batteryIntent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val temp = (batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val volt = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val tech = batteryIntent?.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY) ?: ""
            val healthVal = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, 0) ?: 0
            val statusVal = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, 0) ?: 0
            val plugged = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) ?: 0

            BatteryInfo(
                level = level,
                scale = 100,
                temperature = temp,
                voltage = volt,
                technology = tech,
                health = getBatteryHealth(healthVal, batteryManager),
                status = getBatteryStatus(statusVal, batteryManager),
                isCharging = (statusVal == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                    || statusVal == android.os.BatteryManager.BATTERY_STATUS_FULL)
            )
        } else {
            BatteryInfo()
        }
        _hardwareInfo.value = _hardwareInfo.value.copy(battery = info)
    }

    /** 检测存储信息 */
    fun detectStorage() {
        val runtime = Runtime.getRuntime()
        val totalRam = runtime.totalMemory() + (runtime.maxMemory() - runtime.totalMemory())
        val freeRam = runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalInternal = statFs.totalBytes
        val availableInternal = statFs.availableBytes

        _hardwareInfo.value = _hardwareInfo.value.copy(
            storage = StorageInfo(
                totalRamBytes = totalRam,
                availableRamBytes = freeRam,
                totalInternalBytes = totalInternal,
                availableInternalBytes = availableInternal
            )
        )
    }

    /** 检测网络信息 */
    fun detectNetwork() {
        val wifiManager = context.applicationContext.getSystemService(
            Context.WIFI_SERVICE
        ) as? WifiManager

        val wifiInfo = wifiManager?.connectionInfo
        val wifiEnabled = wifiManager?.isWifiEnabled ?: false

        val bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(BluetoothAdapter::class.java)
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        val btEnabled = bluetoothAdapter?.isEnabled ?: false
        val btName = bluetoothAdapter?.name ?: ""
        val btMac = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter?.address ?: ""
            } else ""
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.address ?: ""
        }

        _hardwareInfo.value = _hardwareInfo.value.copy(
            network = NetworkInfo(
                wifiSsid = wifiInfo?.ssid?.removeSurrounding("\"") ?: "",
                wifiIp = wifiInfo?.let {
                    val ip = it.ipAddress
                    "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
                } ?: "",
                wifiSignalStrength = wifiInfo?.rssi ?: 0,
                bluetoothName = btName,
                bluetoothMac = btMac,
                isWifiEnabled = wifiEnabled,
                isBluetoothEnabled = btEnabled
            )
        )
    }

    /** 检测屏幕信息 */
    fun detectDisplay() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(dm)

        val widthPx = dm.widthPixels
        val heightPx = dm.heightPixels
        val density = dm.densityDpi

        val x = widthPx.toDouble() / density
        val y = heightPx.toDouble() / density
        val screenInches = kotlin.math.sqrt(x * x + y * y)

        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            windowManager?.defaultDisplay?.mode?.refreshRate ?: 60f
        } else 60f

        _hardwareInfo.value = _hardwareInfo.value.copy(
            display = DisplayInfo(
                widthPixels = widthPx,
                heightPixels = heightPx,
                densityDpi = density,
                refreshRate = refreshRate,
                screenSizeInches = screenInches.toFloat()
            )
        )
    }

    /** 检测设备基本信息 */
    fun detectDevice() {
        _hardwareInfo.value = _hardwareInfo.value.copy(
            device = DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                sdkLevel = Build.VERSION.SDK_INT,
                cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
            )
        )
    }

    private fun getBatteryHealth(healthVal: Int, manager: BatteryManager): String {
        return try {
            when (healthVal) {
                android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
                android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
                android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
                android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
                android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "未知故障"
                android.os.BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
                else -> "未知"
            }
        } catch (_: Exception) {
            "未知"
        }
    }

    private fun getBatteryStatus(statusVal: Int, manager: BatteryManager): String {
        return try {
            when (statusVal) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知"
            }
        } catch (_: Exception) {
            "未知"
        }
    }

    private fun isBatteryCharging(manager: BatteryManager): Boolean {
        return try {
            val status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {
            false
        }
    }

    fun release() {
        detectorScope.cancel()
    }
}
