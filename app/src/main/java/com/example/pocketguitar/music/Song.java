package com.example.pocketguitar.music;

import java.util.ArrayList;
import java.util.List;

/** AI 生成的曲谱：标题 + 调性 + 速度 + 拍号 + 段落（和弦进行）+ 旋律。 */
public class Song {
    public String title;
    public String key;
    public int bpm;
    public String timeSignature;
    public List<SongSection> sections = new ArrayList<>();
    public List<MelodyNote> melody = new ArrayList<>();
}
