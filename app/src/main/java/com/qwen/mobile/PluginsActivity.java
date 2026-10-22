package com.qwen.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/** Список .marp-плагинов, установка и управление. */
public class PluginsActivity extends Activity implements PluginManager.Listener {

    private static final int REQ_PICK_MARP = 701;

    private PluginManager manager;
    private LinearLayout content;
    private TextView status;
    private boolean refreshing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = PluginManager.get(this);
        manager.addListener(this);
        buildUi();
        if (!manager.isEngineReady() && !manager.isStarting()) {
            manager.startAsync();
        }
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

        TextView back = text("‹", 30, 0xFFE0E0E0, false);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(10), 0, dp(10), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text("Плагины", 20, 0xFFF5F5F5, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(title, titleParams);

        TextView console = text("Консоль", 13, 0xFFB9AFFF, false);
        console.setPadding(dp(8), dp(8), dp(8), dp(8));
        console.setOnClickListener(v -> startActivity(new Intent(this,
                PluginConsoleActivity.class)));
        top.addView(console, wrap());

        TextView install = text("Установить", 13, 0xFFB9AFFF, false);
        install.setPadding(dp(8), dp(8), dp(8), dp(8));
        install.setOnClickListener(v -> pickMarp());
        top.addView(install, wrap());

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void pickMarp() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQ_PICK_MARP);
        } catch (Throwable t) {
            toast("Нет файлового менеджера: " + t.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_MARP || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        new Thread(() -> {
            try {
                PluginManager.PendingInstall pending = manager.inspect(uri);
                runOnUiThread(() -> confirmInstall(pending));
            } catch (Throwable t) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Не удалось прочитать .marp")
                        .setMessage(String.valueOf(t.getMessage()))
                        .setPositiveButton("OK", null)
                        .show());
            }
        }, "marp-inspect").start();
    }

    private void confirmInstall(PluginManager.PendingInstall pending) {
        JSONObject m = pending.manifest;
        String name = m.optString("name", pending.id);
        String version = m.optString("version", "?");
        String author = m.optString("author", "?");
        StringBuilder perms = new StringBuilder();
        JSONArray arr = m.optJSONArray("permissions");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                if (perms.length() > 0) {
                    perms.append(", ");
                }
                perms.append(arr.optString(i, ""));
            }
        }
        if (perms.length() == 0) {
            perms.append("не заявлены");
        }
        String message = name + " v" + version + "\nавтор: " + author
                + "\n\nЗаявленные права: " + perms
                + "\n\nПлагин — это Python-код без песочницы. Он сможет читать "
                + "файлы приложения и ходить в сеть. Устанавливайте только код, "
                + "который вы прочитали или которому доверяете.";
        new AlertDialog.Builder(this)
                .setTitle("Установить плагин?")
                .setMessage(message)
                .setPositiveButton("Установить", (dialog, which) -> install(pending))
                .setNegativeButton("Отмена", (dialog, which) -> pending.cleanup())
                .setOnCancelListener(dialog -> pending.cleanup())
                .show();
    }

    private void install(PluginManager.PendingInstall pending) {
        new Thread(() -> {
            try {
                manager.install(pending);
                runOnUiThread(() -> {
                    toast("Плагин установлен. Включите его переключателем.");
                    refresh();
                });
            } catch (Throwable t) {
                pending.cleanup();
                runOnUiThread(() -> toast("Ошибка установки: " + t.getMessage()));
            }
        }, "marp-install").start();
    }

    private void refresh() {
        if (refreshing || isFinishing()) {
            return;
        }
        refreshing = true;
        content.removeAllViews();

        LinearLayout warning = card();
        TextView warnTitle = text("Без песочницы", 14, 0xFFFFCC80, true);
        warning.addView(warnTitle);
        warning.addView(text("Плагины выполняются как Python-код с правами "
                + "приложения. Читайте main.py перед установкой.", 12,
                0xFFC7C7C7, false));
        content.addView(warning, cardParams());

        status = text(engineStatusText(), 13,
                manager.getEngineError() == null ? 0xFF9E9E9E : 0xFFFF8A80, false);
        status.setPadding(dp(4), dp(4), dp(4), dp(8));
        content.addView(status);

        if (manager.getEngineError() != null) {
            Button restart = new Button(this);
            restart.setText("Перезапустить Python-хост");
            restart.setAllCaps(false);
            restart.setOnClickListener(v -> {
                manager.restart();
                refresh();
            });
            content.addView(restart, wrap());
        }

        List<PluginInfo> list = manager.getPlugins();
        if (list.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("Плагинов пока нет", 16, 0xFFF5F5F5, true));
            empty.addView(text("Нажмите «Установить» и выберите .marp-файл. "
                    + "Совместимость: формат Margelet .marp и базовый API margelet.",
                    13, 0xFF9E9E9E, false));
            content.addView(empty, cardParams());
        } else {
            for (PluginInfo info : list) {
                content.addView(pluginCard(info), cardParams());
            }
        }
        refreshing = false;
    }

    private String engineStatusText() {
        if (manager.isEngineReady()) {
            return "Python-хост работает · совместимость Margelet "
                    + PluginManager.HOST_VERSION;
        }
        if (manager.isStarting()) {
            return "Запуск Python-хоста…";
        }
        if (manager.getEngineError() != null) {
            return "Python-хост не работает: " + manager.getEngineError();
        }
        return "Python-хост остановлен";
    }

    private View pluginCard(PluginInfo info) {
        LinearLayout card = card();

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        File iconFile = info.icon;
        Bitmap bitmap = decodeIcon(iconFile);
        if (bitmap != null) {
            icon.setImageBitmap(bitmap);
        } else {
            icon.setImageResource(R.mipmap.ic_launcher);
        }
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.rightMargin = dp(10);
        head.addView(icon, iconParams);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.addView(text(info.name, 16, 0xFFF5F5F5, true));
        String sub = info.subtitle();
        if (!sub.isEmpty()) {
            textBox.addView(text(sub, 12, 0xFF9E9E9E, false));
        }
        head.addView(textBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(info.enabled);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                manager.setEnabled(info.id, isChecked));
        head.addView(toggle, wrap());
        card.addView(head);

        if (!info.description.isEmpty()) {
            TextView desc = text(info.description, 13, 0xFFC7C7C7, false);
            desc.setPadding(0, dp(8), 0, 0);
            card.addView(desc);
        }
        TextView perms = text("Права: " + info.permissionsText(), 11,
                0xFF8A8A8A, false);
        perms.setPadding(0, dp(6), 0, 0);
        card.addView(perms);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        if (info.settingsRows != null && info.settingsRows.length() > 0) {
            Button settings = new Button(this);
            settings.setText("Настройки");
            settings.setAllCaps(false);
            settings.setOnClickListener(v -> {
                Intent intent = new Intent(this, PluginSettingsActivity.class);
                intent.putExtra("id", info.id);
                startActivity(intent);
            });
            actions.addView(settings, wrap());
        }

        JSONArray buttons = info.buttons;
        if (buttons != null) {
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject b = buttons.optJSONObject(i);
                if (b == null) {
                    continue;
                }
                String key = b.optString("key", "");
                String title = b.optString("title", key);
                Button action = new Button(this);
                action.setText(title);
                action.setAllCaps(false);
                action.setOnClickListener(v -> manager.buttonClicked(info.id, key));
                actions.addView(action, wrap());
            }
        }

        Button delete = new Button(this);
        delete.setText("Удалить");
        delete.setAllCaps(false);
        delete.setTextColor(0xFFFF8A80);
        delete.setOnClickListener(v -> confirmDelete(info));
        actions.addView(delete, wrap());

        card.addView(actions);
        return card;
    }

    private Bitmap decodeIcon(File file) {
        if (file == null || !file.isFile() || file.length() > 8L * 1024 * 1024) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) {
                sample *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void confirmDelete(PluginInfo info) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить " + info.name + "?")
                .setMessage("Код плагина и его сохранённые настройки будут удалены. "
                        + "Уже запущенный Python-код остановится только после "
                        + "перезапуска приложения.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    manager.delete(info.id);
                    refresh();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1A1A1A);
        bg.setCornerRadius(dp(12));
        layout.setBackground(bg);
        return layout;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        return params;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        view.setLineSpacing(dp(1), 1f);
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPluginsChanged() {
        runOnUiThread(this::refresh);
    }

    @Override
    public void onEngineState() {
        runOnUiThread(this::refresh);
    }

    @Override
    public void onPluginToast(String text) {
        runOnUiThread(() -> toast(text));
    }

    @Override
    protected void onDestroy() {
        manager.removeListener(this);
        super.onDestroy();
    }
}
