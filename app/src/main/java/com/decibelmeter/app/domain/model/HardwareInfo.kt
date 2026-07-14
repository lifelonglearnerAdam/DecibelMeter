package com.decibelmeter.app.domain.model

/**
 * 硬件检测数据模型
 * 参考: ahmmedrejowan/DeviceInfo 的设备信息架构
 */
data class HardwareInfo(
    val microphone: MicStatus = MicStatus.UNKNOWN,
    val speaker: SpeakerStatus = SpeakerStatus.UNKNOWN,
    val sensors: List<SensorItem> = emptyList(),
    val battery: BatteryInfo = BatteryInfo(),
    val storage: StorageInfo = StorageInfo(),
    val network: NetworkInfo = NetworkInfo(),
    val display: DisplayInfo = DisplayInfo(),
    val device: DeviceInfo = DeviceInfo()
)

enum class MicStatus { UNKNOWN, WORKING, NOT_WORKING, TESTING }
enum class SpeakerStatus { UNKNOWN, WORKING, NOT_WORKING, TESTING }

data class SensorItem(
    val name: String,
    val vendor: String,
    val version: String,
    val power: Float,
    val type: Int
)

data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val temperature: Float = 0f,
    val voltage: Int = 0,
    val technology: String = "",
    val health: String = "",
    val status: String = "",
    val isCharging: Boolean = false
) {
    val levelPercent: Int get() = if (scale > 0) (level * 100) / scale else 0
}

data class StorageInfo(
    val totalRamBytes: Long = 0,
    val availableRamBytes: Long = 0,
    val totalInternalBytes: Long = 0,
    val availableInternalBytes: Long = 0
) {
    val usedRamPercent: Int
        get() = if (totalRamBytes > 0) ((totalRamBytes - availableRamBytes) * 100 / totalRamBytes).toInt() else 0
    val usedStoragePercent: Int
        get() = if (totalInternalBytes > 0) ((totalInternalBytes - availableInternalBytes) * 100 / totalInternalBytes).toInt() else 0
}

data class NetworkInfo(
    val wifiSsid: String = "",
    val wifiIp: String = "",
    val wifiSignalStrength: Int = 0,
    val bluetoothName: String = "",
    val bluetoothMac: String = "",
    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false
)

data class DisplayInfo(
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
    val densityDpi: Int = 0,
    val refreshRate: Float = 60f,
    val screenSizeInches: Float = 0f
)

data class DeviceInfo(
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val sdkLevel: Int = 0,
    val cpuAbi: String = ""
)
