package com.example.antibrainrot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

class DurationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
        setContent {
            AntiBrainRotTheme {
                DurationPickerScreen(
                    targetPackage = targetPackage,
                    onTerminate = { goHome(this) }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "TARGET_PACKAGE"
    }
}

@Composable
fun DurationPickerScreen(
    targetPackage: String?,
    onTerminate: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.get(context) }

    BackHandler { onTerminate() }

    val maxMinutes = prefs.getSessionMaxMinutes(targetPackage ?: "")
    var minutes by remember { mutableIntStateOf(1) }
    val vibrator = remember {
        if (hasHapticEngine(context)) {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } else {
            null
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¿Qué tan adicto eres?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            targetPackage?.let { pkg ->
                if (pkg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconSmall(context, pkg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(appLabel(context, pkg), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tiempo del tis tos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = if (minutes == 1) "1 minuto" else "$minutes minutos",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = minutes.toFloat(),
                onValueChange = { newValue ->
                    val snapped = newValue.toInt().coerceIn(1, maxMinutes)
                    if (snapped != minutes) {
                        minutes = snapped
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                },
                valueRange = 1f..maxMinutes.toFloat(),
                steps = (maxMinutes - 2).coerceAtLeast(0)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    startSession(context, targetPackage, minutes)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Empezar a usar la aplicación")
            }
        }
    }
}

private fun startSession(context: Context, targetPackage: String?, minutes: Int) {
    if (targetPackage == null) {
        goHome(context)
        return
    }
    val prefs = PreferencesManager.get(context)
    prefs.setSessionDurationMinutes(targetPackage, minutes)
    prefs.setSessionDeadline(
        targetPackage,
        System.currentTimeMillis() + minutes * 60_000L
    )
    prefs.setSessionState(targetPackage, SessionState.USING)
    launchTargetApp(context, targetPackage)
}

@Composable
private fun AppIconSmall(context: Context, packageName: String) {
    val icon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(56, 56)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    } else {
        Box(modifier = Modifier.size(28.dp))
    }
}

private fun appLabel(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}
