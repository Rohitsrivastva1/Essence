package com.nexusapps.essence

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SimpleDigitalWellbeingActivity : AppCompatActivity() {
    
    private lateinit var totalScreenTimeText: TextView
    private lateinit var focusTimeText: TextView
    private lateinit var breakTimeText: TextView
    private lateinit var sessionTimerText: TextView
    private lateinit var startSessionButton: Button
    private lateinit var pauseSessionButton: Button
    private lateinit var dailyLimitText: TextView
    private lateinit var focusGoalText: TextView
    private lateinit var dailyLimitProgress: ProgressBar
    private lateinit var focusGoalProgress: ProgressBar
    private lateinit var breakReminderText: TextView
    private lateinit var takeBreakButton: Button
    
    private var isSessionActive = false
    private var sessionStartTime = 0L
    private var sessionDuration = 25 * 60 * 1000L // 25 minutes
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_digital_wellbeing)
        
        initializeViews()
        setupClickListeners()
        loadUsageData()
    }
    
    private fun initializeViews() {
        totalScreenTimeText = findViewById(R.id.totalScreenTimeText)
        focusTimeText = findViewById(R.id.focusTimeText)
        breakTimeText = findViewById(R.id.breakTimeText)
        sessionTimerText = findViewById(R.id.sessionTimerText)
        startSessionButton = findViewById(R.id.startSessionButton)
        pauseSessionButton = findViewById(R.id.pauseSessionButton)
        dailyLimitText = findViewById(R.id.dailyLimitText)
        focusGoalText = findViewById(R.id.focusGoalText)
        dailyLimitProgress = findViewById(R.id.dailyLimitProgress)
        focusGoalProgress = findViewById(R.id.focusGoalProgress)
        breakReminderText = findViewById(R.id.breakReminderText)
        takeBreakButton = findViewById(R.id.takeBreakButton)
    }
    
    private fun setupClickListeners() {
        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        startSessionButton.setOnClickListener {
            if (isSessionActive) {
                endFocusSession()
            } else {
                startFocusSession()
            }
        }
        
        pauseSessionButton.setOnClickListener {
            if (isSessionActive) {
                pauseSession()
            } else {
                resumeSession()
            }
        }
        
        takeBreakButton.setOnClickListener {
            takeBreak()
        }
    }
    
    private fun loadUsageData() {
        // Simulate usage data for demo
        totalScreenTimeText.text = "2h 30m"
        focusTimeText.text = "1h 45m"
        breakTimeText.text = "45m"
        
        dailyLimitText.text = "4h 0m"
        focusGoalText.text = "2h 0m"
        
        dailyLimitProgress.progress = 65
        focusGoalProgress.progress = 87
        
        breakReminderText.text = "Next break in 15 minutes"
    }
    
    private fun startFocusSession() {
        isSessionActive = true
        sessionStartTime = System.currentTimeMillis()
        
        startSessionButton.text = "End Session"
        pauseSessionButton.text = "Pause"
        pauseSessionButton.visibility = View.VISIBLE
        
        sessionTimerText.text = formatTime(sessionDuration)
        
        // Start countdown timer
        startCountdownTimer()
    }
    
    private fun endFocusSession() {
        isSessionActive = false
        
        startSessionButton.text = "Start Focus"
        pauseSessionButton.visibility = View.GONE
        
        sessionTimerText.text = "25:00"
        
        // Show completion message
        AlertDialog.Builder(this)
            .setTitle("Focus Session Complete!")
            .setMessage("Great job! You completed a 25-minute focus session.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun pauseSession() {
        isSessionActive = false
        pauseSessionButton.text = "Resume"
    }
    
    private fun resumeSession() {
        isSessionActive = true
        pauseSessionButton.text = "Pause"
        startCountdownTimer()
    }
    
    private fun startCountdownTimer() {
        val remainingTime = sessionDuration - (System.currentTimeMillis() - sessionStartTime)
        if (remainingTime > 0) {
            sessionTimerText.text = formatTime(remainingTime)
            
            // Simple countdown using postDelayed
            sessionTimerText.postDelayed({
                if (isSessionActive) {
                    startCountdownTimer()
                }
            }, 1000)
        } else {
            endFocusSession()
        }
    }
    
    private fun takeBreak() {
        AlertDialog.Builder(this)
            .setTitle("Break Time!")
            .setMessage("Take a 5-minute break to rest your eyes and stretch.")
            .setPositiveButton("Start Break") { _, _ ->
                // Start break session
                startBreakSession()
            }
            .setNegativeButton("Later", null)
            .show()
    }
    
    private fun startBreakSession() {
        breakReminderText.text = "Break in progress... Take a 5-minute rest!"
        
        // Reset break reminder after 5 minutes
        breakReminderText.postDelayed({
            breakReminderText.text = "Break complete! Ready for next focus session."
        }, 5 * 60 * 1000)
    }
    
    private fun formatTime(timeInMillis: Long): String {
        val hours = timeInMillis / (1000 * 60 * 60)
        val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeInMillis % (1000 * 60)) / 1000
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadUsageData()
    }
}
