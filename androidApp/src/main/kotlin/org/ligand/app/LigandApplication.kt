package org.ligand.app

import android.app.Application
import org.ligand.app.di.initKoin

class LigandApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}