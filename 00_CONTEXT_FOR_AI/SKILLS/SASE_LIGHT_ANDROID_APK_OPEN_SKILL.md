# SASE Light — Android APK Open Skill

**INTERNAL OPERATIONAL CONTEXT — NO SECRETS**

Esta skill define el flujo para generar la APK Android de SASE Light en GitHub Codespaces y entregar una URL pública descargable para abrirla en un celular Android o iPad.

## Cuándo usar esta skill

Usar esta skill cuando Hugo diga algo como:

- "Ábreme la aplicación"
- "Dame la URL para abrirla en mi celular"
- "Genera la APK"
- "Quiero probar la app en Android"
- "Sírvela para descargarla"
- "Pásame el APK"

## Entorno

Estamos trabajando en GitHub Codespaces / Linux container.

No usar:

- rutas Windows como `C:\HUGO_SYSTEM\Projects\sase-light`
- `.\gradlew.bat`

Usar la raíz actual del repo.

## Objetivo

1. Verificar que el repo esté limpio.
2. Confirmar JDK 21.
3. Confirmar Android SDK.
4. Ejecutar validación desktop con JDK 21 cuando aplique.
5. Generar APK debug con JDK 21.
6. Servir el APK por HTTP.
7. Hacer público el puerto de Codespaces.
8. Entregar URL pública terminada en `/composeApp-debug.apk`.
9. Mantener el servidor activo hasta que Hugo confirme que ya descargó el APK.

## Reglas duras

- No modificar archivos del repo para generar o servir la APK.
- No usar `git add .`.
- No hacer `git commit`.
- No hacer `git push`.
- No hacer `git stash`.
- No hacer `git reset`.
- No hacer `git restore` sin autorización explícita.
- No commitear `local.properties`, `build/`, `.codex/`, `.opencode/`, patches ni metadata local.
- Si `local.properties` aparece, debe quedar local/ignorado. No agregarlo.

No tocar:

- Kotlin
- UI
- tests
- mocks
- backend
- Supabase
- PDF/impresión
- Gemini
- AGENTS.md
- 00_CONTEXT_FOR_AI
- `.gitignore`
- `.env`
- Color.kt
- Theme.kt
- Type.kt

## Flujo obligatorio

Ejecutar desde la raíz del repo:

```bash
pwd
git status --short
git branch --show-current
```

Si `git status --short` muestra cambios inesperados, detenerse y reportar.

## Java obligatorio

SASE Light en Codespaces debe usar JDK 21 para cualquier tarea Gradle.

No usar Java 25 ni el alias `current` si apunta a Java 25. Java 25 provoca ruido y fallas con Gradle/Kotlin, incluyendo `IllegalArgumentException: 25.0.2`.

Antes de correr Gradle, verificar y fijar Java 21:

```bash
if [ -d "/usr/local/sdkman/candidates/java/21.0.10-ms" ]; then
  export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
elif [ -d "$HOME/java/21.0.10-ms" ]; then
  export JAVA_HOME="$HOME/java/21.0.10-ms"
else
  echo "JDK 21 not found. Stop and report."
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Criterio obligatorio: `java -version` debe mostrar `21.x` antes de correr Gradle.

Si `java -version` muestra `25.x`, detenerse o corregir `JAVA_HOME` antes de continuar.

Ejecutar con Java 21:

```bash
./gradlew :composeApp:desktopTest --no-daemon
./gradlew :composeApp:assembleDebug --no-daemon
```

No modificar archivos del repo para cambiar Java.

## Android SDK

Verificar Android SDK:

```bash
echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
which sdkmanager || true
ls "$HOME/android-sdk" 2>/dev/null || true
```

Si ya existe SDK en `/home/codespace/android-sdk`, usar:

```bash
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Si no existe Android SDK, pedir autorización antes de instalarlo.

No instalar SDK sin autorización explícita.

## local.properties

Si Android SDK existe, asegurar `local.properties` solo para el Codespace:

```bash
printf "sdk.dir=%s\n" "$ANDROID_HOME" > local.properties
```

Recordar: `local.properties` es local y no se commitea.

## Generar APK

Antes de generar APK, confirmar que Java sigue en 21:

```bash
java -version
```

Ejecutar:

```bash
./gradlew :composeApp:assembleDebug --no-daemon
```

Si falla, reportar el error relevante y detenerse.

Si pasa, localizar APK:

```bash
find composeApp/build/outputs/apk/debug -type f -name "*.apk" -print -exec ls -lh {} \;
```

La ruta esperada es:

```text
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Prueba visual en Codespaces

No usar Xvfb como flujo normal para probar Compose Desktop.

Compose Desktop requiere entorno gráfico/OpenGL/GLX y Codespaces no es confiable para eso. La ruta oficial para prueba visual desde iPad/celular es APK Android por puerto público.

Flujo oficial:

1. Generar APK Android con Java 21:

```bash
./gradlew :composeApp:assembleDebug --no-daemon
```

2. Servir APK:

```bash
python3 -m http.server 8000 --directory composeApp/build/outputs/apk/debug
```

3. Hacer público el puerto 8000 en Codespaces.

4. Entregar URL pública:

```text
https://<CODESPACE_NAME>-8000.app.github.dev/composeApp-debug.apk
```

Nunca entregar:

- `localhost`
- IP `10.x`
- URL interna
- URL sin `/composeApp-debug.apk`

Xvfb solo puede usarse como diagnóstico excepcional si Hugo lo autoriza explícitamente, pero no como flujo recomendado.

## Servir APK por HTTP

Antes de iniciar servidor, matar servidores previos del mismo puerto si existen:

```bash
pkill -f "http.server 8000" 2>/dev/null || true
```

Iniciar servidor:

```bash
nohup python3 -m http.server 8000 --directory composeApp/build/outputs/apk/debug >/tmp/sase-apk-http.log 2>&1 &
echo $! > /tmp/sase-apk-http.pid
sleep 2
```

Verificar servidor:

```bash
cat /tmp/sase-apk-http.pid
curl -s -o /dev/null -w "%{http_code} - %{size_download} bytes\n" http://localhost:8000/composeApp-debug.apk
```

Debe responder `200`.

## Hacer público el puerto 8000

Intentar por CLI:

```bash
if [ -n "$CODESPACE_NAME" ]; then
  gh codespace ports visibility 8000:public -c "$CODESPACE_NAME" || true
  gh codespace ports -c "$CODESPACE_NAME" | grep 8000 || true
fi
```

Si el CLI falla, indicar a Hugo:

```text
Abre el panel Puertos/Ports en Codespaces, busca el puerto 8000 y cambia visibilidad a Public.
```

## URL pública

La URL pública debe tener esta forma:

```text
https://<CODESPACE_NAME>-8000.app.github.dev/composeApp-debug.apk
```

Generar la URL:

```bash
echo "https://${CODESPACE_NAME}-8000.app.github.dev/composeApp-debug.apk"
```

Nunca entregar URL interna tipo:

- `http://10.x.x.x:8000/composeApp-debug.apk`
- `http://localhost:8000/composeApp-debug.apk`

Esas no sirven para el celular.

## Entrega a Hugo

Responder exactamente:

```markdown
## APK listo para celular

- APK: composeApp-debug.apk
- Ruta: composeApp/build/outputs/apk/debug/composeApp-debug.apk
- Tamaño:
- Servidor:
- Puerto:
- Visibilidad:
- URL pública:
- Estado HTTP:
- Instrucción: Abre la URL en tu celular Android, descarga el APK, permite instalación desde el navegador/archivos e instala.

No cierro el servidor hasta que confirmes que ya descargaste el APK.
```

## Apagar servidor cuando Hugo confirme descarga

Cuando Hugo diga que ya descargó el APK:

```bash
if [ -f /tmp/sase-apk-http.pid ]; then
  kill "$(cat /tmp/sase-apk-http.pid)" 2>/dev/null || true
  rm -f /tmp/sase-apk-http.pid
fi

pkill -f "http.server 8000" 2>/dev/null || true
ss -tulpn 2>/dev/null | grep 8000 || echo "Servidor APK apagado"
```

## Regla final

Si no puedes generar URL pública, no inventes.

Reporta:

- si el APK existe
- si el puerto 8000 está activo
- si la visibilidad es pública
- qué acción manual debe hacer Hugo en el panel Ports

No entregar IP interna.
No entregar localhost.
No cerrar servidor antes de confirmación.
