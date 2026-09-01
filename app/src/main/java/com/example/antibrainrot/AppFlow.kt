package com.example.antibrainrot

import android.app.Activity
import android.content.Context
import android.content.Intent

internal fun launchTargetApp(context: Context, targetPackage: String?) {
    if (targetPackage == null) {
        if (context is Activity) context.finishAndRemoveTask()
        return
    }
    PreferencesManager.get(context).setAppApproved(targetPackage)
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
