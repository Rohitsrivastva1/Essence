package com.nexusapps.essence

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var appWhitelistManager: AppWhitelistManager
    private lateinit var themeManager: ThemeManager
    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var appsContainer: android.view.ViewGroup
    private lateinit var emptyStateText: TextView
    private lateinit var settingsButton: View
    private lateinit var gestureManager: GestureManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryText: TextView
    private lateinit var favoriteAppsContainer: LinearLayout
    private var batteryReceiver: BroadcastReceiver? = null
    private var cachedFavoriteApps: List<AppInfo>? = null
    private var lastAppRefresh: Long = 0
    private lateinit var callButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var wellbeingButton: ImageButton
    private lateinit var messagesButton: Button
    private lateinit var browserButton: Button
    private lateinit var calculatorButton: Button
    private lateinit var settingsQuickButton: Button
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var clearSearchIcon: ImageView
    private var searchDialog: AlertDialog? = null
    private var searchAdapter: SearchAdapter? = null
    private var isLauncherMode = false
    private var backPressCount = 0
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 30000) // Update every 30 seconds instead of every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Check if this is being launched as a launcher
        isLauncherMode = intent.hasCategory(Intent.CATEGORY_HOME) || 
                        intent.action == Intent.ACTION_MAIN && 
                        intent.categories?.contains(Intent.CATEGORY_HOME) == true
        
        // Debug logging
        Log.d("MainActivity", "Intent action: ${intent.action}")
        Log.d("MainActivity", "Intent categories: ${intent.categories}")
        Log.d("MainActivity", "Is launcher mode: $isLauncherMode")
        
        // Check if onboarding is needed
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_completed", false)) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // If this is the first launch after onboarding, help user set as default launcher
        if (!prefs.getBoolean("launcher_setup_shown", false) && !isLauncherMode) {
            showInitialLauncherSetup()
            prefs.edit().putBoolean("launcher_setup_shown", true).apply()
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
        themeManager = ThemeManager(this)
        wallpaperManager = WallpaperManager(this)
        
        // Apply current theme
        themeManager.applyTheme(this)
        
        // Apply wallpaper
        applyWallpaper()
        
        // Apply current customization settings
        applyCustomizationSettings()
        
        appsContainer = findViewById(R.id.appsContainer)
        emptyStateText = findViewById(R.id.emptyStateText)
        settingsButton = findViewById(R.id.settingsButton)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        batteryText = findViewById(R.id.batteryText)
        favoriteAppsContainer = findViewById(R.id.favoriteAppsContainer)
        callButton = findViewById(R.id.callButton)
        cameraButton = findViewById(R.id.cameraButton)
        wellbeingButton = findViewById(R.id.wellbeingButton)
        messagesButton = findViewById(R.id.messagesButton)
        browserButton = findViewById(R.id.browserButton)
        calculatorButton = findViewById(R.id.calculatorButton)
        settingsQuickButton = findViewById(R.id.settingsQuickButton)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.searchIcon)
        clearSearchIcon = findViewById(R.id.clearSearchIcon)
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
        
        // Set up quick action buttons
        setupQuickActionButtons()
        
        // Set up search functionality
        setupSearch()
        
        // Start clock updates
        updateClock()
        clockHandler.post(clockRunnable)
        
        // Setup battery monitoring
        setupBatteryMonitoring()
        updateBatteryLevel()
        
        // Start performance monitoring
        performanceMonitor.startMonitoring()
        
        // Load and display whitelisted apps
        loadWhitelistedApps()
        
        // Load and display favorite apps
        loadFavoriteApps()
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
        
        // Note: On Android 10+ with gesture navigation, users can still exit using system gestures
        // This is a system limitation that cannot be fully prevented. The LauncherService will
        // attempt to bring the launcher back to front when HOME is pressed, but gesture navigation
        // bypasses traditional key event handling. This works better with 3-button navigation.
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isLauncherMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    // Handle back button - show exit confirmation
                    handleBackPress()
                    return true
                }
                // Note: HOME and APP_SWITCH key handling removed for Android 10+ compatibility
                // On Android 10+ with gesture navigation, onKeyDown cannot intercept HOME/Recents
                // The LauncherService handles bringing the launcher back to front instead
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
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isLauncherMode) {
            // Don't call finish() or moveTaskToBack(true) - keep launcher alive
            handleBackPress()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Debug visibility fix - force window visible
        Log.d("Essence", "MainActivity resumed, visibility=${window.decorView.visibility}")
        window.decorView.visibility = View.VISIBLE
        
        // Ensure root view is visible
        findViewById<View>(R.id.main)?.visibility = View.VISIBLE
        
        // If in launcher mode, ensure we're always on top
        if (isLauncherMode) {
            Log.d("Essence", "In launcher mode - ensuring visibility and focus")
            // Ensure we're still the home launcher
            ensureLauncherMode()
        }
        
        // Refresh the app list and customization settings when returning from settings
        refreshCustomizationSettings()
    }
    
    private fun ensureLauncherMode() {
        // Check if we're still the default launcher
        val packageManager = packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        
        if (resolveInfo?.activityInfo?.packageName == packageName) {
            // We're still the default launcher - keep window visible and focused
            Log.d("Essence", "Still default launcher - maintaining visibility")
            window.decorView.visibility = View.VISIBLE
            // Don't call moveTaskToBack() as it can cause window destruction
        } else {
            // We're no longer the default launcher, show setup instructions
            Log.w("Essence", "No longer default launcher - showing setup")
            showLauncherSetupInstructions()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("Essence", "onNewIntent: ${intent?.action}, categories: ${intent?.categories}")
        
        // Handle new intents when app is already running
        if (intent != null && (intent.hasCategory(Intent.CATEGORY_HOME) || intent.getBooleanExtra("from_service", false))) {
            // This is a home intent or from service - ensure we're visible and focused
            Log.d("Essence", "HOME intent or service intent received - ensuring visibility")
            window.decorView.visibility = View.VISIBLE
            findViewById<View>(R.id.main)?.visibility = View.VISIBLE
            // Don't call moveTaskToBack() - keep the launcher visible
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d("Essence", "Window focus changed: $hasFocus, launcherMode: $isLauncherMode")
        
        if (isLauncherMode) {
            if (hasFocus) {
                // We have focus - ensure visibility
                window.decorView.visibility = View.VISIBLE
                findViewById<View>(R.id.main)?.visibility = View.VISIBLE
            } else {
                // Lost focus - but don't call moveTaskToBack() as it destroys window
                Log.d("Essence", "Lost focus but keeping window alive")
            }
        }
    }
    
    private fun checkLauncherStatus() {
        val packageManager = packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        
        val currentDefaultLauncher = resolveInfo?.activityInfo?.packageName
        Log.d("MainActivity", "Current default launcher: $currentDefaultLauncher")
        Log.d("MainActivity", "Our package name: $packageName")
        
        if (currentDefaultLauncher != packageName) {
            // We're not the default launcher
            Log.w("MainActivity", "Not set as default launcher - HOME button won't work")
            
            val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
            val lastPromptTime = prefs.getLong("last_launcher_prompt", 0)
            val currentTime = System.currentTimeMillis()
            
            // Show prompt more frequently if not set as default
            if (currentTime - lastPromptTime > 60 * 1000) { // Every minute instead of daily
                showLauncherSetupInstructions()
                prefs.edit().putLong("last_launcher_prompt", currentTime).apply()
            }
        } else {
            Log.i("MainActivity", "Successfully set as default launcher!")
        }
        
        // Check UsageStats permission for Android 10+ (only if not already granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkUsageStatsPermission()
        }
    }
    
    private fun checkUsageStatsPermission() {
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        val usageStatsPrompted = prefs.getBoolean("usage_stats_prompted", false)
        
        if (!usageStatsPrompted) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 60 * 60 * 24,
                time
            )
            
            if (stats.isEmpty()) {
                // Permission not granted, show request dialog only once
                showUsageStatsPermissionDialog()
                prefs.edit().putBoolean("usage_stats_prompted", true).apply()
            }
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
    
    private fun showInitialLauncherSetup() {
        val message = """
            🏠 IMPORTANT: Set as Default Launcher
            
            Your HOME button won't work until you set Essence as your default launcher.
            
            Steps:
            1. Tap "Set as Default" below
            2. Select "Essence" from the list
            3. Tap "Always" when prompted
            
            ⚠️ Without this, pressing HOME will do nothing!
        """.trimIndent()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Setup Required")
            .setMessage(message)
            .setPositiveButton("Set as Default") { _, _ ->
                Log.d("MainActivity", "User clicked Set as Default - triggering launcher chooser")
                // Trigger the launcher chooser by sending a HOME intent
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
            }
            .setNegativeButton("Manual Setup") { _, _ ->
                Toast.makeText(this, "Go to: Settings > Apps > Default Apps > Home app > Select Essence", Toast.LENGTH_LONG).show()
                // Also open settings directly
                try {
                    val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not open home settings", e)
                }
            }
            .setCancelable(false)
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
    
    private fun loadFavoriteApps() {
        val currentTime = System.currentTimeMillis()
        
        // Only refresh apps every 30 seconds to reduce lag
        if (cachedFavoriteApps == null || currentTime - lastAppRefresh > 30000) {
            cachedFavoriteApps = appWhitelistManager.getFocusModeApps().take(6)
            lastAppRefresh = currentTime
        }
        
        // Clear existing views
        favoriteAppsContainer.removeAllViews()
        
        // Use cached apps
        cachedFavoriteApps?.let { favoriteApps ->
            if (favoriteApps.isNotEmpty()) {
                for (app in favoriteApps) {
                    val appView = createFavoriteAppView(app)
                    favoriteAppsContainer.addView(appView)
                }
            }
        }
    }
    
    private fun createFavoriteAppView(app: AppInfo): TextView {
        val textView = TextView(this)
        textView.text = app.appName
        textView.textSize = 16f
        textView.setTextColor(0xFFFFFFFF.toInt())
        textView.typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        
        // Set padding and margins
        val padding = (16 * resources.displayMetrics.density).toInt()
        textView.setPadding(0, padding/2, 0, padding/2)
        
        // Set layout params
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        textView.layoutParams = layoutParams
        
        // Set click listener
        textView.setOnClickListener {
            launchApp(app.packageName)
        }
        
        return textView
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
    
    private fun updateClock() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        
        timeText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now)
    }
    
    private fun setupBatteryMonitoring() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Only update battery level on significant changes
                updateBatteryLevel()
            }
        }
        
        // Only monitor significant battery changes to reduce overhead
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        
        // Update battery level every 5 minutes instead of constantly
        val batteryUpdateRunnable = object : Runnable {
            override fun run() {
                updateBatteryLevel()
                clockHandler.postDelayed(this, 300000) // 5 minutes
            }
        }
        clockHandler.postDelayed(batteryUpdateRunnable, 1000)
    }
    
    private fun updateBatteryLevel() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        if (batteryLevel != Integer.MIN_VALUE) {
            batteryText.text = "$batteryLevel%"
        } else {
            // Fallback method for older devices
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()
                    batteryText.text = "$batteryPct%"
                }
            }
        }
    }
    
    private fun setupQuickActionButtons() {
        // Call button
        callButton.setOnClickListener {
            openDialer()
        }
        
        // Camera button
        cameraButton.setOnClickListener {
            openCamera()
        }
        
        // Messages button
        messagesButton.setOnClickListener {
            openMessages()
        }
        
        // Browser button
        browserButton.setOnClickListener {
            openBrowser()
        }
        
        // Calculator button
        calculatorButton.setOnClickListener {
            openCalculator()
        }
        
        // Settings button
        settingsQuickButton.setOnClickListener {
            openSettings()
        }
        
        // Digital Wellbeing button
        wellbeingButton.setOnClickListener {
            openDigitalWellbeing()
        }
    }
    
    private fun setupSearch() {
        // Set up search text watcher
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    clearSearchIcon.visibility = View.VISIBLE
                    performSearch(query)
                } else {
                    clearSearchIcon.visibility = View.GONE
                    hideSearchResults()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Set up clear search button
        clearSearchIcon.setOnClickListener {
            searchEditText.text.clear()
            clearSearchIcon.visibility = View.GONE
            hideSearchResults()
        }
        
        // Set up search icon click
        searchIcon.setOnClickListener {
            searchEditText.requestFocus()
        }
    }
    
    private fun openDialer() {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openCamera() {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivity(cameraIntent)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    private fun openMessages() {
        try {
            val messagesIntent = Intent(Intent.ACTION_MAIN)
            messagesIntent.addCategory(Intent.CATEGORY_APP_MESSAGING)
            if (messagesIntent.resolveActivity(packageManager) != null) {
                startActivity(messagesIntent)
            } else {
                // Fallback to SMS app
                val smsIntent = Intent(Intent.ACTION_VIEW)
                smsIntent.type = "vnd.android-dir/mms-sms"
                startActivity(smsIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open messages", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openBrowser() {
        try {
            val browserIntent = Intent(Intent.ACTION_MAIN)
            browserIntent.addCategory(Intent.CATEGORY_APP_BROWSER)
            if (browserIntent.resolveActivity(packageManager) != null) {
                startActivity(browserIntent)
            } else {
                // Fallback to web search
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openCalculator() {
        try {
            val calculatorIntent = Intent(Intent.ACTION_MAIN)
            calculatorIntent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
            if (calculatorIntent.resolveActivity(packageManager) != null) {
                startActivity(calculatorIntent)
            } else {
                Toast.makeText(this, "No calculator app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open calculator", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openSettings() {
        try {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(settingsIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openDigitalWellbeing() {
        try {
            val wellbeingIntent = Intent(this, DigitalWellbeingActivity::class.java)
            startActivity(wellbeingIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open Digital Wellbeing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop clock updates
        clockHandler.removeCallbacks(clockRunnable)
        
        // Unregister battery receiver
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w("MainActivity", "Error unregistering battery receiver: ${e.message}")
            }
        }
        
        // Stop performance monitoring
        // performanceMonitor.stopMonitoring() // Method doesn't exist
        
        // Stop launcher service if running
        if (isLauncherMode) {
            val serviceIntent = Intent(this, LauncherService::class.java)
            stopService(serviceIntent)
        }
    }
    
    private fun performSearch(query: String) {
        val allApps = appWhitelistManager.getAllInstalledApps()
        val filteredApps = allApps.filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
        }
        
        if (filteredApps.isNotEmpty()) {
            showSearchResults(filteredApps)
        } else {
            hideSearchResults()
        }
    }
    
    private fun showSearchResults(apps: List<AppInfo>) {
        if (searchDialog == null) {
            createSearchDialog()
        }
        
        searchAdapter?.updateApps(apps)
        searchDialog?.show()
    }
    
    private fun hideSearchResults() {
        searchDialog?.dismiss()
    }
    
    private fun createSearchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_results, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        val noResultsText = dialogView.findViewById<TextView>(R.id.noResultsText)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeSearchButton)
        
        // Set up RecyclerView
        searchAdapter = SearchAdapter(this, emptyList()) { app ->
            launchApp(app)
            searchDialog?.dismiss()
            searchEditText.text.clear()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = searchAdapter
        
        // Set up close button
        closeButton.setOnClickListener {
            searchDialog?.dismiss()
            searchEditText.text.clear()
        }
        
        // Create dialog
        searchDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        searchDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    
    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot launch ${app.appName}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching ${app.appName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Note: onTaskRemoved is not available for Activity, only for Service
    // This functionality should be moved to LauncherService if needed
    
    /**
     * Apply wallpaper to the main view
     */
    private fun applyWallpaper() {
        val mainView = findViewById<View>(R.id.main)
        wallpaperManager.applyWallpaperToView(mainView)
    }
    
    /**
     * Apply current customization settings from ThemeManager
     */
    private fun applyCustomizationSettings() {
        applyFontSizeSettings()
        applyIconSizeSettings()
        applyGridDensitySettings()
        applyAccentColorSettings()
    }
    
    /**
     * Apply font size settings to UI elements
     */
    private fun applyFontSizeSettings() {
        val fontSize = themeManager.getCurrentFontSize()
        val scale = fontSize.scale
        
        // Apply to clock text
        timeText.textSize = 48f * scale
        dateText.textSize = 16f * scale
        
        // Apply to battery text
        batteryText.textSize = 14f * scale
        
        // Apply to empty state text
        emptyStateText.textSize = 16f * scale
        
        // Apply to favorite apps text
        applyFontSizeToContainer(favoriteAppsContainer, 16f * scale)
    }
    
    /**
     * Apply icon size settings to UI elements
     */
    private fun applyIconSizeSettings() {
        val iconSize = themeManager.getCurrentIconSize()
        val scale = iconSize.scale
        
        // Apply to quick action buttons
        val quickActionButtons = listOf(callButton, cameraButton)
        quickActionButtons.forEach { button ->
            val layoutParams = button.layoutParams
            layoutParams.width = (64 * scale).toInt()
            layoutParams.height = (64 * scale).toInt()
            button.layoutParams = layoutParams
        }
        
        // Apply to other buttons
        val otherButtons = listOf(messagesButton, browserButton, calculatorButton, settingsQuickButton)
        otherButtons.forEach { button ->
            val layoutParams = button.layoutParams
            layoutParams.height = (48 * scale).toInt()
            button.layoutParams = layoutParams
        }
    }
    
    /**
     * Apply grid density settings to app container
     */
    private fun applyGridDensitySettings() {
        val gridDensity = themeManager.getCurrentGridDensity()
        
        // Update app container layout based on grid density
        when (gridDensity) {
            ThemeManager.GridDensity.COMPACT -> {
                // 4 columns, 6 rows - more apps per screen
                updateAppContainerLayout(4, 6)
            }
            ThemeManager.GridDensity.NORMAL -> {
                // 3 columns, 5 rows - balanced layout
                updateAppContainerLayout(3, 5)
            }
            ThemeManager.GridDensity.SPACIOUS -> {
                // 3 columns, 4 rows - more spacing
                updateAppContainerLayout(3, 4)
            }
        }
    }
    
    /**
     * Update app container layout based on grid density
     */
    private fun updateAppContainerLayout(columns: Int, rows: Int) {
        // Convert LinearLayout to GridLayout for better control
        if (appsContainer is LinearLayout) {
            val gridLayout = android.widget.GridLayout(this).apply {
                id = appsContainer.id
                layoutParams = appsContainer.layoutParams
                columnCount = columns
                rowCount = rows
                useDefaultMargins = true
            }
            
            // Replace the container
            val parent = appsContainer.parent as android.view.ViewGroup
            val index = parent.indexOfChild(appsContainer)
            parent.removeView(appsContainer)
            parent.addView(gridLayout, index)
            
            // Update reference
            appsContainer = gridLayout
        }
    }
    
    /**
     * Apply accent color settings to UI elements
     */
    private fun applyAccentColorSettings() {
        val accentColor = themeManager.getAccentColorValue()
        
        // Apply accent color to various UI elements
        // This would typically involve updating drawable resources or programmatically setting colors
        // For now, we'll apply it to the clock text as an example
        timeText.setTextColor(accentColor)
    }
    
    /**
     * Apply font size to all TextViews in a container
     */
    private fun applyFontSizeToContainer(container: android.view.ViewGroup, textSize: Float) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is TextView) {
                child.textSize = textSize
            } else if (child is android.view.ViewGroup) {
                applyFontSizeToContainer(child, textSize)
            }
        }
    }
    
    /**
     * Refresh customization settings (called when returning from settings)
     */
    private fun refreshCustomizationSettings() {
        applyWallpaper()
        applyCustomizationSettings()
        loadWhitelistedApps()
        loadFavoriteApps()
    }
}