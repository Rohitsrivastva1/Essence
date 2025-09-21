package com.nexusapps.essence

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var appWhitelistManager: AppWhitelistManager
    private lateinit var searchEditText: EditText
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var backButton: ImageButton
    private lateinit var clearButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var focusModeSpinner: Spinner
    private lateinit var themeSpinner: Spinner
    private lateinit var grayscaleSwitch: Switch
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
        searchEditText = findViewById(R.id.searchEditText)
        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        backButton = findViewById(R.id.backButton)
        clearButton = findViewById(R.id.clearButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        focusModeSpinner = findViewById(R.id.focusModeSpinner)
        themeSpinner = findViewById(R.id.themeSpinner)
        grayscaleSwitch = findViewById(R.id.grayscaleSwitch)
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
            appWhitelistManager.clearWhitelist()
            loadApps()
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
        val themes = listOf("Dark", "Light", "High Contrast", "AMOLED Black")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter
        themeSpinner.setSelection(themes.indexOf(appWhitelistManager.getCurrentTheme()))
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appWhitelistManager.setTheme(themes[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Set up grayscale switch
        grayscaleSwitch.isChecked = appWhitelistManager.isGrayscaleModeEnabled()
        grayscaleSwitch.setOnCheckedChangeListener { _, isChecked ->
            appWhitelistManager.setGrayscaleMode(isChecked)
        }
        
        // Set up analytics button
        analyticsButton.setOnClickListener {
            // Open analytics activity
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
        }
        
        // Set up backup button
        backupButton.setOnClickListener {
            val settings = appWhitelistManager.exportSettings()
            // In a real app, you'd save this to a file or share it
            Toast.makeText(this, "Settings exported: ${settings.length} characters", Toast.LENGTH_SHORT).show()
        }
        
        // Set up restore button
        restoreButton.setOnClickListener {
            // In a real app, you'd load from a file
            Toast.makeText(this, "Restore functionality coming soon", Toast.LENGTH_SHORT).show()
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
        allApps = appWhitelistManager.getAllInstalledApps()
        filteredApps = allApps
        appsAdapter.updateApps(filteredApps, appWhitelistManager.getWhitelistedPackages())
    }

    private fun filterApps(query: String) {
        filteredApps = appWhitelistManager.searchApps(query, if (currentCategory == "All") null else currentCategory)
        appsAdapter.updateApps(filteredApps, appWhitelistManager.getWhitelistedPackages())
    }
}

class AppsAdapter(
    private val onAppToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {
    
    private var apps: List<AppInfo> = emptyList()
    private var whitelistedPackages: Set<String> = emptySet()

    fun updateApps(newApps: List<AppInfo>, whitelisted: Set<String>) {
        apps = newApps
        whitelistedPackages = whitelisted
        notifyDataSetChanged()
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

        fun bind(app: AppInfo, isWhitelisted: Boolean, onAppToggled: (AppInfo, Boolean) -> Unit) {
            appName.text = app.appName
            packageName.text = app.packageName
            checkBox.isChecked = isWhitelisted
            appIcon.setImageDrawable(app.icon)
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onAppToggled(app, isChecked)
            }
        }
    }
}
