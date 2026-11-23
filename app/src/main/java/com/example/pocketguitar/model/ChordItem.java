package com.example.pocketguitar.model;

import android.util.Log;
import com.example.pocketguitar.util.ChordImageManager;
import java.io.Serializable;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ChordItem implements Serializable {
    private final String TAG = "ChordItem";
    private int[] fingers;
    private int[] frets;
    private final String root;
    private final String type;

    public ChordItem(String root, String type) {
        this.root = root;
        this.type = type;
    }

    public String getRoot() {
        return this.root;
    }

    public String getType() {
        return this.type;
    }

    public String toString() {
        return this.root + " " + this.type;
    }

    public void setFretsAndFingers() throws JSONException {
        JSONObject jsonChord = ChordImageManager.getInstance().getJsonChord(this.root, this.type);
        ChordFingering chordFingering = new ChordFingering(jsonChord.getString("pattern"), jsonChord.getString("fingers"));
        Log.d("ChordItem", "fengtao setFretsAndFingers: frets: " + Arrays.toString(chordFingering.getFrets()) + " fingers: " + Arrays.toString(chordFingering.getFingers()));
        this.frets = chordFingering.getFrets();
        this.fingers = chordFingering.getFingers();
    }

    public void setFrets() throws JSONException {
        JSONObject jsonChord = ChordImageManager.getInstance().getJsonChord(this.root, this.type);
        ChordFingering chordFingering = new ChordFingering(jsonChord.getString("pattern"), jsonChord.getString("fingers"));
        Log.d("ChordItem", "fengtao setFrets: frets: " + Arrays.toString(chordFingering.getFrets()));
        this.frets = chordFingering.getFrets();
    }

    public int[] getFrets() {
        return this.frets;
    }

    public void setFingers() throws JSONException {
        JSONObject jsonChord = ChordImageManager.getInstance().getJsonChord(this.root, this.type);
        ChordFingering chordFingering = new ChordFingering(jsonChord.getString("pattern"), jsonChord.getString("fingers"));
        Log.d("ChordItem", "fengtao setFingers: fingers: " + Arrays.toString(chordFingering.getFingers()));
        this.fingers = chordFingering.getFingers();
    }

    public int[] getFingers() {
        return this.fingers;
    }
}
