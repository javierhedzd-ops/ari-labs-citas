#!/usr/bin/env bash
# Prueba de humo del APK final (con R8 y firmado): lo instala, lo abre y le da 1500 toques
# al azar (monkey). Si la app se cierra sola en cualquier momento, la prueba sale en rojo.
set -euo pipefail
if [ -z "${FIRMA_LLAVE:-}" ] || [ -z "${FIRMA_CLAVE:-}" ]; then
  echo "Sin secretos de firma: se salta la prueba del APK final."
  exit 0
fi
echo "$FIRMA_LLAVE" | tr -d ' \n\r' | base64 -d > "$RUNNER_TEMP/firma.p12"
export FIRMA_ARCHIVO="$RUNNER_TEMP/firma.p12"
./gradlew --no-daemon :app:assembleRelease
rm -f "$RUNNER_TEMP/firma.p12"

adb uninstall mx.arilabs.citas || true
adb install app/build/outputs/apk/release/app-release.apk
adb shell monkey -p mx.arilabs.citas -c android.intent.category.LAUNCHER 1
sleep 6
adb shell pidof mx.arilabs.citas
# La app debe reconocer su propia firma; si no, los clientes con licencia quedarían bloqueados.
adb logcat -d -s MisCitas | grep -q "original=true" || { echo "La app no reconoce la firma de Ari Labs"; adb logcat -d -s MisCitas; exit 1; }
mkdir -p capturas
adb exec-out screencap -p > capturas/20-final-abierta.png

adb shell monkey -p mx.arilabs.citas --pct-syskeys 0 --ignore-timeouts --ignore-security-exceptions \
  --throttle 120 -s 42 -v 1500 > monkey.txt 2>&1 || { tail -80 monkey.txt; exit 1; }
grep -q "Monkey finished" monkey.txt || { tail -80 monkey.txt; exit 1; }
adb exec-out screencap -p > capturas/21-final-tras-1500-toques.png
echo "APK final: 1500 toques al azar sin cerrarse."
