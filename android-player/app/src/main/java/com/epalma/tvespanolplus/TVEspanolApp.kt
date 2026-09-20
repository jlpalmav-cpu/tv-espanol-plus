package com.epalma.tvespanolplus

import android.app.Application
import android.util.Log

class TVEspanolApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Some TV firmware ships incomplete JobScheduler/WorkManager integrations.
        // A background-refresh scheduling problem must never prevent the app from opening.
        runCatching { RefreshWorker.schedule(this) }
            .onFailure { Log.w("TVEspanolPlus", "Background refresh schedule skipped", it) }
    }
}
