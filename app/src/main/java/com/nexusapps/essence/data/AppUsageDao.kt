package com.nexusapps.essence.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AppUsageDao {
    
    // App Usage queries
    @Query("SELECT * FROM app_usage ORDER BY totalTimeSpent DESC")
    fun getAllAppUsage(): Flow<List<AppUsageEntity>>
    
    @Query("SELECT * FROM app_usage WHERE packageName = :packageName")
    suspend fun getAppUsage(packageName: String): AppUsageEntity?
    
    @Query("SELECT * FROM app_usage WHERE category = :category ORDER BY totalTimeSpent DESC")
    fun getAppsByCategory(category: String): Flow<List<AppUsageEntity>>
    
    @Query("SELECT * FROM app_usage ORDER BY lastUsed DESC LIMIT :limit")
    fun getRecentlyUsedApps(limit: Int): Flow<List<AppUsageEntity>>
    
    @Query("SELECT * FROM app_usage ORDER BY launchCount DESC LIMIT :limit")
    fun getMostUsedApps(limit: Int): Flow<List<AppUsageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(appUsage: AppUsageEntity)
    
    @Update
    suspend fun updateAppUsage(appUsage: AppUsageEntity)
    
    @Delete
    suspend fun deleteAppUsage(appUsage: AppUsageEntity)
    
    // App Session queries
    @Query("SELECT * FROM app_sessions WHERE packageName = :packageName ORDER BY startTime DESC")
    fun getAppSessions(packageName: String): Flow<List<AppSessionEntity>>
    
    @Query("SELECT * FROM app_sessions WHERE startTime >= :startDate AND startTime <= :endDate")
    fun getSessionsInRange(startDate: Date, endDate: Date): Flow<List<AppSessionEntity>>
    
    @Insert
    suspend fun insertAppSession(session: AppSessionEntity): Long
    
    @Update
    suspend fun updateAppSession(session: AppSessionEntity)
    
    @Query("DELETE FROM app_sessions WHERE startTime < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: Date)
    
    // Focus Session queries
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>
    
    @Query("SELECT * FROM focus_sessions WHERE mode = :mode ORDER BY startTime DESC")
    fun getFocusSessionsByMode(mode: String): Flow<List<FocusSessionEntity>>
    
    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity): Long
    
    @Update
    suspend fun updateFocusSession(session: FocusSessionEntity)
    
    @Query("DELETE FROM focus_sessions WHERE startTime < :cutoffDate")
    suspend fun deleteOldFocusSessions(cutoffDate: Date)
    
    // Analytics queries
    @Query("SELECT SUM(totalTimeSpent) FROM app_usage")
    suspend fun getTotalUsageTime(): Long?
    
    @Query("SELECT COUNT(*) FROM app_usage WHERE lastUsed >= :date")
    suspend fun getActiveAppsCount(date: Date): Int
    
    @Query("SELECT category, SUM(totalTimeSpent) as totalTime FROM app_usage GROUP BY category ORDER BY totalTime DESC")
    suspend fun getCategoryUsageStats(): List<CategoryUsageStats>
}

data class CategoryUsageStats(
    val category: String,
    val totalTime: Long
)
