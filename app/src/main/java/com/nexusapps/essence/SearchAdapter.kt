package com.nexusapps.essence

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val context: Context,
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val appCategory: TextView = itemView.findViewById(R.id.appCategory)
        val launchIcon: ImageView = itemView.findViewById(R.id.launchIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_results_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val app = apps[position]
        
        // Set app name
        holder.appName.text = app.appName
        
        // Set app category
        holder.appCategory.text = getAppCategory(app.packageName)
        
        // Set app icon
        try {
            val packageManager = context.packageManager
            val appIcon = packageManager.getApplicationIcon(app.packageName)
            holder.appIcon.setImageDrawable(appIcon)
        } catch (e: Exception) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onAppClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    private fun getAppCategory(packageName: String): String {
        return when {
            packageName.contains("camera") || packageName.contains("photo") -> "Camera"
            packageName.contains("music") || packageName.contains("audio") -> "Music"
            packageName.contains("video") || packageName.contains("player") -> "Video"
            packageName.contains("game") || packageName.contains("play") -> "Games"
            packageName.contains("social") || packageName.contains("chat") -> "Social"
            packageName.contains("browser") || packageName.contains("web") -> "Browser"
            packageName.contains("message") || packageName.contains("sms") -> "Messaging"
            packageName.contains("call") || packageName.contains("phone") -> "Phone"
            packageName.contains("mail") || packageName.contains("email") -> "Email"
            packageName.contains("calendar") || packageName.contains("schedule") -> "Productivity"
            packageName.contains("note") || packageName.contains("memo") -> "Notes"
            packageName.contains("calculator") || packageName.contains("calc") -> "Tools"
            packageName.contains("settings") || packageName.contains("config") -> "Settings"
            packageName.contains("file") || packageName.contains("manager") -> "File Manager"
            packageName.contains("weather") || packageName.contains("forecast") -> "Weather"
            packageName.contains("map") || packageName.contains("navigation") -> "Navigation"
            packageName.contains("bank") || packageName.contains("finance") -> "Finance"
            packageName.contains("shop") || packageName.contains("store") -> "Shopping"
            packageName.contains("news") || packageName.contains("reader") -> "News"
            packageName.contains("book") || packageName.contains("read") -> "Reading"
            packageName.contains("health") || packageName.contains("fitness") -> "Health"
            packageName.contains("travel") || packageName.contains("trip") -> "Travel"
            packageName.contains("food") || packageName.contains("restaurant") -> "Food"
            packageName.contains("education") || packageName.contains("learn") -> "Education"
            packageName.contains("entertainment") || packageName.contains("fun") -> "Entertainment"
            else -> "Other"
        }
    }
}
