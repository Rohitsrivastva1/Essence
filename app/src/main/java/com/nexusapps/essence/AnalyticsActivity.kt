package com.nexusapps.essence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nexusapps.essence.data.CategoryUsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var appWhitelistManager: AppWhitelistManager
    private lateinit var backButton: ImageButton
    private lateinit var totalTimeText: TextView
    private lateinit var activeAppsText: TextView
    private lateinit var mostUsedRecyclerView: RecyclerView
    private lateinit var recentlyUsedRecyclerView: RecyclerView
    private lateinit var categoryStatsRecyclerView: RecyclerView
    private lateinit var mostUsedAdapter: AnalyticsAdapter
    private lateinit var recentlyUsedAdapter: AnalyticsAdapter
    private lateinit var categoryStatsAdapter: CategoryStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        
        // Initialize components
        appWhitelistManager = AppWhitelistManager(this)
        backButton = findViewById(R.id.backButton)
        totalTimeText = findViewById(R.id.totalTimeText)
        activeAppsText = findViewById(R.id.activeAppsText)
        mostUsedRecyclerView = findViewById(R.id.mostUsedRecyclerView)
        recentlyUsedRecyclerView = findViewById(R.id.recentlyUsedRecyclerView)
        categoryStatsRecyclerView = findViewById(R.id.categoryStatsRecyclerView)
        
        setupUI()
        loadAnalytics()
    }

    private fun setupUI() {
        backButton.setOnClickListener {
            finish()
        }
        
        // Set up RecyclerViews
        mostUsedAdapter = AnalyticsAdapter()
        recentlyUsedAdapter = AnalyticsAdapter()
        categoryStatsAdapter = CategoryStatsAdapter()
        
        mostUsedRecyclerView.layoutManager = LinearLayoutManager(this)
        mostUsedRecyclerView.adapter = mostUsedAdapter
        
        recentlyUsedRecyclerView.layoutManager = LinearLayoutManager(this)
        recentlyUsedRecyclerView.adapter = recentlyUsedAdapter
        
        categoryStatsRecyclerView.layoutManager = LinearLayoutManager(this)
        categoryStatsRecyclerView.adapter = categoryStatsAdapter
    }

    private fun loadAnalytics() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load most used apps
                val mostUsedApps = appWhitelistManager.getMostUsedApps(10)
                mostUsedAdapter.updateApps(mostUsedApps)
                
                // Load recently used apps
                val recentlyUsedApps = appWhitelistManager.getRecentlyUsedApps(10)
                recentlyUsedAdapter.updateApps(recentlyUsedApps)
                
                // Load category stats
                val categoryStats = loadCategoryStats()
                categoryStatsAdapter.updateStats(categoryStats)
                
                // Update summary stats
                updateSummaryStats()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadCategoryStats(): List<CategoryUsageStats> {
        return withContext(Dispatchers.IO) {
            appWhitelistManager.appUsageDao.getCategoryUsageStats()
        }
    }

    private fun updateSummaryStats() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val totalTime = withContext(Dispatchers.IO) {
                    appWhitelistManager.appUsageDao.getTotalUsageTime() ?: 0L
                }
                
                val activeApps = withContext(Dispatchers.IO) {
                    val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                    appWhitelistManager.appUsageDao.getActiveAppsCount(yesterday)
                }
                
                val totalHours = totalTime / (1000 * 60 * 60)
                val totalMinutes = (totalTime % (1000 * 60 * 60)) / (1000 * 60)
                
                totalTimeText.text = "Total Usage: ${totalHours}h ${totalMinutes}m"
                activeAppsText.text = "Active Apps (24h): $activeApps"
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class AnalyticsAdapter : RecyclerView.Adapter<AnalyticsAdapter.AnalyticsViewHolder>() {
    private var apps: List<AppInfo> = emptyList()

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AnalyticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analytics_app, parent, false)
        return AnalyticsViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnalyticsViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    class AnalyticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val usageTime: TextView = itemView.findViewById(R.id.usageTime)
        private val launchCount: TextView = itemView.findViewById(R.id.launchCount)
        private val lastUsed: TextView = itemView.findViewById(R.id.lastUsed)
        private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)

        fun bind(app: AppInfo) {
            appName.text = app.appName
            appIcon.setImageDrawable(app.icon)
            
            val hours = app.totalTimeSpent / (1000 * 60 * 60)
            val minutes = (app.totalTimeSpent % (1000 * 60 * 60)) / (1000 * 60)
            usageTime.text = "${hours}h ${minutes}m"
            
            launchCount.text = "Launches: ${app.launchCount}"
            
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            lastUsed.text = "Last: ${dateFormat.format(app.lastUsed)}"
        }
    }
}

class CategoryStatsAdapter : RecyclerView.Adapter<CategoryStatsAdapter.CategoryStatsViewHolder>() {
    private var stats: List<CategoryUsageStats> = emptyList()

    fun updateStats(newStats: List<CategoryUsageStats>) {
        stats = newStats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CategoryStatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_stats, parent, false)
        return CategoryStatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryStatsViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount(): Int = stats.size

    class CategoryStatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val usageTime: TextView = itemView.findViewById(R.id.usageTime)
        private val percentage: TextView = itemView.findViewById(R.id.percentage)

        fun bind(stat: CategoryUsageStats) {
            categoryName.text = stat.category
            
            val hours = stat.totalTime / (1000 * 60 * 60)
            val minutes = (stat.totalTime % (1000 * 60 * 60)) / (1000 * 60)
            usageTime.text = "${hours}h ${minutes}m"
            
            // Calculate percentage (simplified)
            percentage.text = "${(stat.totalTime / 1000 / 60).toInt()} min"
        }
    }
}
