package com.decibelmeter.app.domain.model

/**
 * 助眠声音数据模型
 * 参考: Tosencen/XMSLEEP 的多音轨混音设计
 */
data class SleepSound(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: SoundCategory,
    val resId: Int = 0,           // 内置资源的 raw id
    val iconName: String,          // Material Icons 名称
    val isBuiltIn: Boolean = true,
    val isPlaying: Boolean = false,
    val volume: Float = 0.5f       // 0.0 - 1.0
)

enum class SoundCategory(val label: String) {
    NATURE("自然"),
    WATER("水声"),
    AMBIENT("环境"),
    INSTRUMENT("乐器"),
    WHITE_NOISE("白噪音")
}

/**
 * 预设场景
 */
data class SleepPreset(
    val id: String,
    val name: String,
    val sounds: List<PresetSound>,
    val timerMinutes: Int = 0
)

data class PresetSound(
    val soundId: String,
    val volume: Float
)

/**
 * 睡眠定时器状态
 */
data class SleepTimer(
    val isActive: Boolean = false,
    val totalMinutes: Int = 0,
    val remainingMillis: Long = 0,
    val formattedTime: String = ""
)

/** 内置助眠声音库 */
object BuiltInSounds {
    val ALL = listOf(
        SleepSound("rain", "雨声", "Rain", SoundCategory.WATER, iconName = "water_drop"),
        SleepSound("ocean", "海浪", "Ocean Waves", SoundCategory.WATER, iconName = "waves"),
        SleepSound("stream", "溪流", "Stream", SoundCategory.WATER, iconName = "water"),
        SleepSound("forest", "森林", "Forest", SoundCategory.NATURE, iconName = "forest"),
        SleepSound("birds", "鸟鸣", "Birds", SoundCategory.NATURE, iconName = "raven"),
        SleepSound("cricket", "蟋蟀", "Cricket", SoundCategory.NATURE, iconName = "bug_report"),
        SleepSound("campfire", "篝火", "Campfire", SoundCategory.NATURE, iconName = "local_fire_department"),
        SleepSound("thunder", "雷声", "Thunder", SoundCategory.NATURE, iconName = "thunderstorm"),
        SleepSound("wind", "风声", "Wind", SoundCategory.NATURE, iconName = "air"),
        SleepSound("white_noise", "白噪音", "White Noise", SoundCategory.WHITE_NOISE, iconName = "graphic_eq"),
        SleepSound("pink_noise", "粉红噪音", "Pink Noise", SoundCategory.WHITE_NOISE, iconName = "show_chart"),
        SleepSound("brown_noise", "棕色噪音", "Brown Noise", SoundCategory.WHITE_NOISE, iconName = "bar_chart"),
        SleepSound("fan", "风扇", "Fan", SoundCategory.AMBIENT, iconName = "mode_fan"),
        SleepSound("clock", "钟表", "Clock", SoundCategory.AMBIENT, iconName = "schedule"),
        SleepSound("piano", "钢琴", "Piano", SoundCategory.INSTRUMENT, iconName = "piano"),
        SleepSound("meditation", "冥想", "Meditation", SoundCategory.AMBIENT, iconName = "self_improvement"),
    )
}
