package com.epalma.tvespanolplus

import android.app.Application

class TVEspanolApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RefreshWorker.schedule(this)
    }
}
