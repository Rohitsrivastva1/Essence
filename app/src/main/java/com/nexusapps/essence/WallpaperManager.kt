package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * WallpaperManager handles wallpaper selection, application, and overlay effects
 */
class WallpaperManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val WALLPAPER_PATH_KEY = "wallpaper_path"
        private const val OVERLAY_OPACITY_KEY = "overlay_opacity"
        private const val OVERLAY_COLOR_KEY = "overlay_color"
        private const val BLUR_ENABLED_KEY = "blur_enabled"
        private const val GRADIENT_ENABLED_KEY = "gradient_enabled"
        private const val GRADIENT_COLORS_KEY = "gradient_colors"
    }
    
    enum class OverlayType {
        NONE, SOLID, GRADIENT, BLUR
    }
    
    /**
     * Set custom wallpaper from file path
     */
    fun setWallpaperFromPath(path: String) {
        prefs.edit().putString(WALLPAPER_PATH_KEY, path).apply()
    }
    
    /**
     * Get current wallpaper path
     */
    fun getCurrentWallpaperPath(): String? {
        return prefs.getString(WALLPAPER_PATH_KEY, null)
    }
    
    /**
     * Set overlay opacity (0.0 to 1.0)
     */
    fun setOverlayOpacity(opacity: Float) {
        prefs.edit().putFloat(OVERLAY_OPACITY_KEY, opacity.coerceIn(0f, 1f)).apply()
    }
    
    /**
     * Get overlay opacity
     */
    fun getOverlayOpacity(): Float {
        return prefs.getFloat(OVERLAY_OPACITY_KEY, 0.3f)
    }
    
    /**
     * Set overlay color
     */
    fun setOverlayColor(color: Int) {
        prefs.edit().putInt(OVERLAY_COLOR_KEY, color).apply()
    }
    
    /**
     * Get overlay color
     */
    fun getOverlayColor(): Int {
        return prefs.getInt(OVERLAY_COLOR_KEY, Color.BLACK)
    }
    
    /**
     * Set blur enabled
     */
    fun setBlurEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BLUR_ENABLED_KEY, enabled).apply()
    }
    
    /**
     * Is blur enabled
     */
    fun isBlurEnabled(): Boolean {
        return prefs.getBoolean(BLUR_ENABLED_KEY, false)
    }
    
    /**
     * Set gradient enabled
     */
    fun setGradientEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(GRADIENT_ENABLED_KEY, enabled).apply()
    }
    
    /**
     * Is gradient enabled
     */
    fun isGradientEnabled(): Boolean {
        return prefs.getBoolean(GRADIENT_ENABLED_KEY, false)
    }
    
    /**
     * Set gradient colors
     */
    fun setGradientColors(colors: IntArray) {
        val colorString = colors.joinToString(",") { it.toString() }
        prefs.edit().putString(GRADIENT_COLORS_KEY, colorString).apply()
    }
    
    /**
     * Get gradient colors
     */
    fun getGradientColors(): IntArray {
        val colorString = prefs.getString(GRADIENT_COLORS_KEY, null)
        return if (colorString != null) {
            colorString.split(",").map { it.toInt() }.toIntArray()
        } else {
            intArrayOf(Color.BLACK, Color.TRANSPARENT)
        }
    }
    
    /**
     * Apply wallpaper to a view
     */
    fun applyWallpaperToView(view: View) {
        val wallpaperPath = getCurrentWallpaperPath()
        
        if (wallpaperPath != null && File(wallpaperPath).exists()) {
            // Load custom wallpaper
            val bitmap = BitmapFactory.decodeFile(wallpaperPath)
            if (bitmap != null) {
                val drawable = createWallpaperDrawable(bitmap)
                view.background = drawable
            }
        } else {
            // Apply default theme-based background
            applyDefaultBackground(view)
        }
    }
    
    /**
     * Create wallpaper drawable with overlay effects
     */
    private fun createWallpaperDrawable(bitmap: Bitmap): Drawable {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1920, true)
        val wallpaperDrawable = BitmapDrawable(context.resources, scaledBitmap)
        
        val overlays = mutableListOf<Drawable>()
        overlays.add(wallpaperDrawable)
        
        // Add blur effect if enabled
        if (isBlurEnabled()) {
            val blurredDrawable = createBlurredDrawable(scaledBitmap)
            overlays.add(blurredDrawable)
        }
        
        // Add gradient overlay if enabled
        if (isGradientEnabled()) {
            val gradientDrawable = createGradientDrawable()
            overlays.add(gradientDrawable)
        }
        
        // Add solid overlay
        val solidOverlay = createSolidOverlay()
        overlays.add(solidOverlay)
        
        return LayerDrawable(overlays.toTypedArray())
    }
    
    /**
     * Create blurred drawable
     */
    private fun createBlurredDrawable(bitmap: Bitmap): Drawable {
        // Simple blur effect using color filter
        val blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(blurredBitmap)
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.OVERLAY)
            alpha = 128
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return BitmapDrawable(context.resources, blurredBitmap)
    }
    
    /**
     * Create gradient drawable
     */
    private fun createGradientDrawable(): Drawable {
        val colors = getGradientColors()
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            colors
        )
    }
    
    /**
     * Create solid overlay
     */
    private fun createSolidOverlay(): Drawable {
        val overlayColor = getOverlayColor()
        val opacity = (getOverlayOpacity() * 255).toInt()
        val colorWithOpacity = Color.argb(opacity, Color.red(overlayColor), Color.green(overlayColor), Color.blue(overlayColor))
        
        return GradientDrawable().apply {
            setColor(colorWithOpacity)
        }
    }
    
    /**
     * Apply default theme-based background
     */
    private fun applyDefaultBackground(view: View) {
        val themeManager = ThemeManager(context)
        val theme = themeManager.getCurrentTheme()
        
        val backgroundColor = when (theme) {
            ThemeManager.Theme.DARK -> Color.parseColor("#121212")
            ThemeManager.Theme.LIGHT -> Color.parseColor("#FFFFFF")
            ThemeManager.Theme.HIGH_CONTRAST -> Color.parseColor("#000000")
            ThemeManager.Theme.AMOLED_BLACK -> Color.parseColor("#000000")
        }
        
        view.setBackgroundColor(backgroundColor)
    }
    
    /**
     * Get built-in wallpapers
     */
    fun getBuiltInWallpapers(): List<WallpaperInfo> {
        return listOf(
            WallpaperInfo("Dark Gradient", createDarkGradientWallpaper()),
            WallpaperInfo("Light Gradient", createLightGradientWallpaper()),
            WallpaperInfo("Blue Gradient", createBlueGradientWallpaper()),
            WallpaperInfo("Purple Gradient", createPurpleGradientWallpaper()),
            WallpaperInfo("Green Gradient", createGreenGradientWallpaper()),
            WallpaperInfo("Red Gradient", createRedGradientWallpaper())
        )
    }
    
    /**
     * Create dark gradient wallpaper
     */
    private fun createDarkGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#1a1a1a"),
                Color.parseColor("#000000")
            )
        )
    }
    
    /**
     * Create light gradient wallpaper
     */
    private fun createLightGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#f5f5f5"),
                Color.parseColor("#e0e0e0")
            )
        )
    }
    
    /**
     * Create blue gradient wallpaper
     */
    private fun createBlueGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#1e3c72"),
                Color.parseColor("#2a5298")
            )
        )
    }
    
    /**
     * Create purple gradient wallpaper
     */
    private fun createPurpleGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#667eea"),
                Color.parseColor("#764ba2")
            )
        )
    }
    
    /**
     * Create green gradient wallpaper
     */
    private fun createGreenGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#56ab2f"),
                Color.parseColor("#a8e6cf")
            )
        )
    }
    
    /**
     * Create red gradient wallpaper
     */
    private fun createRedGradientWallpaper(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#ff416c"),
                Color.parseColor("#ff4b2b")
            )
        )
    }
    
    /**
     * Reset to default wallpaper
     */
    fun resetToDefault() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Export wallpaper settings
     */
    fun exportSettings(): String {
        val settings = mapOf(
            "wallpaperPath" to (getCurrentWallpaperPath() ?: ""),
            "overlayOpacity" to getOverlayOpacity(),
            "overlayColor" to getOverlayColor(),
            "blurEnabled" to isBlurEnabled(),
            "gradientEnabled" to isGradientEnabled(),
            "gradientColors" to getGradientColors().joinToString(",")
        )
        return org.json.JSONObject(settings).toString(2)
    }
    
    /**
     * Import wallpaper settings
     */
    fun importSettings(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            
            json.optString("wallpaperPath")?.let { path ->
                if (path.isNotEmpty()) {
                    setWallpaperFromPath(path)
                }
            }
            
            json.optDouble("overlayOpacity")?.let { opacity ->
                setOverlayOpacity(opacity.toFloat())
            }
            
            json.optInt("overlayColor")?.let { color ->
                setOverlayColor(color)
            }
            
            json.optBoolean("blurEnabled")?.let { enabled ->
                setBlurEnabled(enabled)
            }
            
            json.optBoolean("gradientEnabled")?.let { enabled ->
                setGradientEnabled(enabled)
            }
            
            json.optString("gradientColors")?.let { colorsString ->
                val colors = colorsString.split(",").map { it.toInt() }.toIntArray()
                setGradientColors(colors)
            }
        } catch (e: Exception) {
            // Invalid JSON, keep current settings
        }
    }
    
    data class WallpaperInfo(
        val name: String,
        val drawable: Drawable
    )
}
