# Qwen Code Mobile

Автономное Android-приложение с Qwen Code, встроенным терминалом, браузером
и менеджером плагинов Margelet. Termux для работы приложения **не нужен**.

- APK: `app/build/outputs/apk/release/app-release.apk`
- Готовый файл: `/storage/emulated/0/Download/QwenCode-0.2.0-arm64.apk`
- SHA-256 готового файла: `c948b7e7897fbfa900bae7b41df986fc889a5080f2804be4e6134f5edd5feced`
- Пакет: `com.qwen.mobile`
- Версия: `0.2.0`, qwen-code `0.25.3`, Node.js `24.18.0`, Python `3.14`
- Архитектура: `arm64-v8a`, Android 8+ (`minSdk 26`, `targetSdk 28`)
- Рабочая папка: `/storage/emulated/0/QwenCode`

## Что внутри

| Компонент | Назначение |
|---|---|
| `qwen-code 0.25.3` | агент, Web Shell и сервер `qwen serve` |
| Node.js 24.18.0 | runtime для qwen |
| bash, git, ripgrep | инструменты для агента |
| `libexec/git-core/git-remote-*` + CA | `git clone/push` по HTTPS |
| Python 3.14 | хост `.marp`-плагинов Margelet |
| `script` + `stty` | настоящий PTY для терминала |
| `QwenService` | foreground-сервис, держит сервер и Python-хост живыми |
| WebView | мобильный UI Qwen Web Shell |
| xterm.js | терминал: `cd`, job control, Ctrl+C, интерактивные программы |
| BrowserActivity | встроенный WebView-браузер с адресной строкой и загрузками |

Сервер слушает только `127.0.0.1:47100`; наружу порт не открывается.
Runtime распаковывается из `assets/runtime.zip` в `files/runtime` при первом
запуске или после обновления runtime (около 240 МБ).

## Экраны и навигация

Внизу главного экрана Qwen расположена панель:

- **Qwen** — Web Shell агента;
- **Терминал** — PTY-терминал с `bash`, xterm.js и кнопками Ctrl+C/Tab/Esc;
- **Браузер** — отдельный WebView-браузер: история, поиск, загрузки в
  `Download`, выбор файлов для веб-форм;
- **Плагины** — установка `.marp`, включение/выключение, настройки,
  кнопки плагинов и консоль.

Экраны терминала, браузера и плагинов открываются отдельными Activity и
возвращаются к Qwen по кнопке «назад».

## Установка

1. Скопируйте APK на телефон (он уже лежит в `Download`).
2. Разрешите установку из неизвестных источников.
3. Если Play Protect показывает предупреждение — «Подробнее» → «Всё равно
   установить».

Обновление поверх установленной версии сохраняет настройки провайдера,
историю сессий и рабочую папку. Очищать данные приложения при обновлении не
нужно.

Если установка падает с `INSTALL_FAILED_VERIFICATION_FAILURE`, установите
через Termux/root, временно отключив верификатор:

```bash
su -c 'settings put global package_verifier_enable 0; settings put global verifier_verify_adb_installs 0'
su -c 'pm install -r -d /storage/emulated/0/Download/QwenCode-0.2.0-arm64.apk'
su -c 'settings delete global package_verifier_enable; settings delete global verifier_verify_adb_installs'
```

`targetSdk 28` выбран намеренно: Android разрешает `exec()`/`dlopen()` файлов
из data-каталога приложения только сборкам под API < 29. Termux использует
тот же приём. Для Google Play такая сборка не предназначена.

## Первый запуск

1. Приложение распаковывает runtime (30–60 секунд, зависит от накопителя).
2. Запускается `qwen serve` (10–20 секунд), в шторке появляется уведомление
   «Qwen Code».
3. Открывается Web Shell. Настройки → **Model / Provider** → выбрать
   провайдера и ввести API-ключ.
4. Рабочая папка: `/storage/emulated/0/QwenCode`. Если доступ к общей памяти
   не выдан, приложение использует `Android/data/com.qwen.mobile/files/workspace`.
5. Терминал стартует в той же рабочей папке.

Остановить сервер можно кнопкой «Стоп» в уведомлении. Кнопка «Повторить» на
экране ошибки перезапускает сервис.

## Терминал

Терминал — это не построчный запуск команд. `util-linux script` создаёт
псевдотерминал, внутри работает интерактивный `bash`; xterm.js отправляет
ввод и рисует вывод. Поэтому работают `cd`, `git`, `python`, `top`-подобные
программы, Ctrl+C, Tab и job control.

Кнопка **Сброс** перезапускает оболочку и заново применяет размер экрана.
Долгое нажатие на экран терминала открывает обычное выделение текста
WebView; ввод — по нажатию в область терминала.

## Браузер

Браузер — обычный WebView:

- адресная строка принимает URL или поисковый запрос;
- «назад» внутри браузера сначала листает историю, затем закрывает экран;
- ссылки на другие приложения (`intent://`, `tg://` и т. п.) открываются
  системным обработчиком;
- загрузки попадают в системную папку `Download`;
- `<input type="file">` открывает системный выбор файла;
- геолокация, камера и микрофон веб-страницам не выдаются.

## Плагины Margelet

Плагин — обычный zip с расширением `.marp`:

```
plugin.marp
├── manifest.json   обязателен
├── main.py         обязателен
├── icon.png        необязателен
└── ...
```

Установка: **Плагины → Установить → выбрать .marp**. После проверки
показывается предупреждение о правах и отсутствии песочницы. Новый плагин
устанавливается **выключенным**; включите его переключателем. Python-хост
запускается вместе с `QwenService`, поэтому включённые плагины работают и
когда Activity ушла в фон.

Поддерживается базовый API `margelet`:

| API | Состояние |
|---|---|
| `log`, `error`, `toast`, `print` | работает |
| `settings`, `values`, `get`, `set`, `flag` | работает, значения переживают перезапуск |
| `header`, `note`, `switch`, `text`, `choice`, `action` | работает в экране настроек |
| `button` | работает на карточке плагина |
| `on_settings` | работает |
| `on_chat_opened`, `on_message`, `on_deleted` | обработчики принимаются, но Qwen пока не транслирует эти события; реально подключён только `on_send` |
| `on_send` / `dont_send` / возврат строки | работает: мост в Web Shell фильтрует исходящий prompt |
| `send` | отправляет текст в текущую открытую сессию Qwen, если главный экран активен |
| `background`, `every`, `cancel`, `ui` | работает; `ui` выполняется в фоновом потоке |
| `fetch` | HTTP/HTTPS через `urllib`, стандартный стек Python и CA-бандл |
| `activity`, `window`, Java-классы, Xposed | **не поддерживаются**: выводятся понятные ошибки, остальные плагины продолжают работать |

Ограничения совместимости:

- это **не Telegram-клиент**: `org.telegram.*`, чаты, сообщения Telegram,
  Java-хуки и произвольные Android View недоступны;
- `margelet.send` не создаёт Telegram-сообщение; в Qwen он отправляет prompt
  в текущую сессию;
- `targetSdk 28` и sideload-подпись — приложение не для Google Play;
- плагины **не изолированы**. Python-код работает с правами приложения:
  может читать файлы, ходить в сеть и менять данные. Права в manifest — это
  декларация автора, а не ограничение. Устанавливайте только прочитанный код.
- Для плагинов с `min_version` выше `PluginManager.HOST_VERSION` установка
  отклоняется.

Консоль **Плагины → Консоль** показывает `print`, ошибки Python, ответы
сети и служебные сообщения. Файлы плагинов лежат в
`files/plugins/<id>/`, пользовательские значения — в
`files/plugin-data/<id>.json`, метаданные экрана настроек — в
`files/plugin-meta/<id>.json`.

## Сборка

Требуется Termux с Android SDK, JDK 17 и Gradle 9.8.1 (проект проверен на
этом окружении). Также нужны:
`@qwen-code/qwen-code`, `python`, `util-linux` (для `script`/`stty`),
`readelf` из glibc-binutils.

```bash
cd ~/qwen-mobile

# 1. Собрать runtime.zip из установленных пакетов
bash build-runtime.sh

# 2. Собрать подписанный release APK
bash build-apk.sh

cp app/build/outputs/apk/release/app-release.apk \
   /storage/emulated/0/Download/QwenCode-0.2.0-arm64.apk
```

`build-runtime.sh` копирует Node, bash, git, rg, Python 3.14, `script`,
`stty`, qwen-code и библиотеки, удаляет чужие платформы и ненужные части
Python, затем упаковывает всё в `app/src/main/assets/runtime.zip`.
Ключ подписи: `keystore/qwen-release.jks` (alias `qwen`, пароль `qwen2026`).
Для публичного распространения сгенерируйте свой ключ.

## Обновление qwen-code

```bash
npm install -g @qwen-code/qwen-code@latest
# обновите RUNTIME_VERSION в
# app/src/main/java/com/qwen/mobile/QwenRuntime.java
# при необходимости обновите build-runtime.sh (путь Python, версия)
bash build-runtime.sh
bash build-apk.sh
```

При смене runtime обязательно меняйте `RUNTIME_VERSION`: по этому маркеру
приложение понимает, что старую распаковку нужно удалить и распаковать заново.
Файл `files/home` и рабочая папка при этом не удаляются.

## Ограничения

- Только `arm64-v8a`; x86_64/эмуляторы не поддерживаются.
- Нет голосового ввода/audio-capture и browser-use.
- Нет npm, SSH-клиента и произвольной установки Python-пакетов; git по HTTPS
  работает, `git@…` — нет.
- Сервер qwen работает без bearer-токена на loopback. Любое приложение на
  телефоне теоретически может обратиться к порту 47100.
- `targetSdk 28` — только sideload, не Google Play.
- Web Shell — штатный интерфейс qwen-code; его поведение и настройки меняются
  вместе с версией qwen.
- Панель плагинов и Python-хост не являются песочницей.

## Лицензии

Qwen Code распространяется под Apache-2.0; файл `LICENSE` находится внутри
`runtime.zip` (`qwen/LICENSE`). Node.js, Git, ripgrep, Python, util-linux и
библиотеки Termux используются на условиях их собственных лицензий.
