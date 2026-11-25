package com.example.pocketguitar;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pocketguitar.music.Song;
import com.example.pocketguitar.music.SongMeta;
import com.example.pocketguitar.music.SongParser;
import com.example.pocketguitar.music.SongPlaybackManager;
import com.example.pocketguitar.music.SongStorage;
import com.example.pocketguitar.util.TabNavigator;
import java.io.IOException;
import java.util.List;

/** 曲谱列表页：展示已保存的曲谱，支持弹奏、导出、删除和导入操作。 */
public class SongListActivity extends AppCompatActivity implements SongAdapter.Listener {

    private SongStorage storage;
    private SongAdapter adapter;
    private TextView tvEmpty;

    /** 当前准备导出的曲谱 id。 */
    private String pendingExportId;

    // ── SAF 启动器 ──

    /** 导入：选择 .json 文件。 */
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::handleImportResult);

    /** 导出：创建 .json 文件。 */
    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/json"),
                    this::handleExportResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        storage = new SongStorage(this);
        tvEmpty = findViewById(R.id.tv_empty);

        RecyclerView rv = findViewById(R.id.rv_songs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter();
        adapter.setListener(this);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_import).setOnClickListener(v -> doImport());

        setupTopNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    // ── 顶部导航 ──

    private void setupTopNav() {
        // 曲谱列表不属于原有四个 tab，不设任何 tab 选中态
        findViewById(R.id.nav_chord_panel).setOnClickListener(v ->
                TabNavigator.switchTo(this, ChordPanelActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_CHORD_PANEL));
        findViewById(R.id.nav_fingerstyle).setOnClickListener(v ->
                TabNavigator.switchTo(this, FingerstyleActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_FINGERSTYLE));
        findViewById(R.id.nav_compose).setOnClickListener(v ->
                TabNavigator.switchTo(this, ComposeActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_COMPOSE));
        findViewById(R.id.nav_library).setOnClickListener(v ->
                TabNavigator.switchTo(this, ChordShowActivity.class,
                        TabNavigator.TAB_COMPOSE, TabNavigator.TAB_LIBRARY));
        findViewById(R.id.nav_close).setOnClickListener(v -> finishAffinity());
    }

    // ── 列表刷新 ──

    private void refreshList() {
        List<SongMeta> list = storage.list();
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Adapter 回调 ──

    @Override
    public void onOpen(SongMeta meta, int position) {
        try {
            Song song = storage.load(meta.id);
            showSongDetail(meta, song);
        } catch (IOException e) {
            Toast.makeText(this, "加载曲谱失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlayChords(SongMeta meta, int position) {
        try {
            Song song = storage.load(meta.id);
            SongPlaybackManager.getInstance().set(song, SongPlaybackManager.MODE_CHORDS);
            TabNavigator.switchTo(this, ChordPanelActivity.class,
                    TabNavigator.TAB_COMPOSE, TabNavigator.TAB_CHORD_PANEL);
        } catch (IOException e) {
            Toast.makeText(this, "加载曲谱失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlayMelody(SongMeta meta, int position) {
        try {
            Song song = storage.load(meta.id);
            SongPlaybackManager.getInstance().set(song, SongPlaybackManager.MODE_MELODY);
            TabNavigator.switchTo(this, FingerstyleActivity.class,
                    TabNavigator.TAB_COMPOSE, TabNavigator.TAB_FINGERSTYLE);
        } catch (IOException e) {
            Toast.makeText(this, "加载曲谱失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlayBoth(SongMeta meta, int position) {
        try {
            Song song = storage.load(meta.id);
            SongPlaybackManager.getInstance().set(song, SongPlaybackManager.MODE_BOTH);
            TabNavigator.switchTo(this, ChordPanelActivity.class,
                    TabNavigator.TAB_COMPOSE, TabNavigator.TAB_CHORD_PANEL);
        } catch (IOException e) {
            Toast.makeText(this, "加载曲谱失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSongDetail(SongMeta meta, Song song) {
        TextView title = new TextView(this);
        title.setText(meta.title != null ? meta.title : "曲谱");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(18);
        title.setTextColor(getResources().getColor(R.color.dark_text_primary));

        TextView body = new TextView(this);
        body.setText(SongParser.toText(song));
        body.setTextIsSelectable(true);
        body.setTextSize(15);
        body.setTextColor(getResources().getColor(R.color.dark_text_primary));
        body.setPadding(dp(4), dp(8), dp(4), dp(4));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(16), dp(20), dp(8));
        container.addView(title);
        container.addView(scroller);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setPositiveButton("关闭", null)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dark_panel);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.dark_accent));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onExport(SongMeta meta, int position) {
        pendingExportId = meta.id;
        String fileName = (meta.title != null ? meta.title : "曲谱") + ".json";
        exportLauncher.launch(fileName);
    }

    @Override
    public void onDelete(SongMeta meta, int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除曲谱")
                .setMessage("确定删除「" + meta.title + "」吗？此操作不可恢复。")
                .setPositiveButton("删除", (d, w) -> {
                    try {
                        storage.delete(meta.id);
                        refreshList();
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ── 导入 ──

    private void doImport() {
        importLauncher.launch(new String[]{"application/json", "*/*"});
    }

    private void handleImportResult(Uri uri) {
        if (uri == null) return;
        try {
            Song song = storage.importFrom(uri);
            refreshList();
            String name = song.title != null ? song.title : "未命名";
            Toast.makeText(this, "已导入「" + name + "」", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "导入失败：文件格式不正确", Toast.LENGTH_SHORT).show();
        }
    }

    // ── 导出 ──

    private void handleExportResult(Uri uri) {
        if (uri == null || pendingExportId == null) return;
        try {
            storage.exportTo(pendingExportId, uri);
            Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
        } finally {
            pendingExportId = null;
        }
    }
}
