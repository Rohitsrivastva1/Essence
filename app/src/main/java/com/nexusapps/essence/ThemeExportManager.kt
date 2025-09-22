package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader

/**
 * ThemeExportManager handles comprehensive theme export and import
 */
class ThemeExportManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_export_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val EXPORT_DIR = "theme_exports"
        private const val THEME_VERSION = "1.0"
    }
    
    /**
     * Export complete theme configuration
     */
    fun exportCompleteTheme(): String {
        val themeManager = ThemeManager(context)
        val wallpaperManager = WallpaperManager(context)
        val animationManager = AnimationManager(context)
        val iconPackManager = IconPackManager(context)
        val appWhitelistManager = AppWhitelistManager(context)
        
        val themeData = JSONObject().apply {
            put("version", THEME_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("themeName", "Custom Theme")
            
            // Theme settings
            put("theme", JSONObject().apply {
                put("currentTheme", themeManager.getCurrentTheme().name)
                put("accentColor", themeManager.getCurrentAccentColor().name)
                put("fontSize", themeManager.getCurrentFontSize().name)
                put("gridDensity", themeManager.getCurrentGridDensity().name)
                put("iconSize", themeManager.getCurrentIconSize().name)
            })
            
            // Wallpaper settings
            put("wallpaper", JSONObject().apply {
                put("wallpaperPath", wallpaperManager.getCurrentWallpaperPath() ?: "")
                put("overlayOpacity", wallpaperManager.getOverlayOpacity())
                put("overlayColor", wallpaperManager.getOverlayColor())
                put("blurEnabled", wallpaperManager.isBlurEnabled())
                put("gradientEnabled", wallpaperManager.isGradientEnabled())
                put("gradientColors", wallpaperManager.getGradientColors().joinToString(","))
            })
            
            // Animation settings
            put("animation", JSONObject().apply {
                put("animationSpeed", animationManager.getCurrentAnimationSpeed().id)
                put("animationStyle", animationManager.getCurrentAnimationStyle().id)
                put("pageTransition", animationManager.getCurrentPageTransition().id)
                put("iconAnimation", animationManager.getCurrentIconAnimation().id)
                put("launchAnimation", animationManager.getCurrentLaunchAnimation().id)
            })
            
            // Icon pack settings
            put("iconPack", JSONObject().apply {
                put("iconPack", iconPackManager.getCurrentIconPack().id)
                put("iconStyle", iconPackManager.getCurrentIconStyle().id)
                put("iconShape", iconPackManager.getCurrentIconShape().id)
                put("iconSize", iconPackManager.getCurrentIconSize().id)
                put("iconColor", iconPackManager.getCurrentIconColor().id)
                put("iconBackground", iconPackManager.getCurrentIconBackground().id)
            })
            
            // App whitelist settings
            put("appWhitelist", JSONObject().apply {
                put("whitelistedApps", appWhitelistManager.getWhitelistedPackages().toList())
                put("focusMode", appWhitelistManager.getCurrentFocusMode())
                put("grayscaleMode", appWhitelistManager.isGrayscaleModeEnabled())
            })
        }
        
        return themeData.toString(2)
    }
    
    /**
     * Import complete theme configuration
     */
    fun importCompleteTheme(jsonString: String): Boolean {
        return try {
            val themeData = JSONObject(jsonString)
            
            // Validate version
            val version = themeData.optString("version", "1.0")
            if (version != THEME_VERSION) {
                return false
            }
            
            val themeManager = ThemeManager(context)
            val wallpaperManager = WallpaperManager(context)
            val animationManager = AnimationManager(context)
            val iconPackManager = IconPackManager(context)
            val appWhitelistManager = AppWhitelistManager(context)
            
            // Import theme settings
            themeData.optJSONObject("theme")?.let { themeSettings ->
                themeSettings.optString("currentTheme")?.let { themeName ->
                    try {
                        ThemeManager.Theme.valueOf(themeName).let { themeManager.setTheme(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid theme name, skip
                    }
                }
                themeSettings.optString("accentColor")?.let { colorName ->
                    try {
                        ThemeManager.AccentColor.valueOf(colorName).let { themeManager.setAccentColor(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid color name, skip
                    }
                }
                themeSettings.optString("fontSize")?.let { fontSizeName ->
                    try {
                        ThemeManager.FontSize.valueOf(fontSizeName).let { themeManager.setFontSize(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid font size name, skip
                    }
                }
                themeSettings.optString("gridDensity")?.let { gridDensityName ->
                    try {
                        ThemeManager.GridDensity.valueOf(gridDensityName).let { themeManager.setGridDensity(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid grid density name, skip
                    }
                }
                themeSettings.optString("iconSize")?.let { iconSizeName ->
                    try {
                        ThemeManager.IconSize.valueOf(iconSizeName).let { themeManager.setIconSize(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid icon size name, skip
                    }
                }
            }
            
            // Import wallpaper settings
            themeData.optJSONObject("wallpaper")?.let { wallpaperSettings ->
                wallpaperSettings.optString("wallpaperPath")?.let { path ->
                    if (path.isNotEmpty()) {
                        wallpaperManager.setWallpaperFromPath(path)
                    }
                }
                wallpaperSettings.optDouble("overlayOpacity")?.let { opacity ->
                    wallpaperManager.setOverlayOpacity(opacity.toFloat())
                }
                wallpaperSettings.optInt("overlayColor")?.let { color ->
                    wallpaperManager.setOverlayColor(color)
                }
                wallpaperSettings.optBoolean("blurEnabled")?.let { enabled ->
                    wallpaperManager.setBlurEnabled(enabled)
                }
                wallpaperSettings.optBoolean("gradientEnabled")?.let { enabled ->
                    wallpaperManager.setGradientEnabled(enabled)
                }
                wallpaperSettings.optString("gradientColors")?.let { colorsString ->
                    val colors = colorsString.split(",").map { it.toInt() }.toIntArray()
                    wallpaperManager.setGradientColors(colors)
                }
            }
            
            // Import animation settings
            themeData.optJSONObject("animation")?.let { animationSettings ->
                animationSettings.optString("animationSpeed")?.let { speedId ->
                    AnimationManager.AnimationSpeed.fromId(speedId).let { animationManager.setAnimationSpeed(it) }
                }
                animationSettings.optString("animationStyle")?.let { styleId ->
                    AnimationManager.AnimationStyle.fromId(styleId).let { animationManager.setAnimationStyle(it) }
                }
                animationSettings.optString("pageTransition")?.let { transitionId ->
                    AnimationManager.PageTransition.fromId(transitionId).let { animationManager.setPageTransition(it) }
                }
                animationSettings.optString("iconAnimation")?.let { animationId ->
                    AnimationManager.IconAnimation.fromId(animationId).let { animationManager.setIconAnimation(it) }
                }
                animationSettings.optString("launchAnimation")?.let { animationId ->
                    AnimationManager.LaunchAnimation.fromId(animationId).let { animationManager.setLaunchAnimation(it) }
                }
            }
            
            // Import icon pack settings
            themeData.optJSONObject("iconPack")?.let { iconPackSettings ->
                iconPackSettings.optString("iconPack")?.let { packId ->
                    IconPackManager.IconPack.fromId(packId).let { iconPackManager.setIconPack(it) }
                }
                iconPackSettings.optString("iconStyle")?.let { styleId ->
                    IconPackManager.IconStyle.fromId(styleId).let { iconPackManager.setIconStyle(it) }
                }
                iconPackSettings.optString("iconShape")?.let { shapeId ->
                    IconPackManager.IconShape.fromId(shapeId).let { iconPackManager.setIconShape(it) }
                }
                iconPackSettings.optString("iconSize")?.let { sizeId ->
                    IconPackManager.IconSize.fromId(sizeId).let { iconPackManager.setIconSize(it) }
                }
                iconPackSettings.optString("iconColor")?.let { colorId ->
                    IconPackManager.IconColor.fromId(colorId).let { iconPackManager.setIconColor(it) }
                }
                iconPackSettings.optString("iconBackground")?.let { backgroundId ->
                    IconPackManager.IconBackground.fromId(backgroundId).let { iconPackManager.setIconBackground(it) }
                }
            }
            
            // Import app whitelist settings
            themeData.optJSONObject("appWhitelist")?.let { whitelistSettings ->
                whitelistSettings.optJSONArray("whitelistedApps")?.let { appsArray ->
                    val apps = mutableListOf<String>()
                    for (i in 0 until appsArray.length()) {
                        apps.add(appsArray.getString(i))
                    }
                    // Clear existing whitelist and add new packages
                    appWhitelistManager.clearWhitelist()
                    apps.forEach { packageName ->
                        appWhitelistManager.addToWhitelist(packageName)
                    }
                }
                whitelistSettings.optString("focusMode")?.let { focusMode ->
                    appWhitelistManager.setFocusMode(focusMode)
                }
                whitelistSettings.optBoolean("grayscaleMode")?.let { grayscale ->
                    appWhitelistManager.setGrayscaleMode(grayscale)
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save theme to file
     */
    fun saveThemeToFile(themeName: String): String {
        val themeData = exportCompleteTheme()
        val exportDir = File(context.filesDir, EXPORT_DIR)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val fileName = "${themeName.replace(" ", "_")}_${System.currentTimeMillis()}.json"
        val themeFile = File(exportDir, fileName)
        
        FileWriter(themeFile).use { writer ->
            writer.write(themeData)
        }
        
        return themeFile.absolutePath
    }
    
    /**
     * Load theme from file
     */
    fun loadThemeFromFile(filePath: String): Boolean {
        return try {
            val themeFile = File(filePath)
            if (!themeFile.exists()) {
                return false
            }
            
            val themeData = StringBuilder()
            FileReader(themeFile).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        themeData.append(line).append("\n")
                    }
                }
            }
            
            importCompleteTheme(themeData.toString())
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get list of saved themes
     */
    fun getSavedThemes(): List<ThemeInfo> {
        val exportDir = File(context.filesDir, EXPORT_DIR)
        if (!exportDir.exists()) {
            return emptyList()
        }
        
        return exportDir.listFiles()?.filter { it.extension == "json" }?.map { file ->
            val themeName = file.nameWithoutExtension.split("_").dropLast(1).joinToString(" ")
            val timestamp = file.lastModified()
            ThemeInfo(themeName, file.absolutePath, timestamp)
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }
    
    /**
     * Delete saved theme
     */
    fun deleteTheme(filePath: String): Boolean {
        return try {
            val themeFile = File(filePath)
            themeFile.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Export theme to external storage
     */
    fun exportThemeToExternal(themeName: String): String? {
        return try {
            val themeData = exportCompleteTheme()
            val externalDir = File(context.getExternalFilesDir(null), "theme_exports")
            if (!externalDir.exists()) {
                externalDir.mkdirs()
            }
            
            val fileName = "${themeName.replace(" ", "_")}_${System.currentTimeMillis()}.json"
            val themeFile = File(externalDir, fileName)
            
            FileWriter(themeFile).use { writer ->
                writer.write(themeData)
            }
            
            themeFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Import theme from external storage
     */
    fun importThemeFromExternal(filePath: String): Boolean {
        return try {
            val themeFile = File(filePath)
            if (!themeFile.exists()) {
                return false
            }
            
            val themeData = StringBuilder()
            FileReader(themeFile).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        themeData.append(line).append("\n")
                    }
                }
            }
            
            importCompleteTheme(themeData.toString())
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get theme preview data
     */
    fun getThemePreview(themeName: String): ThemePreview? {
        return try {
            val savedThemes = getSavedThemes()
            val themeInfo = savedThemes.find { it.name == themeName }
            
            if (themeInfo != null) {
                val themeFile = File(themeInfo.filePath)
                val themeData = StringBuilder()
                FileReader(themeFile).use { reader ->
                    BufferedReader(reader).use { bufferedReader ->
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            themeData.append(line).append("\n")
                        }
                    }
                }
                
                val json = JSONObject(themeData.toString())
                val themeSettings = json.optJSONObject("theme")
                val wallpaperSettings = json.optJSONObject("wallpaper")
                val animationSettings = json.optJSONObject("animation")
                
                ThemePreview(
                    themeName = themeName,
                    theme = themeSettings?.optString("currentTheme") ?: "dark",
                    accentColor = themeSettings?.optString("accentColor") ?: "blue",
                    wallpaper = wallpaperSettings?.optString("wallpaperPath") ?: "",
                    animation = animationSettings?.optString("animationSpeed") ?: "normal"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    data class ThemeInfo(
        val name: String,
        val filePath: String,
        val timestamp: Long
    )
    
    data class ThemePreview(
        val themeName: String,
        val theme: String,
        val accentColor: String,
        val wallpaper: String,
        val animation: String
    )
}
