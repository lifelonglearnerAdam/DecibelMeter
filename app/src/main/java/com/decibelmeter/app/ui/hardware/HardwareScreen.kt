package com.decibelmeter.app.ui.hardware

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.decibelmeter.app.domain.model.*
import com.decibelmeter.app.ui.theme.*
import com.decibelmeter.app.util.FormatUtil

/**
 * 硬件检测页面
 * 参考: ahmmedrejowan/DeviceInfo 的 UI 设计
 * 参考: rohanagarwal94/Hardware-testing 的测试项组织
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("硬件检测", fontWeight = FontWeight.Bold)
            },
            actions = {
                // 一键检测所有
                TextButton(onClick = { viewModel.runAllTests() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("一键检测")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // 检测进度
        if (uiState.isTesting) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Secondary
            )
            Text(
                text = uiState.testProgress,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 设备基本信息
            item {
                DeviceInfoCard(info = uiState.hardwareInfo.device)
            }

            // 麦克风
            item {
                TestItemCard(
                    title = "麦克风",
                    icon = Icons.Filled.Mic,
                    status = when (uiState.hardwareInfo.microphone) {
                        MicStatus.WORKING -> TestStatus.PASS
                        MicStatus.NOT_WORKING -> TestStatus.FAIL
                        MicStatus.TESTING -> TestStatus.TESTING
                        MicStatus.UNKNOWN -> TestStatus.UNKNOWN
                    },
                    onTest = { viewModel.testMicrophone() }
                )
            }

            // 扬声器
            item {
                TestItemCard(
                    title = "扬声器",
                    icon = Icons.Filled.VolumeUp,
                    status = when (uiState.hardwareInfo.speaker) {
                        SpeakerStatus.WORKING -> TestStatus.PASS
                        SpeakerStatus.NOT_WORKING -> TestStatus.FAIL
                        SpeakerStatus.TESTING -> TestStatus.TESTING
                        SpeakerStatus.UNKNOWN -> TestStatus.UNKNOWN
                    },
                    onTest = { viewModel.testSpeaker() },
                    subtitle = "点击检测可播放测试音"
                )
            }

            // 电池信息
            item {
                BatteryCard(info = uiState.hardwareInfo.battery)
            }

            // 存储信息
            item {
                StorageCard(info = uiState.hardwareInfo.storage)
            }

            // 屏幕信息
            item {
                DisplayCard(info = uiState.hardwareInfo.display)
            }

            // 网络信息
            item {
                NetworkCard(info = uiState.hardwareInfo.network)
            }

            // 传感器列表
            item {
                Text(
                    text = "传感器 (${uiState.hardwareInfo.sensors.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.hardwareInfo.sensors.isEmpty()) {
                item {
                    Text(
                        text = "点击刷新获取传感器信息",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.testSensors() }) {
                        Text("读取传感器")
                    }
                }
            } else {
                items(uiState.hardwareInfo.sensors) { sensor ->
                    SensorItemCard(sensor = sensor)
                }
            }
        }
    }
}

// ── 子组件 ──

enum class TestStatus { UNKNOWN, PASS, FAIL, TESTING }

@Composable
private fun TestItemCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: TestStatus,
    onTest: () -> Unit,
    subtitle: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTest),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(status = status)
        }
    }
}

@Composable
private fun StatusBadge(status: TestStatus) {
    val (text, color) = when (status) {
        TestStatus.PASS -> "正常" to AccentGreen
        TestStatus.FAIL -> "异常" to AccentRed
        TestStatus.TESTING -> "检测中" to AccentYellow
        TestStatus.UNKNOWN -> "未检测" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun DeviceInfoCard(info: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${info.manufacturer} ${info.model}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("系统版本", "Android ${info.androidVersion} (API ${info.sdkLevel})")
            InfoRow("CPU 架构", info.cpuAbi)
        }
    }
}

@Composable
private fun BatteryCard(info: BatteryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔋 电池",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${info.levelPercent}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        info.levelPercent > 60 -> AccentGreen
                        info.levelPercent > 20 -> AccentYellow
                        else -> AccentRed
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { info.levelPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    info.levelPercent > 60 -> AccentGreen
                    info.levelPercent > 20 -> AccentYellow
                    else -> AccentRed
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("状态", info.status)
            InfoRow("温度", "${info.temperature}°C")
            InfoRow("电压", "${info.voltage}mV")
            if (info.technology.isNotEmpty()) {
                InfoRow("技术", info.technology)
            }
            InfoRow("健康", info.health)
        }
    }
}

@Composable
private fun StorageCard(info: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💾 存储",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("RAM 总计", FormatUtil.formatBytes(info.totalRamBytes))
            InfoRow("RAM 可用", FormatUtil.formatBytes(info.availableRamBytes))
            InfoRow("RAM 已用", "${info.usedRamPercent}%")
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("存储总计", FormatUtil.formatBytes(info.totalInternalBytes))
            InfoRow("存储可用", FormatUtil.formatBytes(info.availableInternalBytes))
            InfoRow("存储已用", "${info.usedStoragePercent}%")
        }
    }
}

@Composable
private fun DisplayCard(info: DisplayInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📱 屏幕",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("分辨率", "${info.widthPixels} × ${info.heightPixels}")
            InfoRow("像素密度", "${info.densityDpi} dpi")
            InfoRow("刷新率", "${info.refreshRate} Hz")
            InfoRow("屏幕尺寸", "%.1f 英寸".format(info.screenSizeInches))
        }
    }
}

@Composable
private fun NetworkCard(info: NetworkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🌐 网络",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "WiFi",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            InfoRow("状态", if (info.isWifiEnabled) "已开启" else "已关闭")
            if (info.wifiSsid.isNotEmpty()) {
                InfoRow("SSID", info.wifiSsid)
                InfoRow("IP", info.wifiIp)
                InfoRow("信号", "${info.wifiSignalStrength} dBm (${FormatUtil.wifiSignalDescription(info.wifiSignalStrength)})")
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "蓝牙",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            InfoRow("状态", if (info.isBluetoothEnabled) "已开启" else "已关闭")
            if (info.bluetoothName.isNotEmpty()) {
                InfoRow("名称", info.bluetoothName)
            }
            if (info.bluetoothMac.isNotEmpty()) {
                InfoRow("MAC", info.bluetoothMac)
            }
        }
    }
}

@Composable
private fun SensorItemCard(sensor: SensorItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Sensors,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${sensor.vendor} · v${sensor.version}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${sensor.power}mA",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
