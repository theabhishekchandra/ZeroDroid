package com.abhishek.zerodroid.core.di

import android.content.Context
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.features.dashboard.DeviceInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModule {

    @Provides
    @Singleton
    fun provideHardwareChecker(@ApplicationContext context: Context): HardwareChecker =
        HardwareChecker(context)

    @Provides
    @Singleton
    fun provideDeviceInfo(): DeviceInfo = DeviceInfo.fromBuild()
}
