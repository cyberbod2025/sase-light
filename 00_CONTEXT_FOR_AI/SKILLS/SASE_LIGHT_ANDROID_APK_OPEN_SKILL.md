# SASE Light — Android APK Open Skill

**INTERNAL OPERATIONAL CONTEXT — NO SECRETS**

Esta skill define el flujo para que un agente genere la APK Android de SASE Light en GitHub Codespaces y entregue una URL pública descargable para abrirla en un celular Android.

## Cuándo usar esta skill

Usar esta skill cuando Hugo diga algo como:

- “Ábreme la aplicación”
- “Dame la URL para abrirla en mi celular”
- “Genera la APK”
- “Quiero probar la app en Android”
- “Sírvela para descargarla”
- “Pásame el APK”

## Entorno

Estamos trabajando en:

GitHub Codespaces / Linux container

No usar:

C:\HUGO_SYSTEM\Projects\sase-light
.\gradlew.bat

Usar la raíz actual del repo.

## Objetivo

1. Verificar que el repo esté limpio.
2. Confirmar Java 21.
3. Confirmar Android SDK.
4. Generar APK debug.
5. Servir el APK por HTTP.
6. Hacer público el puerto de Codespaces.
7. Entregar URL pública terminada en:

/composeApp-debug.apk

8. Mantener el servidor activo hasta que Hugo confirme que ya descargó el APK.

## Reglas duras

No modificar archivos del repo.

No hacer:

git add .
git commit
git push
git stash
git reset
git restore

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
- .gitignore
- .env
- Color.kt
- Theme.kt
- Type.kt

No commitear:

- local.properties
- build/
- .codex/
- .opencode/
- patches
- metadata local

Si local.properties aparece, debe quedar local/ignorado. No agregarlo.

## Flujo obligatorio

Ejecutar desde la raíz del repo:

pwd
git status --short
git branch --show-current

Si git status --short muestra cambios inesperados, detenerse y reportar.

## Java

Usar Java 21 si está disponible:

export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.10-ms
export PATH="$JAVA_HOME/bin:$PATH"
java -version

Si esa ruta no existe, detectar Java disponible:

ls /usr/local/sdkman/candidates/java/ 2>/dev/null || true
java -version

No modificar archivos para cambiar Java.

## Android SDK

Verificar Android SDK:

echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
which sdkmanager || true
ls "$HOME/android-sdk" 2>/dev/null || true

Si ya existe SDK en:

/home/codespace/android-sdk

usar:

export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

Si no existe Android SDK, pedir autorización antes de instalarlo.

No instalar SDK sin autorización explícita.

## local.properties

Si Android SDK existe, asegurar local.properties solo para el Codespace:

printf "sdk.dir=%s\n" "$ANDROID_HOME" > local.properties

Después recordar:

local.properties es local, no se commitea.

## Generar APK

Ejecutar:

./gradlew :composeApp:assembleDebug --no-daemon

Si falla, reportar el error relevante y detenerse.

Si pasa, localizar APK:

find composeApp/build/outputs/apk/debug -type f -name "*.apk" -print -exec ls -lh {} \;

La ruta esperada es:

composeApp/build/outputs/apk/debug/composeApp-debug.apk

## Servir APK por HTTP

Antes de iniciar servidor, matar servidores previos del mismo puerto si existen:

pkill -f "http.server 8000" 2>/dev/null || true

Iniciar servidor:

nohup python3 -m http.server 8000 --directory composeApp/build/outputs/apk/debug >/tmp/sase-apk-http.log 2>&1 &
echo $! > /tmp/sase-apk-http.pid
sleep 2

Verificar servidor:

cat /tmp/sase-apk-http.pid
curl -s -o /dev/null -w "%{http_code} - %{size_download} bytes\n" http://localhost:8000/composeApp-debug.apk

Debe responder:

200

## Hacer público el puerto 8000

Intentar por CLI:

if [ -n "$CODESPACE_NAME" ]; then
  gh codespace ports visibility 8000:public -c "$CODESPACE_NAME" || true
  gh codespace ports -c "$CODESPACE_NAME" | grep 8000 || true
fi

Si el CLI falla, indicar a Hugo:

Abre el panel Puertos/Ports en Codespaces, busca el puerto 8000 y cambia visibilidad a Public.

## URL pública

La URL pública debe tener esta forma:

https://<CODESPACE_NAME>-8000.app.github.dev/composeApp-debug.apk

Generar la URL:

echo "https://${CODESPACE_NAME}-8000.app.github.dev/composeApp-debug.apk"

Nunca entregar URL interna tipo:

http://10.x.x.x:8000/composeApp-debug.apk
http://localhost:8000/composeApp-debug.apk

Esas no sirven para el celular.

## Entrega a Hugo

Responder exactamente:

## APK listo para celular

- APK: composeApp-debug.apk
- Ruta: composeApp/build/outputs/apk/debug/composeApp-debug.apk
- Tamaño:
- Servidor:
- Puerto:
- Visibilidad:
- URL pública:
- Estado HTTP:
- Instrucción:
  Abre la URL en tu celular Android, descarga el APK, permite instalación desde el navegador/archivos e instala.

No cierro el servidor hasta que confirmes que ya descargaste el APK.

## Apagar servidor cuando Hugo confirme descarga

Cuando Hugo diga que ya descargó el APK:

if [ -f /tmp/sase-apk-http.pid ]; then
  kill "$(cat /tmp/sase-apk-http.pid)" 2>/dev/null || true
  rm -f /tmp/sase-apk-http.pid
fi

pkill -f "http.server 8000" 2>/dev/null || true

Confirmar:

ss -tulpn 2>/dev/null | grep 8000 || echo "Servidor APK apagado"

## Regla final

Si no puedes generar URL pública, no inventes.

Reporta:

- si el APK existe;
- si el puerto 8000 está activo;
- si la visibilidad es pública;
- qué acción manual debe hacer Hugo en el panel Ports.

No entregar IP interna.
No entregar localhost.
No cerrar servidor antes de confirmación.
