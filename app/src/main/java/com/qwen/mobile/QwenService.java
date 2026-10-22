package com.qwen.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Foreground-сервис, который держит qwen serve живым и переживает уход приложения
 * в фон. Сервер слушает только 127.0.0.1, наружу ничего не открывается.
 */
public class QwenService extends Service {

    public static final String ACTION_STOP = "com.qwen.mobile.action.STOP";
    public static final String ACTION_RESTART = "com.qwen.mobile.action.RESTART";

    private static final String TAG = "QwenService";
    private static final String CHANNEL_ID = "qwen_server";
    private static final int NOTIFICATION_ID = 42;

    private Process process;
    private Thread worker;
    private volatile boolean stopping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        QwenRuntime.logFile(this);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopping = true;
            stopServer();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startForegroundCompat(buildNotification("Qwen Code", "Запуск…"));
        } catch (Throwable t) {
            Log.e(TAG, "startForeground failed", t);
            QwenRuntime.error = "Не удалось запустить foreground-сервис: " + t;
            QwenRuntime.status = "Ошибка";
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_RESTART.equals(action)) {
            stopServer();
            worker = null;
        }

        if (worker == null || !worker.isAlive()) {
            stopping = false;
            QwenRuntime.error = null;
            QwenRuntime.running = false;
            worker = new Thread(this::runServer, "qwen-server");
            worker.start();
        }
        return START_STICKY;
    }

    private void runServer() {
        try {
            QwenRuntime.status = "Распаковка среды…";
            QwenRuntime.progress = 0;
            updateNotification("Подготовка среды…");
            RuntimeInstaller.ensureInstalled(this);
            if (stopping) {
                return;
            }

            // Python-хост плагинов живёт рядом с qwen serve: .marp-плагины
            // должны работать и тогда, когда Activity ушла в фон.
            PluginManager.get(this).startAsync();

            QwenRuntime.status = "Запуск сервера…";
            QwenRuntime.progress = -1;
            updateNotification("Запуск сервера…");

            File workspace = QwenRuntime.workspaceDir(this);
            File runtime = QwenRuntime.runtimeDir(this);
            File node = new File(runtime, "bin/node");
            File cli = new File(runtime, "qwen/cli-entry.js");

            // Если сервер уже поднят (например, после перезапуска Activity) — переиспользуем.
            if (isHealthy()) {
                QwenRuntime.running = true;
                QwenRuntime.status = "Готово";
                updateNotification("Работает");
                return;
            }

            killStaleProcesses();

            File home = QwenRuntime.homeDir(this);
            File tmp = new File(getFilesDir(), "tmp");
            if (!home.isDirectory() && !home.mkdirs()) {
                throw new IOException("Не удалось создать " + home);
            }
            if (!tmp.isDirectory() && !tmp.mkdirs()) {
                throw new IOException("Не удалось создать " + tmp);
            }
            ensureGitConfig(home, runtime);
            File log = QwenRuntime.logFile(this);
            truncateIfLarge(log);

            ProcessBuilder builder = new ProcessBuilder(
                    node.getAbsolutePath(),
                    cli.getAbsolutePath(),
                    "serve",
                    "--port", String.valueOf(QwenRuntime.PORT),
                    "--hostname", "127.0.0.1",
                    "--no-open",
                    "--no-token-qr",
                    "--web",
                    "--workspace", workspace.getAbsolutePath(),
                    "--memory-budget-mb", "1536",
                    "--max-sessions", "4"
            );
            builder.directory(workspace);
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

            Map<String, String> env = builder.environment();
            String bin = new File(runtime, "bin").getAbsolutePath();
            env.put("HOME", home.getAbsolutePath());
            env.put("TMPDIR", tmp.getAbsolutePath());
            env.put("PATH", bin + ":/system/bin:/system/xbin");
            env.put("LD_LIBRARY_PATH", new File(runtime, "lib").getAbsolutePath());
            env.put("LANG", "C.UTF-8");
            env.put("LC_ALL", "C.UTF-8");
            env.put("TERM", "xterm-256color");
            env.put("SHELL", new File(runtime, "bin/bash").getAbsolutePath());
            env.put("GIT_EXEC_PATH", new File(runtime, "libexec/git-core").getAbsolutePath());
            env.put("GIT_TEMPLATE_DIR", new File(runtime, "share/git-core/templates").getAbsolutePath());
            env.put("QWEN_CODE", "1");
            // Termux-сборка OpenSSL ищет конфиг в $PREFIX/etc/tls, куда
            // обычному Android-приложению нет доступа. Пустой конфиг = defaults.
            env.put("OPENSSL_CONF", "/dev/null");
            env.put("TZ", java.util.TimeZone.getDefault().getID());
            env.remove("LD_PRELOAD");
            env.remove("NODE_OPTIONS");

            QwenRuntime.status = "Запуск сервера…";
            process = builder.start();

            for (int i = 0; i < 260 && !stopping; i++) {
                if (!process.isAlive()) {
                    break;
                }
                if (isHealthy()) {
                    QwenRuntime.running = true;
                    QwenRuntime.status = "Готово";
                    QwenRuntime.progress = 100;
                    updateNotification("Работает · " + workspace.getName());
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (stopping) {
                return;
            }
            int code = -1;
            try {
                if (!process.isAlive()) {
                    code = process.exitValue();
                }
            } catch (Throwable ignored) {
            }
            QwenRuntime.error = "qwen serve не запустился"
                    + (code >= 0 ? " (код " + code + ")" : "")
                    + ". Откройте лог, чтобы увидеть причину.";
            QwenRuntime.status = "Ошибка";
            updateNotification("Ошибка запуска");
            stopServer();
        } catch (Throwable t) {
            Log.e(TAG, "server failed", t);
            QwenRuntime.error = t.getClass().getSimpleName() + ": " + t.getMessage();
            QwenRuntime.status = "Ошибка";
            updateNotification("Ошибка запуска");
            stopServer();
        }
    }

    private boolean isHealthy() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + QwenRuntime.PORT + "/health").openConnection();
            connection.setConnectTimeout(700);
            connection.setReadTimeout(700);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code != 200) {
                return false;
            }
            try (java.io.InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[256];
                int read = in.read(buffer);
                return read > 0 && new String(buffer, 0, read, "UTF-8").contains("ok");
            }
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Первый запуск: identity для коммитов и CA-бандл для git https. */
    private void ensureGitConfig(File home, File runtime) {
        File config = new File(home, ".gitconfig");
        if (config.isFile()) {
            return;
        }
        String ca = new File(runtime, "etc/tls/cert.pem").getAbsolutePath();
        String content = "[user]\n"
                + "\tname = Qwen Code\n"
                + "\temail = qwen@localhost\n"
                + "[http]\n"
                + "\tsslCAInfo = " + ca + "\n"
                + "[safe]\n"
                + "\tdirectory = *\n";
        try (FileOutputStream out = new FileOutputStream(config)) {
            out.write(content.getBytes("UTF-8"));
        } catch (Throwable t) {
            Log.w(TAG, "git config write failed", t);
        }
    }

    private void stopServer() {
        Process current = process;
        process = null;
        if (current != null) {
            try {
                current.destroy();
                Thread.sleep(300);
                if (current.isAlive()) {
                    current.destroyForcibly();
                }
            } catch (Throwable ignored) {
            }
        }
        killStaleProcesses();
        QwenRuntime.running = false;
    }

    /** Убивает осиротевшие node/qwen процессы именно этого приложения. */
    private void killStaleProcesses() {
        String needle = QwenRuntime.runtimeDir(this).getAbsolutePath();
        File[] entries = new File("/proc").listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            String name = entry.getName();
            if (!name.matches("\\d+")) {
                continue;
            }
            int pid;
            try {
                pid = Integer.parseInt(name);
            } catch (NumberFormatException e) {
                continue;
            }
            if (pid == android.os.Process.myPid()) {
                continue;
            }
            try {
                String cmdline = readCmdline(new File(entry, "cmdline"));
                if (cmdline.contains(needle) && cmdline.contains("qwen")) {
                    android.os.Process.sendSignal(pid, android.os.Process.SIGNAL_KILL);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String readCmdline(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] data = new byte[(int) Math.min(raf.length(), 4096)];
            raf.readFully(data);
            return new String(data, "UTF-8").replace('\0', ' ');
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void truncateIfLarge(File file) {
        try {
            if (file.isFile() && file.length() > 2 * 1024 * 1024) {
                try (FileOutputStream out = new FileOutputStream(file, false)) {
                    // truncate
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Qwen Code", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Сервер Qwen Code в фоне");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private void updateNotification(String text) {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, buildNotification("Qwen Code", text));
            }
        } catch (Throwable ignored) {
        }
    }

    private void startForegroundCompat(Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, QwenService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(
                this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_qwen)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE);

        builder.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_stat_qwen),
                "Стоп", stopIntent).build());

        return builder.build();
    }

    @Override
    public void onDestroy() {
        stopping = true;
        stopServer();
        PluginManager.get(this).stop();
        if (worker != null) {
            worker.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
