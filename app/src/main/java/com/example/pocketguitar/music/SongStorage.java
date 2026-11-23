package com.example.pocketguitar.music;

import android.content.Context;
import android.net.Uri;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 曲谱本地持久化管理器。
 * <p>
 * 曲谱以 JSON 文件保存在 app 内部私有目录 {@code files/songs/} 下，
 * 文件名格式 {@code <id>.json}；另有一个索引文件 {@code songs/index.json}
 * 记录曲谱元数据列表，用于快速加载列表。
 * <p>
 * 同时支持将曲谱导出到外部 URI 或从外部 URI 导入。
 */
public final class SongStorage {

    private static final String DIR_NAME = "songs";
    private static final String INDEX_FILE = "index.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Type INDEX_TYPE = new TypeToken<List<SongMeta>>() {}.getType();

    private final Context context;

    public SongStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── 保存 ────────────────────────────────────────

    /** 保存曲谱，返回新分配的 id。 */
    public String save(Song song) throws IOException {
        String id = UUID.randomUUID().toString();
        writeFile(songFile(id), GSON.toJson(song));

        SongMeta meta = buildMeta(id, song);
        List<SongMeta> index = readIndex();
        index.add(0, meta);
        writeIndex(index);
        return id;
    }

    // ── 加载 ────────────────────────────────────────

    /** 按 id 加载完整曲谱。 */
    public Song load(String id) throws IOException {
        String json = readFile(songFile(id));
        return GSON.fromJson(json, Song.class);
    }

    // ── 列表 ────────────────────────────────────────

    /** 获取曲谱元数据列表（按保存时间降序）。 */
    public List<SongMeta> list() {
        return readIndex();
    }

    // ── 删除 ────────────────────────────────────────

    /** 删除指定曲谱，同时移除索引条目。 */
    public void delete(String id) throws IOException {
        File file = songFile(id);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        List<SongMeta> index = readIndex();
        Iterator<SongMeta> it = index.iterator();
        while (it.hasNext()) {
            if (id.equals(it.next().id)) {
                it.remove();
            }
        }
        writeIndex(index);
    }

    // ── 导出（写入用户指定的 URI）─────────────────

    /** 导出曲谱到指定 URI（通过 SAF 获取）。 */
    public void exportTo(String id, Uri uri) throws IOException {
        Song song = load(id);
        String json = GSON.toJson(song);
        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
             OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    /** 直接导出 Song 对象到指定 URI。 */
    public void exportTo(Song song, Uri uri) throws IOException {
        String json = GSON.toJson(song);
        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
             OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    // ── 导入（从用户选取的 URI 读取）──────────────

    /** 从 URI 导入曲谱，保存到本地并返回 Song。 */
    public Song importFrom(Uri uri) throws IOException {
        String json = readUri(uri);
        Song song = GSON.fromJson(json, Song.class);
        if (song == null) {
            throw new IOException("导入的文件不包含有效曲谱");
        }
        save(song);
        return song;
    }

    // ── 内部工具 ──────────────────────────────────

    private File songsDir() {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private File songFile(String id) {
        return new File(songsDir(), id + ".json");
    }

    private File indexFile() {
        return new File(songsDir(), INDEX_FILE);
    }

    private List<SongMeta> readIndex() {
        File file = indexFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            String json = readFile(file);
            List<SongMeta> list = GSON.fromJson(json, INDEX_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void writeIndex(List<SongMeta> index) throws IOException {
        writeFile(indexFile(), GSON.toJson(index));
    }

    private SongMeta buildMeta(String id, Song song) {
        SongMeta meta = new SongMeta();
        meta.id = id;
        meta.title = song.title != null && !song.title.trim().isEmpty()
                ? song.title : "未命名";
        meta.key = song.key;
        meta.bpm = song.bpm;
        meta.savedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());
        return meta;
    }

    private String readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private String readUri(Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
