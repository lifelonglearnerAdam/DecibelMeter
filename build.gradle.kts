// Top-level build file for DecibelMeter
// References:
// - XMSLEEP (Tosencen/XMSLEEP, ⭐1.3k): ExoPlayer/Media3, Hilt, Material3 patterns
// - Glyph-Decibel-Meter (iama2z/Glyph-Decibel-Meter): AudioRecord → RMS → dBFS pipeline
// - SoundMeterESP (albertopasqualetto/SoundMeterESP): Compose + chart + foreground service
// - DeviceInfo (ahmmedrejowan/DeviceInfo): Hardware diagnostic patterns

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
