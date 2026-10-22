#!/data/data/com.termux/files/usr/bin/bash
# Собирает автономный runtime (Node + qwen-code + bash/git/rg) и упаковывает
# его в app/src/main/assets/runtime.zip.
#
# Запуск из Termux:
#   bash ~/qwen-mobile/build-runtime.sh
#
# Требуется установленный @qwen-code/qwen-code (npm i -g @qwen-code/qwen-code)
# и glibc-readelf (pkg install binutils-glibc или пакет, где он есть).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
RT="$ROOT/.runtime-stage"
OUT="$ROOT/app/src/main/assets/runtime.zip"
PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
QWEN_SRC="${QWEN_SRC:-$PREFIX/lib/node_modules/@qwen-code/qwen-code}"
READELF="${READELF:-$(command -v readelf || true)}"

if [ -z "$READELF" ] || [ ! -x "$READELF" ]; then
  READELF="$PREFIX/glibc/bin/readelf"
fi
if [ ! -x "$READELF" ]; then
  echo "readelf не найден. Установите пакет с binutils (glibc)." >&2
  exit 1
fi
if [ ! -f "$QWEN_SRC/cli-entry.js" ]; then
  echo "Не найден qwen-code: $QWEN_SRC" >&2
  echo "Сначала: npm install -g @qwen-code/qwen-code" >&2
  exit 1
fi

echo "==> staging $RT"
rm -rf "$RT"
mkdir -p "$RT/bin" "$RT/lib" "$RT/qwen" "$RT/share"

for bin in node bash rg git script; do
  src="$PREFIX/bin/$bin"
  if [ ! -f "$src" ]; then
    echo "Нет $src (pkg install $bin)" >&2
    exit 1
  fi
  cp "$src" "$RT/bin/$bin"
  chmod 755 "$RT/bin/$bin"
done

# stty нужен терминалу, чтобы подогнать размер псевдотерминала под xterm.
# В Termux это символическая ссылка на coreutils, поэтому копируем с -L.
if [ -f "$PREFIX/bin/stty" ]; then
  cp -L "$PREFIX/bin/stty" "$RT/bin/stty"
  chmod 755 "$RT/bin/stty"
fi

# Рекурсивно копируем не-системные .so из $PREFIX/lib.
copy_deps() {
  local file="$1"
  "$READELF" -d "$file" 2>/dev/null \
    | awk '/NEEDED/{gsub(/\[|\]/,"",$5); print $5}' \
    | while read -r lib; do
        case "$lib" in
          libc.so|libm.so|libdl.so|liblog.so|libandroid.so|libstdc++.so) continue ;;
        esac
        [ -f "$RT/lib/$lib" ] && continue
        if [ -f "$PREFIX/lib/$lib" ]; then
          cp "$PREFIX/lib/$lib" "$RT/lib/$lib"
          chmod 644 "$RT/lib/$lib"
          copy_deps "$PREFIX/lib/$lib"
        else
          echo "  ! нет библиотеки: $lib" >&2
        fi
      done
}

for bin in node bash rg git script stty; do
  [ -f "$RT/bin/$bin" ] || continue
  echo "==> deps $bin"
  copy_deps "$RT/bin/$bin"
done

echo "==> Python 3.14 (для плагинов Margelet)"
cp "$PREFIX/bin/python3.14" "$RT/bin/python3"
chmod 755 "$RT/bin/python3"
cp "$PREFIX/lib/libpython3.14.so" "$RT/lib/libpython3.14.so"
chmod 644 "$RT/lib/libpython3.14.so"
cp -a "$PREFIX/lib/python3.14" "$RT/lib/"
# Оставляем только то, что нужно для обычных плагинов: без pip, тестов,
# кэша байткода и заголовков для сборки расширений.
rm -rf \
  "$RT/lib/python3.14/site-packages" \
  "$RT/lib/python3.14/ensurepip" \
  "$RT/lib/python3.14/idlelib" \
  "$RT/lib/python3.14/tkinter" \
  "$RT/lib/python3.14/turtledemo" \
  "$RT/lib/python3.14/test"
find "$RT/lib/python3.14" -type d -name '__pycache__' -prune -exec rm -rf {} +
find "$RT/lib/python3.14" -type f -name '*.pyc' -delete
find "$RT/lib/python3.14" -maxdepth 1 -type d -name 'config-*' -prune -exec rm -rf {} +
chmod -R u+rwX,go+rX "$RT/lib/python3.14"
copy_deps "$RT/bin/python3"
copy_deps "$RT/lib/libpython3.14.so"
for so in "$RT"/lib/python3.14/lib-dynload/*.so; do
  [ -f "$so" ] || continue
  copy_deps "$so"
done

echo "==> qwen-code"
cp -a "$QWEN_SRC/." "$RT/qwen/"
chmod -R u+rwX,go+rX "$RT/qwen"

# Не тащим бинарники чужих платформ и ненужную мобильному агенту периферию.
rm -rf "$RT/qwen/vendor/ripgrep/x64-linux"
rm -rf "$RT/qwen/vendor/ripgrep/x64-darwin"
rm -rf "$RT/qwen/vendor/ripgrep/arm64-darwin"
rm -rf "$RT/qwen/vendor/ripgrep/x64-win32"
rm -rf "$RT/qwen/vendor/landlock-run/x64-linux"
rm -rf "$RT/qwen/bundled/browser-use"
rm -rf "$RT/qwen/node_modules/@qwen-code"
rm -rf "$RT/qwen/node_modules/.bin"
find "$RT/qwen" -name '*.map' -delete

# Termux-node собран с OPENSSLDIR=$PREFIX/etc/tls. Внутри APK этого пути нет,
# а qwen вычищает OPENSSL_CONF из окружения дочерних процессов. Убираем
# OPENSSL_CONF из списка "loader env", чтобы ACP-ребёнок не падал.
grep -rl --include='*.js' '"NODE_PATH","OPENSSL_CONF"' "$RT/qwen" 2>/dev/null \
  | while IFS= read -r file; do
      sed -i 's/"NODE_PATH","OPENSSL_CONF"/"NODE_PATH"/' "$file"
    done

echo "==> git templates"
mkdir -p "$RT/share/git-core"
cp -a "$PREFIX/share/git-core/templates" "$RT/share/git-core/"

echo "==> git https helper + CA bundle"
mkdir -p "$RT/libexec/git-core" "$RT/etc/tls"
cp "$PREFIX/libexec/git-core/git-remote-http" "$RT/libexec/git-core/git-remote-http"
cp "$PREFIX/libexec/git-core/git-remote-http" "$RT/libexec/git-core/git-remote-https"
chmod 755 "$RT/libexec/git-core/git-remote-http" "$RT/libexec/git-core/git-remote-https"
copy_deps "$RT/libexec/git-core/git-remote-http"
cp "$PREFIX/etc/tls/cert.pem" "$RT/etc/tls/cert.pem"
chmod 644 "$RT/etc/tls/cert.pem"

# Symlink'и в runtime не нужны; заменяем файлы их содержимым.
find "$RT/bin" "$RT/lib" "$RT/qwen" "$RT/share" -type l -print0 2>/dev/null \
  | while IFS= read -r -d '' link; do
      target="$(readlink -f "$link")"
      rm -f "$link"
      cp -a "$target" "$link"
    done

echo "==> zip"
mkdir -p "$(dirname "$OUT")"
python3 - "$RT" "$OUT" <<'PY'
import os
import sys
import zipfile

root, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for top in ("bin", "lib", "libexec", "etc", "qwen", "share"):
        base = os.path.join(root, top)
        for dirpath, _, filenames in os.walk(base):
            for name in filenames:
                path = os.path.join(dirpath, name)
                arc = os.path.relpath(path, root)
                z.write(path, arc)
print("wrote", out)
PY

ls -lh "$OUT"
echo "Готово. Не забудьте обновить QwenRuntime.RUNTIME_VERSION при смене версии qwen."
