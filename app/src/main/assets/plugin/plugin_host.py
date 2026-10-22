#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Хост Python-плагинов Margelet для Qwen Code Mobile.

Плагин — это обычный .marp (zip) с manifest.json и main.py. Хост даёт ему
объект margelet с тем же набором базовых методов, что и в Margelet:
логи, настройки, таймеры, fetch, toast, кнопки и события.

Важное ограничение: это не Telegram-клиент. Плагины, которые обращаются к
org.telegram.* или строят Android View через java-мост, работать не будут.
Для них создан явный shim, который пишет понятную ошибку в консоль, а не
роняет остальные плагины.
"""

import argparse
import importlib.util
import json
import os
import re
import sys
import threading
import time
import traceback
import urllib.request

PROTO = sys.stdout


def emit(obj):
    """Единственный безопасный способ писать в Android: одна JSON-строка."""
    try:
        PROTO.write(json.dumps(obj, ensure_ascii=False) + "\n")
        PROTO.flush()
    except Exception:
        pass


def safe_name(value):
    value = re.sub(r"[^A-Za-z0-9_.-]", "_", str(value))
    return value[:120] or "plugin"


class Store:
    """Память плагинов. Живёт вне папки плагина и переживает обновление."""

    def __init__(self, root):
        self.root = root
        os.makedirs(root, exist_ok=True)
        self._lock = threading.RLock()

    def _path(self, plugin_id):
        return os.path.join(self.root, safe_name(plugin_id) + ".json")

    def all(self, plugin_id):
        with self._lock:
            try:
                with open(self._path(plugin_id), "r", encoding="utf-8") as fh:
                    data = json.load(fh)
                return data if isinstance(data, dict) else {}
            except Exception:
                return {}

    def get(self, plugin_id, key, default=None):
        value = self.all(plugin_id).get(str(key))
        return default if value is None else value

    def set(self, plugin_id, key, value):
        with self._lock:
            data = self.all(plugin_id)
            if value is None:
                data.pop(str(key), None)
            else:
                data[str(key)] = str(value)
            path = self._path(plugin_id)
            tmp = path + ".tmp"
            with open(tmp, "w", encoding="utf-8") as fh:
                json.dump(data, fh, ensure_ascii=False, indent=2)
            os.replace(tmp, path)


class GlobalConsole:
    """print() плагина не должен попадать в JSON-протокол."""

    def __init__(self, error=False):
        self.error = error
        self.plugin = "plugin"
        self.buffer = ""
        self.lock = threading.RLock()

    def write(self, text):
        with self.lock:
            self.buffer += str(text)
            while "\n" in self.buffer:
                line, self.buffer = self.buffer.split("\n", 1)
                if line:
                    emit({"type": "log", "id": self.plugin, "name": self.plugin,
                          "text": line, "error": self.error})

    def flush(self):
        with self.lock:
            if self.buffer:
                emit({"type": "log", "id": self.plugin, "name": self.plugin,
                      "text": self.buffer, "error": self.error})
                self.buffer = ""


OUT = GlobalConsole(False)
ERR = GlobalConsole(True)


class Task:
    def __init__(self, host, plugin, call, repeat_ms=None):
        self.host = host
        self.plugin = plugin
        self.call = call
        self.repeat_ms = repeat_ms
        self.cancelled = False
        self._lock = threading.Lock()

    def start(self, delay_ms=0):
        timer = threading.Timer(max(0, delay_ms) / 1000.0, self._run)
        timer.daemon = True
        timer.start()
        return self

    def _run(self):
        with self._lock:
            if self.cancelled or self.plugin.disabled:
                return
        try:
            self.call()
        except Exception:
            self.host.log(self.plugin.name, traceback.format_exc(), True)
            return
        with self._lock:
            if self.repeat_ms and not self.cancelled and not self.plugin.disabled:
                self.start(self.repeat_ms)

    def cancel(self):
        with self._lock:
            self.cancelled = True


class CompatActivity:
    """Заглушка для margelet.activity()."""

    def __getattr__(self, name):
        raise RuntimeError(
            "margelet.activity(): Android/Telegram UI недоступен в Qwen Mobile"
        )


class CompatChat:
    """Минимальный объект чата для плагинов, которым нужно только число."""

    def __init__(self, host, chat_id="qwen"):
        self.host = host
        self.chat_id = str(chat_id)

    def getMessagesCount(self):
        try:
            return int(self.host.storage.get("__qwen__", "message_count", "0"))
        except Exception:
            return 0

    def __str__(self):
        return self.chat_id

    def __getattr__(self, name):
        raise RuntimeError(
            "Объект чата Margelet (%s) недоступен: Qwen Mobile — не Telegram"
            % name
        )


class Margelet:
    def __init__(self, host, plugin_id, name, folder):
        self._host = host
        self.id = plugin_id
        self.name = name
        self.folder = folder
        self.disabled = False
        self._on_chat_opened = []
        self._on_send = []
        self._on_message = []
        self._on_deleted = []
        self._on_settings = []
        self._buttons = {}
        self._button_titles = {}
        self._actions = {}
        self._settings_rows = []
        self._cancel_send = False
        self._warned_ui = False

    # --- базовая жизнь ---
    def log(self, *parts):
        self._host.log(self.name, " ".join(str(p) for p in parts), False)

    def error(self, *parts):
        self._host.log(self.name, " ".join(str(p) for p in parts), True)

    def ui(self, call, delay_ms=0):
        if not self._warned_ui:
            self._warned_ui = True
            self._host.log(
                self.name,
                "ui(): в Qwen Mobile нет Android UI-потока; "
                "код будет выполнен в фоновом потоке",
                False,
            )
        task = Task(self._host, self, call)
        task.start(delay_ms)
        return task

    def every(self, ms, call):
        task = Task(self._host, self, call, int(ms))
        task.start(int(ms))
        return task

    def cancel(self, task=None):
        if task is None:
            self._cancel_send = True
            return False
        try:
            task.cancel()
        except Exception:
            pass
        return None

    def toast(self, text):
        emit({"type": "toast", "text": str(text)})

    def get(self, key, fallback=None):
        value = self._host.storage.get(self.id, key, None)
        return fallback if value is None else value

    def set(self, key, value):
        self._host.storage.set(self.id, key, value)

    def flag(self, key, fallback=False):
        value = self._host.storage.get(self.id, key, None)
        return fallback if value is None else value == "1"

    def background(self, call):
        def runner():
            try:
                call()
            except Exception:
                self._host.log(self.name, traceback.format_exc(), True)

        thread = threading.Thread(target=runner, name="plugin-" + self.id)
        thread.daemon = True
        thread.start()
        return thread

    def fetch(self, url, then):
        def runner():
            result = None
            try:
                request = urllib.request.Request(
                    str(url),
                    headers={"User-Agent": "QwenMobile-Margelet/1.0"},
                )
                with urllib.request.urlopen(request, timeout=30) as response:
                    raw = response.read(4 * 1024 * 1024 + 1)
                if len(raw) <= 4 * 1024 * 1024:
                    result = raw.decode("utf-8", "replace")
            except Exception:
                result = None
            try:
                then(result)
            except Exception:
                self._host.log(self.name, traceback.format_exc(), True)

        thread = threading.Thread(target=runner, name="plugin-fetch-" + self.id)
        thread.daemon = True
        thread.start()

    def send(self, chat, text):
        emit({"type": "send", "id": self.id, "chat": str(chat),
              "text": str(text)})

    def dont_send(self):
        self._cancel_send = True
        return False

    def activity(self):
        return CompatActivity()

    def window(self, title, view):
        self._host.log(
            self.name,
            "window(): Android View из плагина не поддерживается; "
            "заголовок: %s" % title,
            True,
        )
        emit({"type": "toast", "text": "Окно плагина недоступно в этой сборке"})

    def color(self, argb):
        value = int(argb)
        return value - 0x100000000 if value > 0x7FFFFFFF else value

    # --- события ---
    def on_chat_opened(self, call):
        self._on_chat_opened.append(call)

    def on_send(self, call):
        self._on_send.append(call)

    def on_message(self, call):
        self._on_message.append(call)

    def on_deleted(self, call):
        self._on_deleted.append(call)

    def button(self, title, call, key=None):
        key = str(key or title)
        self._buttons[key] = call
        self._button_titles[key] = str(title)
        self._host.log(self.name, "кнопка: " + str(title), False)
        self._host.emit_buttons(self)

    def on_settings(self, call):
        self._on_settings.append(call)

    # --- Java-хуки: не поддерживаются, но не ломают плагин ---
    def hook(self, target_class, method_name, before=None, after=None, priority=50):
        self._host.log(
            self.name,
            "Java-хук %s.%s не поддерживается: Qwen Mobile не Telegram"
            % (target_class, method_name),
            True,
        )
        return None

    def before_method(self, target_class, method_name, priority=50):
        def decorator(func):
            self.hook(target_class, method_name, before=func, priority=priority)
            return func
        return decorator

    def after_method(self, target_class, method_name, priority=50):
        def decorator(func):
            self.hook(target_class, method_name, after=func, priority=priority)
            return func
        return decorator

    def hook_method(self, target_class, method_name, priority=50):
        return self.before_method(target_class, method_name, priority)

    # --- экран настроек ---
    def header(self, title):
        return {"kind": "header", "title": str(title)}

    def note(self, text):
        return {"kind": "note", "title": str(text)}

    def switch(self, key, title, default=False, about=None):
        row = {"kind": "switch", "key": str(key), "title": str(title),
               "default": "1" if default else "0"}
        if about:
            row["about"] = str(about)
        return row

    def text(self, key, title, default="", about=None):
        row = {"kind": "text", "key": str(key), "title": str(title),
               "default": str(default)}
        if about:
            row["about"] = str(about)
        return row

    def choice(self, key, title, options, default=None):
        options = [str(o) for o in options]
        row = {"kind": "choice", "key": str(key), "title": str(title),
               "options": options,
               "default": str(default if default is not None
                              else (options[0] if options else ""))}
        return row

    def action(self, title, call, key=None, danger=False):
        key = str(key or title)
        self._actions[key] = call
        return {"kind": "action", "key": key, "title": str(title),
                "danger": bool(danger)}

    def settings(self, *rows):
        rows = [r for r in rows if r]
        self._settings_rows = rows
        for row in rows:
            key = row.get("key")
            if key and "default" in row and \
                    self._host.storage.get(self.id, key, None) is None:
                self._host.storage.set(self.id, key, row["default"])
        self._host.emit_settings(self)

    def values(self):
        result = {}
        for row in self._settings_rows:
            key = row.get("key")
            if key:
                value = self._host.storage.get(self.id, key, row.get("default", ""))
                result[str(key)] = "" if value is None else str(value)
        return result


class LoadedPlugin:
    def __init__(self, module, margelet, enabled=True):
        self.module = module
        self.margelet = margelet
        self.enabled = enabled

    @property
    def disabled(self):
        return not self.enabled


class Host:
    def __init__(self, plugins_dir, data_dir, version):
        self.plugins_dir = plugins_dir
        self.storage = Store(data_dir)
        self.version = version
        self.loaded = {}
        self.lock = threading.RLock()

    def log(self, name, text, error=False):
        emit({"type": "log", "id": getattr(name, "id", ""),
              "name": str(name), "text": str(text), "error": bool(error)})

    def emit_settings(self, margelet):
        emit({
            "type": "settings",
            "id": margelet.id,
            "rows": margelet._settings_rows,
            "values": margelet.values(),
        })

    def emit_buttons(self, margelet):
        emit({
            "type": "buttons",
            "id": margelet.id,
            "buttons": [
                {"key": key, "title": margelet._button_titles.get(key, key)}
                for key in margelet._buttons.keys()
            ],
        })

    def find(self, plugin_id):
        with self.lock:
            return self.loaded.get(plugin_id)

    def load(self, plugin_id, name, folder, enabled=True):
        with self.lock:
            existing = self.loaded.get(plugin_id)
            if existing is not None:
                existing.enabled = bool(enabled)
                if existing.margelet._settings_rows:
                    self.emit_settings(existing.margelet)
                return
            main_py = os.path.join(folder, "main.py")
            if not os.path.isfile(main_py):
                self.log(name, "нет main.py в " + folder, True)
                return

            margelet = Margelet(self, plugin_id, name, folder)
            margelet.disabled = not enabled
            OUT.plugin = name
            ERR.plugin = name
            old_out, old_err = sys.stdout, sys.stderr
            sys.stdout, sys.stderr = OUT, ERR
            try:
                if folder not in sys.path:
                    sys.path.insert(0, folder)
                spec = importlib.util.spec_from_file_location(
                    "margelet_plugin_" + safe_name(plugin_id), main_py)
                module = importlib.util.module_from_spec(spec)
                module.margelet = margelet
                sys.modules[spec.name] = module
                self.loaded[plugin_id] = LoadedPlugin(module, margelet, enabled)
                spec.loader.exec_module(module)
                if hasattr(module, "on_start"):
                    module.on_start()
                self.log(name, "запущен", False)
            except Exception:
                self.log(name, traceback.format_exc(), True)
                if plugin_id not in self.loaded:
                    self.loaded[plugin_id] = LoadedPlugin(None, margelet, enabled)
            finally:
                sys.stdout.flush()
                sys.stderr.flush()
                sys.stdout, sys.stderr = old_out, old_err

    def disable(self, plugin_id):
        with self.lock:
            item = self.loaded.get(plugin_id)
            if item:
                item.enabled = False
                item.margelet.disabled = True

    def unload(self, plugin_id):
        """Убирает код плагина, чтобы обновление .marp загрузилось заново."""
        with self.lock:
            item = self.loaded.pop(plugin_id, None)
            if item:
                item.enabled = False
                item.margelet.disabled = True
                module = item.module
                name = getattr(module, "__name__", None)
                if name:
                    sys.modules.pop(name, None)

    def event(self, event, payload):
        with self.lock:
            items = list(self.loaded.values())
        for item in items:
            if not item.enabled:
                continue
            m = item.margelet
            try:
                if event == "chat_opened":
                    for call in list(m._on_chat_opened):
                        call(CompatChat(self, payload.get("chat", "qwen")))
                elif event == "message":
                    for call in list(m._on_message):
                        call(str(payload.get("text", "")),
                             str(payload.get("chat", "qwen")),
                             str(payload.get("message_id", "")),
                             bool(payload.get("mine", False)))
                elif event == "deleted":
                    for call in list(m._on_deleted):
                        call(payload.get("message_id", ""),
                             payload.get("chat", ""))
            except Exception:
                self.log(m.name, traceback.format_exc(), True)

    def send(self, text, chat):
        result = str(text)
        cancel = False
        with self.lock:
            items = list(self.loaded.values())
        for item in items:
            if not item.enabled:
                continue
            m = item.margelet
            m._cancel_send = False
            for call in list(m._on_send):
                try:
                    answer = call(result, str(chat))
                except Exception:
                    self.log(m.name, traceback.format_exc(), True)
                    continue
                if answer is False or m._cancel_send:
                    cancel = True
                    break
                if isinstance(answer, str):
                    result = answer
            if cancel:
                break
        return result, cancel

    def button_clicked(self, plugin_id, key):
        item = self.find(plugin_id)
        if not item or not item.enabled:
            return
        call = item.margelet._buttons.get(key)
        if call is None:
            return
        try:
            call(CompatChat(self, "qwen"))
        except Exception:
            self.log(item.margelet.name, traceback.format_exc(), True)

    def settings_changed(self, plugin_id, key, value):
        item = self.find(plugin_id)
        if not item:
            return
        m = item.margelet
        key = str(key)
        if key in m._actions:
            try:
                m._actions[key]()
            except Exception:
                self.log(m.name, traceback.format_exc(), True)
            return
        self.storage.set(m.id, key, value)
        for call in list(m._on_settings):
            try:
                call(key, value)
            except Exception:
                self.log(m.name, traceback.format_exc(), True)
        self.emit_settings(m)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--plugins", required=True)
    parser.add_argument("--data", required=True)
    parser.add_argument("--version", default="1.0")
    args = parser.parse_args()

    global PROTO
    PROTO = sys.stdout
    sys.stdout, sys.stderr = OUT, ERR

    host = Host(args.plugins, args.data, args.version)
    emit({"type": "ready", "version": args.version})
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            command = json.loads(line)
        except Exception:
            host.log("host", "плохая команда: " + line[:200], True)
            continue
        cmd = command.get("cmd")
        try:
            if cmd == "load":
                host.load(str(command.get("id")), str(command.get("name", "")),
                          str(command.get("folder")), bool(command.get("enabled", True)))
            elif cmd == "disable":
                host.disable(str(command.get("id")))
            elif cmd == "unload":
                host.unload(str(command.get("id")))
            elif cmd == "event":
                host.event(str(command.get("event")), command)
            elif cmd == "send":
                text, cancel = host.send(command.get("text", ""),
                                         command.get("chat", "qwen"))
                emit({"type": "send_result", "rid": command.get("rid"),
                      "text": text, "cancel": cancel})
            elif cmd == "button":
                host.button_clicked(str(command.get("id")),
                                    str(command.get("key")))
            elif cmd == "settings":
                host.settings_changed(str(command.get("id")),
                                      str(command.get("key")),
                                      command.get("value"))
            elif cmd == "shutdown":
                break
        except Exception:
            host.log("host", traceback.format_exc(), True)


if __name__ == "__main__":
    main()
