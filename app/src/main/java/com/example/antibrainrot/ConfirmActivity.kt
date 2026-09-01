package com.example.antibrainrot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class ConfirmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
        setContent {
            AntiBrainRotTheme {
                ConfirmScreen(
                    targetPackage = targetPackage,
                    onDone = { finishAndRemoveTask() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val pkg = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (pkg != null) {
            PreferencesManager.get(this).clearSession(pkg)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "TARGET_PACKAGE"
    }
}

@Composable
fun ConfirmScreen(
    targetPackage: String?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.get(context) }

    BackHandler {
        prefs.clearSession(targetPackage ?: "")
        goHome(context)
    }

    val minutes = prefs.getSessionDurationMinutes(targetPackage ?: "")

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
                text = "Fuiste",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (minutes > 0) {
                    "Has estado usando esta aplicación durante $minutes minuto" +
                        (if (minutes == 1) "." else "s. ")
                } else {
                    "Es hora de hacer un balance. "
                } + "¿Vas a ser productivo?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    onContinueToIntervention(context, targetPackage)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar (+5s por gil)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    prefs.clearSession(targetPackage ?: "")
                    goHome(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salir")
            }
        }
    }
}

private fun onContinueToIntervention(context: Context, targetPackage: String?) {
    val pkg = targetPackage ?: run {
        goHome(context)
        return
    }
    val prefs = PreferencesManager.get(context)
    prefs.clearSession(pkg)
    prefs.incrementInterventionPenalty(pkg)
    val timerSeconds = prefs.getTimerSeconds(pkg) + prefs.getInterventionPenaltySeconds(pkg)
    val intent = Intent(context, InterventionActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        putExtra(AppBlockerService.EXTRA_TARGET_PACKAGE, pkg)
        putExtra(AppBlockerService.EXTRA_TIMER_SECONDS, timerSeconds)
    }
    context.startActivity(intent)
    if (context is android.app.Activity) context.finishAndRemoveTask()
}
