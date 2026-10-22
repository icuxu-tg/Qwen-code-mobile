package com.qwen.mobile;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Настоящий PTY-терминал: util-linux script создаёт псевдотерминал, внутри
 * работает интерактивный bash. xterm.js в WebView присылает ввод и рисует
 * вывод. Это не эмуляция «одна команда — один процесс»: cd, job control,
 * Ctrl+C и интерактивные программы работают.
 */
final class TerminalSession {

    interface Output {
        void onOutput(String text);

        void onExit(int code);
    }

    private volatile Process process;
    private OutputStream input;
    private Thread readerThread;
    private volatile boolean stopping;
    private final Object writeLock = new Object();

    void start(Context context, int cols, int rows, Output output) {
        stop();
        stopping = false;
        try {
            File runtime = QwenRuntime.runtimeDir(context);
            File script = new File(runtime, "bin/script");
            File bash = new File(runtime, "bin/bash");
            if (!script.isFile() || !bash.isFile()) {
                output.onOutput("\u001b[31mТерминал недоступен: runtime ещё не распакован.\u001b[0m\r\n");
                output.onExit(-1);
                return;
            }
            File workspace = QwenRuntime.workspaceDir(context);
            File home = QwenRuntime.homeDir(context);
            File tmp = new File(context.getFilesDir(), "tmp");
            home.mkdirs();
            tmp.mkdirs();

            int safeCols = Math.max(20, Math.min(cols, 300));
            int safeRows = Math.max(5, Math.min(rows, 150));
            // --norc обязателен: Termux-bash компилируется с system bashrc по
            // пути Termux, недоступному приложению. Prompt ставим в шелле
            // script, иначе bash может заменить унаследованный PS1 дефолтным.
            String prompt = "\\[\\e[38;5;141m\\]\\u@\\h\\[\\e[0m\\]:"
                    + "\\[\\e[38;5;111m\\]\\w\\[\\e[0m\\]\\$ ";
            String command = "stty cols " + safeCols + " rows " + safeRows
                    + " 2>/dev/null; PS1=" + shellQuote(prompt)
                    + "; export PS1; exec bash --noprofile --norc -i";

            ProcessBuilder builder = new ProcessBuilder(
                    script.getAbsolutePath(), "-q", "-c", command, "/dev/null");
            builder.directory(workspace);
            builder.redirectErrorStream(true);
            Map<String, String> env = builder.environment();
            String bin = new File(runtime, "bin").getAbsolutePath();
            env.put("HOME", home.getAbsolutePath());
            env.put("TMPDIR", tmp.getAbsolutePath());
            env.put("PATH", bin + ":/system/bin:/system/xbin");
            env.put("LD_LIBRARY_PATH", new File(runtime, "lib").getAbsolutePath());
            env.put("SHELL", bash.getAbsolutePath());
            env.put("TERM", "xterm-256color");
            env.put("COLORTERM", "truecolor");
            env.put("LANG", "C.UTF-8");
            env.put("LC_ALL", "C.UTF-8");
            env.put("OPENSSL_CONF", "/dev/null");
            env.put("PS1", "\\[\\e[38;5;141m\\]\\u@\\h\\[\\e[0m\\]:"
                    + "\\[\\e[38;5;111m\\]\\w\\[\\e[0m\\]\\$ ");
            env.put("HISTFILE", new File(home, ".bash_history").getAbsolutePath());
            env.remove("LD_PRELOAD");
            env.remove("NODE_OPTIONS");

            process = builder.start();
            input = new BufferedOutputStream(process.getOutputStream(), 8192);
            final Process current = process;
            readerThread = new Thread(() -> readLoop(current, output), "terminal-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (Throwable t) {
            Log.e("QwenTerminal", "start failed", t);
            output.onOutput("\u001b[31mНе удалось запустить терминал: "
                    + t.getMessage() + "\u001b[0m\r\n");
            output.onExit(-1);
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void readLoop(Process current, Output output) {
        char[] buffer = new char[4096];
        try (Reader reader = new InputStreamReader(
                new BufferedInputStream(current.getInputStream(), 8192),
                StandardCharsets.UTF_8)) {
            int read;
            while ((read = reader.read(buffer)) > 0) {
                if (stopping) {
                    break;
                }
                output.onOutput(new String(buffer, 0, read));
            }
        } catch (Throwable t) {
            if (!stopping) {
                Log.w("QwenTerminal", "read failed", t);
            }
        } finally {
            int code = -1;
            try {
                code = current.waitFor();
            } catch (Throwable ignored) {
            }
            if (current == process && !stopping) {
                output.onExit(code);
            }
        }
    }

    void write(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        synchronized (writeLock) {
            if (input == null) {
                return;
            }
            try {
                input.write(text.getBytes(StandardCharsets.UTF_8));
                input.flush();
            } catch (IOException ignored) {
            }
        }
    }

    /** Смена размера через stty. Ввод команды виден в оболочке, это осознанно. */
    void resize(int cols, int rows) {
        int safeCols = Math.max(20, Math.min(cols, 300));
        int safeRows = Math.max(5, Math.min(rows, 150));
        write("stty cols " + safeCols + " rows " + safeRows + " 2>/dev/null\r");
    }

    void stop() {
        stopping = true;
        Process current = process;
        process = null;
        synchronized (writeLock) {
            try {
                if (input != null) {
                    input.write(new byte[]{4}); // Ctrl+D
                    input.flush();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Throwable ignored) {
            }
            input = null;
        }
        if (current != null) {
            try {
                current.destroy();
                Thread.sleep(150);
            } catch (Throwable ignored) {
            }
            if (current.isAlive()) {
                current.destroyForcibly();
            }
        }
        readerThread = null;
    }
}
