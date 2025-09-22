package com.nexusapps.essence

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

/**
 * ThemePreviewDialog provides a live preview of theme changes
 */
class ThemePreviewDialog(
    private val context: Context,
    private val currentTheme: ThemeManager.Theme,
    private val currentAccentColor: ThemeManager.AccentColor,
    private val currentFontSize: ThemeManager.FontSize,
    private val currentGridDensity: ThemeManager.GridDensity,
    private val currentIconSize: ThemeManager.IconSize,
    private val onThemeApplied: (ThemeManager.Theme, ThemeManager.AccentColor, ThemeManager.FontSize, ThemeManager.GridDensity, ThemeManager.IconSize) -> Unit
) {
    
    private var dialog: Dialog? = null
    private var previewTheme = currentTheme
    private var previewAccentColor = currentAccentColor
    private var previewFontSize = currentFontSize
    private var previewGridDensity = currentGridDensity
    private var previewIconSize = currentIconSize
    
    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_theme_preview, null)
        
        val previewContainer = dialogView.findViewById<FrameLayout>(R.id.previewContainer)
        val themeNameText = dialogView.findViewById<TextView>(R.id.themeNameText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelPreviewButton)
        val applyButton = dialogView.findViewById<Button>(R.id.applyPreviewButton)
        
        // Set up preview
        updatePreview(previewContainer, themeNameText)
        
        // Set up action buttons
        cancelButton.setOnClickListener {
            dialog?.dismiss()
        }
        
        applyButton.setOnClickListener {
            onThemeApplied(previewTheme, previewAccentColor, previewFontSize, previewGridDensity, previewIconSize)
            dialog?.dismiss()
        }
        
        // Create and show dialog
        dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()
    }
    
    /**
     * Update theme preview
     */
    fun updateTheme(theme: ThemeManager.Theme) {
        previewTheme = theme
        updatePreview()
    }
    
    /**
     * Update accent color preview
     */
    fun updateAccentColor(accentColor: ThemeManager.AccentColor) {
        previewAccentColor = accentColor
        updatePreview()
    }
    
    /**
     * Update font size preview
     */
    fun updateFontSize(fontSize: ThemeManager.FontSize) {
        previewFontSize = fontSize
        updatePreview()
    }
    
    /**
     * Update grid density preview
     */
    fun updateGridDensity(gridDensity: ThemeManager.GridDensity) {
        previewGridDensity = gridDensity
        updatePreview()
    }
    
    /**
     * Update icon size preview
     */
    fun updateIconSize(iconSize: ThemeManager.IconSize) {
        previewIconSize = iconSize
        updatePreview()
    }
    
    private fun updatePreview() {
        val previewContainer = dialog?.findViewById<FrameLayout>(R.id.previewContainer)
        val themeNameText = dialog?.findViewById<TextView>(R.id.themeNameText)
        if (previewContainer != null && themeNameText != null) {
            updatePreview(previewContainer, themeNameText)
        }
    }
    
    private fun updatePreview(previewContainer: FrameLayout, themeNameText: TextView) {
        // Update theme name
        themeNameText.text = previewTheme.displayName
        
        // Apply theme colors
        val themeColors = getThemeColors(previewTheme)
        previewContainer.setBackgroundColor(themeColors.background)
        
        // Apply accent color
        val accentColor = getAccentColorValue(previewAccentColor)
        
        // Update text colors
        updateTextColors(previewContainer, themeColors, accentColor)
        
        // Update font sizes
        updateFontSizes(previewContainer, previewFontSize)
        
        // Update icon sizes
        updateIconSizes(previewContainer, previewIconSize)
        
        // Update grid layout
        updateGridLayout(previewContainer, previewGridDensity)
    }
    
    private fun getThemeColors(theme: ThemeManager.Theme): ThemeColors {
        return when (theme) {
            ThemeManager.Theme.DARK -> ThemeColors(
                background = Color.parseColor("#121212"),
                primary = Color.parseColor("#1E1E1E"),
                textPrimary = Color.parseColor("#FFFFFF"),
                textSecondary = Color.parseColor("#B3B3B3")
            )
            ThemeManager.Theme.LIGHT -> ThemeColors(
                background = Color.parseColor("#FFFFFF"),
                primary = Color.parseColor("#F5F5F5"),
                textPrimary = Color.parseColor("#212121"),
                textSecondary = Color.parseColor("#757575")
            )
            ThemeManager.Theme.HIGH_CONTRAST -> ThemeColors(
                background = Color.parseColor("#000000"),
                primary = Color.parseColor("#000000"),
                textPrimary = Color.parseColor("#FFFFFF"),
                textSecondary = Color.parseColor("#CCCCCC")
            )
            ThemeManager.Theme.AMOLED_BLACK -> ThemeColors(
                background = Color.parseColor("#000000"),
                primary = Color.parseColor("#000000"),
                textPrimary = Color.parseColor("#FFFFFF"),
                textSecondary = Color.parseColor("#888888")
            )
        }
    }
    
    private fun getAccentColorValue(accentColor: ThemeManager.AccentColor): Int {
        return when (accentColor) {
            ThemeManager.AccentColor.BLUE -> Color.parseColor("#2196F3")
            ThemeManager.AccentColor.GREEN -> Color.parseColor("#4CAF50")
            ThemeManager.AccentColor.PURPLE -> Color.parseColor("#9C27B0")
            ThemeManager.AccentColor.ORANGE -> Color.parseColor("#FF9800")
            ThemeManager.AccentColor.RED -> Color.parseColor("#F44336")
            ThemeManager.AccentColor.CYAN -> Color.parseColor("#00BCD4")
            ThemeManager.AccentColor.TEAL -> Color.parseColor("#009688")
            ThemeManager.AccentColor.PINK -> Color.parseColor("#E91E63")
            ThemeManager.AccentColor.CUSTOM -> Color.parseColor("#2196F3") // Default to blue
        }
    }
    
    private fun updateTextColors(container: android.view.ViewGroup, colors: ThemeColors, accentColor: Int) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            when (child) {
                is TextView -> {
                    when (child.id) {
                        R.id.previewTime -> child.setTextColor(accentColor)
                        R.id.previewDate -> child.setTextColor(colors.textSecondary)
                        else -> child.setTextColor(colors.textPrimary)
                    }
                }
                is android.view.ViewGroup -> updateTextColors(child, colors, accentColor)
            }
        }
    }
    
    private fun updateFontSizes(container: android.view.ViewGroup, fontSize: ThemeManager.FontSize) {
        val scale = fontSize.scale
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            when (child) {
                is TextView -> {
                    when (child.id) {
                        R.id.previewTime -> child.textSize = 32f * scale
                        R.id.previewDate -> child.textSize = 14f * scale
                        else -> child.textSize = 12f * scale
                    }
                }
                is android.view.ViewGroup -> updateFontSizes(child, fontSize)
            }
        }
    }
    
    private fun updateIconSizes(container: android.view.ViewGroup, iconSize: ThemeManager.IconSize) {
        val scale = iconSize.scale
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            when (child) {
                is android.widget.ImageView -> {
                    val layoutParams = child.layoutParams
                    layoutParams.width = (48 * scale).toInt()
                    layoutParams.height = (48 * scale).toInt()
                    child.layoutParams = layoutParams
                }
                is android.view.ViewGroup -> updateIconSizes(child, iconSize)
            }
        }
    }
    
    private fun updateGridLayout(container: android.view.ViewGroup, gridDensity: ThemeManager.GridDensity) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is android.widget.GridLayout) {
                when (gridDensity) {
                    ThemeManager.GridDensity.COMPACT -> {
                        child.columnCount = 4
                        child.rowCount = 6
                    }
                    ThemeManager.GridDensity.NORMAL -> {
                        child.columnCount = 3
                        child.rowCount = 5
                    }
                    ThemeManager.GridDensity.SPACIOUS -> {
                        child.columnCount = 3
                        child.rowCount = 4
                    }
                }
            } else if (child is android.view.ViewGroup) {
                updateGridLayout(child, gridDensity)
            }
        }
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
    
    data class ThemeColors(
        val background: Int,
        val primary: Int,
        val textPrimary: Int,
        val textSecondary: Int
    )
}
