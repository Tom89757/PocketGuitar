package com.example.pocketguitar.music;

/** 旋律中的一个音符：小节号 + 拍号 + 音高（如 E4）+ 时值（拍）。 */
public class MelodyNote {
    public int bar;
    public double beat;
    public String pitch;
    public double duration;
}
