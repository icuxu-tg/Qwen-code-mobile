package com.qwen.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements PluginManager.Listener {

    private static final String TAG = "QwenMobile";
    private static final int REQ_PERMISSIONS = 100;
    private static final int REQ_FILES = 101;
    private static final String LOCAL_URL = "http://127.0.0.1:" + QwenRuntime.PORT + "/";

    private WebView web;
    private PluginManager pluginManager;
    private String pluginBridgeScript;
    private LinearLayout splash;
    private LinearLayout buttons;
    private LinearLayout navBar;
    private ScrollView logScroll;
    private TextView statusView;
    private TextView logView;
    private ProgressBar progressBar;
    private Button retryButton;
    private Button browserButton;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private ValueCallback<Uri[]> fileCallback;
    private boolean loaded = false;
    private boolean destroyed = false;
    private boolean permissionsRequested = false;
    private boolean serverStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        QwenRuntime.logFile(this);
        pluginManager = PluginManager.get(this);
        pluginManager.addListener(this);
        buildUi();
        requestPermissionsThenStart();
        ui.post(poller);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0D0D0D);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(0xFF0D0D0D);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0D0D0D);
        web.setVisibility(View.INVISIBLE);
        configureWebView();
        shell.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setBackgroundColor(0xFF141414);
        navBar.setVisibility(View.GONE);
        addNavItem(navBar, "Qwen", null);
        addNavItem(navBar, "Терминал", v -> startActivity(new Intent(
                this, TerminalActivity.class)));
        addNavItem(navBar, "Браузер", v -> startActivity(new Intent(
                this, BrowserActivity.class)));
        addNavItem(navBar, "Плагины", v -> startActivity(new Intent(
                this, PluginsActivity.class)));
        shell.addView(navBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setPadding(dp(28), dp(28), dp(28), dp(28));
        splash.setBackgroundColor(0xFF0D0D0D);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(88), dp(88));
        logoParams.bottomMargin = dp(16);
        splash.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("Qwen Code");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTextColor(0xFFF5F5F5);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        splash.addView(title, wrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("мобильный coding-агент");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTextColor(0xFF9E9E9E);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = wrap();
        subtitleParams.topMargin = dp(4);
        splash.addView(subtitle, subtitleParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(ColorStateList.valueOf(0xFF7C6CF6));
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(dp(220), dp(6));
        progressParams.topMargin = dp(24);
        splash.addView(progressBar, progressParams);

        statusView = new TextView(this);
        statusView.setText("Подготовка среды…");
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setTextColor(0xFFC7C7C7);
        statusView.setGravity(Gravity.CENTER);
        statusView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statusParams = wrap();
        statusParams.topMargin = dp(14);
        splash.addView(statusView, statusParams);

        logScroll = new ScrollView(this);
        logScroll.setVisibility(View.GONE);
        logView = new TextView(this);
        logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        logView.setTextColor(0xFFBDBDBD);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        logView.setPadding(dp(12), dp(12), dp(12), dp(12));
        logView.setBackgroundColor(0xFF1A1A1A);
        logScroll.addView(logView, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams logParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190));
        logParams.topMargin = dp(18);
        splash.addView(logScroll, logParams);

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setVisibility(View.GONE);

        retryButton = new Button(this);
        retryButton.setText("Повторить");
        retryButton.setAllCaps(false);
        retryButton.setTextColor(Color.WHITE);
        retryButton.setBackgroundTintList(ColorStateList.valueOf(0xFF7C6CF6));
        retryButton.setOnClickListener(v -> retry());

        browserButton = new Button(this);
        browserButton.setText("В браузере");
        browserButton.setAllCaps(false);
        browserButton.setTextColor(0xFFE0E0E0);
        browserButton.setBackgroundTintList(ColorStateList.valueOf(0xFF2A2A2A));
        browserButton.setOnClickListener(v -> openInBrowser());

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        buttonParams.setMargins(dp(4), 0, dp(4), 0);
        buttons.addView(retryButton, buttonParams);
        buttons.addView(browserButton, buttonParams);

        LinearLayout.LayoutParams buttonsParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsParams.topMargin = dp(16);
        splash.addView(buttons, buttonsParams);

        root.addView(splash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void configureWebView() {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        // true + отсутствие onCreateWindow заставляет WebView отдавать
        // window.open() внешнему браузеру. Web Shell работает в одном окне.
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(false);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);

        // on_send-фильтры Margelet: JS-мост к Python-хосту плагинов.
        loadPluginBridgeScript();
        web.addJavascriptInterface(new PluginBridge(), "AndroidPlugins");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                // Qwen Web Shell запоминает globalThis.fetch в момент
                // инициализации. Вставляем мост прямо в HTML до его скриптов,
                // иначе on_send мог бы остаться незамеченным.
                try {
                    Uri uri = request.getUrl();
                    if (!request.isForMainFrame()
                            || !"GET".equalsIgnoreCase(request.getMethod())
                            || !"http".equalsIgnoreCase(uri.getScheme())
                            || !("127.0.0.1".equals(uri.getHost())
                            || "localhost".equals(uri.getHost()))
                            || uri.getPort() != QwenRuntime.PORT
                            || pluginBridgeScript == null) {
                        return null;
                    }
                    HttpURLConnection connection =
                            (HttpURLConnection) new URL(uri.toString()).openConnection();
                    try {
                        connection.setConnectTimeout(3000);
                        connection.setReadTimeout(5000);
                        connection.setInstanceFollowRedirects(true);
                        connection.setUseCaches(false);
                        connection.setRequestProperty("Accept",
                                "text/html,application/xhtml+xml");
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        int status = connection.getResponseCode();
                        String contentType = connection.getContentType();
                        if (status != 200 || contentType == null
                                || !contentType.toLowerCase(Locale.ROOT)
                                .contains("text/html")) {
                            return null;
                        }
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (InputStream in = connection.getInputStream()) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                            }
                        }
                        String html = new String(out.toByteArray(), StandardCharsets.UTF_8);
                        String injected = injectBridgeIntoHtml(html);
                        Map<String, String> headers = new HashMap<>();
                        for (Map.Entry<String, List<String>> entry
                                : connection.getHeaderFields().entrySet()) {
                            if (entry.getKey() == null || entry.getValue().isEmpty()) {
                                continue;
                            }
                            String name = entry.getKey();
                            if ("Content-Length".equalsIgnoreCase(name)
                                    || "Content-Encoding".equalsIgnoreCase(name)
                                    || "Transfer-Encoding".equalsIgnoreCase(name)) {
                                continue;
                            }
                            headers.put(name, entry.getValue().get(0));
                        }
                        headers.put("Content-Type", contentType);
                        String mime = contentType.split(";", 2)[0].trim();
                        return new WebResourceResponse(mime, "UTF-8", 200, "OK",
                                headers, new ByteArrayInputStream(
                                injected.getBytes(StandardCharsets.UTF_8)));
                    } finally {
                        connection.disconnect();
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "bridge HTML interception failed", t);
                    return null;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Резервный путь: если главный документ не удалось
                // перехватить, ставим обёртку сразу после старта навигации.
                injectPluginBridge();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (loaded && QwenRuntime.error == null) {
                    ui.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        splash.setVisibility(View.GONE);
                        web.setVisibility(View.VISIBLE);
                        if (navBar != null) {
                            navBar.setVisibility(View.VISIBLE);
                        }
                        injectPluginBridge();
                    });
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    QwenRuntime.error = "WebView: " + error.getDescription()
                            + " (код " + error.getErrorCode() + ")";
                    showError();
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Выбрать файл"), REQ_FILES);
                    return true;
                } catch (Throwable t) {
                    Log.w(TAG, "file chooser failed", t);
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Голос/камера в этой сборке не поддерживаются.
                request.deny();
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                Log.d(TAG + "/web", message.message() + " @" + message.lineNumber());
                return true;
            }
        });
    }

    private void loadPluginBridgeScript() {
        if (pluginBridgeScript != null) {
            return;
        }
        try (InputStream in = getAssets().open("plugin_bridge.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            pluginBridgeScript = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            Log.w(TAG, "plugin bridge asset failed", t);
        }
    }

    private void injectPluginBridge() {
        loadPluginBridgeScript();
        if (web == null || destroyed || pluginBridgeScript == null) {
            return;
        }
        web.evaluateJavascript(pluginBridgeScript, null);
    }

    private String injectBridgeIntoHtml(String html) {
        loadPluginBridgeScript();
        if (html == null || pluginBridgeScript == null) {
            return html;
        }
        String script = "<script>"
                + pluginBridgeScript.replace("</script", "<\\/script")
                + "</script>";
        String lower = html.toLowerCase(Locale.ROOT);
        int head = lower.indexOf("<head");
        int insert = head >= 0 ? lower.indexOf('>', head) : -1;
        if (insert >= 0) {
            return html.substring(0, insert + 1) + script + html.substring(insert + 1);
        }
        return script + html;
    }

    /** Вызывается из Web Shell при каждой отправке сообщения. */
    private final class PluginBridge {
        @JavascriptInterface
        public String filterSend(String text) {
            try {
                return pluginManager.filterSend(text, "qwen");
            } catch (Throwable t) {
                Log.w(TAG, "plugin filter failed", t);
                return text;
            }
        }
    }

    @Override
    public void onPluginSend(String pluginId, String chat, String text) {
        runOnUiThread(() -> {
            if (web == null || destroyed) {
                return;
            }
            String js = "window.qwenMobileSend ? window.qwenMobileSend("
                    + JSONObject.quote(chat == null ? "qwen" : chat) + ","
                    + JSONObject.quote(text == null ? "" : text) + ") : false";
            web.evaluateJavascript(js, result -> {
                boolean sent = "true".equals(result);
                Toast.makeText(this, sent
                                ? "Плагин отправил сообщение в Qwen"
                                : "Плагин: откройте сессию Qwen для отправки",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        Log.d(TAG, "url " + uri + " host=" + uri.getHost() + " port=" + uri.getPort()
                + " scheme=" + scheme);
        if (scheme.equals("about") || scheme.equals("data") || scheme.equals("blob")
                || scheme.equals("javascript")) {
            return false;
        }
        if ((scheme.equals("http") || scheme.equals("https"))
                && ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()))
                && uri.getPort() == QwenRuntime.PORT) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Throwable t) {
            Log.w(TAG, "no activity for " + uri, t);
        }
        return true;
    }

    private void requestPermissionsThenStart() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            missing.add("android.permission.POST_NOTIFICATIONS");
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!missing.isEmpty() && !permissionsRequested) {
            permissionsRequested = true;
            QwenRuntime.status = "Ожидание разрешений…";
            try {
                requestPermissions(missing.toArray(new String[0]), REQ_PERMISSIONS);
                ui.postDelayed(this::startServer, 8000);
                return;
            } catch (Throwable t) {
                Log.w(TAG, "permission request failed", t);
            }
        }
        startServer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            startServer();
        }
    }

    private void startServer() {
        if (serverStarted || destroyed) {
            return;
        }
        serverStarted = true;
        QwenRuntime.status = "Запуск сервера…";
        try {
            Intent service = new Intent(this, QwenService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        } catch (Throwable t) {
            QwenRuntime.error = "Не удалось запустить сервис: " + t;
            QwenRuntime.status = "Ошибка";
            showError();
        }
    }

    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            if (destroyed) {
                return;
            }
            statusView.setText(QwenRuntime.status);
            if (QwenRuntime.progress >= 0) {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(QwenRuntime.progress);
            } else {
                progressBar.setIndeterminate(true);
            }

            if (QwenRuntime.running) {
                loadWeb();
                return;
            }
            if (QwenRuntime.error != null) {
                showError();
                return;
            }
            ui.postDelayed(this, 500);
        }
    };

    private void loadWeb() {
        if (loaded || destroyed) {
            return;
        }
        loaded = true;
        QwenRuntime.status = "Загрузка интерфейса…";
        web.setVisibility(View.VISIBLE);
        web.loadUrl(LOCAL_URL);
    }

    private void showError() {
        if (destroyed) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        statusView.setText(QwenRuntime.error == null ? "Неизвестная ошибка" : QwenRuntime.error);
        statusView.setTextColor(0xFFFF8A80);
        logView.setText(QwenRuntime.tailLog(this, 16000));
        logScroll.setVisibility(View.VISIBLE);
        buttons.setVisibility(View.VISIBLE);
        splash.setVisibility(View.VISIBLE);
        web.setVisibility(View.GONE);
        if (navBar != null) {
            navBar.setVisibility(View.GONE);
        }
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void retry() {
        QwenRuntime.resetStatus();
        loaded = false;
        serverStarted = false;
        statusView.setTextColor(0xFFC7C7C7);
        statusView.setText("Перезапуск…");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        logScroll.setVisibility(View.GONE);
        buttons.setVisibility(View.GONE);
        splash.setVisibility(View.VISIBLE);
        web.setVisibility(View.GONE);
        if (navBar != null) {
            navBar.setVisibility(View.GONE);
        }
        ui.removeCallbacks(poller);
        try {
            stopService(new Intent(this, QwenService.class));
        } catch (Throwable ignored) {
        }
        ui.postDelayed(() -> {
            startServer();
            ui.post(poller);
        }, 700);
    }

    private void openInBrowser() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LOCAL_URL)));
        } catch (Throwable t) {
            QwenRuntime.error = "Нет браузера: " + t;
            showError();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILES) {
            if (fileCallback == null) {
                return;
            }
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    result = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        result[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (pluginManager != null) {
            pluginManager.removeListener(this);
        }
        ui.removeCallbacksAndMessages(null);
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private void addNavItem(LinearLayout bar, String label, View.OnClickListener listener) {
        TextView item = new TextView(this);
        item.setText(label);
        item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        item.setTextColor(listener == null ? 0xFFB9AFFF : 0xFFBDBDBD);
        item.setGravity(Gravity.CENTER);
        item.setTypeface(listener == null ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        if (listener != null) {
            item.setOnClickListener(listener);
        }
        bar.addView(item, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
