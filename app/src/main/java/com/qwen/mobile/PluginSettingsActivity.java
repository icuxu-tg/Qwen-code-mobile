package com.qwen.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Экран настроек, который плагин описал через margelet.settings(). */
public class PluginSettingsActivity extends Activity {

    private PluginManager manager;
    private PluginInfo info;
    private LinearLayout content;
    private final Map<String, EditText> edits = new LinkedHashMap<>();
    private final Map<String, Spinner> spinners = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = PluginManager.get(this);
        info = manager.getPlugin(getIntent().getStringExtra("id"));
        if (info == null) {
            finish();
            return;
        }
        buildUi();
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

        TextView title = text("Настройки: " + info.name, 18, 0xFFF5F5F5, true);
        top.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView save = text("Сохранить", 13, 0xFFB9AFFF, false);
        save.setPadding(dp(8), dp(8), dp(8), dp(8));
        save.setOnClickListener(v -> saveAll());
        top.addView(save, wrap());

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        JSONArray rows = info.settingsRows;
        if (rows == null || rows.length() == 0) {
            content.addView(text("У этого плагина нет экрана настроек.", 14,
                    0xFF9E9E9E, false));
        } else {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    addRow(row);
                }
            }
        }

        TextView note = text("Значения хранятся в памяти плагина и переживают "
                + "перезапуск. Java/Telegram-настройки и произвольные Android View "
                + "в этой сборке недоступны.", 11, 0xFF8A8A8A, false);
        note.setPadding(0, dp(18), 0, 0);
        content.addView(note);

        setContentView(root);
    }

    private void addRow(JSONObject row) {
        String kind = row.optString("kind", "");
        String key = row.optString("key", "");
        String title = row.optString("title", "");
        String about = row.optString("about", "");

        if ("header".equals(kind)) {
            TextView header = text(title, 15, 0xFFF5F5F5, true);
            header.setPadding(0, dp(14), 0, dp(6));
            content.addView(header);
            return;
        }
        if ("note".equals(kind)) {
            TextView note = text(title, 12, 0xFF9E9E9E, false);
            note.setPadding(0, dp(6), 0, dp(8));
            content.addView(note);
            return;
        }
        if ("action".equals(kind)) {
            Button action = new Button(this);
            action.setText(title);
            action.setAllCaps(false);
            action.setTextColor(row.optBoolean("danger", false)
                    ? 0xFFFF8A80 : 0xFFE0E0E0);
            action.setOnClickListener(v -> {
                manager.settingsChanged(info.id, key, "1");
                Toast.makeText(this, "Выполнено", Toast.LENGTH_SHORT).show();
            });
            content.addView(action);
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(7), 0, dp(7));

        if (!title.isEmpty()) {
            box.addView(text(title, 14, 0xFFE8E8E8, false));
        }
        if (!about.isEmpty()) {
            TextView hint = text(about, 11, 0xFF8A8A8A, false);
            hint.setPadding(0, dp(2), 0, dp(4));
            box.addView(hint);
        }

        String stored = info.values.optString(key, row.optString("default", ""));
        if ("switch".equals(kind)) {
            Switch toggle = new Switch(this);
            toggle.setChecked("1".equals(stored));
            toggle.setOnCheckedChangeListener((buttonView, checked) ->
                    manager.settingsChanged(info.id, key, checked ? "1" : "0"));
            box.addView(toggle);
        } else if ("text".equals(kind)) {
            EditText edit = new EditText(this);
            edit.setText(stored);
            edit.setSingleLine(false);
            edit.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            edit.setTextColor(0xFFF5F5F5);
            edit.setHintTextColor(0xFF777777);
            box.addView(edit);
            edits.put(key, edit);
        } else if ("choice".equals(kind)) {
            Spinner spinner = new Spinner(this);
            List<String> options = new ArrayList<>();
            JSONArray array = row.optJSONArray("options");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    options.add(array.optString(i, ""));
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(adapter);
            int selected = options.indexOf(stored);
            if (selected >= 0) {
                spinner.setSelection(selected);
            }
            box.addView(spinner);
            spinners.put(key, spinner);
        }
        content.addView(box);
    }

    private void saveAll() {
        for (Map.Entry<String, EditText> entry : edits.entrySet()) {
            manager.settingsChanged(info.id, entry.getKey(),
                    entry.getValue().getText().toString());
        }
        for (Map.Entry<String, Spinner> entry : spinners.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            manager.settingsChanged(info.id, entry.getKey(),
                    selected == null ? "" : String.valueOf(selected));
        }
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
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
}
