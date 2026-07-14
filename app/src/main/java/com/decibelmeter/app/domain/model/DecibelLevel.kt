package com.decibelmeter.app.domain.model

/**
 * 分贝测量数据模型
 * 参考: iama2z/Glyph-Decibel-Meter 的 dB 数据流设计
 */
data class DecibelLevel(
    val currentDb: Float = 0f,
    val maxDb: Float = 0f,
    val minDb: Float = Float.MAX_VALUE,
    val avgDb: Float = 0f,
    val sampleCount: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** 噪音等级分类 (参考 OSHA 和 WHO 噪音标准) */
    val noiseCategory: NoiseCategory
        get() = when {
            currentDb < 30f -> NoiseCategory.QUIET
            currentDb < 50f -> NoiseCategory.NORMAL
            currentDb < 65f -> NoiseCategory.MODERATE
            currentDb < 80f -> NoiseCategory.LOUD
            currentDb < 100f -> NoiseCategory.VERY_LOUD
            else -> NoiseCategory.DANGEROUS
        }
}

/**
 * 噪音等级分类
 * 参考 OSHA 职业噪音暴露标准:
 * - 85dB 以上持续 8 小时会导致听力损伤
 * - 100dB 以上持续 15 分钟就会造成损伤
 */
enum class NoiseCategory(
    val label: String,
    val description: String,
    val colorArgb: Long,    // 用于 UI 颜色指示
    val iconName: String
) {
    QUIET("安静", "适合睡眠的环境", 0xFF4CAF50, "volume_mute"),
    NORMAL("正常", "安静的室内环境", 0xFF8BC34A, "volume_down"),
    MODERATE("中等", "正常交谈音量", 0xFFFFC107, "volume_up"),
    LOUD("嘈杂", "繁忙街道音量", 0xFFFF9800, "volume_up"),
    VERY_LOUD("很吵", "长时间暴露可能损伤听力", 0xFFFF5722, "hearing"),
    DANGEROUS("危险!", "立即离开或佩戴耳罩", 0xFFF44336, "hearing_disabled")
}
