package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat

/**
 * IconPackManager handles icon pack selection and customization
 */
class IconPackManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("icon_pack_prefs", Context.MODE_PRIVATE)
    private val packageManager: PackageManager = context.packageManager
    
    companion object {
        private const val ICON_PACK_KEY = "icon_pack"
        private const val ICON_STYLE_KEY = "icon_style"
        private const val ICON_SHAPE_KEY = "icon_shape"
        private const val ICON_SIZE_KEY = "icon_size"
        private const val ICON_COLOR_KEY = "icon_color"
        private const val ICON_BACKGROUND_KEY = "icon_background"
        
        val DEFAULT_ICON_PACK = IconPack.SYSTEM
        val DEFAULT_ICON_STYLE = IconStyle.ORIGINAL
        val DEFAULT_ICON_SHAPE = IconShape.ROUNDED_SQUARE
        val DEFAULT_ICON_SIZE = IconSize.NORMAL
        val DEFAULT_ICON_COLOR = IconColor.ORIGINAL
        val DEFAULT_ICON_BACKGROUND = IconBackground.NONE
    }
    
    enum class IconPack(val id: String, val displayName: String) {
        SYSTEM("system", "System Icons"),
        MATERIAL("material", "Material Design"),
        CUSTOM("custom", "Custom Icons"),
        MONOCHROME("monochrome", "Monochrome"),
        GRADIENT("gradient", "Gradient Icons");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_PACK
        }
    }
    
    enum class IconStyle(val id: String, val displayName: String) {
        ORIGINAL("original", "Original"),
        OUTLINE("outline", "Outline"),
        FILLED("filled", "Filled"),
        GRADIENT("gradient", "Gradient");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_STYLE
        }
    }
    
    enum class IconShape(val id: String, val displayName: String) {
        SQUARE("square", "Square"),
        ROUNDED_SQUARE("rounded_square", "Rounded Square"),
        CIRCLE("circle", "Circle"),
        ROUNDED_RECTANGLE("rounded_rectangle", "Rounded Rectangle");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_SHAPE
        }
    }
    
    enum class IconSize(val id: String, val displayName: String, val scale: Float) {
        SMALL("small", "Small", 0.8f),
        NORMAL("normal", "Normal", 1.0f),
        LARGE("large", "Large", 1.2f),
        EXTRA_LARGE("extra_large", "Extra Large", 1.4f);
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_SIZE
        }
    }
    
    enum class IconColor(val id: String, val displayName: String) {
        ORIGINAL("original", "Original"),
        MONOCHROME("monochrome", "Monochrome"),
        ACCENT("accent", "Accent Color"),
        THEME("theme", "Theme Color");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_COLOR
        }
    }
    
    enum class IconBackground(val id: String, val displayName: String) {
        NONE("none", "None"),
        SOLID("solid", "Solid"),
        GRADIENT("gradient", "Gradient"),
        OUTLINE("outline", "Outline");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_BACKGROUND
        }
    }
    
    fun setIconPack(iconPack: IconPack) {
        prefs.edit().putString(ICON_PACK_KEY, iconPack.id).apply()
    }
    
    fun getCurrentIconPack(): IconPack {
        val packId = prefs.getString(ICON_PACK_KEY, DEFAULT_ICON_PACK.id) ?: DEFAULT_ICON_PACK.id
        return IconPack.fromId(packId)
    }
    
    fun setIconStyle(style: IconStyle) {
        prefs.edit().putString(ICON_STYLE_KEY, style.id).apply()
    }
    
    fun getCurrentIconStyle(): IconStyle {
        val styleId = prefs.getString(ICON_STYLE_KEY, DEFAULT_ICON_STYLE.id) ?: DEFAULT_ICON_STYLE.id
        return IconStyle.fromId(styleId)
    }
    
    fun setIconShape(shape: IconShape) {
        prefs.edit().putString(ICON_SHAPE_KEY, shape.id).apply()
    }
    
    fun getCurrentIconShape(): IconShape {
        val shapeId = prefs.getString(ICON_SHAPE_KEY, DEFAULT_ICON_SHAPE.id) ?: DEFAULT_ICON_SHAPE.id
        return IconShape.fromId(shapeId)
    }
    
    fun setIconSize(size: IconSize) {
        prefs.edit().putString(ICON_SIZE_KEY, size.id).apply()
    }
    
    fun getCurrentIconSize(): IconSize {
        val sizeId = prefs.getString(ICON_SIZE_KEY, DEFAULT_ICON_SIZE.id) ?: DEFAULT_ICON_SIZE.id
        return IconSize.fromId(sizeId)
    }
    
    fun setIconColor(color: IconColor) {
        prefs.edit().putString(ICON_COLOR_KEY, color.id).apply()
    }
    
    fun getCurrentIconColor(): IconColor {
        val colorId = prefs.getString(ICON_COLOR_KEY, DEFAULT_ICON_COLOR.id) ?: DEFAULT_ICON_COLOR.id
        return IconColor.fromId(colorId)
    }
    
    fun setIconBackground(background: IconBackground) {
        prefs.edit().putString(ICON_BACKGROUND_KEY, background.id).apply()
    }
    
    fun getCurrentIconBackground(): IconBackground {
        val backgroundId = prefs.getString(ICON_BACKGROUND_KEY, DEFAULT_ICON_BACKGROUND.id) ?: DEFAULT_ICON_BACKGROUND.id
        return IconBackground.fromId(backgroundId)
    }
    
    fun getAllIconPacks(): List<IconPack> = IconPack.values().toList()
    fun getAllIconStyles(): List<IconStyle> = IconStyle.values().toList()
    fun getAllIconShapes(): List<IconShape> = IconShape.values().toList()
    fun getAllIconSizes(): List<IconSize> = IconSize.values().toList()
    fun getAllIconColors(): List<IconColor> = IconColor.values().toList()
    fun getAllIconBackgrounds(): List<IconBackground> = IconBackground.values().toList()
    
    /**
     * Apply icon pack customization to a drawable
     */
    fun applyIconPack(drawable: Drawable, packageName: String): Drawable {
        val iconPack = getCurrentIconPack()
        val style = getCurrentIconStyle()
        val shape = getCurrentIconShape()
        val size = getCurrentIconSize()
        val color = getCurrentIconColor()
        val background = getCurrentIconBackground()
        
        return when (iconPack) {
            IconPack.SYSTEM -> applySystemIconPack(drawable, style, shape, size, color, background)
            IconPack.MATERIAL -> applyMaterialIconPack(drawable, style, shape, size, color, background)
            IconPack.CUSTOM -> applyCustomIconPack(drawable, packageName, style, shape, size, color, background)
            IconPack.MONOCHROME -> applyMonochromeIconPack(drawable, style, shape, size, color, background)
            IconPack.GRADIENT -> applyGradientIconPack(drawable, style, shape, size, color, background)
        }
    }
    
    private fun applySystemIconPack(
        drawable: Drawable,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        return createCustomIcon(drawable, style, shape, size, color, background)
    }
    
    private fun applyMaterialIconPack(
        drawable: Drawable,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        // Apply Material Design styling
        val materialDrawable = when (style) {
            IconStyle.OUTLINE -> createOutlineIcon(drawable)
            IconStyle.FILLED -> createFilledIcon(drawable)
            IconStyle.GRADIENT -> createGradientIcon(drawable)
            else -> drawable
        }
        
        return createCustomIcon(materialDrawable, style, shape, size, color, background)
    }
    
    private fun applyCustomIconPack(
        drawable: Drawable,
        packageName: String,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        // Try to load custom icon from resources
        val customIcon = loadCustomIcon(packageName)
        val iconToUse = customIcon ?: drawable
        
        return createCustomIcon(iconToUse, style, shape, size, color, background)
    }
    
    private fun applyMonochromeIconPack(
        drawable: Drawable,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        val monochromeDrawable = createMonochromeIcon(drawable)
        return createCustomIcon(monochromeDrawable, style, shape, size, color, background)
    }
    
    private fun applyGradientIconPack(
        drawable: Drawable,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        val gradientDrawable = createGradientIcon(drawable)
        return createCustomIcon(gradientDrawable, style, shape, size, color, background)
    }
    
    private fun createCustomIcon(
        drawable: Drawable,
        style: IconStyle,
        shape: IconShape,
        size: IconSize,
        color: IconColor,
        background: IconBackground
    ): Drawable {
        val scaledDrawable = scaleDrawable(drawable, size.scale)
        val coloredDrawable = applyColor(scaledDrawable, color)
        val shapedDrawable = applyShape(coloredDrawable, shape)
        val backgroundDrawable = applyBackground(shapedDrawable, background, shape)
        
        return backgroundDrawable
    }
    
    private fun scaleDrawable(drawable: Drawable, scale: Float): Drawable {
        val width = (drawable.intrinsicWidth * scale).toInt()
        val height = (drawable.intrinsicHeight * scale).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun applyColor(drawable: Drawable, color: IconColor): Drawable {
        return when (color) {
            IconColor.ORIGINAL -> drawable
            IconColor.MONOCHROME -> createMonochromeIcon(drawable)
            IconColor.ACCENT -> applyAccentColor(drawable)
            IconColor.THEME -> applyThemeColor(drawable)
        }
    }
    
    private fun applyShape(drawable: Drawable, shape: IconShape): Drawable {
        return when (shape) {
            IconShape.SQUARE -> drawable
            IconShape.ROUNDED_SQUARE -> createRoundedSquareIcon(drawable)
            IconShape.CIRCLE -> createCircleIcon(drawable)
            IconShape.ROUNDED_RECTANGLE -> createRoundedRectangleIcon(drawable)
        }
    }
    
    private fun applyBackground(drawable: Drawable, background: IconBackground, shape: IconShape): Drawable {
        return when (background) {
            IconBackground.NONE -> drawable
            IconBackground.SOLID -> createSolidBackground(drawable, shape)
            IconBackground.GRADIENT -> createGradientBackground(drawable, shape)
            IconBackground.OUTLINE -> createOutlineBackground(drawable, shape)
        }
    }
    
    private fun createOutlineIcon(drawable: Drawable): Drawable {
        // Create outline version of icon
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun createFilledIcon(drawable: Drawable): Drawable {
        // Create filled version of icon
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun createGradientIcon(drawable: Drawable): Drawable {
        // Create gradient version of icon
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"))
        )
        
        return gradientDrawable
    }
    
    private fun createMonochromeIcon(drawable: Drawable): Drawable {
        // Convert to monochrome
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun createRoundedSquareIcon(drawable: Drawable): Drawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 12f
        return gradientDrawable
    }
    
    private fun createCircleIcon(drawable: Drawable): Drawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.OVAL
        return gradientDrawable
    }
    
    private fun createRoundedRectangleIcon(drawable: Drawable): Drawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 8f
        return gradientDrawable
    }
    
    private fun createSolidBackground(drawable: Drawable, shape: IconShape): Drawable {
        val background = when (shape) {
            IconShape.SQUARE -> GradientDrawable().apply {
                setColor(Color.parseColor("#E0E0E0"))
            }
            IconShape.ROUNDED_SQUARE -> GradientDrawable().apply {
                setColor(Color.parseColor("#E0E0E0"))
                cornerRadius = 12f
            }
            IconShape.CIRCLE -> {
                val circleDrawable = GradientDrawable()
                circleDrawable.setColor(Color.parseColor("#E0E0E0"))
                circleDrawable.shape = GradientDrawable.OVAL
                circleDrawable
            }
            IconShape.ROUNDED_RECTANGLE -> GradientDrawable().apply {
                setColor(Color.parseColor("#E0E0E0"))
                cornerRadius = 8f
            }
        }
        
        return background
    }
    
    private fun createGradientBackground(drawable: Drawable, shape: IconShape): Drawable {
        val background = when (shape) {
            IconShape.SQUARE -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"))
            )
            IconShape.ROUNDED_SQUARE -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"))
            ).apply { cornerRadius = 12f }
            IconShape.CIRCLE -> {
                val circleGradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"))
                )
                circleGradient.shape = GradientDrawable.OVAL
                circleGradient
            }
            IconShape.ROUNDED_RECTANGLE -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"))
            ).apply { cornerRadius = 8f }
        }
        
        return background
    }
    
    private fun createOutlineBackground(drawable: Drawable, shape: IconShape): Drawable {
        val background = when (shape) {
            IconShape.SQUARE -> GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#E0E0E0"))
            }
            IconShape.ROUNDED_SQUARE -> GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#E0E0E0"))
                cornerRadius = 12f
            }
            IconShape.CIRCLE -> {
                val circleOutline = GradientDrawable()
                circleOutline.setColor(Color.TRANSPARENT)
                circleOutline.setStroke(2, Color.parseColor("#E0E0E0"))
                circleOutline.shape = GradientDrawable.OVAL
                circleOutline
            }
            IconShape.ROUNDED_RECTANGLE -> GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#E0E0E0"))
                cornerRadius = 8f
            }
        }
        
        return background
    }
    
    private fun loadCustomIcon(packageName: String): Drawable? {
        // Try to load custom icon from resources
        val resourceId = context.resources.getIdentifier(
            "ic_${packageName.replace(".", "_")}",
            "drawable",
            context.packageName
        )
        
        return if (resourceId != 0) {
            ContextCompat.getDrawable(context, resourceId)
        } else {
            null
        }
    }
    
    private fun applyAccentColor(drawable: Drawable): Drawable {
        // Apply accent color to icon
        val themeManager = ThemeManager(context)
        val accentColor = themeManager.getAccentColorValue()
        
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun applyThemeColor(drawable: Drawable): Drawable {
        // Apply theme color to icon
        val themeManager = ThemeManager(context)
        val theme = themeManager.getCurrentTheme()
        
        val color = when (theme) {
            ThemeManager.Theme.DARK -> Color.WHITE
            ThemeManager.Theme.LIGHT -> Color.BLACK
            ThemeManager.Theme.HIGH_CONTRAST -> Color.WHITE
            ThemeManager.Theme.AMOLED_BLACK -> Color.WHITE
        }
        
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        
        drawable.setBounds(0, 0, 48, 48)
        drawable.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    fun exportSettings(): String {
        val settings = mapOf(
            "iconPack" to getCurrentIconPack().id,
            "iconStyle" to getCurrentIconStyle().id,
            "iconShape" to getCurrentIconShape().id,
            "iconSize" to getCurrentIconSize().id,
            "iconColor" to getCurrentIconColor().id,
            "iconBackground" to getCurrentIconBackground().id
        )
        return org.json.JSONObject(settings).toString(2)
    }
    
    fun importSettings(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            
            json.optString("iconPack")?.let { packId ->
                IconPack.fromId(packId).let { setIconPack(it) }
            }
            
            json.optString("iconStyle")?.let { styleId ->
                IconStyle.fromId(styleId).let { setIconStyle(it) }
            }
            
            json.optString("iconShape")?.let { shapeId ->
                IconShape.fromId(shapeId).let { setIconShape(it) }
            }
            
            json.optString("iconSize")?.let { sizeId ->
                IconSize.fromId(sizeId).let { setIconSize(it) }
            }
            
            json.optString("iconColor")?.let { colorId ->
                IconColor.fromId(colorId).let { setIconColor(it) }
            }
            
            json.optString("iconBackground")?.let { backgroundId ->
                IconBackground.fromId(backgroundId).let { setIconBackground(it) }
            }
        } catch (e: Exception) {
            // Invalid JSON, keep current settings
        }
    }
}
