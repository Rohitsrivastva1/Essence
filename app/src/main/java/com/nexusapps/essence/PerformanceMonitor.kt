package com.nexusapps.essence

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Debug
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class PerformanceMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val startTime = SystemClock.elapsedRealtime()
    private val memoryUsage = AtomicLong(0)
    private val cpuUsage = AtomicLong(0)
    private val batteryLevel = AtomicLong(0)
    
    data class PerformanceMetrics(
        val memoryUsageMB: Long,
        val cpuUsagePercent: Long,
        val batteryLevel: Int,
        val uptime: Long,
        val isLowMemory: Boolean,
        val isLowBattery: Boolean
    )
    
    fun startMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                updateMetrics()
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    private fun updateMetrics() {
        // Memory usage
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val usedMemory = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024) // MB
        memoryUsage.set(usedMemory)
        
        // Battery level
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        batteryLevel.set(battery.toLong())
        
        // CPU usage (simplified)
        val cpuUsagePercent = getCpuUsage()
        cpuUsage.set(cpuUsagePercent)
    }
    
    private fun getCpuUsage(): Long {
        // Simplified CPU usage calculation
        // In a real implementation, you'd use more sophisticated methods
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        return (usedMemory * 100 / totalMemory)
    }
    
    fun getCurrentMetrics(): PerformanceMetrics {
        val uptime = SystemClock.elapsedRealtime() - startTime
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return PerformanceMetrics(
            memoryUsageMB = memoryUsage.get(),
            cpuUsagePercent = cpuUsage.get(),
            batteryLevel = batteryLevel.get().toInt(),
            uptime = uptime,
            isLowMemory = memoryInfo.lowMemory,
            isLowBattery = batteryLevel.get() < 20
        )
    }
    
    fun isPerformanceOptimal(): Boolean {
        val metrics = getCurrentMetrics()
        return !metrics.isLowMemory && 
               !metrics.isLowBattery && 
               metrics.memoryUsageMB < 100 && 
               metrics.cpuUsagePercent < 80
    }
    
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = getCurrentMetrics()
        
        if (metrics.isLowMemory) {
            recommendations.add("Close unused apps to free memory")
        }
        
        if (metrics.memoryUsageMB > 150) {
            recommendations.add("High memory usage detected")
        }
        
        if (metrics.cpuUsagePercent > 80) {
            recommendations.add("High CPU usage detected")
        }
        
        if (metrics.isLowBattery) {
            recommendations.add("Low battery - consider power saving mode")
        }
        
        if (metrics.uptime > 24 * 60 * 60 * 1000) { // 24 hours
            recommendations.add("Consider restarting the device")
        }
        
        return recommendations
    }
    
    fun optimizePerformance() {
        // Clear caches and optimize memory
        System.gc()
        
        // In a real implementation, you might:
        // - Clear app caches
        // - Reduce background processes
        // - Optimize database queries
        // - Clear temporary files
    }
}
