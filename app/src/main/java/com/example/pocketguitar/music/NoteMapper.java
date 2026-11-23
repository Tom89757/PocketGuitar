package com.example.pocketguitar.music;

/** 音名→MIDI→(弦,品) 的确定性映射，使用标准调弦。 */
public final class NoteMapper {
    // 6弦..1弦 空弦 MIDI 值
    private static final int[] OPEN_MIDI = {40, 45, 50, 55, 59, 64};

    private NoteMapper() {
    }

    /** 解析音名+八度（如 E4、C#4、Bb3）为 MIDI 值，失败返回 -1。 */
    public static int parsePitch(String pitch) {
        if (pitch == null) {
            return -1;
        }
        String p = pitch.trim();
        if (p.isEmpty()) {
            return -1;
        }
        int semitone = letterSemitone(Character.toUpperCase(p.charAt(0)));
        if (semitone < 0) {
            return -1;
        }
        int idx = 1;
        if (idx < p.length()) {
            char accidental = p.charAt(idx);
            if (accidental == '#') {
                semitone += 1;
                idx++;
            } else if (accidental == 'b') {
                semitone -= 1;
                idx++;
            }
        }
        if (idx >= p.length()) {
            return -1;
        }
        int octave;
        try {
            octave = Integer.parseInt(p.substring(idx));
        } catch (NumberFormatException error) {
            return -1;
        }
        semitone = ((semitone % 12) + 12) % 12;
        return (octave + 1) * 12 + semitone;
    }

    /** 把一个 MIDI 音高映射到 {弦序号, 品位}，找不到可弹位置返回 null。 */
    public static int[] findStringFret(int midi) {
        if (midi < OPEN_MIDI[0]) {
            return null;
        }
        int bestString = -1;
        int bestFret = Integer.MAX_VALUE;
        for (int s = 0; s < 6; s++) {
            int fret = midi - OPEN_MIDI[s];
            if (fret >= 0 && fret <= 24 && fret < bestFret) {
                bestFret = fret;
                bestString = s;
            }
        }
        return bestString < 0 ? null : new int[]{bestString, bestFret};
    }

    private static int letterSemitone(char letter) {
        switch (letter) {
            case 'C':
                return 0;
            case 'D':
                return 2;
            case 'E':
                return 4;
            case 'F':
                return 5;
            case 'G':
                return 7;
            case 'A':
                return 9;
            case 'B':
                return 11;
            default:
                return -1;
        }
    }
}
