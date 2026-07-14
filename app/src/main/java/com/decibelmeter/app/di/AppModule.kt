package com.decibelmeter.app.di

import android.content.Context
import com.decibelmeter.app.data.local.AudioCaptureEngine
import com.decibelmeter.app.data.local.HardwareDetector
import com.decibelmeter.app.data.local.SleepAudioPlayer
import com.decibelmeter.app.data.repository.DecibelRepository
import com.decibelmeter.app.data.repository.HardwareRepository
import com.decibelmeter.app.data.repository.SleepRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 * 参考: Tosencen/XMSLEEP 的 Hilt DI 架构
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioCaptureEngine(): AudioCaptureEngine {
        return AudioCaptureEngine()
    }

    @Provides
    @Singleton
    fun provideSleepAudioPlayer(
        @ApplicationContext context: Context
    ): SleepAudioPlayer {
        return SleepAudioPlayer(context)
    }

    @Provides
    @Singleton
    fun provideHardwareDetector(
        @ApplicationContext context: Context
    ): HardwareDetector {
        return HardwareDetector(context)
    }

    @Provides
    @Singleton
    fun provideDecibelRepository(
        audioCaptureEngine: AudioCaptureEngine
    ): DecibelRepository {
        return DecibelRepository(audioCaptureEngine)
    }

    @Provides
    @Singleton
    fun provideSleepRepository(
        audioPlayer: SleepAudioPlayer
    ): SleepRepository {
        return SleepRepository(audioPlayer)
    }

    @Provides
    @Singleton
    fun provideHardwareRepository(
        detector: HardwareDetector
    ): HardwareRepository {
        return HardwareRepository(detector)
    }
}
