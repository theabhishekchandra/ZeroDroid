package com.abhishek.zerodroid

import android.app.Application
import com.abhishek.zerodroid.core.di.AppContainer

class ZeroDroidApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
