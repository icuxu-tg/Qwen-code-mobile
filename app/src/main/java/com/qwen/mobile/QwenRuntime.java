package com.qwen.mobile;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

/** Общее состояние runtime и сервера, которое читает UI. */
public final class QwenRuntime {

    public static final int PORT = 47100;
    public static final String RUNTIME_VERSION = "0.25.3-4";

    public static volatile String status = "Подготовка среды…";
    public static volatile int progress = -1;
    public static volatile boolean running = false;
    public static volatile String error = null;
    public static volatile File logFile;

    private QwenRuntime() {
    }

    public static File runtimeDir(Context context) {
        return new File(context.getFilesDir(), "runtime");
    }

    public static File homeDir(Context context) {
        return new File(context.getFilesDir(), "home");
    }

    public static File logFile(Context context) {
        File file = logFile;
        if (file == null) {
            file = new File(context.getFilesDir(), "qwen.log");
            logFile = file;
        }
        return file;
    }

    /**
     * Рабочая папка агента. Пытаемся использовать общую память /QwenCode,
     * чтобы проекты было видно из файлового менеджера; иначе — app-specific dir.
     */
    public static File workspaceDir(Context context) {
        File shared = new File(Environment.getExternalStorageDirectory(), "QwenCode");
        if (ensureWritable(shared)) {
            return shared;
        }
        File external = context.getExternalFilesDir("workspace");
        if (external != null && ensureWritable(external)) {
            return external;
        }
        File local = new File(context.getFilesDir(), "workspace");
        ensureWritable(local);
        return local;
    }

    private static boolean ensureWritable(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            File probe = new File(dir, ".qwen-write-test");
            FileOutputStream out = new FileOutputStream(probe);
            out.write(1);
            out.close();
            return probe.delete();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String tailLog(Context context, int maxBytes) {
        File file = logFile(context);
        if (!file.isFile()) {
            return "";
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            long start = Math.max(0, length - maxBytes);
            raf.seek(start);
            byte[] buffer = new byte[(int) (length - start)];
            raf.readFully(buffer);
            return new String(buffer, "UTF-8");
        } catch (Throwable t) {
            return "Не удалось прочитать лог: " + t;
        }
    }

    public static void resetStatus() {
        status = "Подготовка среды…";
        progress = -1;
        running = false;
        error = null;
    }
}
