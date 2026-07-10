package com.cartablet.companion

import android.app.Application

class CompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CompanionApp
            private set
    }
}
