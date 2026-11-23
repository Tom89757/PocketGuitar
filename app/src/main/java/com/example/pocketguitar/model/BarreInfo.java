package com.example.pocketguitar.model;

/* JADX INFO: loaded from: classes3.dex */
public class BarreInfo {
    private int endString;
    private int finger;
    private int fret;
    private int startString;

    public BarreInfo(int finger, int fret, int startString, int endString) {
        this.finger = finger;
        this.fret = fret;
        this.startString = startString;
        this.endString = endString;
    }

    public int getFinger() {
        return this.finger;
    }

    public int getFret() {
        return this.fret;
    }

    public int getStartString() {
        return this.startString;
    }

    public int getEndString() {
        return this.endString;
    }
}
