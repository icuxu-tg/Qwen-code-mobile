package com.qwen.mobile;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Распаковывает встроенный runtime.zip в files/runtime (один раз на версию). */
final class RuntimeInstaller {

    private static final long EXPECTED_BYTES = 255_000_000L;

    private RuntimeInstaller() {
    }

    static synchronized void ensureInstalled(Context context) throws IOException {
        File dir = QwenRuntime.runtimeDir(context);
        File marker = new File(dir, ".installed");
        if (marker.isFile()
                && QwenRuntime.RUNTIME_VERSION.equals(readMarker(marker))
                && new File(dir, "bin/node").isFile()
                && new File(dir, "bin/python3").isFile()
                && new File(dir, "bin/script").isFile()
                && new File(dir, "qwen/cli-entry.js").isFile()) {
            installWebBridge(context, dir);
            QwenRuntime.status = "Среда готова";
            QwenRuntime.progress = 100;
            return;
        }

        QwenRuntime.status = "Распаковка среды…";
        QwenRuntime.progress = 0;
        deleteRecursive(dir);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Не удалось создать " + dir);
        }

        AssetManager assets = context.getAssets();
        long total = 0;
        int entries = 0;
        try (InputStream raw = new BufferedInputStream(assets.open("runtime.zip"), 1 << 16);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw, 1 << 16))) {
            ZipEntry entry;
            byte[] buffer = new byte[1 << 16];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("..")) {
                    continue;
                }
                File out = new File(dir, name);
                if (entry.isDirectory()) {
                    if (!out.mkdirs() && !out.isDirectory()) {
                        throw new IOException("Не удалось создать " + out);
                    }
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Не удалось создать " + parent);
                }
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out), 1 << 16)) {
                    int read;
                    while ((read = zip.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                        total += read;
                    }
                }
                // Node, bash, git, rg, git-remote-* и .so должны быть исполняемыми.
                if (name.startsWith("bin/")
                        || name.startsWith("libexec/git-core/")
                        || name.endsWith(".so")) {
                    out.setExecutable(true, false);
                }
                entries++;
                if ((entries & 31) == 0) {
                    int percent = (int) Math.min(99, (total * 100) / EXPECTED_BYTES);
                    QwenRuntime.progress = percent;
                    QwenRuntime.status = "Распаковка среды… " + (total / (1024 * 1024)) + " МБ";
                }
            }
        }

        File node = new File(dir, "bin/node");
        if (!node.isFile()) {
            throw new IOException("runtime.zip повреждён: нет bin/node");
        }
        node.setExecutable(true, false);
        new File(dir, "bin/bash").setExecutable(true, false);
        new File(dir, "bin/git").setExecutable(true, false);
        new File(dir, "bin/rg").setExecutable(true, false);
        new File(dir, "bin/python3").setExecutable(true, false);
        new File(dir, "bin/script").setExecutable(true, false);
        new File(dir, "bin/stty").setExecutable(true, false);

        writeMarker(marker, QwenRuntime.RUNTIME_VERSION);
        installWebBridge(context, dir);
        QwenRuntime.progress = 100;
        QwenRuntime.status = "Среда готова";
    }

    /**
     * Вставляет JS-мост в index.html Web Shell до его bundle. Так
     * globalThis.fetch оборачивается гарантированно раньше, чем Qwen
     * сохранит ссылку на него для запросов /prompt.
     */
    private static void installWebBridge(Context context, File runtimeDir) {
        File index = new File(runtimeDir, "qwen/web-shell/index.html");
        if (!index.isFile()) {
            return;
        }
        try {
            String html = readText(index);
            String bridge;
            try (InputStream in = context.getAssets().open("plugin_bridge.js")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                bridge = new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            String begin = "<!-- QWEN_MOBILE_BRIDGE_START -->";
            String end = "<!-- QWEN_MOBILE_BRIDGE_END -->";
            int oldBegin = html.indexOf(begin);
            int oldEnd = html.indexOf(end);
            if (oldBegin >= 0 && oldEnd > oldBegin) {
                html = html.substring(0, oldBegin)
                        + html.substring(oldEnd + end.length());
            }
            String script = begin + "<script>"
                    + bridge.replace("</script", "<\\/script")
                    + "</script>" + end;
            String lower = html.toLowerCase(Locale.ROOT);
            int head = lower.indexOf("<head");
            int insert = head >= 0 ? lower.indexOf('>', head) : -1;
            if (insert >= 0) {
                html = html.substring(0, insert + 1) + script + html.substring(insert + 1);
            } else {
                html = script + html;
            }
            File temp = new File(index.getParentFile(), "index.html.bridge.tmp");
            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(html.getBytes(StandardCharsets.UTF_8));
            }
            if (!temp.renameTo(index)) {
                try (OutputStream out = new FileOutputStream(index)) {
                    out.write(html.getBytes(StandardCharsets.UTF_8));
                }
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
        } catch (Throwable t) {
            Log.w("QwenRuntime", "web shell bridge patch failed", t);
        }
    }

    private static String readText(File file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String readMarker(File marker) {
        try {
            byte[] data = new byte[(int) Math.min(marker.length(), 256)];
            try (InputStream in = new java.io.FileInputStream(marker)) {
                int offset = 0;
                while (offset < data.length) {
                    int read = in.read(data, offset, data.length - offset);
                    if (read <= 0) {
                        break;
                    }
                    offset += read;
                }
            }
            return new String(data, "UTF-8").trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void writeMarker(File marker, String version) throws IOException {
        try (OutputStream out = new FileOutputStream(marker)) {
            out.write(version.getBytes("UTF-8"));
        }
    }

    private static void deleteRecursive(File file) {
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
