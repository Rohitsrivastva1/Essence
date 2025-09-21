package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val category: String = "Unknown",
    val totalTimeSpent: Long = 0,
    val launchCount: Int = 0,
    val lastUsed: Date = Date()
)

class AppWhitelistManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_whitelist", Context.MODE_PRIVATE)
    private val packageManager: PackageManager = context.packageManager
    private val categoryManager = AppCategoryManager(context)
    private val database = AppDatabase.getDatabase(context)
    private val appUsageDao = database.appUsageDao()
    
    private val _whitelistedApps = MutableLiveData<List<AppInfo>>()
    val whitelistedApps: LiveData<List<AppInfo>> = _whitelistedApps

    companion object {
        private const val WHITELIST_KEY = "whitelisted_apps"
        private const val SEPARATOR = ","
        private const val FOCUS_MODE_KEY = "current_focus_mode"
        private const val GRAYSCALE_MODE_KEY = "grayscale_mode"
        private const val THEME_KEY = "current_theme"
    }

    /**
     * Get all whitelisted app package names
     */
    fun getWhitelistedPackages(): Set<String> {
        val whitelistString = prefs.getString(WHITELIST_KEY, "") ?: ""
        return if (whitelistString.isEmpty()) {
            emptySet()
        } else {
            whitelistString.split(SEPARATOR).toSet()
        }
    }

    /**
     * Add app to whitelist
     */
    fun addToWhitelist(packageName: String) {
        val currentWhitelist = getWhitelistedPackages().toMutableSet()
        currentWhitelist.add(packageName)
        saveWhitelist(currentWhitelist)
    }

    /**
     * Remove app from whitelist
     */
    fun removeFromWhitelist(packageName: String) {
        val currentWhitelist = getWhitelistedPackages().toMutableSet()
        currentWhitelist.remove(packageName)
        saveWhitelist(currentWhitelist)
    }

    /**
     * Check if app is whitelisted
     */
    fun isWhitelisted(packageName: String): Boolean {
        return getWhitelistedPackages().contains(packageName)
    }

    /**
     * Get whitelisted apps with their info
     */
    fun getWhitelistedApps(): List<AppInfo> {
        val whitelistedPackages = getWhitelistedPackages()
        val apps = mutableListOf<AppInfo>()

        for (packageName in whitelistedPackages) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = categoryManager.categorizeApp(packageName, appName)
                
                // Get usage data from database
                val usageData = CoroutineScope(Dispatchers.IO).run {
                    appUsageDao.getAppUsage(packageName)
                }
                
                val totalTimeSpent = usageData?.totalTimeSpent ?: 0L
                val launchCount = usageData?.launchCount ?: 0
                val lastUsed = usageData?.lastUsed ?: Date()

                apps.add(AppInfo(
                    packageName, 
                    appName, 
                    icon, 
                    isSystemApp, 
                    category,
                    totalTimeSpent,
                    launchCount,
                    lastUsed
                ))
            } catch (e: PackageManager.NameNotFoundException) {
                // App was uninstalled, remove from whitelist
                removeFromWhitelist(packageName)
            }
        }

        return apps.sortedBy { it.appName }
    }
    
    /**
     * Get whitelisted apps with analytics (async)
     */
    suspend fun getWhitelistedAppsWithAnalytics(): List<AppInfo> = withContext(Dispatchers.IO) {
        val whitelistedPackages = getWhitelistedPackages()
        val apps = mutableListOf<AppInfo>()

        for (packageName in whitelistedPackages) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = categoryManager.categorizeApp(packageName, appName)
                
                // Get usage data from database
                val usageData = appUsageDao.getAppUsage(packageName)
                
                val totalTimeSpent = usageData?.totalTimeSpent ?: 0L
                val launchCount = usageData?.launchCount ?: 0
                val lastUsed = usageData?.lastUsed ?: Date()

                apps.add(AppInfo(
                    packageName, 
                    appName, 
                    icon, 
                    isSystemApp, 
                    category,
                    totalTimeSpent,
                    launchCount,
                    lastUsed
                ))
            } catch (e: PackageManager.NameNotFoundException) {
                // App was uninstalled, remove from whitelist
                removeFromWhitelist(packageName)
            }
        }

        apps.sortedBy { it.appName }
    }

    /**
     * Get all installed apps (excluding system apps by default)
     */
    fun getAllInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        
        for (resolveInfo in resolveInfos) {
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                // Skip system apps if not including them
                if (!includeSystemApps && isSystemApp) {
                    continue
                }

                // Skip our own app
                if (packageName == context.packageName) {
                    continue
                }

                apps.add(AppInfo(packageName, appName, icon, isSystemApp))
            } catch (e: Exception) {
                // Skip apps that can't be processed
                continue
            }
        }

        return apps.sortedBy { it.appName }
    }

    /**
     * Get launch intent for an app
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    /**
     * Save whitelist to SharedPreferences
     */
    private fun saveWhitelist(whitelist: Set<String>) {
        val whitelistString = whitelist.joinToString(SEPARATOR)
        prefs.edit().putString(WHITELIST_KEY, whitelistString).apply()
    }

    /**
     * Clear all whitelisted apps
     */
    fun clearWhitelist() {
        prefs.edit().remove(WHITELIST_KEY).apply()
    }
    
    // Focus Mode Management
    fun setFocusMode(mode: String) {
        prefs.edit().putString(FOCUS_MODE_KEY, mode).apply()
    }
    
    fun getCurrentFocusMode(): String {
        return prefs.getString(FOCUS_MODE_KEY, "All") ?: "All"
    }
    
    fun getFocusModeApps(): List<AppInfo> {
        val mode = getCurrentFocusMode()
        val allApps = getWhitelistedApps()
        
        return when (mode) {
            "Work" -> allApps.filter { it.category in listOf("Productivity", "Communication", "Utilities") }
            "Personal" -> allApps.filter { it.category in listOf("Entertainment", "Health", "Education", "Finance") }
            "Emergency" -> allApps.filter { it.packageName.contains("phone") || it.packageName.contains("dialer") || it.packageName.contains("message") || it.packageName.contains("camera") }
            else -> allApps
        }
    }
    
    // Theme Management
    fun setTheme(theme: String) {
        prefs.edit().putString(THEME_KEY, theme).apply()
    }
    
    fun getCurrentTheme(): String {
        return prefs.getString(THEME_KEY, "Dark") ?: "Dark"
    }
    
    // Grayscale Mode
    fun setGrayscaleMode(enabled: Boolean) {
        prefs.edit().putBoolean(GRAYSCALE_MODE_KEY, enabled).apply()
    }
    
    fun isGrayscaleModeEnabled(): Boolean {
        return prefs.getBoolean(GRAYSCALE_MODE_KEY, false)
    }
    
    // App Usage Analytics
    suspend fun trackAppLaunch(packageName: String) = withContext(Dispatchers.IO) {
        val existingUsage = appUsageDao.getAppUsage(packageName)
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        
        val category = categoryManager.categorizeApp(packageName, appName)
        
        val usage = existingUsage?.copy(
            launchCount = existingUsage.launchCount + 1,
            lastUsed = Date()
        ) ?: AppUsageEntity(
            packageName = packageName,
            appName = appName,
            launchCount = 1,
            lastUsed = Date(),
            category = category
        )
        
        appUsageDao.insertAppUsage(usage)
    }
    
    suspend fun trackAppSession(packageName: String, startTime: Date, endTime: Date) = withContext(Dispatchers.IO) {
        val duration = endTime.time - startTime.time
        val session = AppSessionEntity(
            packageName = packageName,
            startTime = startTime,
            endTime = endTime,
            duration = duration
        )
        
        appUsageDao.insertAppSession(session)
        
        // Update total usage time
        val existingUsage = appUsageDao.getAppUsage(packageName)
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        
        val category = categoryManager.categorizeApp(packageName, appName)
        
        val updatedUsage = existingUsage?.copy(
            totalTimeSpent = existingUsage.totalTimeSpent + duration,
            lastUsed = endTime
        ) ?: AppUsageEntity(
            packageName = packageName,
            appName = appName,
            totalTimeSpent = duration,
            lastUsed = endTime,
            category = category
        )
        
        appUsageDao.insertAppUsage(updatedUsage)
    }
    
    // App Filtering and Search
    fun searchApps(query: String, category: String? = null): List<AppInfo> {
        val allApps = getAllInstalledApps()
        var filteredApps = allApps
        
        if (!query.isEmpty()) {
            filteredApps = filteredApps.filter { 
                it.appName.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
        
        if (category != null && category != "All") {
            filteredApps = filteredApps.filter { it.category == category }
        }
        
        return filteredApps.sortedBy { it.appName }
    }
    
    fun getAppsByCategory(category: String): List<AppInfo> {
        return getAllInstalledApps().filter { it.category == category }.sortedBy { it.appName }
    }
    
    fun getMostUsedApps(limit: Int = 10): List<AppInfo> {
        return getWhitelistedApps().sortedByDescending { it.totalTimeSpent }.take(limit)
    }
    
    fun getRecentlyUsedApps(limit: Int = 10): List<AppInfo> {
        return getWhitelistedApps().sortedByDescending { it.lastUsed }.take(limit)
    }
    
    // App Hiding
    fun hideApp(packageName: String) {
        // This would require root access or special permissions
        // For now, we'll just remove from whitelist
        removeFromWhitelist(packageName)
    }
    
    fun isAppHidden(packageName: String): Boolean {
        return !isWhitelisted(packageName)
    }
    
    // Backup and Restore
    fun exportSettings(): String {
        val whitelist = getWhitelistedPackages().joinToString(",")
        val focusMode = getCurrentFocusMode()
        val theme = getCurrentTheme()
        val grayscale = isGrayscaleModeEnabled()
        
        return "$whitelist|$focusMode|$theme|$grayscale"
    }
    
    fun importSettings(settings: String) {
        val parts = settings.split("|")
        if (parts.size >= 4) {
            val whitelist = parts[0].split(",").filter { it.isNotEmpty() }.toSet()
            val focusMode = parts[1]
            val theme = parts[2]
            val grayscale = parts[3].toBoolean()
            
            // Update whitelist
            prefs.edit().putString(WHITELIST_KEY, whitelist.joinToString(",")).apply()
            
            // Update other settings
            setFocusMode(focusMode)
            setTheme(theme)
            setGrayscaleMode(grayscale)
        }
    }
}
