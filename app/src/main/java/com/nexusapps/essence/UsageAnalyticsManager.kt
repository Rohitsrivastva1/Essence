package com.nexusapps.essence

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * UsageAnalyticsManager handles app usage tracking and analytics
 */
class UsageAnalyticsManager(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager: PackageManager = context.packageManager
    private val prefs: SharedPreferences = context.getSharedPreferences("usage_analytics_prefs", Context.MODE_PRIVATE)

    private val _dailyUsageStats = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val dailyUsageStats: StateFlow<List<AppUsageInfo>> = _dailyUsageStats

    private val _totalScreenTime = MutableStateFlow(0L)
    val totalScreenTime: StateFlow<Long> = _totalScreenTime

    private val _focusGoal = MutableStateFlow(0L)
    val focusGoal: StateFlow<Long> = _focusGoal

    private val _screenTimeGoal = MutableStateFlow(0L)
    val screenTimeGoal: StateFlow<Long> = _screenTimeGoal

    private val _breakInterval = MutableStateFlow(0L)
    val breakInterval: StateFlow<Long> = _breakInterval

    companion object {
        private const val FOCUS_GOAL_KEY = "focus_goal" // in milliseconds
        private const val SCREEN_TIME_GOAL_KEY = "screen_time_goal" // in milliseconds
        private const val BREAK_INTERVAL_KEY = "break_interval" // in milliseconds
        private const val APP_CATEGORY_PREFIX = "app_category_"
    }

    init {
        loadGoals()
    }

    private fun loadGoals() {
        _focusGoal.value = prefs.getLong(FOCUS_GOAL_KEY, TimeUnit.HOURS.toMillis(1)) // Default 1 hour
        _screenTimeGoal.value = prefs.getLong(SCREEN_TIME_GOAL_KEY, TimeUnit.HOURS.toMillis(3)) // Default 3 hours
        _breakInterval.value = prefs.getLong(BREAK_INTERVAL_KEY, TimeUnit.MINUTES.toMillis(30)) // Default 30 minutes
    }

    fun setFocusGoal(goalMillis: Long) {
        _focusGoal.value = goalMillis
        prefs.edit().putLong(FOCUS_GOAL_KEY, goalMillis).apply()
    }

    fun setScreenTimeGoal(goalMillis: Long) {
        _screenTimeGoal.value = goalMillis
        prefs.edit().putLong(SCREEN_TIME_GOAL_KEY, goalMillis).apply()
    }

    fun setBreakInterval(intervalMillis: Long) {
        _breakInterval.value = intervalMillis
        prefs.edit().putLong(BREAK_INTERVAL_KEY, intervalMillis).apply()
    }

    suspend fun queryDailyUsageStats() = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val appUsageList = mutableListOf<AppUsageInfo>()
        var currentTotalScreenTime = 0L

        for ((packageName, usageStats) in usageStatsMap) {
            if (usageStats.totalTimeInForeground > 0) {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon = packageManager.getApplicationIcon(appInfo)
                    val category = getAppCategory(packageName)
                    appUsageList.add(AppUsageInfo(appName, packageName, appIcon, usageStats.totalTimeInForeground, category))
                    currentTotalScreenTime += usageStats.totalTimeInForeground
                } catch (e: PackageManager.NameNotFoundException) {
                    // App not found, might have been uninstalled
                    Log.e("UsageAnalyticsManager", "App not found: $packageName")
                }
            }
        }
        appUsageList.sortByDescending { it.totalTimeInForeground }
        _dailyUsageStats.value = appUsageList
        _totalScreenTime.value = currentTotalScreenTime
    }

    fun getAppCategory(packageName: String): AppCategory {
        val categoryId = prefs.getString(APP_CATEGORY_PREFIX + packageName, null)
        return categoryId?.let { AppCategory.fromId(it) } ?: categorizeAppAutomatically(packageName)
    }

    fun setAppCategory(packageName: String, category: AppCategory) {
        prefs.edit().putString(APP_CATEGORY_PREFIX + packageName, category.id).apply()
    }

    private fun categorizeAppAutomatically(packageName: String): AppCategory {
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString().toLowerCase(Locale.ROOT)
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.toLowerCase(Locale.ROOT)
        }

        return when {
            appName.contains("social") || appName.contains("facebook") || appName.contains("instagram") || appName.contains("whatsapp") || appName.contains("twitter") -> AppCategory.SOCIAL_MEDIA
            appName.contains("game") || appName.contains("play") || appName.contains("puzzle") -> AppCategory.ENTERTAINMENT
            appName.contains("mail") || appName.contains("outlook") || appName.contains("docs") || appName.contains("sheets") -> AppCategory.PRODUCTIVITY
            appName.contains("youtube") || appName.contains("netflix") || appName.contains("spotify") -> AppCategory.ENTERTAINMENT
            appName.contains("camera") || appName.contains("gallery") || appName.contains("photos") -> AppCategory.CREATIVE
            appName.contains("browser") || appName.contains("chrome") || appName.contains("firefox") -> AppCategory.UTILITY
            else -> AppCategory.UNCATEGORIZED
        }
    }

    data class AppUsageInfo(
        val appName: String,
        val packageName: String,
        val appIcon: Drawable,
        val totalTimeInForeground: Long, // in milliseconds
        val category: AppCategory
    )

    enum class AppCategory(val id: String, val displayName: String) {
        PRODUCTIVITY("productivity", "Productivity"),
        SOCIAL_MEDIA("social_media", "Social Media"),
        ENTERTAINMENT("entertainment", "Entertainment"),
        COMMUNICATION("communication", "Communication"),
        CREATIVE("creative", "Creative"),
        UTILITY("utility", "Utility"),
        EDUCATION("education", "Education"),
        HEALTH_FITNESS("health_fitness", "Health & Fitness"),
        NEWS("news", "News"),
        FINANCE("finance", "Finance"),
        SHOPPING("shopping", "Shopping"),
        TRAVEL("travel", "Travel"),
        UNCATEGORIZED("uncategorized", "Uncategorized");

        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: UNCATEGORIZED
        }
    }
}
