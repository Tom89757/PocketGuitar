package com.example.pocketguitar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketguitar.ai.AiModelConfig;
import com.example.pocketguitar.ai.AiPrefs;

/** AI 谱曲模型配置页：接口地址 / 模型名 / API Key。 */
public class AiSettingsActivity extends AppCompatActivity {

    private EditText etBaseUrl;
    private EditText etModel;
    private EditText etApiKey;
    private TextView tvStatus;
    private AiPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        prefs = new AiPrefs(this);
        etBaseUrl = findViewById(R.id.et_base_url);
        etModel = findViewById(R.id.et_model);
        etApiKey = findViewById(R.id.et_api_key);
        tvStatus = findViewById(R.id.tv_status);

        AiModelConfig config = prefs.readConfig();
        etBaseUrl.setText(config.baseUrl());
        etModel.setText(config.model());
        etApiKey.setText("");
        updateStatus();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_clear_key).setOnClickListener(v -> clearKey());
    }

    private void save() {
        String baseUrl = etBaseUrl.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();

        if (!baseUrl.isEmpty() && !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            tvStatus.setText("接口地址无效，需以 http:// 或 https:// 开头");
            return;
        }
        prefs.saveModel(baseUrl, apiKey, model);
        etApiKey.setText("");
        updateStatus();
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    private void clearKey() {
        prefs.clearApiKey();
        updateStatus();
        Toast.makeText(this, "已清除 API Key", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus() {
        AiModelConfig config = prefs.readConfig();
        if (config.configured()) {
            tvStatus.setText("已配置：模型 " + config.model());
        } else {
            tvStatus.setText(prefs.hasApiKey()
                    ? "未配置完整（缺少地址或模型名）"
                    : "未配置模型");
        }
    }
}
