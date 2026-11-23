package com.example.pocketguitar.ai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AI 谱曲模型配置存储。API Key 只写入应用私有偏好文件，绝不回显到界面或日志。
 */
public final class AiPrefs {
    private static final String FILE = "ai_compose_prefs";
    private static final String K_BASE_URL = "model_base_url";
    private static final String K_API_KEY = "model_api_key";
    private static final String K_MODEL = "model_name";

    private final SharedPreferences preferences;

    public AiPrefs(Context context) {
        preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public AiModelConfig readConfig() {
        String baseUrl = preferences.getString(K_BASE_URL, "");
        String apiKey = preferences.getString(K_API_KEY, "");
        String model = preferences.getString(K_MODEL, "");
        return new AiModelConfig(baseUrl, apiKey, model, "");
    }

    public boolean hasApiKey() {
        return !preferences.getString(K_API_KEY, "").trim().isEmpty();
    }

    /** 保存模型设置；空 Key 保留已存的密钥，便于只改地址/模型而不回显密钥。 */
    public void saveModel(String baseUrl, String apiKey, String model) {
        SharedPreferences.Editor edit = preferences.edit()
                .putString(K_BASE_URL, baseUrl == null ? "" : baseUrl.trim())
                .putString(K_MODEL, model == null ? "" : model.trim());
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            edit.putString(K_API_KEY, apiKey.trim());
        }
        edit.apply();
    }

    public void clearApiKey() {
        preferences.edit().remove(K_API_KEY).apply();
    }
}
