package com.sik.sikcamerasample

import android.app.Application
import com.sik.sikcore.SIKCore

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        SIKCore.init(this)
    }
}