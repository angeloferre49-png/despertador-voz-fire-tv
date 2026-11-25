# Despertador por Voz — Amazon Fire TV

Aplicación Android nativa en Kotlin, con alarmas locales persistentes, TTS y UI horizontal para control remoto.

## Compilación automática

1. Crear un repositorio GitHub y subir el contenido de esta carpeta a la raíz.
2. Usar la rama `main` (o `master`).
3. Abrir **Actions → Build Fire TV APK** y ejecutar el workflow, o hacer un push.
4. Al finalizar, abrir la ejecución y descargar el artefacto `firetv-voice-alarm-debug-apk`.
5. El APK está en el ZIP del artefacto como `app-debug.apk`.

El workflow compila con JDK 17 y Gradle 8.9. No necesita Android Studio local.

## Funciones implementadas

- Crear, editar, activar/desactivar y eliminar varias alarmas.
- Hora, mensaje, activación y repetición diaria guardados en SharedPreferences/JSON.
- `AlarmManager.RTC_WAKEUP` + `setExactAndAllowWhileIdle`.
- `PendingIntent` distinto por ID de alarma.
- Reprogramación diaria individual y restauración tras reinicio/actualización.
- Actividad de alarma a pantalla completa, WakeLock acotado, pantalla encendida y TTS repetido.
- BACK no descarta la alarma.
- Interfaz construida en Kotlin y navegable por controles Android estándar.

## Límites que deben probarse en el Fire TV real

**No se puede garantizar por código que un televisor apagado vuelva a mostrar HDMI.** El Fire TV debe permanecer alimentado, el televisor debe soportar y tener habilitado HDMI-CEC/encendido automático, y el firmware puede bloquear el encendido de pantalla o el inicio de actividades desde segundo plano.

El permiso de alarmas exactas depende de la versión de Fire OS. En Android 12+ la app comprueba `canScheduleExactAlarms()` y abre la pantalla de permiso cuando el sistema la ofrece. Algunos firmwares de Fire OS pueden no exponer esa pantalla o aplicar políticas propias.

El lanzamiento de una Activity desde un `BroadcastReceiver` puede estar restringido por ciertas versiones del sistema. Se incluye el intento directo para compatibilidad con Fire OS, pero no se afirma que funcione en todos los modelos/firmwares. Si el sistema lo bloquea, se necesitará adaptar el disparo a una notificación de pantalla completa/servicio compatible con esa versión.

El volumen, el motor TTS instalado y el paquete de idioma español dependen del dispositivo. Si no hay voz española instalada, se intenta usar el idioma predeterminado.

## Prueba recomendada

1. Instalar el APK y abrir la app una vez.
2. Conceder permiso de alarmas exactas si Fire OS lo solicita.
3. Crear dos alarmas separadas por dos minutos, ambas con repetición diaria.
4. Dejar el Fire TV encendido y comprobar ambas locuciones.
5. Apagar la primera; comprobar que la segunda sigue sonando.
6. Repetir con el TV en standby y Fire TV conectado a corriente.
7. Reiniciar Fire TV y verificar restauración de alarmas.

## Seguridad y alcance

El APK de Actions es un build **debug**, apto para pruebas y sideload. No está firmado como release de producción. Para distribución pública conviene configurar una clave de firma privada en GitHub Secrets y generar un APK release firmado.

## Versiones de construcción

- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- Gradle: 8.9
- JDK: 17
- compileSdk: 35
- minSdk: 23
- targetSdk: 32 (elegido para mantener compatibilidad con Fire OS y evitar elevar innecesariamente requisitos de plataforma; validar en el modelo destino).
