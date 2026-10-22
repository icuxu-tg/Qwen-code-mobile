package com.qwen.mobile;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** Встроенный браузер: обычный WebView с адресной строкой и историей. */
public class BrowserActivity extends Activity {

    private static final String TAG = "QwenBrowser";
    private static final String HOME = "https://ya.ru";
    private static final int REQ_FILES = 801;

    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
        web.loadUrl(HOME);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0D);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(4), dp(6), dp(6), dp(6));
        top.setBackgroundColor(0xFF141414);

        TextView back = text("‹", 30, 0xFFE0E0E0);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(6), 0, dp(6), 0);
        back.setOnClickListener(v -> {
            if (web.canGoBack()) {
                web.goBack();
            } else {
                finish();
            }
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(42)));

        TextView forward = text("›", 30, 0xFFE0E0E0);
        forward.setGravity(Gravity.CENTER);
        forward.setPadding(dp(4), 0, dp(4), 0);
        forward.setOnClickListener(v -> {
            if (web.canGoForward()) {
                web.goForward();
            }
        });
        top.addView(forward, new LinearLayout.LayoutParams(dp(34), dp(42)));

        TextView reload = text("⟳", 22, 0xFFE0E0E0);
        reload.setGravity(Gravity.CENTER);
        reload.setPadding(dp(4), 0, dp(4), 0);
        reload.setOnClickListener(v -> web.reload());
        top.addView(reload, new LinearLayout.LayoutParams(dp(34), dp(42)));

        address = new EditText(this);
        address.setSingleLine(true);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setTextColor(0xFFF0F0F0);
        address.setHintTextColor(0xFF777777);
        address.setHint("адрес или поиск");
        address.setTextSize(13);
        address.setBackgroundColor(0xFF242424);
        address.setPadding(dp(10), dp(4), dp(10), dp(4));
        address.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                go();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        addressParams.setMargins(dp(4), 0, dp(4), 0);
        top.addView(address, addressParams);

        TextView go = text("→", 22, 0xFFB9AFFF);
        go.setGravity(Gravity.CENTER);
        go.setPadding(dp(6), 0, dp(6), 0);
        go.setOnClickListener(v -> go());
        top.addView(go, new LinearLayout.LayoutParams(dp(38), dp(42)));

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        web = new WebView(this);
        web.setBackgroundColor(0xFF0D0D0D);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(true);
        }
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

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
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                address.setText(url);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
                progress.setVisibility(View.GONE);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                address.setText(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(BrowserActivity.this,
                            "Ошибка загрузки: " + error.getDescription(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

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
                    startActivityForResult(Intent.createChooser(intent, "Выбрать файл"),
                            REQ_FILES);
                    return true;
                } catch (Throwable t) {
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.deny();
            }
        });

        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> {
            try {
                if (!URLUtil.isHttpUrl(url) && !URLUtil.isHttpsUrl(url)) {
                    Toast.makeText(this, "Этот тип ссылки нельзя скачать",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Загрузка из Qwen Browser");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(this, "Загрузка начата", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                Toast.makeText(this, "Не удалось скачать: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void go() {
        String raw = address.getText().toString().trim();
        if (raw.isEmpty()) {
            return;
        }
        String url;
        if (raw.contains("://")) {
            url = raw;
        } else if (raw.contains(" ") || !raw.contains(".")) {
            url = "https://duckduckgo.com/?q=" + Uri.encode(raw);
        } else {
            url = "https://" + raw;
        }
        address.setText(url);
        web.loadUrl(url);
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(address.getWindowToken(), 0);
            }
        } catch (Throwable ignored) {
        }
        address.clearFocus();
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        if (scheme.equals("http") || scheme.equals("https")
                || scheme.equals("about") || scheme.equals("data")
                || scheme.equals("blob") || scheme.equals("javascript")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Throwable t) {
            Toast.makeText(this, "Нет приложения для " + uri, Toast.LENGTH_SHORT).show();
        }
        return true;
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
    protected void onPause() {
        if (web != null) {
            web.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
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
