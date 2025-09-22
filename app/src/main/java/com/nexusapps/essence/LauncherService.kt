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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class LauncherService : Service() {
    
    private lateinit var homeKeyReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())
    private var launcherCheckRunnable: Runnable? = null
    
    companion object {
        private const val TAG = "LauncherService"
        private const val ACTION_HOME_KEY = "android.intent.action.CLOSE_SYSTEM_DIALOGS"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "launcher_service_channel"
        private const val LAUNCHER_CHECK_INTERVAL = 5000L // Check every 5 seconds
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LauncherService created")
        
        // Create notification channel for foreground service
        createNotificationChannel()
        
        // Start as foreground service to prevent being killed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ - use FOREGROUND_SERVICE_TYPE_SPECIAL_USE for less obtrusive notification
            startForegroundService(Intent(this, LauncherService::class.java))
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ - use FOREGROUND_SERVICE_TYPE_SPECIAL_USE for less obtrusive notification
            startForegroundService(Intent(this, LauncherService::class.java))
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            // Older Android versions - use standard foreground service
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
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
        
        // Start periodic launcher check to handle system gestures
        startLauncherCheck()
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
        
        // Stop periodic launcher check
        stopLauncherCheck()
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
                
                // Check if launcher is already running and visible
                val tasks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use getRunningAppProcesses for newer Android versions
                    val runningProcesses = activityManager.runningAppProcesses
                    runningProcesses?.find { it.processName == packageName } != null
                } else {
                    @Suppress("DEPRECATION")
                    val tasks = activityManager.getRunningTasks(10)
                    tasks.any { task -> task.topActivity?.className == MainActivity::class.java.name }
                }
                
                if (!tasks) {
                    // Launch MainActivity if not running
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("from_service", true)
                    }
                    startActivity(launcherIntent)
                    Log.d(TAG, "Launched MainActivity from service")
                } else {
                    // Bring existing launcher to front
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("from_service", true)
                    }
                    startActivity(launcherIntent)
                    Log.d(TAG, "Brought existing launcher to front")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error bringing launcher to front: ${e.message}")
                
                // Fallback: just start the main activity
                val launcherIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("from_service", true)
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
                // Make the channel less obtrusive
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
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
            val builder = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Essence Launcher")
                .setContentText("Launcher is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_LOW)
            
            // For Android 12+, make the notification less obtrusive
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setCategory(Notification.CATEGORY_SERVICE)
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            
            builder.build()
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
    
    private fun startLauncherCheck() {
        launcherCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    // Check if launcher is still the default and running
                    val pm = packageManager
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    val resolveInfo = pm.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                    
                    if (resolveInfo?.activityInfo?.packageName == packageName) {
                        // We're still the default launcher, ensure we're visible
                        ensureLauncherVisibility()
                    } else {
                        Log.w(TAG, "No longer default launcher, stopping periodic check")
                        stopLauncherCheck()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in launcher check: ${e.message}")
                }
                
                // Schedule next check
                handler.postDelayed(this, LAUNCHER_CHECK_INTERVAL)
            }
        }
        
        handler.post(launcherCheckRunnable!!)
        Log.d(TAG, "Started periodic launcher check")
    }
    
    private fun stopLauncherCheck() {
        launcherCheckRunnable?.let {
            handler.removeCallbacks(it)
            launcherCheckRunnable = null
            Log.d(TAG, "Stopped periodic launcher check")
        }
    }
    
    private fun ensureLauncherVisibility() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Check if our launcher is currently visible
            val isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val runningProcesses = activityManager.runningAppProcesses
                runningProcesses?.any { it.processName == packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND } == true
            } else {
                @Suppress("DEPRECATION")
                val tasks = activityManager.getRunningTasks(1)
                tasks.isNotEmpty() && tasks[0].topActivity?.className == MainActivity::class.java.name
            }
            
            if (!isVisible) {
                Log.d(TAG, "Launcher not visible, bringing to front")
                bringLauncherToFront()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring launcher visibility: ${e.message}")
        }
    }
}