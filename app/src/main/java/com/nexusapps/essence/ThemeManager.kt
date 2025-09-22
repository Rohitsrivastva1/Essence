package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * ThemeManager handles all theme-related functionality including:
 * - Theme switching and persistence
 * - Dynamic theme application
 * - Color palette management
 * - Theme preview functionality
 */
class ThemeManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val THEME_KEY = "current_theme"
        private const val ACCENT_COLOR_KEY = "accent_color"
        private const val CUSTOM_ACCENT_KEY = "custom_accent_color"
        private const val FONT_SIZE_KEY = "font_size"
        private const val GRID_DENSITY_KEY = "grid_density"
        private const val ICON_SIZE_KEY = "icon_size"
    }
    
    enum class Theme(val displayName: String, val resourceName: String) {
        DARK("Dark", "DarkTheme"),
        LIGHT("Light", "LightTheme"),
        HIGH_CONTRAST("High Contrast", "HighContrastTheme"),
        AMOLED_BLACK("AMOLED Black", "AmoledBlackTheme")
    }
    
    enum class AccentColor(val displayName: String, val colorValue: Int) {
        BLUE("Blue", Color.parseColor("#2196F3")),
        GREEN("Green", Color.parseColor("#4CAF50")),
        PURPLE("Purple", Color.parseColor("#9C27B0")),
        ORANGE("Orange", Color.parseColor("#FF9800")),
        RED("Red", Color.parseColor("#F44336")),
        CYAN("Cyan", Color.parseColor("#00BCD4")),
        TEAL("Teal", Color.parseColor("#009688")),
        PINK("Pink", Color.parseColor("#E91E63")),
        CUSTOM("Custom", Color.parseColor("#2196F3")) // Default to blue
    }
    
    enum class FontSize(val displayName: String, val scale: Float) {
        SMALL("Small", 0.85f),
        MEDIUM("Medium", 1.0f),
        LARGE("Large", 1.15f),
        EXTRA_LARGE("Extra Large", 1.3f)
    }
    
    enum class GridDensity(val displayName: String, val columns: Int, val rows: Int) {
        COMPACT("Compact", 4, 6),
        NORMAL("Normal", 3, 5),
        SPACIOUS("Spacious", 3, 4)
    }
    
    enum class IconSize(val displayName: String, val scale: Float) {
        SMALL("Small", 0.8f),
        MEDIUM("Medium", 1.0f),
        LARGE("Large", 1.2f)
    }
    
    /**
     * Get current theme
     */
    fun getCurrentTheme(): Theme {
        val themeName = prefs.getString(THEME_KEY, Theme.DARK.name) ?: Theme.DARK.name
        return try {
            Theme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            Theme.DARK
        }
    }
    
    /**
     * Set current theme
     */
    fun setTheme(theme: Theme) {
        prefs.edit().putString(THEME_KEY, theme.name).apply()
    }
    
    /**
     * Get current accent color
     */
    fun getCurrentAccentColor(): AccentColor {
        val colorName = prefs.getString(ACCENT_COLOR_KEY, AccentColor.BLUE.name) ?: AccentColor.BLUE.name
        return try {
            AccentColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            AccentColor.BLUE
        }
    }
    
    /**
     * Set accent color
     */
    fun setAccentColor(color: AccentColor) {
        prefs.edit().putString(ACCENT_COLOR_KEY, color.name).apply()
    }
    
    /**
     * Set custom accent color
     */
    fun setCustomAccentColor(color: Int) {
        prefs.edit().putInt(CUSTOM_ACCENT_KEY, color).apply()
        setAccentColor(AccentColor.CUSTOM)
    }
    
    /**
     * Get custom accent color
     */
    fun getCustomAccentColor(): Int {
        return prefs.getInt(CUSTOM_ACCENT_KEY, AccentColor.BLUE.colorValue)
    }
    
    /**
     * Get current font size
     */
    fun getCurrentFontSize(): FontSize {
        val fontSizeName = prefs.getString(FONT_SIZE_KEY, FontSize.MEDIUM.name) ?: FontSize.MEDIUM.name
        return try {
            FontSize.valueOf(fontSizeName)
        } catch (e: IllegalArgumentException) {
            FontSize.MEDIUM
        }
    }
    
    /**
     * Set font size
     */
    fun setFontSize(fontSize: FontSize) {
        prefs.edit().putString(FONT_SIZE_KEY, fontSize.name).apply()
    }
    
    /**
     * Get current grid density
     */
    fun getCurrentGridDensity(): GridDensity {
        val gridName = prefs.getString(GRID_DENSITY_KEY, GridDensity.NORMAL.name) ?: GridDensity.NORMAL.name
        return try {
            GridDensity.valueOf(gridName)
        } catch (e: IllegalArgumentException) {
            GridDensity.NORMAL
        }
    }
    
    /**
     * Set grid density
     */
    fun setGridDensity(gridDensity: GridDensity) {
        prefs.edit().putString(GRID_DENSITY_KEY, gridDensity.name).apply()
    }
    
    /**
     * Get current icon size
     */
    fun getCurrentIconSize(): IconSize {
        val iconSizeName = prefs.getString(ICON_SIZE_KEY, IconSize.MEDIUM.name) ?: IconSize.MEDIUM.name
        return try {
            IconSize.valueOf(iconSizeName)
        } catch (e: IllegalArgumentException) {
            IconSize.MEDIUM
        }
    }
    
    /**
     * Set icon size
     */
    fun setIconSize(iconSize: IconSize) {
        prefs.edit().putString(ICON_SIZE_KEY, iconSize.name).apply()
    }
    
    /**
     * Apply theme to activity
     */
    fun applyTheme(activity: AppCompatActivity) {
        val theme = getCurrentTheme()
        val themeResId = getThemeResourceId(theme)
        activity.setTheme(themeResId)
        
        // Apply system UI theme
        applySystemUiTheme(activity, theme)
    }
    
    /**
     * Get theme resource ID
     */
    private fun getThemeResourceId(theme: Theme): Int {
        return when (theme) {
            Theme.DARK -> R.style.DarkTheme
            Theme.LIGHT -> R.style.LightTheme
            Theme.HIGH_CONTRAST -> R.style.HighContrastTheme
            Theme.AMOLED_BLACK -> R.style.AmoledBlackTheme
        }
    }
    
    /**
     * Apply system UI theme
     */
    private fun applySystemUiTheme(activity: AppCompatActivity, theme: Theme) {
        val window = activity.window
        
        when (theme) {
            Theme.DARK -> {
                window.statusBarColor = Color.parseColor("#000000")
                window.navigationBarColor = Color.parseColor("#000000")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.window.decorView.systemUiVisibility = 
                        activity.window.decorView.systemUiVisibility and 
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
            Theme.LIGHT -> {
                window.statusBarColor = Color.parseColor("#FFFFFF")
                window.navigationBarColor = Color.parseColor("#FFFFFF")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.window.decorView.systemUiVisibility = 
                        activity.window.decorView.systemUiVisibility or 
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
            Theme.HIGH_CONTRAST -> {
                window.statusBarColor = Color.parseColor("#000000")
                window.navigationBarColor = Color.parseColor("#000000")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.window.decorView.systemUiVisibility = 
                        activity.window.decorView.systemUiVisibility and 
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
            Theme.AMOLED_BLACK -> {
                window.statusBarColor = Color.parseColor("#000000")
                window.navigationBarColor = Color.parseColor("#000000")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.window.decorView.systemUiVisibility = 
                        activity.window.decorView.systemUiVisibility and 
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }
    
    /**
     * Get current accent color value
     */
    fun getAccentColorValue(): Int {
        val accentColor = getCurrentAccentColor()
        return if (accentColor == AccentColor.CUSTOM) {
            getCustomAccentColor()
        } else {
            accentColor.colorValue
        }
    }
    
    /**
     * Check if dark theme is active
     */
    fun isDarkTheme(): Boolean {
        val theme = getCurrentTheme()
        return theme == Theme.DARK || theme == Theme.AMOLED_BLACK || theme == Theme.HIGH_CONTRAST
    }
    
    /**
     * Check if high contrast theme is active
     */
    fun isHighContrastTheme(): Boolean {
        return getCurrentTheme() == Theme.HIGH_CONTRAST
    }
    
    /**
     * Check if AMOLED black theme is active
     */
    fun isAmoledBlackTheme(): Boolean {
        return getCurrentTheme() == Theme.AMOLED_BLACK
    }
    
    /**
     * Reset all theme settings to default
     */
    fun resetToDefault() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get all available themes
     */
    fun getAllThemes(): List<Theme> {
        return Theme.values().toList()
    }
    
    /**
     * Get all available accent colors
     */
    fun getAllAccentColors(): List<AccentColor> {
        return AccentColor.values().toList()
    }
    
    /**
     * Get all available font sizes
     */
    fun getAllFontSizes(): List<FontSize> {
        return FontSize.values().toList()
    }
    
    /**
     * Get all available grid densities
     */
    fun getAllGridDensities(): List<GridDensity> {
        return GridDensity.values().toList()
    }
    
    /**
     * Get all available icon sizes
     */
    fun getAllIconSizes(): List<IconSize> {
        return IconSize.values().toList()
    }
    
    /**
     * Export theme settings as JSON
     */
    fun exportThemeSettings(): String {
        val settings = mapOf(
            "theme" to getCurrentTheme().name,
            "accentColor" to getCurrentAccentColor().name,
            "customAccentColor" to getCustomAccentColor(),
            "fontSize" to getCurrentFontSize().name,
            "gridDensity" to getCurrentGridDensity().name,
            "iconSize" to getCurrentIconSize().name
        )
        return org.json.JSONObject(settings).toString(2)
    }
    
    /**
     * Import theme settings from JSON
     */
    fun importThemeSettings(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            
            json.optString("theme")?.let { themeName ->
                try {
                    setTheme(Theme.valueOf(themeName))
                } catch (e: IllegalArgumentException) {
                    // Invalid theme name, keep current
                }
            }
            
            json.optString("accentColor")?.let { colorName ->
                try {
                    setAccentColor(AccentColor.valueOf(colorName))
                } catch (e: IllegalArgumentException) {
                    // Invalid color name, keep current
                }
            }
            
            json.optInt("customAccentColor")?.let { colorValue ->
                setCustomAccentColor(colorValue)
            }
            
            json.optString("fontSize")?.let { fontSizeName ->
                try {
                    setFontSize(FontSize.valueOf(fontSizeName))
                } catch (e: IllegalArgumentException) {
                    // Invalid font size name, keep current
                }
            }
            
            json.optString("gridDensity")?.let { gridName ->
                try {
                    setGridDensity(GridDensity.valueOf(gridName))
                } catch (e: IllegalArgumentException) {
                    // Invalid grid density name, keep current
                }
            }
            
            json.optString("iconSize")?.let { iconSizeName ->
                try {
                    setIconSize(IconSize.valueOf(iconSizeName))
                } catch (e: IllegalArgumentException) {
                    // Invalid icon size name, keep current
                }
            }
        } catch (e: Exception) {
            // Invalid JSON, keep current settings
        }
    }
}
