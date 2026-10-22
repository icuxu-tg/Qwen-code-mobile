package com.qwen.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Метаданные установленного .marp-плагина. */
final class PluginInfo {

    String id = "";
    String name = "";
    String version = "";
    String author = "";
    String description = "";
    final List<String> permissions = new ArrayList<>();
    boolean enabled = false;
    File folder;
    File icon;
    JSONArray settingsRows = new JSONArray();
    JSONObject values = new JSONObject();
    JSONArray buttons = new JSONArray();

    static PluginInfo fromManifest(JSONObject manifest, File folder) {
        PluginInfo info = new PluginInfo();
        info.id = manifest.optString("id", "");
        info.name = manifest.optString("name", info.id);
        info.version = manifest.optString("version", "");
        info.author = manifest.optString("author", "");
        info.description = manifest.optString("description", "");
        info.folder = folder;
        info.icon = new File(folder, "icon.png");
        JSONArray perms = manifest.optJSONArray("permissions");
        if (perms != null) {
            for (int i = 0; i < perms.length(); i++) {
                String p = perms.optString(i, "");
                if (!p.isEmpty()) {
                    info.permissions.add(p);
                }
            }
        }
        return info;
    }

    static PluginInfo fromJson(JSONObject json, File pluginsDir) {
        PluginInfo info = new PluginInfo();
        info.id = json.optString("id", "");
        info.name = json.optString("name", info.id);
        info.version = json.optString("version", "");
        info.author = json.optString("author", "");
        info.description = json.optString("description", "");
        info.enabled = json.optBoolean("enabled", false);
        info.folder = new File(pluginsDir, info.id);
        info.icon = new File(info.folder, "icon.png");
        JSONArray perms = json.optJSONArray("permissions");
        if (perms != null) {
            for (int i = 0; i < perms.length(); i++) {
                String p = perms.optString(i, "");
                if (!p.isEmpty()) {
                    info.permissions.add(p);
                }
            }
        }
        return info;
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("version", version);
            json.put("author", author);
            json.put("description", description);
            json.put("enabled", enabled);
            json.put("permissions", new JSONArray(permissions));
        } catch (Exception ignored) {
        }
        return json;
    }

    String permissionsText() {
        if (permissions.isEmpty()) {
            return "не заявлены";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : permissions) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p);
        }
        return sb.toString();
    }

    String subtitle() {
        StringBuilder sb = new StringBuilder();
        if (!version.isEmpty()) {
            sb.append("v").append(version);
        }
        if (!author.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(author);
        }
        return sb.toString();
    }
}
