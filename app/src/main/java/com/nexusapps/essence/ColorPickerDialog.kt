package com.nexusapps.essence

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

/**
 * ColorPickerDialog provides a custom color picker interface for selecting accent colors
 */
class ColorPickerDialog(
    private val context: Context,
    private val currentColor: Int,
    private val onColorSelected: (Int) -> Unit
) {
    
    private var dialog: Dialog? = null
    private var selectedColor: Int = currentColor
    private var customColorPreview: View? = null
    
    companion object {
        private val PREDEFINED_COLORS = listOf(
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#E91E63")  // Pink
        )
    }
    
    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        
        customColorPreview = dialogView.findViewById(R.id.customColorPreview)
        val predefinedColorsGrid = dialogView.findViewById<GridLayout>(R.id.predefinedColorsGrid)
        val chooseCustomColorButton = dialogView.findViewById<Button>(R.id.chooseCustomColorButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val applyButton = dialogView.findViewById<Button>(R.id.applyButton)
        
        // Set up predefined colors grid
        setupPredefinedColorsGrid(predefinedColorsGrid)
        
        // Set up custom color preview
        updateCustomColorPreview()
        
        // Set up custom color picker button
        chooseCustomColorButton.setOnClickListener {
            showCustomColorPicker()
        }
        
        // Set up action buttons
        cancelButton.setOnClickListener {
            dialog?.dismiss()
        }
        
        applyButton.setOnClickListener {
            onColorSelected(selectedColor)
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
    
    private fun setupPredefinedColorsGrid(gridLayout: GridLayout) {
        // Clear existing views
        gridLayout.removeAllViews()
        
        PREDEFINED_COLORS.forEach { color ->
            val colorView = createColorView(color)
            gridLayout.addView(colorView)
        }
    }
    
    private fun createColorView(color: Int): View {
        val colorView = View(context)
        val size = (48 * context.resources.displayMetrics.density).toInt()
        
        val layoutParams = GridLayout.LayoutParams().apply {
            width = size
            height = size
            setMargins(8, 8, 8, 8)
        }
        
        colorView.layoutParams = layoutParams
        colorView.setBackgroundColor(color)
        colorView.background = ContextCompat.getDrawable(context, R.drawable.circular_button_background)
        
        // Add border if this is the selected color
        if (color == selectedColor) {
            colorView.background = ContextCompat.getDrawable(context, R.drawable.circular_button_background)
        }
        
        colorView.setOnClickListener {
            selectedColor = color
            updateCustomColorPreview()
            updatePredefinedColorsSelection()
        }
        
        return colorView
    }
    
    private fun updateCustomColorPreview() {
        customColorPreview?.setBackgroundColor(selectedColor)
    }
    
    private fun updatePredefinedColorsSelection() {
        // Update the predefined colors grid to show selection
        val gridLayout = dialog?.findViewById<GridLayout>(R.id.predefinedColorsGrid)
        gridLayout?.let { grid ->
            for (i in 0 until grid.childCount) {
                val child = grid.getChildAt(i)
                val isSelected = PREDEFINED_COLORS[i] == selectedColor
                
                if (isSelected) {
                    child.background = ContextCompat.getDrawable(context, R.drawable.circular_button_background)
                } else {
                    child.setBackgroundColor(PREDEFINED_COLORS[i])
                }
            }
        }
    }
    
    private fun showCustomColorPicker() {
        // For now, show a simple toast. In a real implementation, you would use a proper color picker
        Toast.makeText(context, "Custom color picker coming soon!", Toast.LENGTH_SHORT).show()
        
        // For demonstration, cycle through some colors
        val colors = listOf(
            Color.parseColor("#FF6B6B"), // Coral
            Color.parseColor("#4ECDC4"), // Turquoise
            Color.parseColor("#45B7D1"), // Sky Blue
            Color.parseColor("#96CEB4"), // Mint
            Color.parseColor("#FFEAA7"), // Yellow
            Color.parseColor("#DDA0DD"), // Plum
            Color.parseColor("#98D8C8"), // Seafoam
            Color.parseColor("#F7DC6F")  // Gold
        )
        
        val randomColor = colors.random()
        selectedColor = randomColor
        updateCustomColorPreview()
        updatePredefinedColorsSelection()
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}
