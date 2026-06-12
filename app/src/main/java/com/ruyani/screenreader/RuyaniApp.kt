package com.ruyani.screenreader

import android.app.Application

class RuyaniApp : Application() {

    companion object {
        lateinit var instance: RuyaniApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
