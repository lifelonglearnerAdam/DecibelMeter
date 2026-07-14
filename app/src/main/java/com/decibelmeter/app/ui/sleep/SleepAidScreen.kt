package com.decibelmeter.app.ui.sleep

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.decibelmeter.app.data.local.SleepAudioPlayer
import com.decibelmeter.app.domain.model.*
import com.decibelmeter.app.ui.components.LabeledSlider
import com.decibelmeter.app.ui.theme.*
import com.decibelmeter.app.util.FormatUtil

/**
 * 助眠音乐页面
 * 参考: Tosencen/XMSLEEP ⭐1.3k 的 Material3 多音轨混音 UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAidScreen(
    viewModel: SleepAidViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    "助眠音乐",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // 定时器按钮
                IconButton(onClick = { viewModel.showTimerDialog() }) {
                    BadgedBox(
                        badge = {
                            if (uiState.timerState is SleepAudioPlayer.TimerState.Running) {
                                Badge { Text("●") }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = "定时器")
                    }
                }
                // 预设按钮
                IconButton(onClick = { viewModel.showPresetDialog() }) {
                    Icon(Icons.Filled.Bookmarks, contentDescription = "预设")
                }
                // 停止所有
                if (uiState.isAnyPlaying) {
                    IconButton(onClick = { viewModel.stopAll() }) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "停止",
                            tint = AccentRed
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // 定时器状态条
        AnimatedVisibility(visible = uiState.timerState is SleepAudioPlayer.TimerState.Running) {
            val timerState = uiState.timerState as? SleepAudioPlayer.TimerState.Running
            if (timerState != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Secondary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱ 定时剩余: ${FormatUtil.formatTime(timerState.remainingMillis)}",
                            fontSize = 14.sp,
                            color = Secondary
                        )
                        TextButton(onClick = { viewModel.cancelTimer() }) {
                            Text("取消", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 正在播放的声音
        if (uiState.activeSoundIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在播放: ${uiState.activeSoundIds.size} 个声音",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { viewModel.stopAll() }) {
                        Text("停止全部", fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 分类标签
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("全部") }
                )
            }
            items(SoundCategory.values()) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = {
                        viewModel.selectCategory(
                            if (uiState.selectedCategory == category) null else category
                        )
                    },
                    label = { Text(category.label) }
                )
            }
        }

        // 声音列表
        val groupedSounds = viewModel.getFilteredSounds()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedSounds.forEach { (category, sounds) ->
                item {
                    Text(
                        text = category.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(sounds, key = { it.id }) { sound ->
                    val isActive = uiState.activeSoundIds.contains(sound.id)
                    SoundItem(
                        sound = sound,
                        isActive = isActive,
                        onToggle = { viewModel.toggleSound(sound, 0) },
                        onVolumeChange = { volume -> viewModel.setVolume(sound.id, volume) }
                    )
                }
            }
        }
    }

    // 定时器对话框
    if (uiState.showTimerDialog) {
        TimerDialog(
            onDismiss = { viewModel.hideTimerDialog() },
            onSelect = { minutes -> viewModel.startTimer(minutes) }
        )
    }

    // 预设对话框
    if (uiState.showPresetDialog) {
        PresetDialog(
            presets = uiState.presets,
            onDismiss = { viewModel.hidePresetDialog() },
            onSave = { name -> viewModel.savePreset(name) },
            onLoad = { preset -> viewModel.loadPreset(preset) }
        )
    }
}

/**
 * 单个声音项
 * 参考 XMSLEEP 的声音卡片设计
 */
@Composable
private fun SoundItem(
    sound: SleepSound,
    isActive: Boolean,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var volume by remember(sound.id) { mutableStateOf(sound.volume) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 声音图标
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSoundIcon(sound.id),
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sound.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${sound.nameEn} · ${sound.category.label}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 播放状态指示
                if (isActive) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = "播放中",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 音量滑块 (仅在激活时显示)
            AnimatedVisibility(visible = isActive) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledSlider(
                        label = "音量",
                        value = volume,
                        onValueChange = { newVolume ->
                            volume = newVolume
                            onVolumeChange(newVolume)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 定时器对话框
 * 参考 XMSLEEP 的睡眠定时器 UI
 */
@Composable
private fun TimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        "关闭" to 0,
        "15分钟" to 15,
        "30分钟" to 30,
        "45分钟" to 45,
        "60分钟" to 60,
        "90分钟" to 90,
        "120分钟" to 120
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时器", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "设定时间后自动停止播放",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { (label, minutes) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 预设对话框
 */
@Composable
private fun PresetDialog(
    presets: List<SleepPreset>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onLoad: (SleepPreset) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预设场景", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 保存当前
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    placeholder = { Text("如：睡前组合") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (presetName.isNotBlank()) onSave(presetName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存当前预设")
                }

                // 已保存的预设
                if (presets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "已保存的预设",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLoad(preset) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = preset.name,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (preset.timerMinutes > 0) {
                                    Text(
                                        text = "${preset.timerMinutes}分",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/** 简单的声音图标映射 */
@Composable
private fun getSoundIcon(soundId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (soundId) {
        "rain" -> Icons.Filled.WaterDrop
        "ocean" -> Icons.Filled.Waves
        "stream" -> Icons.Filled.Water
        "forest" -> Icons.Filled.Forest
        "birds" -> Icons.Filled.Park
        "campfire" -> Icons.Filled.LocalFireDepartment
        "thunder" -> Icons.Filled.Thunderstorm
        "wind" -> Icons.Filled.Air
        "fan" -> Icons.Filled.AcUnit
        "clock" -> Icons.Filled.Schedule
        "piano" -> Icons.Filled.Piano
        "white_noise", "pink_noise", "brown_noise" -> Icons.Filled.GraphicEq
        else -> Icons.Filled.MusicNote
    }
}
