package com.nexusapps.essence

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class GestureManager(
    private val context: Context,
    private val appWhitelistManager: AppWhitelistManager
) : View.OnTouchListener, GestureDetector.SimpleOnGestureListener() {
    
    private val gestureDetector = GestureDetector(context, this)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var startTime: Long = 0
    private var gestureStartX: Float = 0f
    private var gestureStartY: Float = 0f
    
    companion object {
        private const val SWIPE_THRESHOLD = 80 // Reduced for better sensitivity
        private const val SWIPE_VELOCITY_THRESHOLD = 80 // Reduced for better sensitivity
        private const val LONG_PRESS_DURATION = 500
        private const val HAPTIC_FEEDBACK_DURATION = 50L
    }
    
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!)
    }
    
    override fun onDown(e: MotionEvent): Boolean {
        startTime = System.currentTimeMillis()
        gestureStartX = e.x
        gestureStartY = e.y
        return true
    }
    
    
    override fun onLongPress(e: MotionEvent) {
        // Long press - show quick actions
        performHapticFeedback()
        showQuickActions()
    }
    
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val diffY = e2.y - (e1?.y ?: gestureStartY)
        val diffX = e2.x - (e1?.x ?: gestureStartX)
        
        val gestureDetected = when {
            Math.abs(diffY) > Math.abs(diffX) -> {
                when {
                    Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        if (diffY > 0) {
                            // Swipe down - show quick settings
                            performHapticFeedback()
                            showQuickSettings()
                        } else {
                            // Swipe up - show app drawer
                            performHapticFeedback()
                            showAppDrawer()
                        }
                        true
                    }
                    else -> false
                }
            }
            Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                when {
                    diffX > 0 -> {
                        // Swipe right - show favorites and all apps
                        performHapticFeedback()
                        showFavoritesAndAllApps()
                    }
                    else -> {
                        // Swipe left - next focus mode
                        performHapticFeedback()
                        switchToNextFocusMode()
                    }
                }
                true
            }
            else -> false
        }
        
        return gestureDetected
    }
    
    private fun showQuickActions() {
        // Show quick actions menu
        val actions = listOf(
            "Toggle Grayscale" to { 
                val current = appWhitelistManager.isGrayscaleModeEnabled()
                appWhitelistManager.setGrayscaleMode(!current)
                Toast.makeText(context, "Grayscale ${if (!current) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
            },
            "Show Analytics" to {
                val intent = android.content.Intent(context, AnalyticsActivity::class.java)
                context.startActivity(intent)
            },
            "Settings" to {
                val intent = android.content.Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            }
        )
        
        // In a real implementation, you'd show a popup menu or bottom sheet
        Toast.makeText(context, "Quick Actions: Long press detected", Toast.LENGTH_SHORT).show()
    }
    
    private fun showQuickSettings() {
        // Show quick settings panel
        val currentMode = appWhitelistManager.getCurrentFocusMode()
        val currentTheme = appWhitelistManager.getCurrentTheme()
        val grayscale = appWhitelistManager.isGrayscaleModeEnabled()
        
        val message = "Focus: $currentMode\nTheme: $currentTheme\nGrayscale: $grayscale"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showAppDrawer() {
        // Show all apps (not just whitelisted)
        val allApps = appWhitelistManager.getAllInstalledApps()
        if (allApps.isNotEmpty()) {
            // Show categorized apps dialog
            showCategorizedAppsDialog(allApps)
        } else {
            Toast.makeText(context, "No apps found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAppsDialog(title: String, apps: List<AppInfo>) {
        val appNames = apps.take(10).map { it.appName } // Show first 10 apps
        val message = if (apps.size > 10) {
            appNames.joinToString("\n") + "\n... and ${apps.size - 10} more"
        } else {
            appNames.joinToString("\n")
        }
        
        android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showCategorizedAppsDialog(apps: List<AppInfo>) {
        val categorizedApps = apps.groupBy { app ->
            getAppCategory(app.packageName)
        }.toSortedMap()
        
        val message = buildString {
            categorizedApps.forEach { (category, categoryApps) ->
                append("$category (${categoryApps.size}):\n")
                categoryApps.take(5).forEach { app ->
                    append("• ${app.appName}\n")
                }
                if (categoryApps.size > 5) {
                    append("... and ${categoryApps.size - 5} more\n")
                }
                append("\n")
            }
        }
        
        android.app.AlertDialog.Builder(context)
            .setTitle("All Apps (${apps.size})")
            .setMessage(message.trim())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun getAppCategory(packageName: String): String {
        return when {
            packageName.contains("camera") || packageName.contains("photo") -> "📷 Camera"
            packageName.contains("music") || packageName.contains("audio") -> "🎵 Music"
            packageName.contains("video") || packageName.contains("player") -> "🎬 Video"
            packageName.contains("game") || packageName.contains("play") -> "🎮 Games"
            packageName.contains("social") || packageName.contains("chat") -> "💬 Social"
            packageName.contains("browser") || packageName.contains("web") -> "🌐 Browser"
            packageName.contains("message") || packageName.contains("sms") -> "📱 Messaging"
            packageName.contains("call") || packageName.contains("phone") -> "📞 Phone"
            packageName.contains("mail") || packageName.contains("email") -> "📧 Email"
            packageName.contains("calendar") || packageName.contains("schedule") -> "📅 Productivity"
            packageName.contains("note") || packageName.contains("memo") -> "📝 Notes"
            packageName.contains("calculator") || packageName.contains("calc") -> "🧮 Tools"
            packageName.contains("settings") || packageName.contains("config") -> "⚙️ Settings"
            packageName.contains("file") || packageName.contains("manager") -> "📁 File Manager"
            packageName.contains("weather") || packageName.contains("forecast") -> "🌤️ Weather"
            packageName.contains("map") || packageName.contains("navigation") -> "🗺️ Navigation"
            packageName.contains("bank") || packageName.contains("finance") -> "💰 Finance"
            packageName.contains("shop") || packageName.contains("store") -> "🛒 Shopping"
            packageName.contains("news") || packageName.contains("reader") -> "📰 News"
            packageName.contains("book") || packageName.contains("read") -> "📚 Reading"
            packageName.contains("health") || packageName.contains("fitness") -> "💪 Health"
            packageName.contains("travel") || packageName.contains("trip") -> "✈️ Travel"
            packageName.contains("food") || packageName.contains("restaurant") -> "🍕 Food"
            packageName.contains("education") || packageName.contains("learn") -> "🎓 Education"
            packageName.contains("entertainment") || packageName.contains("fun") -> "🎭 Entertainment"
            else -> "📱 Other"
        }
    }
    
    private fun showFavoritesAndAllApps() {
        val favorites = appWhitelistManager.getMostUsedApps(5) // Top 5 most used apps as favorites
        val allApps = appWhitelistManager.getAllInstalledApps()
        
        val favoritesText = if (favorites.isNotEmpty()) {
            "Favorites:\n" + favorites.joinToString("\n") { it.appName }
        } else {
            "No favorites yet"
        }
        
        val allAppsText = if (allApps.isNotEmpty()) {
            "\n\nAll Apps (${allApps.size}):\n" + allApps.take(10).joinToString("\n") { it.appName } +
            if (allApps.size > 10) "\n... and ${allApps.size - 10} more" else ""
        } else {
            "\n\nNo apps found"
        }
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Apps & Favorites")
            .setMessage(favoritesText + allAppsText)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun switchToNextFocusMode() {
        val modes = listOf("All", "Work", "Personal", "Emergency")
        val currentMode = appWhitelistManager.getCurrentFocusMode()
        val currentIndex = modes.indexOf(currentMode)
        val nextIndex = (currentIndex + 1) % modes.size
        val nextMode = modes[nextIndex]
        
        appWhitelistManager.setFocusMode(nextMode)
        Toast.makeText(context, "Focus Mode: $nextMode", Toast.LENGTH_SHORT).show()
    }
    
    private fun switchToPreviousFocusMode() {
        val modes = listOf("All", "Work", "Personal", "Emergency")
        val currentMode = appWhitelistManager.getCurrentFocusMode()
        val currentIndex = modes.indexOf(currentMode)
        val prevIndex = if (currentIndex == 0) modes.size - 1 else currentIndex - 1
        val prevMode = modes[prevIndex]
        
        appWhitelistManager.setFocusMode(prevMode)
        Toast.makeText(context, "Focus Mode: $prevMode", Toast.LENGTH_SHORT).show()
    }
    
    private fun performHapticFeedback() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(HAPTIC_FEEDBACK_DURATION, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(HAPTIC_FEEDBACK_DURATION)
            }
        }
    }
}
