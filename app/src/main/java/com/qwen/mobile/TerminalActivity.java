package com.qwen.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** Полноценный терминал на xterm.js + PTY (util-linux script + bash). */
public class TerminalActivity extends Activity {

    private static final String TAG = "QwenTerminal";
    private WebView web;
    private final TerminalSession session = new TerminalSession();
    private final StringBuilder pending = new StringBuilder();
    private boolean pageReady;
    private int lastCols = 80;
    private int lastRows = 24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0D);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(6), dp(6), dp(6), dp(6));
        top.setBackgroundColor(0xFF141414);

        TextView back = text("‹", 30, 0xFFE0E0E0);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(8), 0, dp(8), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = text("Терминал", 18, 0xFFF5F5F5);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        addKey(top, "Ctrl+C", "\u0003");
        addKey(top, "Tab", "\t");
        addKey(top, "Esc", "\u001b");

        TextView reset = text("Сброс", 12, 0xFFB9AFFF);
        reset.setPadding(dp(7), dp(7), dp(7), dp(7));
        reset.setOnClickListener(v -> restartShell());
        top.addView(reset);

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        web = new WebView(this);
        web.setBackgroundColor(0xFF0D0D0D);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                flushPending();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                Log.d(TAG + "/web", message.message() + " @" + message.lineNumber());
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidTerminal");
        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        web.loadUrl("file:///android_asset/terminal/terminal.html");
    }

    private void addKey(LinearLayout top, String label, String data) {
        TextView key = text(label, 12, 0xFFE0E0E0);
        key.setPadding(dp(7), dp(7), dp(7), dp(7));
        key.setOnClickListener(v -> session.write(data));
        top.addView(key);
    }

    private void restartShell() {
        session.stop();
        if (web != null) {
            web.evaluateJavascript("window.termReset && window.termReset();", null);
        }
        session.start(this, lastCols, lastRows, output);
    }

    private void flushPending() {
        if (web == null || !pageReady) {
            return;
        }
        String text;
        synchronized (pending) {
            if (pending.length() == 0) {
                return;
            }
            text = pending.toString();
            pending.setLength(0);
        }
        writeToTerminal(text);
    }

    private void writeToTerminal(String text) {
        if (web == null || !pageReady) {
            synchronized (pending) {
                pending.append(text);
                if (pending.length() > 200_000) {
                    pending.delete(0, pending.length() - 200_000);
                }
            }
            return;
        }
        try {
            web.evaluateJavascript("window.termWrite && window.termWrite("
                    + JSONObject.quote(text) + ");", null);
        } catch (Throwable t) {
            Log.w(TAG, "write failed", t);
        }
    }

    private final TerminalSession.Output output = new TerminalSession.Output() {
        @Override
        public void onOutput(String text) {
            runOnUiThread(() -> writeToTerminal(text));
        }

        @Override
        public void onExit(int code) {
            runOnUiThread(() -> writeToTerminal(
                    "\r\n\u001b[38;5;244m[процесс завершён, код " + code
                            + "]\u001b[0m\r\n"));
        }
    };

    private class Bridge {
        @JavascriptInterface
        public void start(int cols, int rows) {
            lastCols = Math.max(20, cols);
            lastRows = Math.max(5, rows);
            runOnUiThread(() -> session.start(TerminalActivity.this,
                    lastCols, lastRows, output));
        }

        @JavascriptInterface
        public void input(String data) {
            session.write(data);
        }

        @JavascriptInterface
        public void resize(int cols, int rows) {
            lastCols = Math.max(20, cols);
            lastRows = Math.max(5, rows);
            // Не дёргаем stty на каждый пиксель: команда эхом попадёт в вывод.
            // Новый размер применится при следующем «Сбросе».
        }
    }

    @Override
    protected void onDestroy() {
        session.stop();
        if (web != null) {
            web.removeJavascriptInterface("AndroidTerminal");
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
