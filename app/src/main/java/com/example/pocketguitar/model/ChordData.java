package com.example.pocketguitar.model;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ChordData {
    private ChordFingering fingering;
    private final String fingers;
    private final String pattern;

    public ChordData(String pattern, String fingers) {
        this.pattern = pattern;
        this.fingers = fingers;
    }

    public static ChordData fromJson(JSONObject jsonObject) throws JSONException {
        String pattern = jsonObject.getString("pattern");
        String fingers = jsonObject.getString("fingers");
        return new ChordData(pattern, fingers);
    }

    public ChordFingering getFingering() {
        if (this.fingering == null) {
            this.fingering = new ChordFingering(this.pattern, this.fingers);
        }
        return this.fingering;
    }

    public String getPattern() {
        return this.pattern;
    }

    public String getFingers() {
        return this.fingers;
    }
}
