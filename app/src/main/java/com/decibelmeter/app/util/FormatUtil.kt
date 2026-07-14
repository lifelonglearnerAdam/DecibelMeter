package com.decibelmeter.app.util

import java.text.DecimalFormat
import java.util.Locale

/**
 * 格式化工具类
 */
object FormatUtil {

    private val dbFormat = DecimalFormat("##0.0")

    /** 格式化分贝值 */
    fun formatDb(db: Float): String {
        return try {
            dbFormat.format(db.coerceIn(-99f, 150f))
        } catch (_: Exception) {
            "0.0"
        }
    }

    /** 格式化时间 (毫秒 → mm:ss) */
    fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /** 格式化存储大小 */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(Locale.getDefault(), "%.2f MB", bytes / 1_048_576.0)
            bytes >= 1024L -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /** WiFi 信号强度描述 */
    fun wifiSignalDescription(rssi: Int): String {
        return when {
            rssi >= -50 -> "优秀"
            rssi >= -60 -> "良好"
            rssi >= -70 -> "一般"
            rssi >= -80 -> "较弱"
            rssi != 0 -> "很差"
            else -> "无信号"
        }
    }
}
