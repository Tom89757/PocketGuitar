package com.example.pocketguitar.music;

import java.util.ArrayList;
import java.util.List;

/** 曲谱中的一个段落，例如主歌 / 副歌。 */
public class SongSection {
    public String name;
    public int bars;
    public List<String> chords = new ArrayList<>();
}
