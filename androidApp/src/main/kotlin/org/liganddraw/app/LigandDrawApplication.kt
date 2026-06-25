package org.liganddraw.app

import android.app.Application
import org.liganddraw.app.di.initKoin

class LigandDrawApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}