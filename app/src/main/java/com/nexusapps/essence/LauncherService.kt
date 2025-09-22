package com.nexusapps.essence

import android.app.ActivityManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class LauncherService : Service() {
    
    private lateinit var homeKeyReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val TAG = "LauncherService"
        private const val ACTION_HOME_KEY = "android.intent.action.CLOSE_SYSTEM_DIALOGS"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LauncherService created")
        
        // Register receiver for home key presses
        homeKeyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_HOME_KEY) {
                    val reason = intent.getStringExtra("reason")
                    if (reason == "homekey" || reason == "recentapps") {
                        // Home key was pressed, ensure our launcher is active
                        bringLauncherToFront()
                    }
                }
            }
        }
        
        val filter = IntentFilter(ACTION_HOME_KEY)
        registerReceiver(homeKeyReceiver, filter)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LauncherService started")
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LauncherService destroyed")
        
        try {
            unregisterReceiver(homeKeyReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // This is not a bound service
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed, restarting launcher")
        
        // If the launcher task is removed, restart the main activity
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        
        // Restart this service
        val serviceIntent = Intent(this, LauncherService::class.java)
        startService(serviceIntent)
    }
    
    private fun bringLauncherToFront() {
        handler.postDelayed({
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val tasks = activityManager.getRunningTasks(10)
                
                var launcherRunning = false
                for (task in tasks) {
                    if (task.topActivity?.className == MainActivity::class.java.name) {
                        launcherRunning = true
                        break
                    }
                }
                
                if (!launcherRunning) {
                    // Launch MainActivity if not running
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(launcherIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error bringing launcher to front: ${e.message}")
                
                // Fallback: just start the main activity
                val launcherIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(launcherIntent)
            }
        }, 100)
    }
}