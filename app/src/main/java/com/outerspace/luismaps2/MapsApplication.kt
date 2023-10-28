package com.outerspace.luismaps2

import android.app.Application

class MapsApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MapsApplication
            private set
    }
}