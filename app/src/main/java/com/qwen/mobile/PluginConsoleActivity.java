package com.qwen.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Консоль плагинов: то, что не видно без отдельного экрана. */
public class PluginConsoleActivity extends Activity implements PluginManager.Listener {

    private PluginManager manager;
    private TextView console;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = PluginManager.get(this);
        manager.addListener(this);
        buildUi();
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0D);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(8), dp(8), dp(8));
        top.setBackgroundColor(0xFF141414);

        TextView back = text("‹", 30, 0xFFE0E0E0);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(10), 0, dp(10), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text("Консоль плагинов", 18, 0xFFF5F5F5);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = text("Очистить", 13, 0xFFB9AFFF);
        clear.setPadding(dp(8), dp(8), dp(8), dp(8));
        clear.setOnClickListener(v -> {
            manager.clearLogs();
            refresh();
        });
        top.addView(clear, wrap());

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF121212);
        console = new TextView(this);
        console.setTextSize(11);
        console.setTextColor(0xFFC7C7C7);
        console.setTypeface(Typeface.MONOSPACE);
        console.setTextIsSelectable(true);
        console.setPadding(dp(10), dp(10), dp(10), dp(10));
        console.setLineSpacing(dp(1), 1f);
        scroll.addView(console, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void refresh() {
        if (console == null) {
            return;
        }
        List<PluginManager.LogEntry> logs = manager.getLogs();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (PluginManager.LogEntry entry : logs) {
            append(builder, entry);
        }
        console.setText(builder);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void append(SpannableStringBuilder builder, PluginManager.LogEntry entry) {
        int start = builder.length();
        builder.append(entry.timeText()).append("  ");
        int nameStart = builder.length();
        builder.append(entry.plugin.isEmpty() ? "plugin" : entry.plugin);
        builder.setSpan(new ForegroundColorSpan(0xFF8AB4F8), nameStart,
                builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("  ").append(entry.text).append('\n');
        if (entry.error) {
            builder.setSpan(new ForegroundColorSpan(0xFFFF8A80), start,
                    builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onPluginLog(PluginManager.LogEntry entry) {
        runOnUiThread(() -> {
            if (console == null) {
                return;
            }
            SpannableStringBuilder builder = new SpannableStringBuilder(console.getText());
            append(builder, entry);
            console.setText(builder);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onPluginsChanged() {
        runOnUiThread(this::refresh);
    }

    @Override
    protected void onDestroy() {
        manager.removeListener(this);
        super.onDestroy();
    }
}
