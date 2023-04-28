package com.yzq.demo

import android.app.Application
import android.util.Log

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("", "BuildConfig.LOG_DEBUG:${BuildConfig.LOG_DEBUG}")

    }
}