package com.example.antibrainrot

import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.paint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class InterventionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent?.getStringExtra(AppBlockerService.EXTRA_TARGET_PACKAGE)
        val timerSeconds = intent?.getIntExtra(
            AppBlockerService.EXTRA_TIMER_SECONDS,
            PreferencesManager.DEFAULT_TIMER_SECONDS
        ) ?: PreferencesManager.DEFAULT_TIMER_SECONDS

        setContent {
            AntiBrainRotTheme {
                InterventionScreen(
                    targetPackage = targetPackage,
                    durationSeconds = timerSeconds
                )
            }
        }
    }
}

@Composable
fun InterventionScreen(targetPackage: String?, durationSeconds: Int) {
    val context = LocalContext.current

    var secondsRemaining by remember { mutableIntStateOf(durationSeconds) }
    var countingDown by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(1f) }

    BackHandler {
        goHome(context)
    }

    LaunchedEffect(countingDown, durationSeconds) {
        if (!countingDown) return@LaunchedEffect
        val start = SystemClock.elapsedRealtime()
        val total = durationSeconds.coerceAtLeast(1)
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - start
            val remainingMillis = (total * 1000L - elapsed).coerceAtLeast(0L)
            progress = remainingMillis / (total * 1000f)
            secondsRemaining = ((remainingMillis + 999) / 1000).toInt()
            if (remainingMillis <= 0) {
                countingDown = false
                break
            }
            delay(16)
        }
    }

    val imagePainter = painterResource(R.drawable.ic_launcher_full)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = imagePainter,
                contentScale = ContentScale.Crop,
                alpha = 0.65f
            )
            .background(Color(0x99000000))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BreathingCircle(progress = progress)

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "ES LA HORA DEL CELU",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (countingDown) {
            Text(
                text = "Esperando $secondsRemaining segundos...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (!countingDown) {
            Button(
                onClick = {
                    onContinue(context, targetPackage)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Entrar igual")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = {
                goHome(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ya no quiero")
        }
    }
}

@Composable
fun BreathingCircle(progress: Float) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = -1f },
            strokeWidth = 8.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = CircleShape,
            color = Color(0xFFBBDEFB),
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {}
    }
}

private fun onContinue(context: Context, targetPackage: String?) {
    if (targetPackage == null) {
        goHome(context)
        return
    }
    if (PreferencesManager.get(context).getSessionEnabled(targetPackage)) {
        launchDurationPicker(context, targetPackage)
    } else {
        launchTargetApp(context, targetPackage)
    }
}
