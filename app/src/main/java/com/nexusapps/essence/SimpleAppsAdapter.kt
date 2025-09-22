package com.nexusapps.essence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleAppsAdapter(
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<SimpleAppsAdapter.VH>() {

    private var apps: List<AppInfo> = emptyList()

    fun submit(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_results_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.title.text = app.appName
        holder.itemView.setOnClickListener { onClick(app) }
        // Premium minimalist: slightly higher line height via padding
        val padV = (12 * holder.itemView.resources.displayMetrics.density).toInt()
        holder.itemView.setPadding(
            holder.itemView.paddingLeft,
            padV,
            holder.itemView.paddingRight,
            padV
        )
    }

    override fun getItemCount(): Int = apps.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.appName)
    }
}


