package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * FocusSessionManager handles focus sessions, Pomodoro timers, and productivity tracking
 */
class FocusSessionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("focus_session_prefs", Context.MODE_PRIVATE)

    private var countDownTimer: CountDownTimer? = null

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _timeLeftMillis = MutableStateFlow(0L)
    val timeLeftMillis: StateFlow<Long> = _timeLeftMillis

    private val _currentSessionType = MutableStateFlow(SessionType.FOCUS)
    val currentSessionType: StateFlow<SessionType> = _currentSessionType

    private val _completedFocusSessionsToday = MutableStateFlow(0)
    val completedFocusSessionsToday: StateFlow<Int> = _completedFocusSessionsToday

    private val _totalFocusTimeToday = MutableStateFlow(0L)
    val totalFocusTimeToday: StateFlow<Long> = _totalFocusTimeToday

    private val _sessionHistory = MutableStateFlow<List<SessionRecord>>(emptyList())
    val sessionHistory: StateFlow<List<SessionRecord>> = _sessionHistory

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val FOCUS_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(25)
    private val SHORT_BREAK_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(5)
    private val LONG_BREAK_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(15)
    private val SESSIONS_BEFORE_LONG_BREAK = 4

    companion object {
        private const val LAST_SESSION_DATE_KEY = "last_session_date"
        private const val COMPLETED_FOCUS_SESSIONS_TODAY_KEY = "completed_focus_sessions_today"
        private const val TOTAL_FOCUS_TIME_TODAY_KEY = "total_focus_time_today"
        private const val SESSION_HISTORY_KEY = "session_history"
        private const val CURRENT_STREAK_KEY = "current_streak"
    }

    init {
        loadSessionData()
        checkNewDay()
    }

    private fun checkNewDay() {
        val today = getTodayDateString()
        val lastSessionDate = prefs.getString(LAST_SESSION_DATE_KEY, null)

        if (lastSessionDate != today) {
            // New day, reset daily stats
            _completedFocusSessionsToday.value = 0
            _totalFocusTimeToday.value = 0L
            prefs.edit()
                .putInt(COMPLETED_FOCUS_SESSIONS_TODAY_KEY, 0)
                .putLong(TOTAL_FOCUS_TIME_TODAY_KEY, 0L)
                .putString(LAST_SESSION_DATE_KEY, today)
                .apply()

            // Reset streak if previous day was not active
            if (lastSessionDate != null && !isYesterday(lastSessionDate)) {
                _currentStreak.value = 0
                prefs.edit().putInt(CURRENT_STREAK_KEY, 0).apply()
            }
        }
    }

    private fun isYesterday(dateString: String): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(yesterday.time) == dateString
    }

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    private fun loadSessionData() {
        _completedFocusSessionsToday.value = prefs.getInt(COMPLETED_FOCUS_SESSIONS_TODAY_KEY, 0)
        _totalFocusTimeToday.value = prefs.getLong(TOTAL_FOCUS_TIME_TODAY_KEY, 0L)
        _currentStreak.value = prefs.getInt(CURRENT_STREAK_KEY, 0)

        val historyJson = prefs.getString(SESSION_HISTORY_KEY, "[]")
        _sessionHistory.value = parseSessionHistory(historyJson ?: "[]")
    }

    private fun saveSessionData() {
        prefs.edit()
            .putInt(COMPLETED_FOCUS_SESSIONS_TODAY_KEY, _completedFocusSessionsToday.value)
            .putLong(TOTAL_FOCUS_TIME_TODAY_KEY, _totalFocusTimeToday.value)
            .putInt(CURRENT_STREAK_KEY, _currentStreak.value)
            .putString(SESSION_HISTORY_KEY, serializeSessionHistory(_sessionHistory.value))
            .apply()
    }

    fun startSession(type: SessionType) {
        if (_sessionState.value != SessionState.IDLE && _sessionState.value != SessionState.PAUSED) {
            return // Session already running or paused
        }

        _currentSessionType.value = type
        val duration = when (type) {
            SessionType.FOCUS -> FOCUS_DURATION_MILLIS
            SessionType.SHORT_BREAK -> SHORT_BREAK_DURATION_MILLIS
            SessionType.LONG_BREAK -> LONG_BREAK_DURATION_MILLIS
        }
        _timeLeftMillis.value = duration
        startTimer(duration)
        _sessionState.value = SessionState.RUNNING
    }

    fun pauseSession() {
        if (_sessionState.value == SessionState.RUNNING) {
            countDownTimer?.cancel()
            _sessionState.value = SessionState.PAUSED
        }
    }

    fun resumeSession() {
        if (_sessionState.value == SessionState.PAUSED) {
            startTimer(_timeLeftMillis.value)
            _sessionState.value = SessionState.RUNNING
        }
    }

    fun endSession(wasCompleted: Boolean) {
        countDownTimer?.cancel()
        val duration = when (_currentSessionType.value) {
            SessionType.FOCUS -> FOCUS_DURATION_MILLIS
            SessionType.SHORT_BREAK -> SHORT_BREAK_DURATION_MILLIS
            SessionType.LONG_BREAK -> LONG_BREAK_DURATION_MILLIS
        }
        val actualDuration = duration - _timeLeftMillis.value

        val record = SessionRecord(
            timestamp = System.currentTimeMillis(),
            type = _currentSessionType.value,
            durationMillis = actualDuration,
            completed = wasCompleted
        )
        addSessionRecord(record)

        if (_currentSessionType.value == SessionType.FOCUS && wasCompleted) {
            _completedFocusSessionsToday.value++
            _totalFocusTimeToday.value += actualDuration
            updateStreak()
        }

        _sessionState.value = SessionState.IDLE
        _timeLeftMillis.value = 0L
        saveSessionData()
    }

    private fun startTimer(duration: Long) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftMillis.value = millisUntilFinished
            }

            override fun onFinish() {
                _timeLeftMillis.value = 0L
                endSession(true) // Session completed
                // Automatically start next session type
                when (_currentSessionType.value) {
                    SessionType.FOCUS -> {
                        if (_completedFocusSessionsToday.value % SESSIONS_BEFORE_LONG_BREAK == 0) {
                            startSession(SessionType.LONG_BREAK)
                        } else {
                            startSession(SessionType.SHORT_BREAK)
                        }
                    }
                    SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> {
                        startSession(SessionType.FOCUS)
                    }
                }
            }
        }.start()
    }

    private fun addSessionRecord(record: SessionRecord) {
        val currentHistory = _sessionHistory.value.toMutableList()
        currentHistory.add(0, record) // Add to the beginning
        _sessionHistory.value = currentHistory
    }

    private fun updateStreak() {
        val today = getTodayDateString()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday.time)

        val lastRecord = _sessionHistory.value.firstOrNull { it.type == SessionType.FOCUS && it.completed }
        if (lastRecord != null) {
            val lastRecordDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(lastRecord.timestamp))
            if (lastRecordDate == today) {
                // Already incremented today, or still within the same day
                if (_completedFocusSessionsToday.value == 1) { // First focus session of the day
                    val previousDayRecord = _sessionHistory.value.firstOrNull {
                        it.type == SessionType.FOCUS && it.completed &&
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) == yesterdayDateString
                    }
                    if (previousDayRecord != null || _currentStreak.value > 0) {
                        _currentStreak.value++
                    } else {
                        _currentStreak.value = 1
                    }
                }
            } else if (lastRecordDate == yesterdayDateString) {
                _currentStreak.value++
            } else {
                _currentStreak.value = 1
            }
        } else {
            _currentStreak.value = 1
        }
    }

    fun getProductivityScore(): Int {
        // Simple score: 100 points per completed focus session
        return _completedFocusSessionsToday.value * 100
    }

    // Data classes and enums
    enum class SessionState {
        IDLE, RUNNING, PAUSED
    }

    enum class SessionType {
        FOCUS, SHORT_BREAK, LONG_BREAK
    }

    data class SessionRecord(
        val timestamp: Long,
        val type: SessionType,
        val durationMillis: Long,
        val completed: Boolean
    ) {
        fun toJsonObject(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("type", type.name)
                put("durationMillis", durationMillis)
                put("completed", completed)
            }
        }

        companion object {
            fun fromJsonObject(json: JSONObject): SessionRecord {
                return SessionRecord(
                    json.getLong("timestamp"),
                    SessionType.valueOf(json.getString("type")),
                    json.getLong("durationMillis"),
                    json.getBoolean("completed")
                )
            }
        }
    }

    private fun serializeSessionHistory(history: List<SessionRecord>): String {
        val jsonArray = JSONArray()
        history.forEach { jsonArray.put(it.toJsonObject()) }
        return jsonArray.toString()
    }

    private fun parseSessionHistory(jsonString: String): List<SessionRecord> {
        val jsonArray = JSONArray(jsonString)
        val history = mutableListOf<SessionRecord>()
        for (i in 0 until jsonArray.length()) {
            history.add(SessionRecord.fromJsonObject(jsonArray.getJSONObject(i)))
        }
        return history
    }
}
