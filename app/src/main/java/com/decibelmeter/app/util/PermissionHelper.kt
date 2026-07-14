package com.decibelmeter.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 权限辅助类
 */
object PermissionHelper {

    /** 分贝仪所需的权限 */
    val METER_PERMISSIONS: Array<String>
        get() = arrayOf(Manifest.permission.RECORD_AUDIO)

    /** 硬件检测额外需要的权限 */
    val HARDWARE_PERMISSIONS: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_WIFI_STATE,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_WIFI_STATE,
            )
        }

    /** 通知权限 (Android 13+) */
    val NOTIFICATION_PERMISSION: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null

    /** 检查录音权限 */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 检查蓝牙权限 */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** 检查通知权限 */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** 权限被拒绝且不应该再询问 */
    fun shouldShowRationale(
        context: Context,
        onShowRationale: () -> Unit,
        onProceed: () -> Unit
    ) {
        if (!hasRecordAudioPermission(context)) {
            onShowRationale()
        } else {
            onProceed()
        }
    }
}
