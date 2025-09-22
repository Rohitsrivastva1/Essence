package com.nexusapps.essence

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var appWhitelistManager: AppWhitelistManager
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var adapter: SimpleAppsAdapter

    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)

        appWhitelistManager = AppWhitelistManager(this)

        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        searchEditText = findViewById(R.id.searchInput)
        backButton = findViewById(R.id.backButton)
        settingsButton = findViewById(R.id.settingsButtonDrawer)

        adapter = SimpleAppsAdapter { appInfo ->
            // Launch and finish drawer
            try {
                val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) startActivity(intent)
            } finally {
                finish()
            }
        }

        appsRecyclerView.layoutManager = LinearLayoutManager(this)
        appsRecyclerView.adapter = adapter

        backButton.setOnClickListener { finish() }
        settingsButton.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString().orEmpty())
            }
        })

        loadApps()
    }

    private fun loadApps() {
        // Load once, sorted by name
        allApps = appWhitelistManager.getAllInstalledApps().sortedBy { it.appName.lowercase() }
        adapter.submit(allApps)
        toggleEmpty(allApps.isEmpty())
    }

    private fun filterApps(query: String) {
        if (query.isBlank()) {
            adapter.submit(allApps)
            toggleEmpty(allApps.isEmpty())
            return
        }

        val filtered = allApps.filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }
        adapter.submit(filtered)
        toggleEmpty(filtered.isEmpty())
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        findViewById<View>(R.id.emptyText).visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
}


