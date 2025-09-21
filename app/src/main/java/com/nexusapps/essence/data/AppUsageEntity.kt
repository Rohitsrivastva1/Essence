package com.nexusapps.essence.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val totalTimeSpent: Long = 0, // in milliseconds
    val lastUsed: Date = Date(),
    val launchCount: Int = 0,
    val category: String = "Unknown"
)

@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val startTime: Date,
    val endTime: Date? = null,
    val duration: Long = 0 // in milliseconds
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String, // Work, Personal, Emergency, Custom
    val startTime: Date,
    val endTime: Date? = null,
    val duration: Long = 0,
    val appsBlocked: String = "" // comma-separated package names
)
