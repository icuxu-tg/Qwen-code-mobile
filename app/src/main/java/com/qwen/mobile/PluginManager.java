package com.qwen.mobile;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Установка .marp-плагинов и мост к Python-хосту.
 *
 * Хост — отдельный процесс runtime/bin/python3. Он получает команды JSON-
 * строками через stdin, а события присылает JSON-строками через stdout.
 * Это не Chaquopy и не Telegram: плагинам доступен API margelet, но не
 * Android/Telegram-классы. Такие плагины получают понятную ошибку в консоли.
 */
public final class PluginManager {

    private static final String TAG = "QwenPlugins";
    public static final String HOST_VERSION = "1.0";
    private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final int MAX_LOG = 500;
    private static final long MAX_PLUGIN_BYTES = 64L * 1024 * 1024;
    private static final long MAX_MANIFEST_BYTES = 512L * 1024;
    private static final long MAX_MAIN_BYTES = 4L * 1024 * 1024;

    public interface Listener {
        default void onPluginsChanged() {
        }

        default void onPluginLog(LogEntry entry) {
        }

        default void onEngineState() {
        }

        default void onPluginToast(String text) {
        }

        default void onPluginSend(String pluginId, String chat, String text) {
        }
    }

    public static final class LogEntry {
        public final long time;
        public final String plugin;
        public final String text;
        public final boolean error;

        LogEntry(String plugin, String text, boolean error) {
            this.time = System.currentTimeMillis();
            this.plugin = plugin == null ? "" : plugin;
            this.text = text == null ? "" : text;
            this.error = error;
        }

        public String timeText() {
            return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(time));
        }
    }

    public static final class PendingInstall {
        public final File dir;
        public final JSONObject manifest;
        public final String id;

        PendingInstall(File dir, JSONObject manifest, String id) {
            this.dir = dir;
            this.manifest = manifest;
            this.id = id;
        }

        public void cleanup() {
            deleteRecursive(dir);
        }
    }

    private static volatile PluginManager instance;

    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File pluginsDir;
    private final File dataDir;
    private final File metaDir;
    private final File engineDir;
    private final File registryFile;
    private final File logFile;
    private final Object writeLock = new Object();
    private final Object listLock = new Object();
    private final List<PluginInfo> plugins = new ArrayList<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<LogEntry> logs = new ArrayList<>();
    private final Map<Integer, CompletableFuture<JSONObject>> pending =
            new ConcurrentHashMap<>();
    private final AtomicInteger requestId = new AtomicInteger(1);

    private Process process;
    private BufferedWriter writer;
    private Thread readerThread;
    private volatile boolean starting;
    private volatile boolean stopping;
    private volatile boolean engineReady;
    private volatile String engineError;

    private PluginManager(Context context) {
        app = context.getApplicationContext();
        pluginsDir = new File(app.getFilesDir(), "plugins");
        dataDir = new File(app.getFilesDir(), "plugin-data");
        metaDir = new File(app.getFilesDir(), "plugin-meta");
        engineDir = new File(app.getFilesDir(), "plugin-engine");
        registryFile = new File(app.getFilesDir(), "plugins.json");
        logFile = new File(app.getFilesDir(), "plugin-console.log");
        pluginsDir.mkdirs();
        dataDir.mkdirs();
        metaDir.mkdirs();
        loadRegistry();
        loadLogFile();
    }

    public static PluginManager get(Context context) {
        PluginManager local = instance;
        if (local == null) {
            synchronized (PluginManager.class) {
                local = instance;
                if (local == null) {
                    local = new PluginManager(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public List<PluginInfo> getPlugins() {
        synchronized (listLock) {
            return new ArrayList<>(plugins);
        }
    }

    public PluginInfo getPlugin(String id) {
        synchronized (listLock) {
            for (PluginInfo p : plugins) {
                if (p.id.equals(id)) {
                    return p;
                }
            }
        }
        return null;
    }

    public List<LogEntry> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
        if (logFile.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            logFile.delete();
        }
        notifyChanged();
    }

    public boolean isEngineReady() {
        return engineReady;
    }

    public String getEngineError() {
        return engineError;
    }

    public boolean isStarting() {
        return starting;
    }

    public void startAsync() {
        if (starting || (process != null && process.isAlive())) {
            return;
        }
        starting = true;
        notifyEngineState();
        Thread thread = new Thread(this::startBlocking, "plugin-engine-start");
        thread.setDaemon(true);
        thread.start();
    }

    private void startBlocking() {
        synchronized (writeLock) {
            if (process != null && process.isAlive()) {
                starting = false;
                notifyEngineState();
                return;
            }
            stopping = false;
            engineReady = false;
            try {
                File runtime = QwenRuntime.runtimeDir(app);
                File python = new File(runtime, "bin/python3");
                if (!python.isFile()) {
                    throw new IOException("Python ещё не распакован. Сначала запустите Qwen.");
                }
                ensureEngineAssets();
                File host = new File(engineDir, "plugin_host.py");
                if (!host.isFile()) {
                    throw new IOException("Не найден plugin_host.py");
                }
                File home = QwenRuntime.homeDir(app);
                File tmp = new File(app.getFilesDir(), "tmp");
                home.mkdirs();
                tmp.mkdirs();
                File workspace = QwenRuntime.workspaceDir(app);

                ProcessBuilder builder = new ProcessBuilder(
                        python.getAbsolutePath(),
                        "-u",
                        host.getAbsolutePath(),
                        "--plugins", pluginsDir.getAbsolutePath(),
                        "--data", dataDir.getAbsolutePath(),
                        "--version", HOST_VERSION
                );
                builder.directory(workspace);
                builder.redirectErrorStream(true);
                Map<String, String> env = builder.environment();
                env.put("HOME", home.getAbsolutePath());
                env.put("TMPDIR", tmp.getAbsolutePath());
                env.put("PATH", new File(runtime, "bin").getAbsolutePath()
                        + ":/system/bin:/system/xbin");
                env.put("LD_LIBRARY_PATH", new File(runtime, "lib").getAbsolutePath());
                env.put("PYTHONHOME", runtime.getAbsolutePath());
                env.put("PYTHONPATH", engineDir.getAbsolutePath());
                env.put("PYTHONUNBUFFERED", "1");
                env.put("PYTHONUTF8", "1");
                env.put("LANG", "C.UTF-8");
                env.put("LC_ALL", "C.UTF-8");
                env.put("SSL_CERT_FILE",
                        new File(runtime, "etc/tls/cert.pem").getAbsolutePath());
                env.put("OPENSSL_CONF", "/dev/null");
                env.remove("LD_PRELOAD");
                env.remove("NODE_OPTIONS");

                process = builder.start();
                writer = new BufferedWriter(new OutputStreamWriter(
                        process.getOutputStream(), StandardCharsets.UTF_8));
                engineError = null;
                starting = false;
                readerThread = new Thread(this::readLoop, "plugin-engine-reader");
                readerThread.setDaemon(true);
                readerThread.start();
                notifyEngineState();
            } catch (Throwable t) {
                engineError = t.getClass().getSimpleName() + ": " + t.getMessage();
                starting = false;
                engineReady = false;
                Log.e(TAG, "plugin engine start failed", t);
                notifyEngineState();
            }
        }
    }

    private void readLoop() {
        Process current = process;
        try (Reader reader = new InputStreamReader(current.getInputStream(),
                StandardCharsets.UTF_8);
             BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            while ((line = buffered.readLine()) != null) {
                handleLine(line);
            }
        } catch (Throwable t) {
            if (!stopping) {
                engineError = "Чтение Python-хоста: " + t;
            }
        } finally {
            engineReady = false;
            synchronized (writeLock) {
                if (process == current) {
                    process = null;
                    writer = null;
                }
            }
            failPending();
            if (!stopping && engineError == null) {
                engineError = "Python-хост завершился";
            }
            notifyEngineState();
        }
    }

    private void handleLine(String line) {
        line = line == null ? "" : line.trim();
        if (line.isEmpty()) {
            return;
        }
        if (!line.startsWith("{")) {
            addLog("host", line, true);
            return;
        }
        final JSONObject event;
        try {
            event = new JSONObject(line);
        } catch (JSONException e) {
            addLog("host", line, true);
            return;
        }
        String type = event.optString("type", "");
        if ("ready".equals(type)) {
            engineReady = true;
            engineError = null;
            addLog("host", "Python-хост готов", false);
            loadEnabled();
            notifyEngineState();
            return;
        }
        if ("log".equals(type)) {
            String plugin = event.optString("name", event.optString("id", "plugin"));
            addLog(plugin, event.optString("text", ""), event.optBoolean("error", false));
            return;
        }
        if ("settings".equals(type) || "buttons".equals(type)) {
            String id = event.optString("id", "");
            PluginInfo info = getPlugin(id);
            if (info != null) {
                if ("settings".equals(type)) {
                    info.settingsRows = event.optJSONArray("rows");
                    if (info.settingsRows == null) {
                        info.settingsRows = new JSONArray();
                    }
                    info.values = event.optJSONObject("values");
                    if (info.values == null) {
                        info.values = new JSONObject();
                    }
                } else {
                    info.buttons = event.optJSONArray("buttons");
                    if (info.buttons == null) {
                        info.buttons = new JSONArray();
                    }
                }
                saveMeta(info);
                notifyChanged();
            }
            return;
        }
        if ("toast".equals(type)) {
            final String text = event.optString("text", "");
            main.post(() -> {
                Toast.makeText(app, text, Toast.LENGTH_SHORT).show();
                for (Listener l : listeners) {
                    l.onPluginToast(text);
                }
            });
            return;
        }
        if ("send".equals(type)) {
            final String id = event.optString("id", "");
            final String chat = event.optString("chat", "");
            final String text = event.optString("text", "");
            addLog(id, "send(" + chat + "): " + text, false);
            main.post(() -> {
                for (Listener l : listeners) {
                    l.onPluginSend(id, chat, text);
                }
            });
            return;
        }
        if ("send_result".equals(type)) {
            int rid = event.optInt("rid", -1);
            CompletableFuture<JSONObject> future = pending.remove(rid);
            if (future != null) {
                future.complete(event);
            }
            return;
        }
        addLog("host", line, false);
    }

    private void loadEnabled() {
        for (PluginInfo info : getPlugins()) {
            if (info.enabled) {
                sendLoad(info);
            }
        }
    }

    private void sendLoad(PluginInfo info) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "load");
            cmd.put("id", info.id);
            cmd.put("name", info.name);
            cmd.put("folder", info.folder.getAbsolutePath());
            cmd.put("enabled", info.enabled);
        } catch (JSONException ignored) {
        }
        sendCommand(cmd);
    }

    private boolean sendCommand(JSONObject command) {
        synchronized (writeLock) {
            if (writer == null) {
                return false;
            }
            try {
                writer.write(command.toString());
                writer.write("\n");
                writer.flush();
                return true;
            } catch (IOException e) {
                Log.w(TAG, "plugin command failed", e);
                engineError = "Запись в Python-хост: " + e.getMessage();
                return false;
            }
        }
    }

    public void setEnabled(String id, boolean enabled) {
        PluginInfo info = getPlugin(id);
        if (info == null) {
            return;
        }
        info.enabled = enabled;
        saveRegistry();
        if (enabled) {
            startAsync();
            sendLoad(info);
        } else {
            JSONObject cmd = new JSONObject();
            try {
                cmd.put("cmd", "disable");
                cmd.put("id", id);
            } catch (JSONException ignored) {
            }
            sendCommand(cmd);
        }
        notifyChanged();
    }

    private void sendUnload(String id) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "unload");
            cmd.put("id", id);
        } catch (JSONException ignored) {
        }
        sendCommand(cmd);
    }

    public PendingInstall inspect(Uri uri) throws Exception {
        File temp = new File(app.getCacheDir(), "marp-" + System.nanoTime());
        if (!temp.mkdirs()) {
            throw new IOException("Не удалось создать временную папку");
        }
        try {
            extractZip(uri, temp);
            File manifestFile = new File(temp, "manifest.json");
            File mainFile = new File(temp, "main.py");
            if (!manifestFile.isFile()) {
                throw new IOException("В архиве нет manifest.json");
            }
            if (!mainFile.isFile()) {
                throw new IOException("В архиве нет main.py");
            }
            JSONObject manifest = new JSONObject(readString(manifestFile, MAX_MANIFEST_BYTES));
            String id = manifest.optString("id", "");
            if (!PLUGIN_ID.matcher(id).matches()) {
                throw new IOException("Некорректный id плагина: " + id);
            }
            String min = manifest.optString("min_version", "");
            if (!min.isEmpty() && compareVersions(HOST_VERSION, min) < 0) {
                throw new IOException("Нужен Margelet " + min + " или новее; "
                        + "совместимость этой сборки: " + HOST_VERSION);
            }
            return new PendingInstall(temp, manifest, id);
        } catch (Exception e) {
            deleteRecursive(temp);
            throw e;
        }
    }

    public PluginInfo install(PendingInstall pending) throws Exception {
        PluginInfo existing = getPlugin(pending.id);
        if (existing != null && existing.enabled) {
            setEnabled(existing.id, false);
        }
        // Старый модуль Python мог остаться в памяти. Без unload после
        // замены папки включение запустило бы прежний код.
        sendUnload(pending.id);
        File target = new File(pluginsDir, pending.id);
        if (target.exists()) {
            deleteRecursive(target);
        }
        if (!pending.dir.renameTo(target)) {
            copyRecursive(pending.dir, target);
            deleteRecursive(pending.dir);
        }
        PluginInfo info = PluginInfo.fromManifest(pending.manifest, target);
        info.enabled = false;
        if (existing != null) {
            info.settingsRows = existing.settingsRows;
            info.values = existing.values;
            info.buttons = existing.buttons;
        }
        synchronized (listLock) {
            plugins.removeIf(p -> p.id.equals(info.id));
            plugins.add(info);
        }
        saveRegistry();
        saveMeta(info);
        notifyChanged();
        return info;
    }

    public void delete(String id) {
        PluginInfo info = getPlugin(id);
        if (info == null) {
            return;
        }
        sendUnload(id);
        deleteRecursive(new File(pluginsDir, id));
        //noinspection ResultOfMethodCallIgnored
        new File(dataDir, id + ".json").delete();
        //noinspection ResultOfMethodCallIgnored
        new File(metaDir, id + ".json").delete();
        synchronized (listLock) {
            plugins.removeIf(p -> p.id.equals(id));
        }
        saveRegistry();
        notifyChanged();
    }

    public void settingsChanged(String id, String key, Object value) {
        PluginInfo info = getPlugin(id);
        if (info == null) {
            return;
        }
        try {
            info.values.put(key, value == null ? "" : String.valueOf(value));
        } catch (JSONException ignored) {
        }
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "settings");
            cmd.put("id", id);
            cmd.put("key", key);
            cmd.put("value", value);
        } catch (JSONException ignored) {
        }
        if (!sendCommand(cmd)) {
            startAsync();
        }
        saveMeta(info);
        notifyChanged();
    }

    public void buttonClicked(String id, String key) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "button");
            cmd.put("id", id);
            cmd.put("key", key);
        } catch (JSONException ignored) {
        }
        sendCommand(cmd);
    }

    public void chatOpened(String chat) {
        sendEvent("chat_opened", chat, null, null, false);
    }

    public void message(String text, String chat, String messageId, boolean mine) {
        sendEvent("message", chat, text, messageId, mine);
    }

    public void deleted(String messageId, String chat) {
        sendEvent("deleted", chat, null, messageId, false);
    }

    private void sendEvent(String event, String chat, String text,
                           String messageId, boolean mine) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "event");
            cmd.put("event", event);
            cmd.put("chat", chat == null ? "qwen" : chat);
            if (text != null) {
                cmd.put("text", text);
            }
            if (messageId != null) {
                cmd.put("message_id", messageId);
            }
            cmd.put("mine", mine);
        } catch (JSONException ignored) {
        }
        sendCommand(cmd);
    }

    /**
     * Синхронный фильтр для on_send. Возвращает null, если плагин попросил
     * не отправлять текст; иначе итоговый текст.
     */
    public String filterSend(String text, String chat) {
        if (text == null || !engineReady) {
            return text;
        }
        int rid = requestId.getAndIncrement();
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        pending.put(rid, future);
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "send");
            cmd.put("rid", rid);
            cmd.put("text", text);
            cmd.put("chat", chat == null ? "qwen" : chat);
        } catch (JSONException ignored) {
        }
        if (!sendCommand(cmd)) {
            pending.remove(rid);
            return text;
        }
        try {
            JSONObject result = future.get(450, TimeUnit.MILLISECONDS);
            if (result.optBoolean("cancel", false)) {
                return null;
            }
            return result.optString("text", text);
        } catch (Throwable ignored) {
            pending.remove(rid);
            return text;
        }
    }

    public void stop() {
        stopping = true;
        engineReady = false;
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("cmd", "shutdown");
        } catch (JSONException ignored) {
        }
        sendCommand(cmd);
        synchronized (writeLock) {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Throwable ignored) {
            }
            writer = null;
            Process current = process;
            process = null;
            if (current != null) {
                current.destroy();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (current.isAlive()) {
                    current.destroyForcibly();
                }
            }
        }
        failPending();
        notifyEngineState();
    }

    public void restart() {
        stop();
        starting = false;
        startAsync();
    }

    private void failPending() {
        for (Integer key : new ArrayList<>(pending.keySet())) {
            CompletableFuture<JSONObject> f = pending.remove(key);
            if (f != null) {
                f.complete(new JSONObject());
            }
        }
    }

    private void notifyChanged() {
        main.post(() -> {
            for (Listener l : listeners) {
                l.onPluginsChanged();
            }
        });
    }

    private void notifyEngineState() {
        main.post(() -> {
            for (Listener l : listeners) {
                l.onEngineState();
            }
        });
    }

    private void addLog(String plugin, String text, boolean error) {
        LogEntry entry = new LogEntry(plugin, text, error);
        synchronized (logs) {
            logs.add(entry);
            while (logs.size() > MAX_LOG) {
                logs.remove(0);
            }
        }
        appendLog(entry);
        main.post(() -> {
            for (Listener l : listeners) {
                l.onPluginLog(entry);
            }
        });
    }

    private void appendLog(LogEntry entry) {
        try {
            if (logFile.isFile() && logFile.length() > 1024 * 1024) {
                //noinspection ResultOfMethodCallIgnored
                logFile.delete();
            }
            try (OutputStream out = new FileOutputStream(logFile, true)) {
                String line = entry.timeText() + " ["
                        + (entry.plugin.isEmpty() ? "plugin" : entry.plugin) + "] "
                        + (entry.error ? "! " : "") + entry.text + "\n";
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }

    private void loadLogFile() {
        if (!logFile.isFile()) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            int start = Math.max(0, lines.size() - 200);
            synchronized (logs) {
                for (int i = start; i < lines.size(); i++) {
                    logs.add(new LogEntry("console", lines.get(i), false));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void ensureEngineAssets() throws IOException {
        deleteRecursive(engineDir);
        if (!engineDir.mkdirs() && !engineDir.isDirectory()) {
            throw new IOException("Не удалось создать " + engineDir);
        }
        copyAssetTree(app.getAssets(), "plugin", engineDir);
    }

    private static void copyAssetTree(AssetManager assets, String path, File target)
            throws IOException {
        String[] children = assets.list(path);
        if (children == null || children.length == 0) {
            File parent = target.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Не удалось создать " + parent);
            }
            try (InputStream in = assets.open(path);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            }
            return;
        }
        if (!target.mkdirs() && !target.isDirectory()) {
            throw new IOException("Не удалось создать " + target);
        }
        for (String child : children) {
            copyAssetTree(assets, path + "/" + child, new File(target, child));
        }
    }

    private void extractZip(Uri uri, File root) throws IOException {
        InputStream input = app.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("Не удалось открыть архив");
        }
        long total = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(input, 16384))) {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > 4000) {
                    throw new IOException("Слишком много файлов в архиве");
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("..")
                        || name.startsWith("__MACOSX/")) {
                    continue;
                }
                File out = new File(root, name);
                String rootPath = root.getCanonicalPath();
                String outPath = out.getCanonicalPath();
                if (!outPath.equals(rootPath)
                        && !outPath.startsWith(rootPath + File.separator)) {
                    throw new IOException("Небезопасный путь в архиве: " + name);
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Не удалось создать " + parent);
                }
                if ("manifest.json".equals(name)
                        && entry.getSize() > MAX_MANIFEST_BYTES) {
                    throw new IOException("manifest.json слишком большой");
                }
                if ("main.py".equals(name) && entry.getSize() > MAX_MAIN_BYTES) {
                    throw new IOException("main.py слишком большой");
                }
                try (OutputStream os = new BufferedOutputStream(
                        new FileOutputStream(out))) {
                    int read;
                    while ((read = zip.read(buffer)) > 0) {
                        total += read;
                        if (total > MAX_PLUGIN_BYTES) {
                            throw new IOException("Архив больше 64 МБ");
                        }
                        os.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void loadRegistry() {
        synchronized (listLock) {
            plugins.clear();
            if (registryFile.isFile()) {
                try {
                    JSONArray array = new JSONArray(readString(registryFile, 2 * 1024 * 1024));
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject json = array.optJSONObject(i);
                        if (json == null) {
                            continue;
                        }
                        PluginInfo info = PluginInfo.fromJson(json, pluginsDir);
                        if (!PLUGIN_ID.matcher(info.id).matches()
                                || !info.folder.isDirectory()) {
                            continue;
                        }
                        loadMeta(info);
                        plugins.add(info);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "registry read failed", t);
                }
            }
            if (plugins.isEmpty()) {
                File[] dirs = pluginsDir.listFiles();
                if (dirs != null) {
                    for (File dir : dirs) {
                        if (!dir.isDirectory()) {
                            continue;
                        }
                        File manifestFile = new File(dir, "manifest.json");
                        if (!manifestFile.isFile()) {
                            continue;
                        }
                        try {
                            JSONObject manifest = new JSONObject(
                                    readString(manifestFile, MAX_MANIFEST_BYTES));
                            PluginInfo info = PluginInfo.fromManifest(manifest, dir);
                            if (!PLUGIN_ID.matcher(info.id).matches()) {
                                continue;
                            }
                            info.enabled = false;
                            loadMeta(info);
                            plugins.add(info);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        }
    }

    private void saveRegistry() {
        JSONArray array = new JSONArray();
        for (PluginInfo info : getPlugins()) {
            array.put(info.toJson());
        }
        writeString(registryFile, array.toString());
    }

    private void loadMeta(PluginInfo info) {
        File file = new File(metaDir, info.id + ".json");
        if (!file.isFile()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(readString(file, 2 * 1024 * 1024));
            JSONArray rows = json.optJSONArray("rows");
            if (rows != null) {
                info.settingsRows = rows;
            }
            JSONObject values = json.optJSONObject("values");
            if (values != null) {
                info.values = values;
            }
            JSONArray buttons = json.optJSONArray("buttons");
            if (buttons != null) {
                info.buttons = buttons;
            }
        } catch (Throwable ignored) {
        }
    }

    private void saveMeta(PluginInfo info) {
        JSONObject json = new JSONObject();
        try {
            json.put("rows", info.settingsRows);
            json.put("values", info.values);
            json.put("buttons", info.buttons);
        } catch (JSONException ignored) {
        }
        writeString(new File(metaDir, info.id + ".json"), json.toString());
    }

    private static int compareVersions(String a, String b) {
        String[] aa = a.split("[^0-9]+");
        String[] bb = b.split("[^0-9]+");
        int count = Math.max(aa.length, bb.length);
        for (int i = 0; i < count; i++) {
            int av = i < aa.length ? parseInt(aa[i]) : 0;
            int bv = i < bb.length ? parseInt(bb[i]) : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String readString(File file, long max) throws IOException {
        if (file.length() > max) {
            throw new IOException("Файл слишком большой: " + file.getName());
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int offset = 0;
            while (offset < data.length) {
                int read = in.read(data, offset, data.length - offset);
                if (read <= 0) {
                    break;
                }
                offset += read;
            }
            return new String(data, 0, offset, StandardCharsets.UTF_8);
        }
    }

    private static void writeString(File file, String text) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            Log.w(TAG, "write failed " + file, t);
        }
    }

    private static void copyRecursive(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.mkdirs() && !target.isDirectory()) {
                throw new IOException("Не удалось создать " + target);
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyRecursive(child, new File(target, child.getName()));
                }
            }
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Не удалось создать " + parent);
        }
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
