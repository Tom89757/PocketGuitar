package com.example.pocketguitar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
                    + "3. beat 用数字（1、2、3、4），duration 用拍数（0.5、1、1.5、2 等）。\n"
                    + "4. 用户明确指定的调式、曲风、结构和速度必须严格遵循，sections 的段落划分要与指定结构一致。";

    private static final String AUTO = "自动";

    private static final String[] KEY_LABELS = {
            AUTO, "C 大调", "G 大调", "D 大调", "A 大调", "E 大调", "F 大调", "Bb 大调",
            "A 小调", "E 小调", "D 小调", "B 小调", "G 小调", "C 小调"};
    private static final String[] STYLE_LABELS = {
            AUTO, "民谣", "流行", "摇滚", "蓝调", "乡村", "指弹", "爵士", "朋克", "金属", "R&B", "中国风"};
    private static final String[] STRUCTURE_LABELS = {
            AUTO, "ABAB 主歌副歌", "ABABCB 流行", "AABA 三段式", "通谱式",
            "12 小节蓝调", "完整编曲", "副歌循环"};
    private static final String[] STRUCTURE_VALUES = {
            null,
            "主歌-副歌-主歌-副歌（ABAB 两段式）",
            "主歌-副歌-主歌-副歌-桥段-副歌（ABABCB 完整流行结构）",
            "AABA 三段式（两段主歌 + 桥段 + 回到主歌）",
            "通谱式（每段旋律相同，叙事民谣常用）",
            "12 小节蓝调（I-I-I-I-IV-IV-I-I-V-IV-I-V）",
            "完整编曲（前奏-主歌-副歌-间奏-主歌-副歌-尾奏）",
            "副歌循环（简短 Hook 结构）"};
    private static final String[] TEMPO_LABELS = {
            AUTO, "慢速 60-70", "中速 80-100", "中快 100-120", "快速 120-140", "极速 140+"};
    private static final String[] TEMPO_VALUES = {
            null, "慢速（60-70 BPM）", "中速（80-100 BPM）", "中快（100-120 BPM）",
            "快速（120-140 BPM）", "极速（140 BPM 以上）"};

    private EditText etDesc;
    private Spinner spKey;
    private Spinner spStyle;
    private Spinner spStructure;
    private Spinner spTempo;
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
        spKey = findViewById(R.id.spinner_key);
        spStyle = findViewById(R.id.spinner_style);
        spStructure = findViewById(R.id.spinner_structure);
        spTempo = findViewById(R.id.spinner_tempo);
        setupSpinner(spKey, KEY_LABELS);
        setupSpinner(spStyle, STYLE_LABELS);
        setupSpinner(spStructure, STRUCTURE_LABELS);
        setupSpinner(spTempo, TEMPO_LABELS);

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

    private void setupSpinner(Spinner spinner, String[] labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, labels);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    /** 返回选中项对应的需求描述；选择「自动」时返回 null。 */
    private String selectedValue(Spinner spinner, String[] labels, String[] values) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position >= labels.length) {
            return null;
        }
        return values == null ? labels[position] : values[position];
    }

    private String buildRequirement() {
        StringBuilder requirement = new StringBuilder();
        appendRequirement(requirement, "调式", selectedValue(spKey, KEY_LABELS, null));
        appendRequirement(requirement, "曲风", selectedValue(spStyle, STYLE_LABELS, null));
        appendRequirement(requirement, "结构", selectedValue(spStructure, STRUCTURE_LABELS, STRUCTURE_VALUES));
        appendRequirement(requirement, "速度", selectedValue(spTempo, TEMPO_LABELS, TEMPO_VALUES));
        String extra = etDesc.getText().toString().trim();
        if (!extra.isEmpty()) {
            requirement.append("补充描述：").append(extra).append('\n');
        }
        return requirement.toString();
    }

    private void appendRequirement(StringBuilder builder, String name, String value) {
        if (value != null) {
            builder.append(name).append("：").append(value).append('\n');
        }
    }

    private void generate() {
        AiModelConfig saved = prefs.readConfig();
        if (!saved.configured()) {
            tvStatus.setText("尚未配置模型，请先点击「模型设置」填写接口地址、模型名和 API Key");
            tvResult.setText("");
            return;
        }
        String requirement = buildRequirement();
        if (requirement.isEmpty()) {
            tvStatus.setText("请选择调式、曲风、结构或速度，或输入补充描述");
            return;
        }

        AiModelConfig config = new AiModelConfig(
                saved.baseUrl(), saved.apiKey(), saved.model(), COMPOSE_SYSTEM_PROMPT);
        AiClient client = new AiClient(config);
        String prompt = "请为以下需求谱曲：\n" + requirement;

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
