package com.teleconizer.app

import android.app.Application
import android.util.Log

class TeleconizerApplication : Application() {

    companion object {
        private const val TAG = "TeleconizerApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Hapus FirebaseApp.initializeApp() karena Firebase sudah otomatis initialize
        Log.d(TAG, "Application onCreate executed normally")
    }
}