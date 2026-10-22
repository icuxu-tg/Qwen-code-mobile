#!/data/data/com.termux/files/usr/bin/bash
# Собирает подписанный release APK. Требуется уже собранный
# app/src/main/assets/runtime.zip (см. build-runtime.sh).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [ ! -f "$ROOT/app/src/main/assets/runtime.zip" ]; then
  echo "Нет app/src/main/assets/runtime.zip. Сначала: bash build-runtime.sh" >&2
  exit 1
fi

if [ -z "${GRADLE:-}" ]; then
  GRADLE="$(ls -d "$HOME"/.gradle/wrapper/dists/gradle-9.8.1-bin/*/gradle-9.8.1/bin/gradle 2>/dev/null | head -1 || true)"
fi
if [ -z "${GRADLE:-}" ] || [ ! -x "$GRADLE" ]; then
  echo "Gradle 9.8.1 не найден. Укажите GRADLE=/path/to/gradle" >&2
  exit 1
fi

cd "$ROOT"
exec "$GRADLE" :app:assembleRelease --offline --console=plain "$@"
