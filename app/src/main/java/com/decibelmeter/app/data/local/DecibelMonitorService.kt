package com.decibelmeter.app.data.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.decibelmeter.app.MainActivity
import com.decibelmeter.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * 前台服务：后台分贝监测
 * 参考: albertopasqualetto/SoundMeterESP 的前台服务实现
 *
 * 在通知栏显示当前噪音水平，允许用户在后台持续监测环境噪音。
 */
@AndroidEntryPoint
class DecibelMonitorService : Service() {

    @Inject lateinit var audioCaptureEngine: AudioCaptureEngine

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var captureJob: Job? = null
    private var currentDb: Float = 0f

    companion object {
        const val CHANNEL_ID = "decibel_monitor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.decibelmeter.app.STOP_MONITOR"
        const val EXTRA_DB = "extra_db"

        fun start(context: Context) {
            val intent = Intent(context, DecibelMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DecibelMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        startMonitoring()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        captureJob?.cancel()
        captureJob = serviceScope.launch {
            try {
                audioCaptureEngine.startCapturing().collect { db ->
                    currentDb = db
                    updateNotification()
                }
            } catch (_: Exception) {
                // 采集异常时用静默值
            }
        }
    }

    private fun stopMonitoring() {
        captureJob?.cancel()
        audioCaptureEngine.stopCapturing()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "分贝监测",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台环境噪音监测"
            setShowBadge(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, DecibelMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("分贝监测运行中")
            .setContentText("当前: ${String.format("%.1f", currentDb)} dB")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
