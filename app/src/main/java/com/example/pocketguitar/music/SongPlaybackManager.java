package com.example.pocketguitar.music;

/** 跨 Activity 传递待演奏的曲谱与演奏模式。 */
public final class SongPlaybackManager {
    public static final int MODE_NONE = 0;
    public static final int MODE_CHORDS = 1;
    public static final int MODE_MELODY = 2;
    /** 同时演奏和弦伴奏与旋律，在和弦面板执行。 */
    public static final int MODE_BOTH = 3;

    private static final SongPlaybackManager INSTANCE = new SongPlaybackManager();

    private Song song;
    private int mode = MODE_NONE;

    private SongPlaybackManager() {
    }

    public static SongPlaybackManager getInstance() {
        return INSTANCE;
    }

    public synchronized void set(Song song, int mode) {
        this.song = song;
        this.mode = mode;
    }

    public synchronized Song song() {
        return song;
    }

    /** 取走模式并复位，避免重复触发。 */
    public synchronized int takeMode() {
        int value = mode;
        mode = MODE_NONE;
        return value;
    }

    public synchronized void clear() {
        song = null;
        mode = MODE_NONE;
    }
}
