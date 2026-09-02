# AntiBrainRot

## Descarga y seguridad

Última versión del APK (debug):

**[Descargar AntiBrainRot.apk](https://github.com/Josuxo/AntiBrainRot/releases/download/latest-build/AntiBrainRot.apk)**

- Requiere **Android 7.0 (API 25)** o superior.
- El enlace siempre apunta a la última compilación: se genera automáticamente con cada actualización del código.

### Advertencias al instalar

- **Play Protect** puede mostrar un aviso de "App poco habitual" al sideladear el APK. Es normal en apps que se instalan fuera de la Play Store y no significa que la app sea peligrosa.
- Al activar el **servicio de accesibilidad**, Android muestra un aviso obligatorio sobre el acceso que la app puede tener a la pantalla. Es inherente a todas las apps de accesibilidad y no se puede quitar por código.
### Privacidad

La app es **100 % local y privada**:
- No pide permisos de red y no envía ni recibe datos. Nada sale de tu dispositivo.
- El control de accesibilidad se usa **solo** para detectar cuándo abres una app vigilada y mostrarte la pantalla de respiración.
- Todas tus apps vigiladas y ajustes se guardan únicamente en el almacenamiento interno del teléfono.

## ¿Qué es AntiBrainRot?

Una aplicación Android que te ayuda a evitar el *brain rot*: intercepta la apertura de las apps que elijas limitar y te muestra una pantalla de respiración antes de poder entrar, para que pares un momento y decidas si de verdad quieres usarla.

## Cómo funciona

1. Añades y activas una app vigilada y configuras sus ajustes.
2. Al abrirla, aparece la pantalla de respiración **"ES LA HORA DEL CELU"** con una cuenta atrás. Puedes tocar **"Entrar igual"** (tras esperar el tiempo de espera) o **"Ya no quiero"** (que te lleva a la pantalla de inicio).
3. **Tiempo de espera**: cuántos segundos esperar en la pantalla de respiración antes de poder continuar.
4. **Tiempo de gracia**: cuánto tiempo puedes estar **fuera** de la app antes de volver a tener que pasar por la pantalla. La cuenta empieza cuando **sales** de la app, así que si vas a responder un mensaje y vuelves dentro de esa ventana, entras directamente.
5. **Temporizador / Re-intervención**: si está activo, al continuar eliges cuántos minutos usar la app (slider "¿Qué tan adicto eres?"). El contador **se pausa mientras estás fuera** y se reanuda donde estaba si vuelves dentro del tiempo de gracia; si vuelves pasado ese tiempo, la app se reinterviene y hay que elegir un nuevo contador.
6. Cuando el contador llega a cero con la app abierta, aparece la pantalla **"Fuiste"**: puedes **"Continuar (+5s por gil)"** (añade penalización al siguiente tiempo de espera) o **"Salir"**.

## Instalación

1. Descarga el APK con el enlace de arriba y ábrelo para instalarlo.
2. Abre **AntiBrainRot** y completa el asistente inicial.
3. Activa el **servicio de accesibilidad** de AntiBrainRot desde Ajustes (Android te lo pedirá).
4. En la pantalla principal, toca **"Añadir aplicación"**, busca la app y congrúrala a tu gusto con **Configurar** (tiempo de espera, tiempo de gracia y temporizador/re-intervención).
5. Ya está: al abrir esa app, se mostrará la pantalla de respiración.

## Compilar desde el código

Requisitos: JDK 17+ (o el de tu Android Studio) y el Android SDK.

```
.\gradlew.bat :app:assembleDebug
```

El APK se genera en `app\build\outputs\apk\debug\AntiBrainRot.apk`.

El APK también se compila automáticamente en cada push a `main` (workflow `build.yml`) y se sube al release `latest-build`, por eso el enlace de descarga nunca cambia.