package com.example.pocketguitar.util;

import android.animation.TimeInterpolator;

/* JADX INFO: loaded from: classes4.dex */
public class StringVibrationInterpolator implements TimeInterpolator {
    private final float damping;
    private final float frequency;

    public StringVibrationInterpolator(float frequency, float decay) {
        this.frequency = frequency;
        this.damping = decay;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        float envelope = (float) Math.pow(1.0f - input, this.damping);
        return ((float) Math.sin(((double) this.frequency) * 6.283185307179586d * ((double) input))) * envelope;
    }
}
