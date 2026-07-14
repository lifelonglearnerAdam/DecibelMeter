package com.decibelmeter.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 类
 * Hilt 入口点 — 参考 XMSLEEP 的 Hilt 架构
 */
@HiltAndroidApp
class DecibelMeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
