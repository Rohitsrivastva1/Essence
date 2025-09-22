package com.nexusapps.essence

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var appWhitelistManager: AppWhitelistManager
    private lateinit var appsContainer: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var settingsButton: View
    private lateinit var gestureManager: GestureManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private var isLauncherMode = false
    private var backPressCount = 0
    private val backPressHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Check if this is being launched as a launcher
        isLauncherMode = intent.hasCategory(Intent.CATEGORY_HOME) || 
                        intent.action == Intent.ACTION_MAIN && 
                        intent.categories?.contains(Intent.CATEGORY_HOME) == true
        
        // Check if onboarding is needed
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_completed", false)) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // If in launcher mode, prevent access to system launcher
        if (isLauncherMode) {
            setupLauncherMode()
        } else {
            // Check if we should be the default launcher
            checkLauncherStatus()
        }
        
        // Initialize components
        appWhitelistManager = AppWhitelistManager(this)
        appsContainer = findViewById(R.id.appsContainer)
        emptyStateText = findViewById(R.id.emptyStateText)
        settingsButton = findViewById(R.id.settingsButton)
        gestureManager = GestureManager(this, appWhitelistManager)
        performanceMonitor = PerformanceMonitor(this)
        
        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Set up gesture controls
        findViewById<View>(R.id.main).setOnTouchListener(gestureManager)
        
        // Set up settings button click listener
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Start performance monitoring
        performanceMonitor.startMonitoring()
        
        // Load and display whitelisted apps
        loadWhitelistedApps()
    }
    
    private fun setupLauncherMode() {
        // Modern Android (API 30+) immersive mode handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Legacy immersive mode for older Android versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        // Start launcher service to maintain launcher functionality
        val serviceIntent = Intent(this, LauncherService::class.java)
        startService(serviceIntent)
        
        // Note: On Android 10+ with gesture navigation, users can still exit using gestures
        // This is a system limitation that cannot be fully prevented
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isLauncherMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    // Handle back button - show exit confirmation
                    handleBackPress()
                    return true
                }
                KeyEvent.KEYCODE_HOME -> {
                    // Prevent home button from working
                    return true
                }
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    // Prevent recent apps button from working
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun handleBackPress() {
        backPressCount++
        if (backPressCount == 1) {
            Toast.makeText(this, "Press back again to exit launcher mode", Toast.LENGTH_SHORT).show()
            backPressHandler.postDelayed({
                backPressCount = 0
            }, 2000)
        } else if (backPressCount >= 2) {
            // Show exit confirmation dialog
            showExitLauncherDialog()
        }
    }
    
    override fun onBackPressed() {
        if (isLauncherMode) {
            handleBackPress()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // If in launcher mode, ensure we're always on top
        if (isLauncherMode) {
            // Bring this activity to front to prevent access to system launcher
            bringToFront()
        }
        
        // Refresh the app list when returning from settings
        loadWhitelistedApps()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle new intents when app is already running
        if (intent != null && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // This is a home intent, bring our launcher to front
            bringToFront()
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (isLauncherMode && !hasFocus) {
            // If we lose focus in launcher mode, bring ourselves back to front
            Handler(Looper.getMainLooper()).postDelayed({
                if (isLauncherMode) {
                    bringToFront()
                }
            }, 100)
        }
    }
    
    private fun checkLauncherStatus() {
        val packageManager = packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        
        if (resolveInfo?.activityInfo?.packageName != packageName) {
            // We're not the default launcher, show instructions
            showLauncherSetupInstructions()
        }
        
        // Check UsageStats permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkUsageStatsPermission()
        }
    }
    
    private fun checkUsageStatsPermission() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 60 * 60 * 24,
            time
        )
        
        if (stats.isEmpty()) {
            // Permission not granted, show request dialog
            showUsageStatsPermissionDialog()
        }
    }
    
    private fun showUsageStatsPermissionDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Usage Stats Permission Required")
            .setMessage("To track app usage and provide analytics, please grant Usage Stats permission.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Skip", null)
            .show()
    }
    
    private fun showLauncherSetupInstructions() {
        val message = """
            To use Essence as a distraction-free launcher:
            
            1. Go to Settings > Apps > Default Apps
            2. Select "Home app"
            3. Choose "Essence Launcher"
            
            This will replace your home screen with our minimalist interface.
            
            Note: On Android 10+ with gesture navigation, users can still exit using system gestures. This is a system limitation.
        """.trimIndent()
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Show a more detailed dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Set as Default Launcher")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }
    
    private fun showExitLauncherDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Exit Launcher Mode")
            .setMessage("Are you sure you want to exit launcher mode? This will allow access to all apps and may reduce your focus.")
            .setPositiveButton("Exit") { _, _ ->
                // Stop launcher service
                val serviceIntent = Intent(this, LauncherService::class.java)
                stopService(serviceIntent)
                
                // Exit launcher mode
                isLauncherMode = false
                
                // Show system launcher
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            .setNegativeButton("Stay Focused") { _, _ ->
                backPressCount = 0
            }
            .setOnCancelListener {
                backPressCount = 0
            }
            .show()
    }

    private fun loadWhitelistedApps() {
        // Clear existing views
        appsContainer.removeAllViews()
        
        // Get apps based on current focus mode
        val appsToShow = appWhitelistManager.getFocusModeApps()
        
        if (appsToShow.isEmpty()) {
            // Show empty state
            emptyStateText.visibility = View.VISIBLE
            appsContainer.visibility = View.GONE
        } else {
            // Hide empty state and show apps
            emptyStateText.visibility = View.GONE
            appsContainer.visibility = View.VISIBLE
            
            // Create app buttons
            for (app in appsToShow) {
                val appButton = createAppButton(app)
                appsContainer.addView(appButton)
            }
        }
    }

    private fun createAppButton(app: AppInfo): Button {
        val button = Button(this, null, 0, R.style.AppItemStyle)
        button.text = app.appName
        button.setOnClickListener {
            launchApp(app.packageName)
        }
        return button
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = appWhitelistManager.getLaunchIntent(packageName)
            if (launchIntent != null) {
                // Track app launch for analytics
                CoroutineScope(Dispatchers.IO).launch {
                    appWhitelistManager.trackAppLaunch(packageName)
                }
                
                startActivity(launchIntent)
            } else {
                // App not found, remove from whitelist
                appWhitelistManager.removeFromWhitelist(packageName)
                loadWhitelistedApps() // Refresh the list
            }
        } catch (e: Exception) {
            // Handle launch error
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up performance monitoring
        // performanceMonitor cleanup is handled automatically
        
        // Stop launcher service if running
        if (isLauncherMode) {
            val serviceIntent = Intent(this, LauncherService::class.java)
            stopService(serviceIntent)
        }
    }
    
    /**
     * Brings the launcher to the front of the screen
     */
    private fun bringToFront() {
        try {
            // Method 1: Use moveTaskToBack to dismiss other activities
            moveTaskToBack(false)
            
            // Method 2: Bring this activity to front
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error bringing launcher to front: ${e.message}")
        }
    }
    
}