package com.nexusapps.essence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver to handle system events and maintain launcher functionality
 */
class LauncherReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "LauncherReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed, starting launcher service")
                // Start launcher service on boot
                val serviceIntent = Intent(context, LauncherService::class.java)
                context.startService(serviceIntent)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced, restarting launcher")
                // Restart launcher when app is updated
                val mainIntent = Intent(context, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mainIntent.addCategory(Intent.CATEGORY_HOME)
                context.startActivity(mainIntent)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced: ${intent.data}")
                // Handle package replacement
            }
        }
    }
}

