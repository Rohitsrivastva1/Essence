package com.nexusapps.essence

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class GestureManager(
    private val context: Context,
    private val appWhitelistManager: AppWhitelistManager
) : View.OnTouchListener, GestureDetector.SimpleOnGestureListener() {
    
    private val gestureDetector = GestureDetector(context, this)
    private var startTime: Long = 0
    
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        private const val LONG_PRESS_DURATION = 500
    }
    
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!)
    }
    
    override fun onDown(e: MotionEvent): Boolean {
        startTime = System.currentTimeMillis()
        return true
    }
    
    
    override fun onLongPress(e: MotionEvent) {
        // Long press - show quick actions
        showQuickActions()
    }
    
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val diffY = e2.y - (e1?.y ?: 0f)
        val diffX = e2.x - (e1?.x ?: 0f)
        
        return when {
            Math.abs(diffY) > Math.abs(diffX) -> {
                when {
                    Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        if (diffY > 0) {
                            // Swipe down - show quick settings
                            showQuickSettings()
                        } else {
                            // Swipe up - show app drawer
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
                        // Swipe right - previous focus mode
                        switchToPreviousFocusMode()
                    }
                    else -> {
                        // Swipe left - next focus mode
                        switchToNextFocusMode()
                    }
                }
                true
            }
            else -> false
        }
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
        Toast.makeText(context, "App Drawer: Swipe up detected", Toast.LENGTH_SHORT).show()
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
}
