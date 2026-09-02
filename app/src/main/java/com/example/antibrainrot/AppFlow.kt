package com.example.antibrainrot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator

internal fun hasHapticEngine(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return false
    return vibrator.hasVibrator() && vibrator.hasAmplitudeControl()
}

internal fun launchTargetApp(context: Context, targetPackage: String?) {
    if (targetPackage == null) {
        if (context is Activity) context.finishAndRemoveTask()
        return
    }
    PreferencesManager.get(context).approveAppEntry(targetPackage)
    val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
    if (context is Activity) context.finishAndRemoveTask()
}

internal fun goHome(context: Context) {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(homeIntent)
    if (context is Activity) context.finishAndRemoveTask()
}

internal fun launchDurationPicker(context: Context, targetPackage: String?) {
    if (targetPackage == null) {
        goHome(context)
        return
    }
    PreferencesManager.get(context).clearSession(targetPackage)
    val intent = Intent(context, DurationPickerActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        putExtra(DurationPickerActivity.EXTRA_TARGET_PACKAGE, targetPackage)
    }
    context.startActivity(intent)
    if (context is Activity) context.finishAndRemoveTask()
}
