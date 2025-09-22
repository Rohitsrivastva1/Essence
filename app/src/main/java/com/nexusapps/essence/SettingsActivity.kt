    package com.nexusapps.essence

    import android.app.AlertDialog
    import android.content.Intent
    import android.graphics.drawable.Drawable
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.view.LayoutInflater
    import android.view.View
    import android.widget.*
    import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
    import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader

    class SettingsActivity : AppCompatActivity() {
        private lateinit var appWhitelistManager: AppWhitelistManager
        private lateinit var themeManager: ThemeManager
        private lateinit var wallpaperManager: WallpaperManager
        private lateinit var animationManager: AnimationManager
        private lateinit var iconPackManager: IconPackManager
        private lateinit var themeExportManager: ThemeExportManager
        private lateinit var searchEditText: EditText
        private lateinit var appsRecyclerView: RecyclerView
        private lateinit var appsAdapter: AppsAdapter
        private lateinit var backButton: ImageButton
        private lateinit var clearButton: Button
        private lateinit var categorySpinner: Spinner
        private lateinit var focusModeSpinner: Spinner
        private lateinit var themeSpinner: Spinner
        private lateinit var accentColorSpinner: Spinner
        private lateinit var fontSizeSpinner: Spinner
        private lateinit var gridDensitySpinner: Spinner
        private lateinit var iconSizeSpinner: Spinner
        private lateinit var grayscaleSwitch: Switch
        private lateinit var wallpaperButton: Button
        private lateinit var wallpaperSettingsButton: Button
        private lateinit var animationSpeedSpinner: Spinner
        private lateinit var animationStyleSpinner: Spinner
        private lateinit var iconPackSpinner: Spinner
        private lateinit var iconStyleSpinner: Spinner
        private lateinit var themePreviewButton: Button
        private lateinit var animationPreviewButton: Button
        private lateinit var exportThemeButton: Button
        private lateinit var importThemeButton: Button
        private lateinit var analyticsButton: Button
        private lateinit var backupButton: Button
        private lateinit var restoreButton: Button
        
        private var allApps: List<AppInfo> = emptyList()
        private var filteredApps: List<AppInfo> = emptyList()
        private var currentCategory = "All"

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_settings)
            
            // Initialize components
            appWhitelistManager = AppWhitelistManager(this)
            themeManager = ThemeManager(this)
            wallpaperManager = WallpaperManager(this)
            animationManager = AnimationManager(this)
            iconPackManager = IconPackManager(this)
            themeExportManager = ThemeExportManager(this)
            
            // Apply current theme
            themeManager.applyTheme(this)
            
            searchEditText = findViewById(R.id.searchEditText)
            appsRecyclerView = findViewById(R.id.appsRecyclerView)
            backButton = findViewById(R.id.backButton)
            clearButton = findViewById(R.id.clearButton)
            categorySpinner = findViewById(R.id.categorySpinner)
            focusModeSpinner = findViewById(R.id.focusModeSpinner)
            themeSpinner = findViewById(R.id.themeSpinner)
            accentColorSpinner = findViewById(R.id.accentColorSpinner)
            fontSizeSpinner = findViewById(R.id.fontSizeSpinner)
            gridDensitySpinner = findViewById(R.id.gridDensitySpinner)
            iconSizeSpinner = findViewById(R.id.iconSizeSpinner)
            grayscaleSwitch = findViewById(R.id.grayscaleSwitch)
            wallpaperButton = findViewById(R.id.wallpaperButton)
            wallpaperSettingsButton = findViewById(R.id.wallpaperSettingsButton)
            animationSpeedSpinner = findViewById(R.id.animationSpeedSpinner)
            animationStyleSpinner = findViewById(R.id.animationStyleSpinner)
            iconPackSpinner = findViewById(R.id.iconPackSpinner)
            iconStyleSpinner = findViewById(R.id.iconStyleSpinner)
            themePreviewButton = findViewById(R.id.themePreviewButton)
            animationPreviewButton = findViewById(R.id.animationPreviewButton)
            exportThemeButton = findViewById(R.id.exportThemeButton)
            importThemeButton = findViewById(R.id.importThemeButton)
            analyticsButton = findViewById(R.id.analyticsButton)
            backupButton = findViewById(R.id.backupButton)
            restoreButton = findViewById(R.id.restoreButton)
            
            setupUI()
            loadApps()
        }

        private fun setupUI() {
            // Set up back button
            backButton.setOnClickListener {
                finish()
            }
            
            // Set up clear button
            clearButton.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                appWhitelistManager.clearWhitelist()
                    }
                loadApps()
                }
            }
            
            // Set up search functionality
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    filterApps(s.toString())
                }
            })
            
            // Set up category spinner
            val categories = listOf("All") + appWhitelistManager.categoryManager.getAllCategories()
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = categoryAdapter
            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentCategory = categories[position]
                    filterApps(searchEditText.text.toString())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up focus mode spinner
            val focusModes = listOf("All", "Work", "Personal", "Emergency")
            val focusModeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, focusModes)
            focusModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            focusModeSpinner.adapter = focusModeAdapter
            focusModeSpinner.setSelection(focusModes.indexOf(appWhitelistManager.getCurrentFocusMode()))
            focusModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    appWhitelistManager.setFocusMode(focusModes[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up theme spinner
            val themes = themeManager.getAllThemes()
            val themeNames = themes.map { it.displayName }
            val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeNames)
            themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            themeSpinner.adapter = themeAdapter
            themeSpinner.setSelection(themes.indexOf(themeManager.getCurrentTheme()))
            themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedTheme = themes[position]
                    themeManager.setTheme(selectedTheme)
                    applyThemeImmediately(selectedTheme)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up accent color spinner
            val accentColors = themeManager.getAllAccentColors()
            val accentColorNames = accentColors.map { it.displayName }
            val accentColorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accentColorNames)
            accentColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            accentColorSpinner.adapter = accentColorAdapter
            accentColorSpinner.setSelection(accentColors.indexOf(themeManager.getCurrentAccentColor()))
            accentColorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedColor = accentColors[position]
                    if (selectedColor == ThemeManager.AccentColor.CUSTOM) {
                        // Show color picker dialog
                        showColorPickerDialog()
                    } else {
                        themeManager.setAccentColor(selectedColor)
                        applyAccentColorImmediately(selectedColor)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up font size spinner
            val fontSizes = themeManager.getAllFontSizes()
            val fontSizeNames = fontSizes.map { it.displayName }
            val fontSizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontSizeNames)
            fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fontSizeSpinner.adapter = fontSizeAdapter
            fontSizeSpinner.setSelection(fontSizes.indexOf(themeManager.getCurrentFontSize()))
            fontSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedFontSize = fontSizes[position]
                    themeManager.setFontSize(selectedFontSize)
                    applyFontSizeImmediately(selectedFontSize)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up grid density spinner
            val gridDensities = themeManager.getAllGridDensities()
            val gridDensityNames = gridDensities.map { it.displayName }
            val gridDensityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gridDensityNames)
            gridDensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            gridDensitySpinner.adapter = gridDensityAdapter
            gridDensitySpinner.setSelection(gridDensities.indexOf(themeManager.getCurrentGridDensity()))
            gridDensitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedGridDensity = gridDensities[position]
                    themeManager.setGridDensity(selectedGridDensity)
                    applyGridDensityImmediately(selectedGridDensity)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up icon size spinner
            val iconSizes = themeManager.getAllIconSizes()
            val iconSizeNames = iconSizes.map { it.displayName }
            val iconSizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconSizeNames)
            iconSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            iconSizeSpinner.adapter = iconSizeAdapter
            iconSizeSpinner.setSelection(iconSizes.indexOf(themeManager.getCurrentIconSize()))
            iconSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedIconSize = iconSizes[position]
                    themeManager.setIconSize(selectedIconSize)
                    applyIconSizeImmediately(selectedIconSize)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up grayscale switch
            grayscaleSwitch.isChecked = appWhitelistManager.isGrayscaleModeEnabled()
            grayscaleSwitch.setOnCheckedChangeListener { _, isChecked ->
                appWhitelistManager.setGrayscaleMode(isChecked)
                applyGrayscaleImmediately(isChecked)
            }
            
            // Set up wallpaper button
            wallpaperButton.setOnClickListener {
                showWallpaperSelectionDialog()
            }
            
            // Set up wallpaper settings button
            wallpaperSettingsButton.setOnClickListener {
                showWallpaperSettingsDialog()
            }
            
            // Set up animation speed spinner
            val animationSpeeds = animationManager.getAllAnimationSpeeds()
            val animationSpeedNames = animationSpeeds.map { it.displayName }
            val animationSpeedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animationSpeedNames)
            animationSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            animationSpeedSpinner.adapter = animationSpeedAdapter
            animationSpeedSpinner.setSelection(animationSpeeds.indexOf(animationManager.getCurrentAnimationSpeed()))
            animationSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedSpeed = animationSpeeds[position]
                    animationManager.setAnimationSpeed(selectedSpeed)
                    Toast.makeText(this@SettingsActivity, "Animation speed changed to ${selectedSpeed.displayName}", Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up animation style spinner
            val animationStyles = animationManager.getAllAnimationStyles()
            val animationStyleNames = animationStyles.map { it.displayName }
            val animationStyleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animationStyleNames)
            animationStyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            animationStyleSpinner.adapter = animationStyleAdapter
            animationStyleSpinner.setSelection(animationStyles.indexOf(animationManager.getCurrentAnimationStyle()))
            animationStyleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedStyle = animationStyles[position]
                    animationManager.setAnimationStyle(selectedStyle)
                    Toast.makeText(this@SettingsActivity, "Animation style changed to ${selectedStyle.displayName}", Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up theme preview button
            themePreviewButton.setOnClickListener {
                showThemePreviewDialog()
            }
            
            // Set up animation preview button
            animationPreviewButton.setOnClickListener {
                showAnimationPreviewDialog()
            }
            
            // Set up icon pack spinner
            val iconPacks = iconPackManager.getAllIconPacks()
            val iconPackNames = iconPacks.map { it.displayName }
            val iconPackAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconPackNames)
            iconPackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            iconPackSpinner.adapter = iconPackAdapter
            iconPackSpinner.setSelection(iconPacks.indexOf(iconPackManager.getCurrentIconPack()))
            iconPackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedPack = iconPacks[position]
                    iconPackManager.setIconPack(selectedPack)
                    Toast.makeText(this@SettingsActivity, "Icon pack changed to ${selectedPack.displayName}", Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up icon style spinner
            val iconStyles = iconPackManager.getAllIconStyles()
            val iconStyleNames = iconStyles.map { it.displayName }
            val iconStyleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconStyleNames)
            iconStyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            iconStyleSpinner.adapter = iconStyleAdapter
            iconStyleSpinner.setSelection(iconStyles.indexOf(iconPackManager.getCurrentIconStyle()))
            iconStyleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedStyle = iconStyles[position]
                    iconPackManager.setIconStyle(selectedStyle)
                    Toast.makeText(this@SettingsActivity, "Icon style changed to ${selectedStyle.displayName}", Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Set up theme export button
            exportThemeButton.setOnClickListener {
                showThemeExportDialog()
            }
            
            // Set up theme import button
            importThemeButton.setOnClickListener {
                showThemeImportDialog()
            }
            
            // Set up analytics button
            analyticsButton.setOnClickListener {
                // Open analytics activity
                val intent = Intent(this, AnalyticsActivity::class.java)
                startActivity(intent)
            }
            
            // Set up backup button
            backupButton.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val backupFile = createBackupFile()
                        Toast.makeText(this@SettingsActivity, "Settings backed up to: $backupFile", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@SettingsActivity, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            // Set up restore button
            restoreButton.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val restored = restoreFromBackup()
                        if (restored) {
                            Toast.makeText(this@SettingsActivity, "Settings restored successfully", Toast.LENGTH_SHORT).show()
                            loadApps()
                        } else {
                            Toast.makeText(this@SettingsActivity, "No backup file found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SettingsActivity, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            // Set up RecyclerView
            appsAdapter = AppsAdapter { app, isChecked ->
                if (isChecked) {
                    appWhitelistManager.addToWhitelist(app.packageName)
                } else {
                    appWhitelistManager.removeFromWhitelist(app.packageName)
                }
            }
            appsRecyclerView.layoutManager = LinearLayoutManager(this)
            appsRecyclerView.adapter = appsAdapter
        }

        private fun loadApps() {
            lifecycleScope.launch {
                val apps = withContext(Dispatchers.IO) {
                    appWhitelistManager.getAllInstalledApps()
                }
                allApps = apps
                filteredApps = apps
            appsAdapter.updateApps(filteredApps, appWhitelistManager.getWhitelistedPackages())
            }
        }

        private fun filterApps(query: String) {
            lifecycleScope.launch {
                val filtered = withContext(Dispatchers.IO) {
                    appWhitelistManager.searchApps(query, if (currentCategory == "All") null else currentCategory)
                }
                filteredApps = filtered
            appsAdapter.updateApps(filteredApps, appWhitelistManager.getWhitelistedPackages())
            }
        }
        
        private fun applyThemeImmediately(theme: ThemeManager.Theme) {
            // Apply theme changes immediately to the current activity
            themeManager.applyTheme(this)
            
            // Recreate the activity to apply theme changes
            recreate()
        }
        
        private fun showColorPickerDialog() {
            val currentColor = themeManager.getAccentColorValue()
            val colorPickerDialog = ColorPickerDialog(this, currentColor) { selectedColor ->
                themeManager.setCustomAccentColor(selectedColor)
                applyAccentColorImmediately(ThemeManager.AccentColor.CUSTOM)
            }
            colorPickerDialog.show()
        }
        
        private fun applyAccentColorImmediately(color: ThemeManager.AccentColor) {
            // Apply accent color changes immediately
            // This will be applied when the activity recreates
            Toast.makeText(this, "Accent color changed to ${color.displayName}", Toast.LENGTH_SHORT).show()
        }
        
        private fun applyFontSizeImmediately(fontSize: ThemeManager.FontSize) {
            // Apply font size changes immediately
            // This will be applied when the activity recreates
            Toast.makeText(this, "Font size changed to ${fontSize.displayName}", Toast.LENGTH_SHORT).show()
        }
        
        private fun applyGridDensityImmediately(gridDensity: ThemeManager.GridDensity) {
            // Apply grid density changes immediately
            // This will be applied when the activity recreates
            Toast.makeText(this, "Grid density changed to ${gridDensity.displayName}", Toast.LENGTH_SHORT).show()
        }
        
        private fun applyIconSizeImmediately(iconSize: ThemeManager.IconSize) {
            // Apply icon size changes immediately
            // This will be applied when the activity recreates
            Toast.makeText(this, "Icon size changed to ${iconSize.displayName}", Toast.LENGTH_SHORT).show()
        }
        
        private fun applyGrayscaleImmediately(enabled: Boolean) {
            // Apply grayscale filter to the current view
            val rootView = findViewById<View>(android.R.id.content)
            if (enabled) {
                // Apply grayscale color matrix
                val colorMatrix = android.graphics.ColorMatrix()
                colorMatrix.setSaturation(0f)
                val colorMatrixFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                rootView.background?.setColorFilter(colorMatrixFilter)
            } else {
                // Remove grayscale filter
                rootView.background?.clearColorFilter()
            }
        }
        
        private fun showWallpaperSelectionDialog() {
            val wallpapers = wallpaperManager.getBuiltInWallpapers()
            val wallpaperNames = wallpapers.map { it.name }
            
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose Wallpaper")
            builder.setItems(wallpaperNames.toTypedArray()) { _, which ->
                val selectedWallpaper = wallpapers[which]
                applyWallpaper(selectedWallpaper.drawable)
                Toast.makeText(this, "Wallpaper changed to ${selectedWallpaper.name}", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
        
        private fun showWallpaperSettingsDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Wallpaper Settings")
            
            val view = layoutInflater.inflate(R.layout.dialog_wallpaper_settings, null)
            
            // Set up overlay opacity seekbar
            val opacitySeekBar = view.findViewById<SeekBar>(R.id.opacitySeekBar)
            val opacityText = view.findViewById<TextView>(R.id.opacityText)
            val currentOpacity = (wallpaperManager.getOverlayOpacity() * 100).toInt()
            opacitySeekBar.progress = currentOpacity
            opacityText.text = "Overlay Opacity: ${currentOpacity}%"
            
            opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    opacityText.text = "Overlay Opacity: ${progress}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            // Set up blur checkbox
            val blurCheckBox = view.findViewById<CheckBox>(R.id.blurCheckBox)
            blurCheckBox.isChecked = wallpaperManager.isBlurEnabled()
            
            // Set up gradient checkbox
            val gradientCheckBox = view.findViewById<CheckBox>(R.id.gradientCheckBox)
            gradientCheckBox.isChecked = wallpaperManager.isGradientEnabled()
            
            builder.setView(view)
            builder.setPositiveButton("Apply") { _, _ ->
                val opacity = opacitySeekBar.progress / 100f
                wallpaperManager.setOverlayOpacity(opacity)
                wallpaperManager.setBlurEnabled(blurCheckBox.isChecked)
                wallpaperManager.setGradientEnabled(gradientCheckBox.isChecked)
                
                // Apply wallpaper with new settings
                val mainView = findViewById<View>(android.R.id.content)
                wallpaperManager.applyWallpaperToView(mainView)
                
                Toast.makeText(this, "Wallpaper settings updated", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
        
        private fun applyWallpaper(drawable: Drawable) {
            val mainView = findViewById<View>(android.R.id.content)
            mainView.background = drawable
        }
        
        private fun showThemePreviewDialog() {
            val currentTheme = themeManager.getCurrentTheme()
            val currentAccentColor = themeManager.getCurrentAccentColor()
            val currentFontSize = themeManager.getCurrentFontSize()
            val currentGridDensity = themeManager.getCurrentGridDensity()
            val currentIconSize = themeManager.getCurrentIconSize()
            
            val previewDialog = ThemePreviewDialog(
                this,
                currentTheme,
                currentAccentColor,
                currentFontSize,
                currentGridDensity,
                currentIconSize
            ) { theme, accentColor, fontSize, gridDensity, iconSize ->
                // Apply the selected theme settings
                themeManager.setTheme(theme)
                themeManager.setAccentColor(accentColor)
                themeManager.setFontSize(fontSize)
                themeManager.setGridDensity(gridDensity)
                themeManager.setIconSize(iconSize)
                
                // Apply immediately
                applyThemeImmediately(theme)
                applyAccentColorImmediately(accentColor)
                applyFontSizeImmediately(fontSize)
                applyGridDensityImmediately(gridDensity)
                applyIconSizeImmediately(iconSize)
                
                Toast.makeText(this, "Theme applied successfully!", Toast.LENGTH_SHORT).show()
            }
            previewDialog.show()
        }
        
        private fun showAnimationPreviewDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Animation Preview")
            
            val view = layoutInflater.inflate(R.layout.dialog_animation_preview, null)
            
            val previewButton = view.findViewById<Button>(R.id.previewAnimationButton)
            val speedText = view.findViewById<TextView>(R.id.speedText)
            val styleText = view.findViewById<TextView>(R.id.styleText)
            
            // Update text with current settings
            speedText.text = "Speed: ${animationManager.getCurrentAnimationSpeed().displayName}"
            styleText.text = "Style: ${animationManager.getCurrentAnimationStyle().displayName}"
            
            previewButton.setOnClickListener {
                // Create and start animation
                val animation = animationManager.createIconAnimation()
                previewButton.startAnimation(animation)
            }
            
            builder.setView(view)
            builder.setPositiveButton("Close", null)
            builder.show()
        }
        
        private fun showThemeExportDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Export Theme")
            
            val view = layoutInflater.inflate(R.layout.dialog_theme_export, null)
            val themeNameEditText = view.findViewById<EditText>(R.id.themeNameEditText)
            val exportButton = view.findViewById<Button>(R.id.exportButton)
            
            exportButton.setOnClickListener {
                val themeName = themeNameEditText.text.toString().trim()
                if (themeName.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val filePath = themeExportManager.saveThemeToFile(themeName)
                            Toast.makeText(this@SettingsActivity, "Theme exported to: $filePath", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@SettingsActivity, "Failed to export theme: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "Please enter a theme name", Toast.LENGTH_SHORT).show()
                }
            }
            
            builder.setView(view)
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
        
        private fun showThemeImportDialog() {
            val savedThemes = themeExportManager.getSavedThemes()
            val themeNames = savedThemes.map { it.name }
            
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Import Theme")
            
            if (themeNames.isNotEmpty()) {
                builder.setItems(themeNames.toTypedArray()) { _, which ->
                    val selectedTheme = savedThemes[which]
                    lifecycleScope.launch {
                        try {
                            val success = themeExportManager.loadThemeFromFile(selectedTheme.filePath)
                            if (success) {
                                Toast.makeText(this@SettingsActivity, "Theme imported successfully!", Toast.LENGTH_SHORT).show()
                                // Refresh the activity to apply the imported theme
                                recreate()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Failed to import theme", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@SettingsActivity, "Failed to import theme: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                builder.setMessage("No saved themes found. Export a theme first to import it later.")
            }
            
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
        
        private suspend fun createBackupFile(): String = withContext(Dispatchers.IO) {
            val backupDir = File(filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "essence_backup_$timestamp.json")
            
            val json = JSONObject().apply {
                put("whitelistedApps", appWhitelistManager.getWhitelistedPackages().toList())
                put("focusMode", appWhitelistManager.getCurrentFocusMode())
                put("theme", appWhitelistManager.getCurrentTheme())
                put("grayscaleMode", appWhitelistManager.isGrayscaleModeEnabled())
                put("timestamp", timestamp)
                put("version", "1.0")
            }
            
            FileWriter(backupFile).use { writer ->
                writer.write(json.toString(2))
            }
            
            backupFile.absolutePath
        }
        
        private suspend fun restoreFromBackup(): Boolean = withContext(Dispatchers.IO) {
            val backupDir = File(filesDir, "backups")
            if (!backupDir.exists()) return@withContext false
            
            val backupFiles = backupDir.listFiles { file -> file.name.startsWith("essence_backup_") && file.name.endsWith(".json") }
            if (backupFiles.isNullOrEmpty()) return@withContext false
            
            // Get the most recent backup file
            val latestBackup = backupFiles.maxByOrNull { it.lastModified() } ?: return@withContext false
            
            FileReader(latestBackup).use { reader ->
                val jsonString = BufferedReader(reader).readText()
                val json = JSONObject(jsonString)
                
                // Restore settings
                val whitelistedApps = json.getJSONArray("whitelistedApps")
                val whitelistSet = mutableSetOf<String>()
                for (i in 0 until whitelistedApps.length()) {
                    whitelistSet.add(whitelistedApps.getString(i))
                }
                
                // Update whitelist
                appWhitelistManager.clearWhitelist()
                whitelistSet.forEach { packageName ->
                    appWhitelistManager.addToWhitelist(packageName)
                }
                
                // Update other settings
                appWhitelistManager.setFocusMode(json.getString("focusMode"))
                appWhitelistManager.setTheme(json.getString("theme"))
                appWhitelistManager.setGrayscaleMode(json.getBoolean("grayscaleMode"))
            }
            
            true
        }
    }

    class AppsAdapter(
        private val onAppToggled: (AppInfo, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {
        
        private var apps: List<AppInfo> = emptyList()
        private var whitelistedPackages: Set<String> = emptySet()

        fun updateApps(newApps: List<AppInfo>, whitelisted: Set<String>) {
            val diffCallback = AppDiffCallback(apps, newApps, whitelistedPackages, whitelisted)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            
            apps = newApps
            whitelistedPackages = whitelisted
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            val isWhitelisted = whitelistedPackages.contains(app.packageName)
            
            holder.bind(app, isWhitelisted, onAppToggled)
        }

        override fun getItemCount(): Int = apps.size

        class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val appName: TextView = itemView.findViewById(R.id.appName)
            private val packageName: TextView = itemView.findViewById(R.id.packageName)
            private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
            private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
            
            private var currentApp: AppInfo? = null
            private var onAppToggled: ((AppInfo, Boolean) -> Unit)? = null

            init {
                // Set up the listener once in init, not in bind()
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    currentApp?.let { app ->
                        onAppToggled?.invoke(app, isChecked)
                    }
                }
            }

            fun bind(app: AppInfo, isWhitelisted: Boolean, onAppToggled: (AppInfo, Boolean) -> Unit) {
                // Store current app and callback
                currentApp = app
                this.onAppToggled = onAppToggled
                
                // Update UI elements
                appName.text = app.appName
                packageName.text = app.packageName
                appIcon.setImageDrawable(app.icon)
                
                // Temporarily remove listener to prevent firing during state change
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = isWhitelisted
                // Re-attach listener after state is set
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    currentApp?.let { currentApp ->
                        this.onAppToggled?.invoke(currentApp, isChecked)
                }
            }
        }
    }
    
    // DiffUtil callback for efficient RecyclerView updates
    class AppDiffCallback(
        private val oldApps: List<AppInfo>,
        private val newApps: List<AppInfo>,
        private val oldWhitelisted: Set<String>,
        private val newWhitelisted: Set<String>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldApps.size
        
        override fun getNewListSize(): Int = newApps.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldApps[oldItemPosition].packageName == newApps[newItemPosition].packageName
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldApp = oldApps[oldItemPosition]
            val newApp = newApps[newItemPosition]
            val oldWhitelisted = oldWhitelisted.contains(oldApp.packageName)
            val newWhitelisted = newWhitelisted.contains(newApp.packageName)
            
            return oldApp == newApp && oldWhitelisted == newWhitelisted
            }
        }
    }
