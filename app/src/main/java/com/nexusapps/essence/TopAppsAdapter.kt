package com.nexusapps.essence

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class TopAppsAdapter(
    private val context: Context,
    private var apps: List<UsageAnalyticsManager.AppUsageInfo>
) : RecyclerView.Adapter<TopAppsAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_top_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<UsageAnalyticsManager.AppUsageInfo>) {
        val diffResult = DiffUtil.calculateDiff(AppDiffCallback(this.apps, newApps))
        this.apps = newApps
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appName: TextView = itemView.findViewById(R.id.appNameText)
        private val appCategory: TextView = itemView.findViewById(R.id.categoryText)
        private val appUsageTime: TextView = itemView.findViewById(R.id.usageTimeText)

        fun bind(app: UsageAnalyticsManager.AppUsageInfo) {
            appName.text = app.appName
            appCategory.text = app.category.displayName
            appUsageTime.text = formatMillisToHoursMinutes(app.totalTimeInForeground)
        }

        private fun formatMillisToHoursMinutes(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            return "${hours}h ${minutes}m"
        }
    }

    class AppDiffCallback(
        private val oldList: List<UsageAnalyticsManager.AppUsageInfo>,
        private val newList: List<UsageAnalyticsManager.AppUsageInfo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
