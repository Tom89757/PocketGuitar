package com.example.pocketguitar.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.pocketguitar.model.ChordItem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes4.dex */
public class SharedChordManager {
    private static final String KEY_CHORD_LIST = "chord_list";
    private static final String KEY_CURRENT_PLAY_ROOT = "current_play_root";
    private static final String KEY_CURRENT_PLAY_TYPE = "current_play_type";
    private static final String KEY_CURRENT_SELECT_ROOT = "current_select_root";
    private static final String KEY_CURRENT_SELECT_TYPE = "current_select_type";
    private static final String PREFS_NAME = "chord_prefs";
    private static final String TAG = "SharedChordManager";
    private static SharedChordManager instance;
    private Context appContext;
    private final List<ChordItem> chordList = new CopyOnWriteArrayList();
    private final List<ChordListChangeListener> listeners = new ArrayList();
    private final List<CurrentSelectChordChangeListener> currentSelectChordListeners = new ArrayList();
    private final List<CurrentPlayChordChangeListener> playChordListeners = new ArrayList();
    private String currentSelectRoot = "C";
    private String currentSelectChordType = "Major";
    private String currentPlayRoot = "";
    private String currentPlayChordType = "";

    public interface ChordListChangeListener {
        void onChordListChanged(List<ChordItem> list);
    }

    public interface CurrentPlayChordChangeListener {
        void onPlayChordChanged(String str, String str2);
    }

    public interface CurrentSelectChordChangeListener {
        void onCurrentSelectChordChanged(String str, String str2);
    }

    private SharedChordManager() {
    }

    public static synchronized SharedChordManager getInstance() {
        if (instance == null) {
            instance = new SharedChordManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (this.appContext == null) {
            this.appContext = context.getApplicationContext();
            loadChords();
            loadCurrentChord();
        }
    }

    private void loadChords() {
        if (this.appContext == null) {
            return;
        }
        SharedPreferences prefs = this.appContext.getSharedPreferences(PREFS_NAME, 0);
        String chordsJson = prefs.getString(KEY_CHORD_LIST, null);
        this.chordList.clear();
        if (chordsJson != null && !chordsJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(chordsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String root = jsonObject.getString("root");
                    String type = jsonObject.getString("type");
                    this.chordList.add(new ChordItem(root, type));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing saved chords", e);
            }
        }
        if (this.chordList.isEmpty()) {
            addDefaultChords();
        }
        notifyListeners();
    }

    private void loadCurrentChord() {
        if (this.appContext == null) {
            return;
        }
        SharedPreferences prefs = this.appContext.getSharedPreferences(PREFS_NAME, 0);
        this.currentSelectRoot = prefs.getString(KEY_CURRENT_SELECT_ROOT, "C");
        this.currentSelectChordType = prefs.getString(KEY_CURRENT_SELECT_TYPE, "Major");
        this.currentPlayRoot = prefs.getString(KEY_CURRENT_PLAY_ROOT, "");
        this.currentPlayChordType = prefs.getString(KEY_CURRENT_PLAY_TYPE, "");
        notifyCurrentChordListeners();
        notifyPlayChordListeners();
    }

    private void notifyPlayChordListeners() {
        for (CurrentPlayChordChangeListener listener : this.playChordListeners) {
            listener.onPlayChordChanged(this.currentPlayRoot, this.currentPlayChordType);
        }
    }

    public void addDefaultChords() {
        this.chordList.add(new ChordItem("A", "Major"));
        this.chordList.add(new ChordItem("D", "Major"));
        this.chordList.add(new ChordItem("G", "Major"));
        this.chordList.add(new ChordItem("C", "Major"));
        this.chordList.add(new ChordItem("F", "Major"));
        this.chordList.add(new ChordItem("E", "Major"));
        saveChords();
    }

    private void saveChords() {
        if (this.appContext == null) {
            return;
        }
        try {
            JSONArray jsonArray = new JSONArray();
            for (ChordItem chord : this.chordList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("root", chord.getRoot());
                jsonObject.put("type", chord.getType());
                jsonArray.put(jsonObject);
            }
            SharedPreferences prefs = this.appContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CHORD_LIST, jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving chords", e);
        }
    }

    private void saveCurrentSelectChord() {
        if (this.appContext == null) {
            return;
        }
        SharedPreferences prefs = this.appContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CURRENT_SELECT_ROOT, this.currentSelectRoot);
        editor.putString(KEY_CURRENT_SELECT_TYPE, this.currentSelectChordType);
        editor.apply();
    }

    private void saveCurrentPlayChord() {
        if (this.appContext == null) {
            return;
        }
        SharedPreferences prefs = this.appContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CURRENT_PLAY_ROOT, this.currentPlayRoot);
        editor.putString(KEY_CURRENT_PLAY_TYPE, this.currentPlayRoot);
        editor.apply();
    }

    public List<ChordItem> getChordList() {
        return this.chordList;
    }

    public boolean addChord(ChordItem chord) throws JSONException {
        for (ChordItem item : this.chordList) {
            if (item.getRoot().equals(chord.getRoot()) && item.getType().equals(chord.getType())) {
                return false;
            }
        }
        if (this.chordList.size() >= 8) {
            return false;
        }
        chord.setFretsAndFingers();
        this.chordList.add(chord);
        saveChords();
        notifyListeners();
        return true;
    }

    public String getCurrentSelectRoot() {
        return this.currentSelectRoot;
    }

    public String getCurrentSelectChordType() {
        return this.currentSelectChordType;
    }

    public void setCurrentChord(String root, String type) {
        boolean changed = (this.currentSelectRoot.equals(root) && this.currentSelectChordType.equals(type)) ? false : true;
        if (changed) {
            this.currentSelectRoot = root;
            this.currentSelectChordType = type;
            saveCurrentSelectChord();
            notifyCurrentChordListeners();
        }
    }

    public void removeChord(int position) {
        if (position >= 0 && position < this.chordList.size()) {
            this.chordList.remove(position);
            saveChords();
            notifyListeners();
        }
    }

    public void clearChords() {
        this.chordList.clear();
        saveChords();
        notifyListeners();
    }

    public void addChangeListener(ChordListChangeListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeChangeListener(ChordListChangeListener listener) {
        this.listeners.remove(listener);
    }

    public void addCurrentSelectChordListener(CurrentSelectChordChangeListener listener) {
        if (!this.currentSelectChordListeners.contains(listener)) {
            this.currentSelectChordListeners.add(listener);
        }
    }

    public void removeCurrentChordListener(CurrentSelectChordChangeListener listener) {
        this.currentSelectChordListeners.remove(listener);
    }

    private void notifyListeners() {
        for (ChordListChangeListener listener : this.listeners) {
            listener.onChordListChanged(this.chordList);
        }
    }

    private void notifyCurrentChordListeners() {
        for (CurrentSelectChordChangeListener listener : this.currentSelectChordListeners) {
            listener.onCurrentSelectChordChanged(this.currentSelectRoot, this.currentSelectChordType);
        }
    }

    public void addPlayChordListener(CurrentPlayChordChangeListener listener) {
        if (!this.playChordListeners.contains(listener)) {
            this.playChordListeners.add(listener);
        }
    }

    public String getCurrentPlayRoot() {
        return this.currentPlayRoot;
    }

    public String getCurrentPlayTypeChord() {
        return this.currentPlayChordType;
    }

    public void setCurrentPlayChord(String root, String type) {
        boolean changed = (this.currentPlayRoot.equals(root) && this.currentPlayChordType.equals(type)) ? false : true;
        if (changed) {
            this.currentPlayRoot = root;
            this.currentPlayChordType = type;
            saveCurrentPlayChord();
            notifyPlayChordListeners();
        }
    }

    public ChordItem findChordItem(String root, String type) {
        for (ChordItem item : this.chordList) {
            if (item.getRoot().equals(root) && item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

    public void removePlayerChordListener(CurrentPlayChordChangeListener listener) {
        this.playChordListeners.remove(listener);
    }
}
