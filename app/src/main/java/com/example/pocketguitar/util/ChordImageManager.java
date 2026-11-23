package com.example.pocketguitar.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes4.dex */
public class ChordImageManager {
    private static final String CHORD_ASSETS_DIR = "chords";
    private static final String TAG = "ChordImageManager";
    private static ChordImageManager instance;
    private final Context appContext;
    private final Map<String, Drawable> chordCache = new HashMap();
    private JSONObject chordsData;

    public ChordImageManager(Context context) {
        this.appContext = context.getApplicationContext();
        loadChordsData();
    }

    public static synchronized ChordImageManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChordImageManager(context);
        }
        return instance;
    }

    public static ChordImageManager getInstance() {
        return instance;
    }

    public static void initialize(Context context) {
        getInstance(context);
    }

    private void loadChordsData() {
        try {
            InputStream is = this.appContext.getResources().openRawResource(this.appContext.getResources().getIdentifier(CHORD_ASSETS_DIR, "raw", this.appContext.getPackageName()));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            this.chordsData = new JSONObject(json);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading chords data", e);
        }
    }

    public Drawable getChordImage(String root, String type) {
        String cacheKey = root + "_" + type;
        if (this.chordCache.containsKey(cacheKey)) {
            return this.chordCache.get(cacheKey);
        }
        try {
            String fileName = root + "_" + type + ".png";
            String filePath = "chords/" + root + "/" + fileName;
            InputStream is = this.appContext.getAssets().open(filePath);
            Drawable drawable = Drawable.createFromStream(is, null);
            is.close();
            this.chordCache.put(cacheKey, drawable);
            return drawable;
        } catch (IOException e) {
            Log.e(TAG, "Error loading chord image: " + root + " " + type, e);
            try {
                String safeRoot = root.replace("#", "sharp").replace("b", "flat");
                String safeType = type.replace("#", "sharp").replace("b", "flat");
                String filePath2 = "chords/" + safeRoot + "/" + safeRoot + "_" + safeType + ".png";
                InputStream is2 = this.appContext.getAssets().open(filePath2);
                Drawable drawable2 = Drawable.createFromStream(is2, null);
                is2.close();
                this.chordCache.put(cacheKey, drawable2);
                return drawable2;
            } catch (IOException e2) {
                Log.e(TAG, "Error loading chord image with fallback path: " + root + " " + type, e2);
                return null;
            }
        }
    }

    public List<String> getAvailableChordTypes(String root) {
        List<String> types = new ArrayList<>();
        try {
            AssetManager assetManager = this.appContext.getAssets();
            String rootDir = "chords/" + root;
            String[] files = assetManager.list(rootDir);
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".png")) {
                        String chordType = file.substring(file.indexOf("_") + 1, file.lastIndexOf("."));
                        types.add(chordType);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing chord types for root: " + root, e);
        }
        return types;
    }

    public List<String> getAvailableRoots() {
        List<String> roots = new ArrayList<>();
        try {
            AssetManager assetManager = this.appContext.getAssets();
            String[] directories = assetManager.list(CHORD_ASSETS_DIR);
            if (directories != null) {
                roots.addAll(Arrays.asList(directories));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing chord roots", e);
        }
        return roots;
    }

    public JSONObject getJsonChord(String root, String type) {
        try {
            if (this.chordsData != null && this.chordsData.has(CHORD_ASSETS_DIR)) {
                JSONObject chords = this.chordsData.getJSONObject(CHORD_ASSETS_DIR);
                if (chords.has(type)) {
                    JSONObject chordType = chords.getJSONObject(type);
                    if (chordType.has(root)) {
                        return chordType.getJSONObject(root);
                    }
                    return null;
                }
                return null;
            }
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Error getting fingering pattern", e);
            return null;
        }
    }

    public String getFingeringPattern(String root, String type) {
        try {
            if (this.chordsData != null && this.chordsData.has(CHORD_ASSETS_DIR)) {
                JSONObject chords = this.chordsData.getJSONObject(CHORD_ASSETS_DIR);
                if (chords.has(type)) {
                    JSONObject chordType = chords.getJSONObject(type);
                    if (chordType.has(root)) {
                        JSONObject chord = chordType.getJSONObject(root);
                        return chord.getString("pattern");
                    }
                    return null;
                }
                return null;
            }
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Error getting fingering pattern", e);
            return null;
        }
    }

    public String getFingeringPositions(String root, String type) {
        try {
            if (this.chordsData != null && this.chordsData.has(CHORD_ASSETS_DIR)) {
                JSONObject chords = this.chordsData.getJSONObject(CHORD_ASSETS_DIR);
                if (chords.has(type)) {
                    JSONObject chordType = chords.getJSONObject(type);
                    if (chordType.has(root)) {
                        JSONObject chord = chordType.getJSONObject(root);
                        return chord.getString("fingers");
                    }
                    return null;
                }
                return null;
            }
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Error getting fingering positions", e);
            return null;
        }
    }

    public boolean chordImageExists(String root, String type) {
        try {
            String fileName = root + "_" + type + ".png";
            String filePath = "chords/" + root + "/" + fileName;
            this.appContext.getAssets().open(filePath).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void preloadRootChords(final String root) {
        new Thread(new Runnable() { // from class: com.example.pocketguitar.util.ChordImageManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ChordImageManager.this.m83x9d3934e1(root);
            }
        }).start();
    }

    /* JADX INFO: renamed from: lambda$preloadRootChords$0$com-example-guitardemo-util-ChordImageManager, reason: not valid java name */
    /* synthetic */ void m83x9d3934e1(String root) {
        List<String> types = getAvailableChordTypes(root);
        for (String type : types) {
            getChordImage(root, type);
        }
        Log.d(TAG, "预加载完成 " + root + " 的所有和弦");
    }

    public void clearCache() {
        this.chordCache.clear();
    }

    public JSONObject getChordsData() {
        return this.chordsData;
    }
}
