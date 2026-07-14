package com.decibelmeter.app.ui.meter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.decibelmeter.app.ui.components.DecibelGauge
import com.decibelmeter.app.ui.components.StatCard
import com.decibelmeter.app.ui.components.WaveformChart
import com.decibelmeter.app.ui.theme.*
import com.decibelmeter.app.util.FormatUtil
import com.decibelmeter.app.util.PermissionHelper

/**
 * 分贝仪主页面
 * 参考: iama2z/Glyph-Decibel-Meter 的 UI 布局
 * 参考: albertopasqualetto/SoundMeterESP 的图表集成
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecibelMeterScreen(
    viewModel: DecibelMeterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 录音权限
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasRecordAudioPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.toggleMeasuring()
        }
    }

    // 校准对话框
    var showCalibration by remember { mutableStateOf(false) }
    var calibrationValue by remember { mutableStateOf(uiState.calibration.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "分贝仪",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                IconButton(onClick = { showCalibration = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = "校准")
                }
                IconButton(onClick = { viewModel.reset() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重置")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 分贝仪表盘
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DecibelGauge(
                    currentDb = uiState.decibelLevel.currentDb,
                    size = 220
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 开始/停止按钮
                Button(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleMeasuring()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isMeasuring) AccentRed else Primary
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isMeasuring) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isMeasuring) "停止测量" else "开始测量",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 权限提示
                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "需要录音权限才能测量分贝",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 统计数据卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "最大 dB",
                value = FormatUtil.formatDb(uiState.decibelLevel.maxDb),
                modifier = Modifier.weight(1f),
                color = AccentRed
            )
            StatCard(
                label = "最小 dB",
                value = FormatUtil.formatDb(uiState.decibelLevel.minDb),
                modifier = Modifier.weight(1f),
                color = AccentGreen
            )
            StatCard(
                label = "平均 dB",
                value = FormatUtil.formatDb(uiState.decibelLevel.avgDb),
                modifier = Modifier.weight(1f),
                color = Secondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 历史波形图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "实时波形",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("清除", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                WaveformChart(
                    data = uiState.history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    lineColor = Secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 噪音等级参考
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "噪音等级参考",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                NoiseLevelBar("0-30 dB", "安静（适合睡眠）", AccentGreen)
                NoiseLevelBar("30-50 dB", "正常（室内环境）", Color(0xFF8BC34A))
                NoiseLevelBar("50-65 dB", "中等（正常交谈）", AccentYellow)
                NoiseLevelBar("65-80 dB", "嘈杂（繁忙街道）", AccentOrange)
                NoiseLevelBar("80-100 dB", "很吵（可能损伤听力）", Color(0xFFFF5722))
                NoiseLevelBar("100+ dB", "危险（立即防护）", AccentRed)
            }
        }
    }

    // 校准对话框
    if (showCalibration) {
        AlertDialog(
            onDismissRequest = { showCalibration = false },
            title = { Text("校准设置") },
            text = {
                Column {
                    Text(
                        text = "使用标准声级计进行对比，输入偏移量来校准。\n例如：标准声级计显示 70dB，APP 显示 65dB，则输入 +5。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = calibrationValue,
                        onValueChange = { calibrationValue = it },
                        label = { Text("偏移量 (dB)") },
                        placeholder = { Text("-10 ~ +10") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    calibrationValue.toFloatOrNull()?.let {
                        viewModel.setCalibration(it)
                    }
                    showCalibration = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibration = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun NoiseLevelBar(range: String, description: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .then(
                    Modifier.background(
                        color = color,
                        shape = RoundedCornerShape(2.dp)
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = range,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
