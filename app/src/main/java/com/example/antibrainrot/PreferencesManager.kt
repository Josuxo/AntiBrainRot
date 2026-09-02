package com.example.antibrainrot

import android.content.Context
import android.content.SharedPreferences

enum class SessionState {
    NONE,
    USING,
    CONFIRM
}

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var monitoredCache: Set<String>? = null

    fun getMonitoredPackages(): Set<String> {
        monitoredCache?.let { return it }
        val fromPrefs = prefs.getStringSet(KEY_MONITORED_PACKAGES, emptySet()) ?: emptySet()
        monitoredCache = fromPrefs
        return fromPrefs
    }

    fun addMonitoredPackage(packageName: String) {
        val updated = getMonitoredPackages().toMutableSet().apply { add(packageName) }
        prefs.edit().putStringSet(KEY_MONITORED_PACKAGES, updated).apply()
        monitoredCache = updated
    }

    fun removeMonitoredPackage(packageName: String) {
        val updated = getMonitoredPackages().toMutableSet().apply { remove(packageName) }
        prefs.edit().putStringSet(KEY_MONITORED_PACKAGES, updated).apply()
        monitoredCache = updated
        removeAppData(packageName)
    }

    fun removeAppData(packageName: String) {
        prefs.edit()
            .remove(timerKey(packageName))
            .remove(graceKey(packageName))
            .remove(approvalKey(packageName))
            .remove(sessionEnabledKey(packageName))
            .remove(interventionEnabledKey(packageName))
            .remove(sessionMaxKey(packageName))
            .remove(sessionStateKey(packageName))
            .remove(sessionDeadlineKey(packageName))
            .remove(sessionDurationKey(packageName))
            .remove(sessionPausedKey(packageName))
            .remove(penaltyKey(packageName))
            .remove(appEnteredKey(packageName))
            .apply()
    }

    fun getTimerSeconds(packageName: String): Int =
        prefs.getInt(timerKey(packageName), DEFAULT_TIMER_SECONDS)

    fun setTimerSeconds(packageName: String, seconds: Int) {
        prefs.edit()
            .putInt(timerKey(packageName), seconds.coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS))
            .apply()
    }

    fun getGraceSeconds(packageName: String): Int =
        prefs.getInt(graceKey(packageName), DEFAULT_GRACE_SECONDS)

    fun setGraceSeconds(packageName: String, seconds: Int) {
        prefs.edit()
            .putInt(graceKey(packageName), seconds.coerceIn(MIN_GRACE_SECONDS, MAX_GRACE_SECONDS))
            .apply()
    }

    fun approveAppEntry(packageName: String) {
        prefs.edit()
            .putLong(
                approvalKey(packageName),
                System.currentTimeMillis() + ENTRY_APPROVAL_SECONDS * 1000L
            )
            .putBoolean(appEnteredKey(packageName), true)
            .apply()
    }

    fun approveAppExit(packageName: String) {
        if (!isAppEntered(packageName)) return
        prefs.edit()
            .putLong(
                approvalKey(packageName),
                System.currentTimeMillis() + getGraceSeconds(packageName) * 1000L
            )
            .remove(appEnteredKey(packageName))
            .apply()
    }

    fun setAppEntered(packageName: String, entered: Boolean) {
        prefs.edit().putBoolean(appEnteredKey(packageName), entered).apply()
    }

    fun isAppEntered(packageName: String): Boolean =
        prefs.getBoolean(appEnteredKey(packageName), false)

    fun isAppApproved(packageName: String): Boolean {
        val approvedUntil = prefs.getLong(approvalKey(packageName), 0L)
        if (approvedUntil == 0L) return false
        if (System.currentTimeMillis() >= approvedUntil) {
            prefs.edit().remove(approvalKey(packageName)).apply()
            return false
        }
        return true
    }

    fun getSessionEnabled(packageName: String): Boolean =
        prefs.getBoolean(sessionEnabledKey(packageName), false)

    fun setSessionEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean(sessionEnabledKey(packageName), enabled).apply()
    }

    fun getInterventionEnabled(packageName: String): Boolean =
        prefs.getBoolean(interventionEnabledKey(packageName), true)

    fun setInterventionEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean(interventionEnabledKey(packageName), enabled).apply()
    }

    fun getSessionMaxMinutes(packageName: String): Int {
        val max = prefs.getInt(sessionMaxKey(packageName), DEFAULT_SESSION_MAX_MINUTES)
        return SESSION_MAX_OPTIONS.firstOrNull { it == max } ?: DEFAULT_SESSION_MAX_MINUTES
    }

    fun setSessionMaxMinutes(packageName: String, minutes: Int) {
        val safe = SESSION_MAX_OPTIONS.firstOrNull { it == minutes } ?: DEFAULT_SESSION_MAX_MINUTES
        prefs.edit().putInt(sessionMaxKey(packageName), safe).apply()
    }

    fun getSessionState(packageName: String): SessionState {
        val raw = prefs.getString(sessionStateKey(packageName), SessionState.NONE.name)
        return runCatching { SessionState.valueOf(raw!!) }.getOrDefault(SessionState.NONE)
    }

    fun setSessionState(packageName: String, state: SessionState) {
        prefs.edit().putString(sessionStateKey(packageName), state.name).apply()
    }

    fun getSessionDeadline(packageName: String): Long =
        prefs.getLong(sessionDeadlineKey(packageName), 0L)

    fun setSessionDeadline(packageName: String, millis: Long) {
        prefs.edit().putLong(sessionDeadlineKey(packageName), millis).apply()
    }

    fun getSessionPausedAt(packageName: String): Long =
        prefs.getLong(sessionPausedKey(packageName), 0L)

    fun setSessionPausedAt(packageName: String, millis: Long) {
        prefs.edit().putLong(sessionPausedKey(packageName), millis).apply()
    }

    fun getSessionDurationMinutes(packageName: String): Int =
        prefs.getInt(sessionDurationKey(packageName), 0)

    fun setSessionDurationMinutes(packageName: String, minutes: Int) {
        prefs.edit().putInt(sessionDurationKey(packageName), minutes.coerceAtLeast(1)).apply()
    }

    fun clearSession(packageName: String) {
        prefs.edit()
            .remove(sessionStateKey(packageName))
            .remove(sessionDeadlineKey(packageName))
            .remove(sessionDurationKey(packageName))
            .remove(sessionPausedKey(packageName))
            .remove(appEnteredKey(packageName))
            .apply()
    }

    fun getInterventionPenaltySeconds(packageName: String): Int =
        prefs.getInt(penaltyKey(packageName), 0)

    fun incrementInterventionPenalty(packageName: String, amount: Int = INTERVENTION_PENALTY_INCREMENT) {
        val current = getInterventionPenaltySeconds(packageName)
        prefs.edit().putInt(penaltyKey(packageName), current + amount).apply()
    }

    fun resetInterventionPenalty(packageName: String) {
        prefs.edit().remove(penaltyKey(packageName)).apply()
    }

    private fun timerKey(packageName: String): String = "$KEY_TIMER_PREFIX$packageName"
    private fun graceKey(packageName: String): String = "$KEY_GRACE_PREFIX$packageName"
    private fun approvalKey(packageName: String): String = "$KEY_APPROVAL_PREFIX$packageName"
    private fun sessionEnabledKey(packageName: String): String = "$KEY_SESSION_ENABLED_PREFIX$packageName"
    private fun interventionEnabledKey(packageName: String): String = "$KEY_INTERVENTION_ENABLED_PREFIX$packageName"
    private fun sessionMaxKey(packageName: String): String = "$KEY_SESSION_MAX_PREFIX$packageName"
    private fun sessionStateKey(packageName: String): String = "$KEY_SESSION_STATE_PREFIX$packageName"
    private fun sessionDeadlineKey(packageName: String): String = "$KEY_SESSION_DEADLINE_PREFIX$packageName"
    private fun sessionDurationKey(packageName: String): String = "$KEY_SESSION_DURATION_PREFIX$packageName"
    private fun sessionPausedKey(packageName: String): String = "$KEY_SESSION_PAUSED_PREFIX$packageName"
    private fun penaltyKey(packageName: String): String = "$KEY_PENALTY_PREFIX$packageName"
    private fun appEnteredKey(packageName: String): String = "$KEY_APP_ENTERED_PREFIX$packageName"

    fun isSetupComplete(): Boolean =
        prefs.getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    companion object {
        private const val PREFS_NAME = "antibrainrot_prefs"
        private const val KEY_MONITORED_PACKAGES = "monitored_packages"
        private const val KEY_TIMER_PREFIX = "timer_duration_"
        private const val KEY_GRACE_PREFIX = "grace_duration_"
        private const val KEY_APPROVAL_PREFIX = "approved_until_"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_SESSION_ENABLED_PREFIX = "session_enabled_"
        private const val KEY_INTERVENTION_ENABLED_PREFIX = "intervention_enabled_"
        private const val KEY_SESSION_MAX_PREFIX = "session_max_"
        private const val KEY_SESSION_STATE_PREFIX = "session_state_"
        private const val KEY_SESSION_DEADLINE_PREFIX = "session_deadline_"
        private const val KEY_SESSION_DURATION_PREFIX = "session_duration_"
        private const val KEY_SESSION_PAUSED_PREFIX = "session_paused_"
        private const val KEY_PENALTY_PREFIX = "intervention_penalty_"
        private const val KEY_APP_ENTERED_PREFIX = "app_entered_"

        const val ENTRY_APPROVAL_SECONDS = 5
        const val INTERVENTION_PENALTY_INCREMENT = 5

        const val DEFAULT_TIMER_SECONDS = 10
        const val MIN_TIMER_SECONDS = 1
        const val MAX_TIMER_SECONDS = 30

        const val DEFAULT_GRACE_SECONDS = 5
        const val MIN_GRACE_SECONDS = 1
        const val MAX_GRACE_SECONDS = 60

        const val DEFAULT_SESSION_MAX_MINUTES = 30
        val SESSION_MAX_OPTIONS = listOf(10, 20, 30, 40, 60, 120)

        @Volatile
        private var instance: PreferencesManager? = null

        fun get(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
