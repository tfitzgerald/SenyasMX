package com.lsm.translator

import android.app.Application
import android.util.Log
import com.lsm.translator.service.PhraseRepository

class LSMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm the phrase pack on startup so the first screen loads instantly
        PhraseRepository(this).load().onFailure { e ->
            Log.e("LSMApplication", "Phrase pack failed to load: ${e.message}")
        }
    }
}
