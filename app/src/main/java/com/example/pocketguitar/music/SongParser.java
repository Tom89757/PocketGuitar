package com.example.pocketguitar.music;

import com.example.pocketguitar.ai.AiModelException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;

/** 把模型返回的文本解析为 {@link Song}，并渲染成可读的文本曲谱。 */
public final class SongParser {
    private static final Gson GSON = new Gson();

    private SongParser() {
    }

    /** 从模型输出中抽出 JSON 对象（容忍 Markdown 代码块与前后赘述）。 */
    public static String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int start = text.indexOf('\n', fence);
            start = start < 0 ? fence + 3 : start + 1;
            int end = text.indexOf("```", start);
            text = end >= 0 ? text.substring(start, end) : text.substring(start);
        }
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first < 0 || last < 0 || last <= first) {
            return null;
        }
        return text.substring(first, last + 1);
    }

    public static Song parse(String raw) throws AiModelException {
        String json = extractJson(raw);
        if (json == null) {
            throw new AiModelException(AiModelException.Kind.PROTOCOL, "模型未返回有效 JSON 曲谱");
        }
        try {
            Song song = GSON.fromJson(json, Song.class);
            if (song == null) {
                throw new AiModelException(AiModelException.Kind.PROTOCOL, "曲谱内容为空");
            }
            return song;
        } catch (JsonSyntaxException error) {
            throw new AiModelException(AiModelException.Kind.PROTOCOL, "曲谱 JSON 解析失败", error);
        }
    }

    public static String toText(Song song) {
        StringBuilder out = new StringBuilder();
        out.append("《").append(blankTo(song.title, "未命名")).append("》");
        boolean hasMeta = false;
        if (notBlank(song.key)) {
            out.append("  调性 ").append(song.key);
            hasMeta = true;
        }
        if (song.bpm > 0) {
            out.append("  BPM ").append(song.bpm);
            hasMeta = true;
        }
        if (notBlank(song.timeSignature)) {
            out.append("  ").append(song.timeSignature);
            hasMeta = true;
        }
        if (hasMeta) {
            out.append('\n');
        }

        List<SongSection> sections = song.sections;
        if (sections == null || sections.isEmpty()) {
            out.append("\n（无段落信息）");
            return out.toString();
        }
        for (SongSection section : sections) {
            if (section == null) {
                continue;
            }
            out.append("\n[").append(blankTo(section.name, "段落")).append("]");
            if (section.bars > 0) {
                out.append("  ").append(section.bars).append(" 小节");
            }
            out.append('\n');
            out.append("  ").append(joinChords(section.chords)).append('\n');
        }

        List<MelodyNote> melody = song.melody;
        if (melody != null && !melody.isEmpty()) {
            out.append("\n[主旋律]\n");
            for (MelodyNote note : melody) {
                if (note == null) {
                    continue;
                }
                out.append("  第 ").append(note.bar).append(" 小节");
                if (note.beat > 0) {
                    out.append(" 第 ").append(formatBeat(note.beat)).append(" 拍");
                }
                out.append("  ").append(displayNote(note.pitch));
                if (note.duration > 0) {
                    out.append("（").append(formatBeat(note.duration)).append("拍）");
                }
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static String formatBeat(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String displayNote(String pitch) {
        if (pitch == null || pitch.trim().isEmpty()) {
            return "?";
        }
        return pitch.replace("#", "♯").replace("b", "♭");
    }

    private static String joinChords(List<String> chords) {
        if (chords == null || chords.isEmpty()) {
            return "（无和弦）";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chords.size(); i++) {
            if (i > 0) {
                builder.append("   ");
            }
            String chord = chords.get(i);
            builder.append(chord == null ? "" : chord.replace("b", "♭").replace("#", "♯"));
        }
        return builder.toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String blankTo(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }
}
