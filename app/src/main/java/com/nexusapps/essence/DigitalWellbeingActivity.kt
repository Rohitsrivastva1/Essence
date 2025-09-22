package com.nexusapps.essence

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DigitalWellbeingActivity : AppCompatActivity() {

    private lateinit var usageAnalyticsManager: UsageAnalyticsManager
    private lateinit var focusSessionManager: FocusSessionManager
    private lateinit var themeManager: ThemeManager

    private lateinit var totalScreenTimeText: TextView
    private lateinit var focusTimeText: TextView
    private lateinit var breakTimeText: TextView
    private lateinit var sessionTimerText: TextView
    private lateinit var startSessionButton: Button
    private lateinit var pauseSessionButton: Button
    private lateinit var topAppsRecyclerView: RecyclerView
    private lateinit var dailyLimitProgress: ProgressBar
    private lateinit var dailyLimitText: TextView
    private lateinit var focusGoalProgress: ProgressBar
    private lateinit var focusGoalText: TextView
    private lateinit var breakReminderText: TextView
    private lateinit var takeBreakButton: Button

    private lateinit var topAppsAdapter: TopAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeManager = ThemeManager(this)
        themeManager.applyTheme(this)
        setContentView(R.layout.activity_digital_wellbeing)

        usageAnalyticsManager = UsageAnalyticsManager(this)
        focusSessionManager = FocusSessionManager(this)

        bindViews()
        setupListeners()
        setupRecyclerView()
        observeData()

        checkUsageStatsPermission()
    }

    override fun onResume() {
        super.onResume()
        // Refresh usage stats when activity resumes
        lifecycleScope.launch {
            usageAnalyticsManager.queryDailyUsageStats()
        }
    }

    private fun bindViews() {
        totalScreenTimeText = findViewById(R.id.totalScreenTimeText)
        focusTimeText = findViewById(R.id.focusTimeText)
        breakTimeText = findViewById(R.id.breakTimeText)
        sessionTimerText = findViewById(R.id.sessionTimerText)
        startSessionButton = findViewById(R.id.startSessionButton)
        pauseSessionButton = findViewById(R.id.pauseSessionButton)
        topAppsRecyclerView = findViewById(R.id.topAppsRecyclerView)
        dailyLimitProgress = findViewById(R.id.dailyLimitProgress)
        dailyLimitText = findViewById(R.id.dailyLimitText)
        focusGoalProgress = findViewById(R.id.focusGoalProgress)
        focusGoalText = findViewById(R.id.focusGoalText)
        breakReminderText = findViewById(R.id.breakReminderText)
        takeBreakButton = findViewById(R.id.takeBreakButton)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }

        startSessionButton.setOnClickListener {
            when (focusSessionManager.sessionState.value) {
                FocusSessionManager.SessionState.IDLE -> {
                    focusSessionManager.startSession(FocusSessionManager.SessionType.FOCUS)
                }
                FocusSessionManager.SessionState.RUNNING -> {
                    focusSessionManager.pauseSession()
                }
                FocusSessionManager.SessionState.PAUSED -> {
                    focusSessionManager.resumeSession()
                }
            }
        }

        pauseSessionButton.setOnClickListener {
            when (focusSessionManager.sessionState.value) {
                FocusSessionManager.SessionState.RUNNING -> {
                    focusSessionManager.pauseSession()
                }
                FocusSessionManager.SessionState.PAUSED -> {
                    focusSessionManager.resumeSession()
                }
                else -> {
                    focusSessionManager.endSession(false) // End session
                }
            }
        }

        takeBreakButton.setOnClickListener {
            showBreakDialog()
        }
    }

    private fun setupRecyclerView() {
        topAppsAdapter = TopAppsAdapter(this, emptyList())
        topAppsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DigitalWellbeingActivity)
            adapter = topAppsAdapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            usageAnalyticsManager.totalScreenTime.collectLatest { totalTime ->
                totalScreenTimeText.text = formatMillisToHoursMinutes(totalTime)
                updateScreenTimeGoalProgress(totalTime, usageAnalyticsManager.screenTimeGoal.value)
            }
        }

        lifecycleScope.launch {
            usageAnalyticsManager.dailyUsageStats.collectLatest { appUsageList ->
                topAppsAdapter.updateApps(appUsageList)
            }
        }

        lifecycleScope.launch {
            focusSessionManager.timeLeftMillis.collectLatest { timeLeft ->
                sessionTimerText.text = formatMillisToMinutesSeconds(timeLeft)
            }
        }

        lifecycleScope.launch {
            focusSessionManager.sessionState.collectLatest { state ->
                updateSessionUI(state, focusSessionManager.currentSessionType.value)
            }
        }

        lifecycleScope.launch {
            focusSessionManager.currentSessionType.collectLatest { type ->
                updateSessionUI(focusSessionManager.sessionState.value, type)
            }
        }

        lifecycleScope.launch {
            focusSessionManager.totalFocusTimeToday.collectLatest { totalFocusTime ->
                focusTimeText.text = formatMillisToHoursMinutes(totalFocusTime)
                updateFocusTimeGoalProgress(totalFocusTime, usageAnalyticsManager.focusGoal.value)
            }
        }

        lifecycleScope.launch {
            focusSessionManager.completedFocusSessionsToday.collectLatest { completedSessions ->
                // Calculate breaks taken
                val longBreaks = completedSessions / 4
                val shortBreaks = completedSessions % 4
                breakTimeText.text = "$longBreaks long breaks, $shortBreaks short breaks"
            }
        }

        lifecycleScope.launch {
            usageAnalyticsManager.screenTimeGoal.collectLatest { goal ->
                updateScreenTimeGoalProgress(usageAnalyticsManager.totalScreenTime.value, goal)
            }
        }

        lifecycleScope.launch {
            usageAnalyticsManager.focusGoal.collectLatest { goal ->
                updateFocusTimeGoalProgress(focusSessionManager.totalFocusTimeToday.value, goal)
            }
        }
    }

    private fun updateSessionUI(state: FocusSessionManager.SessionState, type: FocusSessionManager.SessionType) {
        when (state) {
            FocusSessionManager.SessionState.IDLE -> {
                startSessionButton.text = "Start Focus"
                startSessionButton.isEnabled = true
                pauseSessionButton.text = "End Session"
                pauseSessionButton.isEnabled = false
                sessionTimerText.setTextColor(getColor(android.R.color.holo_blue_light))
            }
            FocusSessionManager.SessionState.RUNNING -> {
                startSessionButton.text = "Pause"
                startSessionButton.isEnabled = true
                pauseSessionButton.text = "End Session"
                pauseSessionButton.isEnabled = true
                sessionTimerText.setTextColor(
                    when (type) {
                        FocusSessionManager.SessionType.FOCUS -> getColor(android.R.color.holo_green_light)
                        FocusSessionManager.SessionType.SHORT_BREAK, FocusSessionManager.SessionType.LONG_BREAK -> getColor(android.R.color.holo_blue_light)
                    }
                )
            }
            FocusSessionManager.SessionState.PAUSED -> {
                startSessionButton.text = "Resume"
                startSessionButton.isEnabled = true
                pauseSessionButton.text = "End Session"
                pauseSessionButton.isEnabled = true
                sessionTimerText.setTextColor(getColor(android.R.color.holo_orange_light))
            }
        }
    }

    private fun updateScreenTimeGoalProgress(current: Long, goal: Long) {
        if (goal > 0) {
            dailyLimitProgress.max = goal.toInt()
            dailyLimitProgress.progress = current.toInt()
            dailyLimitText.text = "${formatMillisToHoursMinutes(current)} / ${formatMillisToHoursMinutes(goal)}"
        } else {
            dailyLimitProgress.max = 1
            dailyLimitProgress.progress = 0
            dailyLimitText.text = "${formatMillisToHoursMinutes(current)} / No Goal Set"
        }
    }

    private fun updateFocusTimeGoalProgress(current: Long, goal: Long) {
        if (goal > 0) {
            focusGoalProgress.max = goal.toInt()
            focusGoalProgress.progress = current.toInt()
            focusGoalText.text = "${formatMillisToHoursMinutes(current)} / ${formatMillisToHoursMinutes(goal)}"
        } else {
            focusGoalProgress.max = 1
            focusGoalProgress.progress = 0
            focusGoalText.text = "${formatMillisToHoursMinutes(current)} / No Goal Set"
        }
    }

    private fun formatMillisToHoursMinutes(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return "${hours}h ${minutes}m"
    }

    private fun formatMillisToMinutesSeconds(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun checkUsageStatsPermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog.Builder(this)
                .setTitle("Usage Access Required")
                .setMessage("Essence needs permission to track app usage for Digital Wellbeing features. Please grant 'Usage access' in settings.")
                .setPositiveButton("Grant") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Digital Wellbeing features will be limited without usage access.", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

    private fun showBreakDialog() {
        AlertDialog.Builder(this)
            .setTitle("Break Time!")
            .setMessage("Take a 5-minute break to rest your eyes and stretch.")
            .setPositiveButton("Start Break") { _, _ ->
                // Start break session
                focusSessionManager.startSession(FocusSessionManager.SessionType.SHORT_BREAK)
                breakReminderText.text = "Break in progress... Take a 5-minute rest!"
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
