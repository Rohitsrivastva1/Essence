package com.nexusapps.essence

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "launcher_service_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LauncherService created")
        
        // Create notification channel for foreground service
        createNotificationChannel()
        
        // Start as foreground service to prevent being killed
        startForeground(NOTIFICATION_ID, createNotification())
        
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(homeKeyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(homeKeyReceiver, filter)
        }
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
        
        // Schedule launcher restart with delay to avoid immediate killing
        handler.postDelayed({
            restartLauncher()
        }, 1000)
    }
    
    private fun restartLauncher() {
        try {
            // If the launcher task is removed, restart the main activity
            val restartIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(restartIntent)
            
            // Restart this service
            val serviceIntent = Intent(this, LauncherService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting launcher: ${e.message}")
        }
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
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Launcher Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Essence launcher running"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Essence Launcher")
                .setContentText("Launcher is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Essence Launcher")
                .setContentText("Launcher is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
        }
    }
}