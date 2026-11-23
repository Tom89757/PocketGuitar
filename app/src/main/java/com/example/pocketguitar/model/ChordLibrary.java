package com.example.pocketguitar.model;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ChordLibrary {
    private static final String TAG = "ChordLibrary";
    private String[] chordSuffixes;
    private final Map<String, ChordFingering> chordMap = new HashMap();
    private String[] rootNotes = {"C", "C#", "Cb"};

    public boolean loadFromResource(Context context, int resourceId) {
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    jsonString.append(line);
                } else {
                    reader.close();
                    inputStream.close();
                    return loadFromJson(jsonString.toString());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading chord resource", e);
            return false;
        }
    }

    public boolean loadFromJson(String jsonString) {
        try {
            JSONObject jsonRoot = new JSONObject(jsonString);
            JSONArray rootsArray = jsonRoot.getJSONArray("roots");
            this.rootNotes = new String[rootsArray.length()];
            for (int i = 0; i < rootsArray.length(); i++) {
                this.rootNotes[i] = rootsArray.getString(i);
            }
            JSONObject chordsObj = jsonRoot.getJSONObject("chords");
            this.chordSuffixes = new String[chordsObj.length()];
            int suffixIndex = 0;
            Iterator<String> it = chordsObj.keys();
            while (it.hasNext()) {
                String suffix = it.next();
                int suffixIndex2 = suffixIndex + 1;
                this.chordSuffixes[suffixIndex] = suffix;
                JSONObject suffixObj = chordsObj.getJSONObject(suffix);
                Iterator<String> iter = suffixObj.keys();
                while (iter.hasNext()) {
                    String root = iter.next();
                    JSONObject chordObj = suffixObj.getJSONObject(root);
                    ChordData chordData = ChordData.fromJson(chordObj);
                    String chordKey = root + suffix;
                    this.chordMap.put(chordKey, chordData.getFingering());
                }
                suffixIndex = suffixIndex2;
            }
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing chord JSON", e);
            return false;
        }
    }

    public ChordFingering getChord(String root, String suffix) {
        return this.chordMap.get(root + suffix);
    }

    public ChordFingering getChordByName(String chordName) {
        for (String root : this.rootNotes) {
            if (chordName.startsWith(root)) {
                String suffix = chordName.substring(root.length());
                ChordFingering fingering = getChord(root, suffix);
                if (fingering != null) {
                    return fingering;
                }
            }
        }
        return null;
    }

    public boolean hasChord(String root, String suffix) {
        return this.chordMap.containsKey(root + suffix);
    }

    public String[] getRootNotes() {
        return this.rootNotes;
    }

    public String[] getChordSuffixes() {
        return this.chordSuffixes;
    }

    public int getChordCount() {
        return this.chordMap.size();
    }
}
