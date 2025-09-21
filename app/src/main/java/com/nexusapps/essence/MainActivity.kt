package com.nexusapps.essence

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Check if onboarding is needed
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_completed", false)) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
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

    override fun onResume() {
        super.onResume()
        // Refresh the app list when returning from settings
        loadWhitelistedApps()
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
}