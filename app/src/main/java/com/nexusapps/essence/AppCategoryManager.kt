package com.nexusapps.essence

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppCategoryManager(private val context: Context) {
    
    private val packageManager: PackageManager = context.packageManager
    
    // App categories based on package names and app names
    private val categoryKeywords = mapOf(
        "Productivity" to listOf(
            "office", "word", "excel", "powerpoint", "google", "docs", "sheets", "slides",
            "notion", "evernote", "onenote", "trello", "asana", "slack", "teams",
            "calendar", "todo", "task", "note", "memo", "reminder"
        ),
        "Communication" to listOf(
            "whatsapp", "telegram", "signal", "messenger", "discord", "skype", "zoom",
            "phone", "dialer", "contact", "message", "sms", "mail", "email", "gmail",
            "outlook", "thunderbird", "chat", "call", "voice"
        ),
        "Entertainment" to listOf(
            "youtube", "netflix", "spotify", "music", "video", "movie", "game",
            "entertainment", "fun", "play", "watch", "listen", "stream", "media",
            "social", "instagram", "tiktok", "snapchat", "twitter", "facebook"
        ),
        "Utilities" to listOf(
            "file", "manager", "explorer", "browser", "chrome", "firefox", "safari",
            "calculator", "clock", "alarm", "timer", "weather", "camera", "gallery",
            "photo", "image", "pdf", "reader", "viewer", "editor"
        ),
        "System" to listOf(
            "settings", "system", "android", "google", "play", "store", "update",
            "security", "battery", "storage", "memory", "cpu", "device", "hardware"
        ),
        "Health" to listOf(
            "health", "fitness", "workout", "exercise", "meditation", "sleep",
            "heart", "step", "walk", "run", "yoga", "gym", "tracker", "monitor"
        ),
        "Education" to listOf(
            "learn", "study", "course", "education", "school", "university", "book",
            "read", "language", "duolingo", "khan", "academy", "tutorial", "guide"
        ),
        "Finance" to listOf(
            "bank", "money", "finance", "payment", "wallet", "card", "credit",
            "debit", "paypal", "venmo", "cash", "budget", "expense", "investment"
        )
    )
    
    fun categorizeApp(packageName: String, appName: String): String {
        val searchText = "$packageName $appName".lowercase()
        
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> searchText.contains(keyword) }) {
                return category
            }
        }
        
        return "Other"
    }
    
    fun getCategoryIcon(category: String): String {
        return when (category) {
            "Productivity" -> "📊"
            "Communication" -> "💬"
            "Entertainment" -> "🎮"
            "Utilities" -> "🔧"
            "System" -> "⚙️"
            "Health" -> "💪"
            "Education" -> "📚"
            "Finance" -> "💰"
            else -> "📱"
        }
    }
    
    fun getAllCategories(): List<String> {
        return categoryKeywords.keys.toList() + "Other"
    }
    
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    fun isUserApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
