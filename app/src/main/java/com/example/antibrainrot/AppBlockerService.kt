package com.example.antibrainrot

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_HOME
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockerService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager

    private var lastForegroundPackage: String? = null

    private val homeLauncherPackage: String? by lazy {
        val intent = Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)
        packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    private val trackableCache = HashMap<String, Boolean>()

    private fun shouldTrackForeground(packageName: String): Boolean {
        trackableCache[packageName]?.let { return it }
        val result = packageManager.getLaunchIntentForPackage(packageName) != null ||
            packageName == homeLauncherPackage ||
            packageName == this.packageName
        trackableCache[packageName] = result
        return result
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager.get(this)
        startSessionTimer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        val prevForeground = lastForegroundPackage
        val tracked = shouldTrackForeground(packageName)
        if (tracked) {
            lastForegroundPackage = packageName
        }

        val monitored = prefs.getMonitoredPackages()

        if (tracked && prevForeground != null && prevForeground in monitored && prevForeground != packageName) {
            prefs.approveAppExit(prevForeground)
            if (prefs.getSessionState(prevForeground) == SessionState.USING &&
                prefs.getSessionPausedAt(prevForeground) == 0L
            ) {
                prefs.setSessionPausedAt(prevForeground, System.currentTimeMillis())
            }
        }

        if (monitored.contains(packageName)) {

            checkSessionDeadline()
            startSessionTimer()

            val state = prefs.getSessionState(packageName)
            if (state == SessionState.USING) {
                handleSessionResume(packageName, prevForeground)
                return
            }
            if (state == SessionState.CONFIRM) {
                return
            }

            if (prefs.isAppApproved(packageName)) {
                return
            }

            if (prevForeground != packageName) {
                launchIntervention(packageName)
            }
        }
    }

    private fun handleSessionResume(packageName: String, prevForeground: String?) {
        val pausedAt = prefs.getSessionPausedAt(packageName)
        if (pausedAt == 0L) return
        prefs.setSessionPausedAt(packageName, 0L)
        val awayMillis = System.currentTimeMillis() - pausedAt
        if (awayMillis < prefs.getGraceSeconds(packageName) * 1000L) {
            val deadline = prefs.getSessionDeadline(packageName)
            if (deadline > 0L) {
                prefs.setSessionDeadline(packageName, deadline + awayMillis)
            }
        } else if (prevForeground != packageName) {
            launchIntervention(packageName)
        }
    }

    private fun startSessionTimer() {
        if (timerJob?.isActive == true) return
        timerJob = scope.launch {
            while (isActive) {
                checkSessionDeadline()
                val next = computeNextDeadline()
                if (next == null) break
                val wait = next - System.currentTimeMillis()
                if (wait > 0) {
                    delay(minOf(wait, MAX_SLEEP_CHUNK_MS))
                }
            }
        }
    }

    private fun computeNextDeadline(): Long? {
        if (!::prefs.isInitialized) return null
        val now = System.currentTimeMillis()
        var next: Long? = null
        for (packageName in prefs.getMonitoredPackages()) {
            if (prefs.getSessionState(packageName) != SessionState.USING) continue
            if (prefs.getSessionPausedAt(packageName) != 0L) continue
            val deadline = prefs.getSessionDeadline(packageName)
            if (deadline <= now) continue
            next = if (next == null) deadline else minOf(next, deadline)
        }
        return next
    }

    private fun checkSessionDeadline() {
        if (!::prefs.isInitialized) return
        val monitored = prefs.getMonitoredPackages()
        val now = System.currentTimeMillis()
        for (packageName in monitored) {
            if (prefs.getSessionState(packageName) != SessionState.USING) continue
            if (prefs.getSessionPausedAt(packageName) != 0L) continue
            val deadline = prefs.getSessionDeadline(packageName)
            if (deadline > 0 && now >= deadline) {
                if (lastForegroundPackage == packageName) {
                    prefs.setSessionState(packageName, SessionState.CONFIRM)
                    launchConfirm(packageName)
                } else {
                    prefs.clearSession(packageName)
                }
            }
        }
    }

    private fun launchIntervention(packageName: String) {
        prefs.clearSession(packageName)
        prefs.resetInterventionPenalty(packageName)
        val timerSeconds = prefs.getTimerSeconds(packageName)
        val intent = Intent(this, InterventionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(EXTRA_TARGET_PACKAGE, packageName)
            putExtra(EXTRA_TIMER_SECONDS, timerSeconds)
        }
        startActivity(intent)
    }

    private fun launchConfirm(packageName: String) {
        val intent = Intent(this, ConfirmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(ConfirmActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "TARGET_PACKAGE"
        const val EXTRA_TIMER_SECONDS = "TIMER_SECONDS"
        private const val MAX_SLEEP_CHUNK_MS = 60_000L
    }
}
