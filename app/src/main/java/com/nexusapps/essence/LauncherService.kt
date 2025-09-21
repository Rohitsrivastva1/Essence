package com.nexusapps.essence

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service to maintain launcher functionality and prevent fallback to system launcher
 */
class LauncherService : Service() {
    
    companion object {
        private const val TAG = "LauncherService"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LauncherService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LauncherService started")
        
        // Keep the service running
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LauncherService destroyed")
    }
}
