package com.example.pocketguitar.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import android.view.View;
import com.example.pocketguitar.R;
import com.example.pocketguitar.model.ChordFingering;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* JADX INFO: loaded from: classes4.dex */
public class StringPlayer {
    private static final float SEMITONE_RATIO = 1.0594631f;
    private static final Map<Integer, Integer> STRING_INDEX_MAP = new HashMap();
    private final Context context;
    private SoundPool soundPool;
    private final String TAG = "StringPlayer";
    private final Map<Integer, Integer> stringSounds = new HashMap();
    private final Map<Integer, ObjectAnimator> activeAnimations = new HashMap();
    private int[] currentFrets = {0, 0, 0, 0, 0, 0};
    private int[] currentFingers = {0, 0, 0, 0, 0, 0};
    private float[] pitchRates = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    static {
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string1), 5);
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string2), 4);
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string3), 3);
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string4), 2);
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string5), 1);
        STRING_INDEX_MAP.put(Integer.valueOf(R.id.string6), 0);
    }

    private static final int[] STRING_VIEW_IDS = {R.id.string6, R.id.string5, R.id.string4, R.id.string3, R.id.string2, R.id.string1};

    public StringPlayer(Context context) {
        this.context = context;
        initSoundPool();
        loadSounds();
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(14).setContentType(4).build();
        this.soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(audioAttributes).build();
    }

    private void loadSounds() {
        this.stringSounds.put(Integer.valueOf(R.id.string1), Integer.valueOf(this.soundPool.load(this.context, R.raw.string1, 1)));
        this.stringSounds.put(Integer.valueOf(R.id.string2), Integer.valueOf(this.soundPool.load(this.context, R.raw.string2, 1)));
        this.stringSounds.put(Integer.valueOf(R.id.string3), Integer.valueOf(this.soundPool.load(this.context, R.raw.string3, 1)));
        this.stringSounds.put(Integer.valueOf(R.id.string4), Integer.valueOf(this.soundPool.load(this.context, R.raw.string4, 1)));
        this.stringSounds.put(Integer.valueOf(R.id.string5), Integer.valueOf(this.soundPool.load(this.context, R.raw.string5, 1)));
        this.stringSounds.put(Integer.valueOf(R.id.string6), Integer.valueOf(this.soundPool.load(this.context, R.raw.string6, 1)));
    }

    public void setChordFingering(ChordFingering fingering) {
        if (fingering != null) {
            this.currentFrets = fingering.getFrets();
        } else {
            this.currentFrets = new int[]{0, 0, 0, 0, 0};
        }
    }

    public void setFrets(int[] frets) {
        if (frets != null && frets.length == 6) {
            this.currentFrets = frets;
            for (int i = 0; i < frets.length; i++) {
                int fret = frets[i];
                if (fret == -1) {
                    this.pitchRates[i] = 0.0f;
                } else {
                    this.pitchRates[i] = calculatePitchRate(fret);
                }
            }
        }
        Log.d("StringPlayer", "setFrets fengtao: currentFrets: " + Arrays.toString(this.currentFrets));
        Log.d("StringPlayer", "setFrets fengtao: pitchRates: " + Arrays.toString(this.pitchRates));
    }

    public void playString(View stringView, int stringId) {
        if (this.activeAnimations.containsKey(Integer.valueOf(stringView.getId()))) {
            ((ObjectAnimator) Objects.requireNonNull(this.activeAnimations.get(Integer.valueOf(stringView.getId())))).cancel();
        }
        Integer stringIndex = STRING_INDEX_MAP.get(Integer.valueOf(stringId));
        if (stringIndex == null) {
            return;
        }
        Integer soundId = this.stringSounds.getOrDefault(Integer.valueOf(stringView.getId()), 0);
        if (soundId.intValue() > 0) {
            this.soundPool.play(soundId.intValue(), 1.0f, 1.0f, 1, 0, this.pitchRates[stringIndex.intValue()]);
        }
        ObjectAnimator vibrationAnim = createVibrationAnimation(stringView, stringId);
        vibrationAnim.start();
        this.activeAnimations.put(Integer.valueOf(stringId), vibrationAnim);
    }

    public void playString(int stringIndex, int fret) {
        if (stringIndex < 0 || stringIndex >= STRING_VIEW_IDS.length) {
            return;
        }
        Integer soundId = this.stringSounds.get(Integer.valueOf(STRING_VIEW_IDS[stringIndex]));
        if (soundId != null && soundId.intValue() > 0) {
            this.soundPool.play(soundId.intValue(), 1.0f, 1.0f, 1, 0, calculatePitchRate(fret));
        }
    }

    private ObjectAnimator createVibrationAnimation(final View stringView, final int stringId) {
        float amplitude;
        int duration;
        float damping;
        if (stringId == R.id.string6) {
            amplitude = 4.0f;
            duration = 1800;
            damping = 0.9f;
        } else if (stringId == R.id.string5) {
            amplitude = 3.5f;
            duration = 1700;
            damping = 0.85f;
        } else if (stringId == R.id.string4) {
            amplitude = 3.0f;
            duration = 1600;
            damping = 0.8f;
        } else if (stringId == R.id.string3) {
            amplitude = 2.5f;
            duration = 1500;
            damping = 0.75f;
        } else if (stringId == R.id.string2) {
            amplitude = 2.5f;
            duration = 1400;
            damping = 0.7f;
        } else {
            amplitude = 2.5f;
            duration = 1300;
            damping = 0.65f;
        }
        PropertyValuesHolder translationY = PropertyValuesHolder.ofFloat("translationY", 0.0f, amplitude);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(stringView, translationY);
        float frequency = (6 - (stringId - R.id.string1)) + 8.0f;
        StringVibrationInterpolator interpolator = new StringVibrationInterpolator(frequency, damping);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addListener(new AnimatorListenerAdapter() { // from class: com.example.pocketguitar.util.StringPlayer.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                StringPlayer.this.activeAnimations.remove(Integer.valueOf(stringId));
                stringView.setTranslationY(0.0f);
            }
        });
        return animator;
    }

    private float calculatePitchRate(int fret) {
        return (float) Math.pow(1.0594631433486938d, fret);
    }

    public void release() {
        if (this.soundPool != null) {
            this.soundPool.release();
            this.soundPool = null;
        }
    }

    public void setFingers(int[] fingers) {
        if (fingers != null && fingers.length == 6) {
            this.currentFingers = fingers;
        }
        Log.d("StringPlayer", "setFingers fengtao: currentFingers: " + Arrays.toString(this.currentFingers));
    }
}
