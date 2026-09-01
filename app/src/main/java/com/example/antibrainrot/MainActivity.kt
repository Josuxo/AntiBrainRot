package com.example.antibrainrot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AntiBrainRotTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.get(context) }

    val overlayEnabled = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val accessibilityEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayEnabled.value = Settings.canDrawOverlays(context)
                accessibilityEnabled.value = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupDone = remember { mutableStateOf(prefs.isSetupComplete()) }
    val startOnDashboard = setupDone.value && overlayEnabled.value && accessibilityEnabled.value
    val currentScreen = remember { mutableStateOf(if (startOnDashboard) "dashboard" else "setup") }
    val configPackage = remember { mutableStateOf<String?>(null) }

    when (currentScreen.value) {
        "setup" -> SetupScreen(
            overlayEnabled = overlayEnabled.value,
            accessibilityEnabled = accessibilityEnabled.value,
            onSetupComplete = {
                prefs.setSetupComplete(true)
                setupDone.value = true
                currentScreen.value = "dashboard"
            }
        )
        "dashboard" -> DashboardScreen(
            prefs = prefs,
            overlayEnabled = overlayEnabled.value,
            accessibilityEnabled = accessibilityEnabled.value,
            onAddApp = { currentScreen.value = "picker" },
            onConfigure = { packageName ->
                configPackage.value = packageName
                currentScreen.value = "config"
            }
        )
        "config" -> {
            val packageName = configPackage.value
            if (packageName != null) {
                AppConfigScreen(
                    prefs = prefs,
                    packageName = packageName,
                    onDone = { currentScreen.value = "dashboard" }
                )
            } else {
                currentScreen.value = "dashboard"
            }
        }
        "picker" -> AppPickerScreen(
            prefs = prefs,
            onDone = { currentScreen.value = "dashboard" }
        )
    }
}

@Composable
fun SetupScreen(
    overlayEnabled: Boolean,
    accessibilityEnabled: Boolean,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Configuración de AntiBrainRot",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Para bloquear aplicaciones distractoras, debes activar el servicio de accesibilidad \u201cAntiBrainRot\u201d en los ajustes del sistema. Esto permite que la app detecte cuándo se abre una aplicación vigilada para mostrar una pantalla de intervención.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activar Servicio de Accesibilidad")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    openOverlaySettings(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Permitir Mostrar sobre Otras Aplicaciones")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = when {
                    accessibilityEnabled && overlayEnabled -> "Todos los pasos de configuración están listos."
                    accessibilityEnabled -> "Accesibilidad activada. Activa la superposición de pantalla."
                    overlayEnabled -> "Superposición activada. Activa el servicio de accesibilidad."
                    else -> "La configuración no está completa. Activa ambos permisos de arriba."
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (accessibilityEnabled && overlayEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                textAlign = TextAlign.Center
            )

            if (accessibilityEnabled && overlayEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empezar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    prefs: PreferencesManager,
    overlayEnabled: Boolean,
    accessibilityEnabled: Boolean,
    onAddApp: () -> Unit,
    onConfigure: (String) -> Unit
) {
    val context = LocalContext.current

    var monitored by remember { mutableStateOf(prefs.getMonitoredPackages()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AntiBrainRot") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = permissionStatus(accessibilityEnabled, overlayEnabled),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (accessibilityEnabled && overlayEnabled) {
                        Color(0xFF81C784)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Aplicaciones vigiladas", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (monitored.isEmpty()) {
                item {
                    Text(
                        text = "Aún no vigilas ninguna aplicación. Toca 'Añadir aplicación' para empezar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                items(monitored.toList()) { packageName ->
                    MonitoredAppRow(
                        context = context,
                        packageName = packageName,
                        onConfigure = { onConfigure(packageName) },
                        onRemove = {
                            prefs.removeMonitoredPackage(packageName)
                            monitored = prefs.getMonitoredPackages()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                OutlinedButton(
                    onClick = onAddApp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Añadir aplicación")
                }
            }
        }
    }
}

@Composable
fun MonitoredAppRow(
    context: Context,
    packageName: String,
    onConfigure: () -> Unit,
    onRemove: () -> Unit
) {
    val appName = getAppLabel(context, packageName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(context, packageName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onConfigure,
                modifier = Modifier.weight(1f)
            ) {
                Text("Configurar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.weight(1f)
            ) {
                Text("Quitar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    prefs: PreferencesManager,
    packageName: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val appName = getAppLabel(context, packageName)

    var timerSeconds by remember(packageName) {
        mutableStateOf(prefs.getTimerSeconds(packageName).toFloat())
    }
    var graceSeconds by remember(packageName) {
        mutableStateOf(prefs.getGraceSeconds(packageName).toFloat())
    }
    var sessionEnabled by remember(packageName) {
        mutableStateOf(prefs.getSessionEnabled(packageName))
    }
    var sessionMaxMinutes by remember(packageName) {
        mutableStateOf(prefs.getSessionMaxMinutes(packageName))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(context, packageName)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(appName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Tiempo de espera antes de abrir la aplicación", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cuánto tiempo esperar antes de poder abrir la aplicación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${timerSeconds.toInt()} segundos",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Slider(
                value = timerSeconds,
                onValueChange = { timerSeconds = it },
                onValueChangeFinished = {
                    prefs.setTimerSeconds(
                        packageName,
                        timerSeconds.toInt().coerceIn(
                            PreferencesManager.MIN_TIMER_SECONDS,
                            PreferencesManager.MAX_TIMER_SECONDS
                        )
                    )
                },
                valueRange = 1f..30f,
                steps = 28
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Dejar desbloqueada tras continuar", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Después de tocar \u201cEntrar igual\u201d, esta aplicación permanece desbloqueada " +
                    "durante este tiempo antes de poder ser interceptada de nuevo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${graceSeconds.toInt()} segundos",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Slider(
                value = graceSeconds,
                onValueChange = { graceSeconds = it },
                onValueChangeFinished = {
                    prefs.setGraceSeconds(
                        packageName,
                        graceSeconds.toInt().coerceIn(
                            PreferencesManager.MIN_GRACE_SECONDS,
                            PreferencesManager.MAX_GRACE_SECONDS
                        )
                    )
                },
                valueRange = 5f..120f,
                steps = 22
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Temporizador / Re-intervención", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pregunta de nuevo cuánto tiempo quieres usar la aplicación y, " +
                            "tras ese tiempo, vuelve a preguntar si quieres continuar o salir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sessionEnabled,
                    onCheckedChange = { checked ->
                        sessionEnabled = checked
                        prefs.setSessionEnabled(packageName, checked)
                    }
                )
            }

            if (sessionEnabled) {
                Spacer(modifier = Modifier.height(24.dp))

                Text("Duración máxima de la sesión", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tiempo máximo que puedes elegir antes de que se te vuelva a preguntar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                PreferencesManager.SESSION_MAX_OPTIONS.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { minutes ->
                            FilterChip(
                                selected = sessionMaxMinutes == minutes,
                                onClick = {
                                    sessionMaxMinutes = minutes
                                    prefs.setSessionMaxMinutes(packageName, minutes)
                                },
                                label = {
                                    Text(
                                        if (minutes >= 60) {
                                            "${minutes / 60} hora" +
                                                (if (minutes / 60 > 1) "s" else "")
                                        } else {
                                            "$minutes min"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    prefs: PreferencesManager,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    val installedApps = remember { getLaunchableApps(context) }
    var monitored by remember { mutableStateOf(prefs.getMonitoredPackages()) }

    val filtered = installedApps.filter {
        it.packageName != context.packageName &&
            (searchQuery.isBlank() ||
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir aplicación") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar aplicaciones...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isMonitored = monitored.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(context, app.packageName)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                if (isMonitored) {
                                    prefs.removeMonitoredPackage(app.packageName)
                                } else {
                                    prefs.addMonitoredPackage(app.packageName)
                                }
                                monitored = prefs.getMonitoredPackages()
                            }
                        ) {
                            Text(if (isMonitored) "Quitar" else "Añadir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(context: Context, packageName: String) {
    val icon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(80, 80)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    } else {
        Box(modifier = Modifier.size(40.dp))
    }
}

private fun getAppLabel(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}

private data class AppInfo(
    val label: String,
    val packageName: String
)

private fun getLaunchableApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    return pm.getInstalledApplications(0)
        .asSequence()
        .mapNotNull { appInfo ->
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                null
            } else {
                val label = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: RuntimeException) {
                    appInfo.packageName
                }
                AppInfo(label = label, packageName = appInfo.packageName)
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}

private fun permissionStatus(accessibilityEnabled: Boolean, overlayEnabled: Boolean): String {
    return when {
        accessibilityEnabled && overlayEnabled ->
            "Servicio de accesibilidad: Activo  |  Superposición: Activa"
        accessibilityEnabled ->
            "Servicio de accesibilidad: Activo  |  Superposición: Desactivada"
        overlayEnabled ->
            "Servicio de accesibilidad: Desactivado  |  Superposición: Activa"
        else ->
            "Servicio de accesibilidad: Desactivado  |  Superposición: Desactivada"
    }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponent = ComponentName(context, AppBlockerService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':')
        .any { ComponentName.unflattenFromString(it) == expectedComponent }
}
