package com.example.pocketguitar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketguitar.ai.AiClient;
import com.example.pocketguitar.ai.AiModelConfig;
import com.example.pocketguitar.ai.AiModelException;
import com.example.pocketguitar.ai.AiPrefs;
import com.example.pocketguitar.music.Song;
import com.example.pocketguitar.music.SongParser;
import com.example.pocketguitar.music.SongPlaybackManager;
import com.example.pocketguitar.music.SongStorage;
import com.example.pocketguitar.util.TabNavigator;

/** AI 谱曲页：输入描述，生成和弦进行 + 旋律，并可分别在对应面板自动弹奏。 */
public class ComposeActivity extends AppCompatActivity {

    private static final String COMPOSE_SYSTEM_PROMPT =
            "你是一名吉他谱曲助手。根据用户描述创作吉他弹唱/指弹曲谱，必须同时包含和弦进行与旋律。"
                    + "只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。JSON 结构：\n"
                    + "{\"title\":\"曲名\",\"key\":\"调性，如 C\",\"bpm\":速度数字,"
                    + "\"timeSignature\":\"4/4\","
                    + "\"sections\":[{\"name\":\"段落名，如 主歌\",\"bars\":小节数,"
                    + "\"chords\":[\"C\",\"G\",\"Am\",\"F\"]}],"
                    + "\"melody\":[{\"bar\":小节号,\"beat\":拍号,\"pitch\":\"音名+八度，如 E4\","
                    + "\"duration\":时值拍数}]}\n"
                    + "要求：\n"
                    + "1. 和弦使用常见吉他和弦标记，如 C、G、Am、F、Dm、Em、E、A、D、Bm、C7、G7、Am7、Cmaj7 等。\n"
                    + "2. 旋律音高用音名+八度表示（如 E4、C4、G4），音域在 E2 到 E5 之间。\n"
                    + "3. beat 用数字（1、2、3、4），duration 用拍数（0.5、1、1.5、2 等）。";

    private EditText etDesc;
    private Button btnGenerate;
    private TextView tvStatus;
    private TextView tvResult;
    private Button btnPlayChords;
    private Button btnPlayMelody;
    private Button btnSave;
    private AiPrefs prefs;
    private SongStorage songStorage;
    private Song currentSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        prefs = new AiPrefs(this);
        songStorage = new SongStorage(this);
        etDesc = findViewById(R.id.et_desc);
        btnGenerate = findViewById(R.id.btn_generate);
        tvStatus = findViewById(R.id.tv_status);
        tvResult = findViewById(R.id.tv_result);
        btnPlayChords = findViewById(R.id.btn_play_chords);
        btnPlayMelody = findViewById(R.id.btn_play_melody);
        btnSave = findViewById(R.id.btn_save);

        setupTopNav();

        btnGenerate.setOnClickListener(v -> generate());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, AiSettingsActivity.class)));
        btnPlayChords.setOnClickListener(v -> playChordsOnPanel());
        btnPlayMelody.setOnClickListener(v -> playMelodyOnPanel());
        btnSave.setOnClickListener(v -> saveSong());
        findViewById(R.id.btn_my_songs).setOnClickListener(v ->
                startActivity(new Intent(this, SongListActivity.class)));
    }

    private void setupTopNav() {
        findViewById(R.id.nav_compose).setSelected(true);
        findViewById(R.id.nav_chord_panel).setOnClickListener(v ->
                TabNavigator.switchTo(this, ChordPanelActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_CHORD_PANEL));
        findViewById(R.id.nav_fingerstyle).setOnClickListener(v ->
                TabNavigator.switchTo(this, FingerstyleActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_FINGERSTYLE));
        findViewById(R.id.nav_library).setOnClickListener(v ->
                TabNavigator.switchTo(this, ChordShowActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_LIBRARY));
        findViewById(R.id.nav_close).setOnClickListener(v -> finishAffinity());
    }

    private void generate() {
        AiModelConfig saved = prefs.readConfig();
        if (!saved.configured()) {
            tvStatus.setText("尚未配置模型，请先点击「模型设置」填写接口地址、模型名和 API Key");
            tvResult.setText("");
            return;
        }
        String description = etDesc.getText().toString().trim();
        if (description.isEmpty()) {
            tvStatus.setText("请先输入曲风描述");
            return;
        }

        AiModelConfig config = new AiModelConfig(
                saved.baseUrl(), saved.apiKey(), saved.model(), COMPOSE_SYSTEM_PROMPT);
        AiClient client = new AiClient(config);
        String prompt = "请为以下需求谱曲：" + description;

        btnGenerate.setEnabled(false);
        tvStatus.setText("生成中，请稍候…");
        tvResult.setText("");

        client.complete(prompt, null).whenComplete((text, error) -> {
            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                if (error != null) {
                    tvStatus.setText(friendlyError(error));
                    return;
                }
                try {
                    Song song = SongParser.parse(text);
                    currentSong = song;
                    tvStatus.setText("生成完成");
                    tvResult.setText(SongParser.toText(song));
                    btnPlayChords.setEnabled(true);
                    btnPlayMelody.setEnabled(hasMelody(song));
                    btnSave.setEnabled(true);
                } catch (AiModelException parseError) {
                    tvStatus.setText(parseError.getMessage());
                }
            });
        });
    }

    private void saveSong() {
        if (currentSong == null) return;
        try {
            songStorage.save(currentSong);
            tvStatus.setText("曲谱已保存");
            btnSave.setEnabled(false);
        } catch (Exception e) {
            tvStatus.setText("保存失败：" + e.getMessage());
        }
    }

    private void playChordsOnPanel() {
        if (currentSong == null) {
            return;
        }
        SongPlaybackManager.getInstance().set(currentSong, SongPlaybackManager.MODE_CHORDS);
        TabNavigator.switchTo(this, ChordPanelActivity.class,
                TabNavigator.TAB_COMPOSE, TabNavigator.TAB_CHORD_PANEL);
    }

    private void playMelodyOnPanel() {
        if (currentSong == null) {
            return;
        }
        SongPlaybackManager.getInstance().set(currentSong, SongPlaybackManager.MODE_MELODY);
        TabNavigator.switchTo(this, FingerstyleActivity.class,
                TabNavigator.TAB_COMPOSE, TabNavigator.TAB_FINGERSTYLE);
    }

    private boolean hasMelody(Song song) {
        return song.melody != null && !song.melody.isEmpty();
    }

    private String friendlyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        return "生成失败：" + error.getClass().getSimpleName();
    }
}
