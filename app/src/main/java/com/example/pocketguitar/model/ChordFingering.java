package com.example.pocketguitar.model;

import android.util.Log;

/* JADX INFO: loaded from: classes3.dex */
public class ChordFingering {
    private final int[] fingers;
    private final String pattern;
    private final String TAG = "ChordFingering";
    private final int[] frets = new int[6];

    public ChordFingering(String pattern, String finger) {
        this.pattern = pattern;
        for (int i = 0; i < 6 && i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == 'x') {
                this.frets[i] = -1;
            } else if (c >= '0' && c <= '9') {
                this.frets[i] = c - '0';
            } else if (c >= 'a' && c <= 'z') {
                this.frets[i] = (c - 'a') + 10;
            } else if (c == '-') {
                this.frets[i] = -1;
            }
        }
        this.fingers = new int[6];
        if (finger != null) {
            for (int i2 = 0; i2 < 6 && i2 < finger.length(); i2++) {
                char c2 = finger.charAt(i2);
                if (c2 == '-') {
                    this.fingers[i2] = 0;
                } else if (c2 >= '1' && c2 <= '4') {
                    this.fingers[i2] = c2 - '0';
                }
            }
        }
    }

    public int[] getFrets() {
        Log.d("ChordFingering", "fengtao getFrets: frets: " + this.frets);
        return this.frets;
    }

    public int[] getFingers() {
        Log.d("ChordFingering", "fengtao getFrets: fingers: " + this.fingers);
        return this.fingers;
    }

    public String getPattern() {
        return this.pattern;
    }

    public boolean isStringMuted(int stringIndex) {
        return stringIndex < 0 || stringIndex >= this.frets.length || this.frets[stringIndex] == -1;
    }

    public String toString() {
        return this.pattern;
    }
}
