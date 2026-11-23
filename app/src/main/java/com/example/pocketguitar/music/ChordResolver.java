package com.example.pocketguitar.music;

import java.util.HashMap;
import java.util.Map;

/** 把 AI 输出的和弦标记（如 C、Am、C7、Cmaj7）解析为 (根音, 类型)。 */
public final class ChordResolver {
    private static final String[] ROOTS = {
            "C", "C#", "Cb", "D", "D#", "Db", "E", "E#", "Eb", "F", "F#", "Fb",
            "G", "G#", "Gb", "A", "A#", "Ab", "B", "B#", "Bb"
    };

    private static final Map<String, String> SUFFIX_TO_TYPE = new HashMap<>();

    static {
        SUFFIX_TO_TYPE.put("", "Major");
        SUFFIX_TO_TYPE.put("M", "Major");
        SUFFIX_TO_TYPE.put("maj", "Major");
        SUFFIX_TO_TYPE.put("m", "Minor");
        SUFFIX_TO_TYPE.put("min", "Minor");
        SUFFIX_TO_TYPE.put("minor", "Minor");
        SUFFIX_TO_TYPE.put("5", "Power");
        SUFFIX_TO_TYPE.put("7", "7");
        SUFFIX_TO_TYPE.put("maj7", "maj7");
        SUFFIX_TO_TYPE.put("M7", "maj7");
        SUFFIX_TO_TYPE.put("m7", "m7");
        SUFFIX_TO_TYPE.put("min7", "m7");
        SUFFIX_TO_TYPE.put("dim", "dim");
        SUFFIX_TO_TYPE.put("dim7", "dim7");
        SUFFIX_TO_TYPE.put("aug", "aug");
        SUFFIX_TO_TYPE.put("sus2", "sus2");
        SUFFIX_TO_TYPE.put("sus4", "sus4");
        SUFFIX_TO_TYPE.put("6", "6");
        SUFFIX_TO_TYPE.put("9", "9");
        SUFFIX_TO_TYPE.put("11", "11");
        SUFFIX_TO_TYPE.put("13", "13");
        SUFFIX_TO_TYPE.put("m6", "m6");
        SUFFIX_TO_TYPE.put("m9", "m9");
        SUFFIX_TO_TYPE.put("maj9", "maj9");
        SUFFIX_TO_TYPE.put("add9", "add9");
        SUFFIX_TO_TYPE.put("add4", "add4");
        SUFFIX_TO_TYPE.put("7sus4", "7sus4");
        SUFFIX_TO_TYPE.put("m7b5", "m7b5");
    }

    private ChordResolver() {
    }

    /** 返回 {根音, 类型}；无法解析返回 null。 */
    public static String[] resolve(String chordName) {
        if (chordName == null) {
            return null;
        }
        String name = chordName.trim();
        if (name.isEmpty()) {
            return null;
        }

        String bestRoot = null;
        for (String root : ROOTS) {
            if (name.startsWith(root) && (bestRoot == null || root.length() > bestRoot.length())) {
                bestRoot = root;
            }
        }
        if (bestRoot == null) {
            return null;
        }
        String suffix = name.substring(bestRoot.length());
        String type = SUFFIX_TO_TYPE.get(suffix);
        if (type == null) {
            return null;
        }
        return new String[]{bestRoot, type};
    }
}
