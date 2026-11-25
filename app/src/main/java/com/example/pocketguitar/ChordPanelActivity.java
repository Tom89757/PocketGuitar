package com.example.pocketguitar;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.pocketguitar.model.BarreInfo;
import com.example.pocketguitar.model.ChordFingering;
import com.example.pocketguitar.model.ChordItem;
import com.example.pocketguitar.music.ChordResolver;
import com.example.pocketguitar.music.MelodyNote;
import com.example.pocketguitar.music.NoteMapper;
import com.example.pocketguitar.music.Song;
import com.example.pocketguitar.music.SongPlaybackManager;
import com.example.pocketguitar.music.SongSection;
import com.example.pocketguitar.util.ChordImageManager;
import com.example.pocketguitar.util.ScreenAdapt;
import com.example.pocketguitar.util.SharedChordManager;
import com.example.pocketguitar.util.StringPlayer;
import com.example.pocketguitar.util.TabNavigator;
import com.example.pocketguitar.util.TouchDelegateComposite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes5.dex */
public class ChordPanelActivity extends AppCompatActivity implements SharedChordManager.ChordListChangeListener, SharedChordManager.CurrentPlayChordChangeListener {
    private LinearLayout chordButtonsGroup;
    List<ChordItem> chordList;
    private LinearLayout rhythmButtonsGroup;
    private SharedChordManager sharedChordManager;
    private StringPlayer stringPlayer;
    private TextView tvPlayingNote;
    private final String TAG = "ChordPanelActivity";
    private final List<String> rhythmList = new ArrayList();
    private String currentPlayRoot = "";
    private String currentPlayChordType = "";
    private int[] currentFrets = {0, 0, 0, 0, 0, 0};
    private int[] currentFingers = {0, 0, 0, 0, 0, 0};
    private final int[] chordBackground = {R.drawable.chord1, R.drawable.chord2, R.drawable.chord3, R.drawable.chord4, R.drawable.chord5, R.drawable.chord6, R.drawable.chord7, R.drawable.chord8};
    private final int[][] rhythmBackground = {new int[]{R.drawable.pick1, R.drawable.pick2, R.drawable.pick3, R.drawable.pick4, R.drawable.pick5, R.drawable.pick6, R.drawable.pick7, R.drawable.pick8, R.drawable.strum1, R.drawable.strum2, R.drawable.strum3, R.drawable.strum4}, new int[]{R.drawable.pick1_normal, R.drawable.pick2_normal, R.drawable.pick3_normal, R.drawable.pick4_normal, R.drawable.pick5_normal, R.drawable.pick6_normal, R.drawable.pick7_normal, R.drawable.pick8_normal, R.drawable.strum1_normal, R.drawable.strum2_normal, R.drawable.strum3_normal, R.drawable.strum4_normal}};
    private final int[] stringIds = {R.id.string6, R.id.string5, R.id.string4, R.id.string3, R.id.string2, R.id.string1};
    private int offsetFret = 0;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenAdapt.adapt(this);
        setContentView(R.layout.activity_chord_panel);
        this.sharedChordManager = SharedChordManager.getInstance();
        this.sharedChordManager.initialize(getApplicationContext());
        this.sharedChordManager.addChangeListener(this);
        this.chordList = this.sharedChordManager.getChordList();
        this.sharedChordManager.addPlayChordListener(this);
        this.currentPlayRoot = this.sharedChordManager.getCurrentPlayRoot();
        this.currentPlayChordType = this.sharedChordManager.getCurrentPlayTypeChord();
        this.chordButtonsGroup = (LinearLayout) findViewById(R.id.chord_buttons_group);
        this.tvPlayingNote = (TextView) findViewById(R.id.tv_playing_note);
        try {
            loadChordList();
            createChordButtons();
            this.stringPlayer = new StringPlayer(this);
            setupStringTouchListeners2();
            setupTopNav();
            findViewById(R.id.add_chord).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TabNavigator.switchTo(ChordPanelActivity.this, ChordShowActivity.class,
                            TabNavigator.TAB_CHORD_PANEL, TabNavigator.TAB_LIBRARY);
                }
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupTopNav() {
        findViewById(R.id.nav_chord_panel).setSelected(true);
        findViewById(R.id.nav_fingerstyle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordPanelActivity.this, FingerstyleActivity.class,
                        TabNavigator.TAB_CHORD_PANEL, TabNavigator.TAB_FINGERSTYLE);
            }
        });
        findViewById(R.id.nav_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordPanelActivity.this, ChordShowActivity.class,
                        TabNavigator.TAB_CHORD_PANEL, TabNavigator.TAB_LIBRARY);
            }
        });
        findViewById(R.id.nav_compose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordPanelActivity.this, ComposeActivity.class,
                        TabNavigator.TAB_CHORD_PANEL, TabNavigator.TAB_COMPOSE);
            }
        });
        findViewById(R.id.nav_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }

    private void setupStringTouchListeners() {
        final View parent = findViewById(R.id.strings);
        final TouchDelegateComposite touchDelegateComposite = new TouchDelegateComposite(new Rect(0, 0, 0, 0), parent);
        parent.post(new Runnable() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ChordPanelActivity.this.m62x27976565(touchDelegateComposite, parent);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setupStringTouchListeners$4$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ void m62x27976565(TouchDelegateComposite touchDelegateComposite, View parent) {
        for (final int stringId : this.stringIds) {
            View stringView = findViewById(stringId);
            stringView.setOnTouchListener(new View.OnTouchListener() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda4
                @Override // android.view.View.OnTouchListener
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    return ChordPanelActivity.this.m61x4255f6a4(stringId, view, motionEvent);
                }
            });
            Rect delegateArea = new Rect();
            stringView.getHitRect(delegateArea);
            int extraSpace = (int) (getResources().getDisplayMetrics().density * 40.0f);
            delegateArea.top -= extraSpace / 2;
            delegateArea.bottom += extraSpace / 2;
            TouchDelegate touchDelegate = new TouchDelegate(delegateArea, stringView);
            touchDelegateComposite.addDelegate(touchDelegate);
        }
        parent.setTouchDelegate(touchDelegateComposite);
    }

    /* JADX INFO: renamed from: lambda$setupStringTouchListeners$3$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ boolean m61x4255f6a4(int stringId, View view, MotionEvent event) {
        if (event.getAction() == 0) {
            this.stringPlayer.playString(view, stringId);
            return true;
        }
        return false;
    }

    private void setupStringTouchListeners2() {
        final View parent = findViewById(R.id.strings);
        final TouchDelegateComposite touchDelegateComposite = new TouchDelegateComposite(new Rect(0, 0, 0, 0), parent);
        final Set<Integer> playedStrings = new HashSet<>();
        final int[] currentStringId = {-1};
        final Map<Integer, Rect> stringBounds = new HashMap<>();
        parent.post(new Runnable() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ChordPanelActivity.this.m64x6d29d9d7(stringBounds, touchDelegateComposite, parent, playedStrings, currentStringId);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setupStringTouchListeners2$6$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ void m64x6d29d9d7(final Map stringBounds, TouchDelegateComposite touchDelegateComposite, View parent, final Set playedStrings, final int[] currentStringId) {
        for (int stringId : this.stringIds) {
            View stringView = findViewById(stringId);
            Rect delegateArea = new Rect();
            stringView.getHitRect(delegateArea);
            int extraSpace = (int) (getResources().getDisplayMetrics().density * 40.0f);
            delegateArea.top -= extraSpace / 2;
            delegateArea.bottom += extraSpace / 2;
            stringBounds.put(Integer.valueOf(stringId), new Rect(delegateArea));
            TouchDelegate touchDelegate = new TouchDelegate(delegateArea, stringView);
            touchDelegateComposite.addDelegate(touchDelegate);
        }
        parent.setOnTouchListener(new View.OnTouchListener() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return ChordPanelActivity.this.m63x87e86b16(playedStrings, currentStringId, stringBounds, view, motionEvent);
            }
        });
        parent.setTouchDelegate(touchDelegateComposite);
    }

    /* JADX INFO: renamed from: lambda$setupStringTouchListeners2$5$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ boolean m63x87e86b16(Set playedStrings, int[] currentStringId, Map stringBounds, View v, MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case 0:
                playedStrings.clear();
                currentStringId[0] = -1;
                for (int stringId : this.stringIds) {
                    if (((Rect) Objects.requireNonNull((Rect) stringBounds.get(Integer.valueOf(stringId)))).contains((int) x, (int) y)) {
                        View stringView = findViewById(stringId);
                        if (stringView.isEnabled()) {
                            this.stringPlayer.playString(stringView, stringId);
                            playedStrings.add(Integer.valueOf(stringId));
                            currentStringId[0] = stringId;
                            return true;
                        }
                        return true;
                    }
                }
                return true;
            case 1:
            case 3:
                playedStrings.clear();
                currentStringId[0] = -1;
                return true;
            case 2:
                boolean foundString = false;
                for (int stringId2 : this.stringIds) {
                    if (((Rect) Objects.requireNonNull((Rect) stringBounds.get(Integer.valueOf(stringId2)))).contains((int) x, (int) y)) {
                        foundString = true;
                        if (currentStringId[0] != stringId2) {
                            if (!playedStrings.contains(Integer.valueOf(stringId2))) {
                                View stringView2 = findViewById(stringId2);
                                if (stringView2.isEnabled()) {
                                    this.stringPlayer.playString(stringView2, stringId2);
                                    playedStrings.add(Integer.valueOf(stringId2));
                                }
                            }
                            currentStringId[0] = stringId2;
                        }
                        if (foundString && currentStringId[0] != -1) {
                            currentStringId[0] = -1;
                            playedStrings.clear();
                            return true;
                        }
                    }
                }
                return foundString ? true : true;
            default:
                return false;
        }
    }

    private void loadChordList() throws JSONException {
        for (ChordItem chord : this.chordList) {
            chord.setFretsAndFingers();
        }
    }

    private void loadRhythmList() {
        this.rhythmList.add("");
        this.rhythmList.add("");
        this.rhythmList.add("");
        this.rhythmList.add("");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createChordButtons() {
        this.chordButtonsGroup.removeAllViews();
        for (int i = 0; i < this.chordList.size(); i++) {
            final Button button = new Button(this);
            final String chordName = this.chordList.get(i).getRoot();
            final String chordSuffix = this.chordList.get(i).getType();
            int backgroundIndex = i % this.chordBackground.length;
            button.setId(View.generateViewId());
            if (chordSuffix != null && !chordSuffix.isEmpty()) {
                String formattedRoot = chordName.replace("b", "♭").replace("#", "♯");
                String displayText = formattedRoot + "\n" + chordSuffix;
                SpannableString spannableString = new SpannableString(displayText);
                spannableString.setSpan(new RelativeSizeSpan(1.5f), 0, formattedRoot.length(), 0);
                spannableString.setSpan(new RelativeSizeSpan(0.7f), formattedRoot.length() + 1, displayText.length(), 0);
                button.setText(spannableString);
                button.setLineSpacing(-8.0f, 1.0f);
            } else {
                button.setText(chordName.replace("b", "♭").replace("#", "♯"));
            }
            button.setTextSize(2, 16.0f);
            button.setTextColor(-1);
            button.setGravity(17);
            button.setPadding(0, -8, 0, 12);
            button.setAllCaps(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dp_60), getResources().getDimensionPixelSize(R.dimen.dp_60));
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);
            button.setBackground(getResources().getDrawable(this.chordBackground[backgroundIndex]));
            button.setTag(R.id.tag_background_index, Integer.valueOf(backgroundIndex));
            button.setTag(R.id.tag_selected, false);
            button.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda5
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ChordPanelActivity.this.m56xf94e9d15(button, chordName, chordSuffix, view);
                }
            });
            this.chordButtonsGroup.addView(button);
        }
    }

    /* JADX INFO: renamed from: lambda$createChordButtons$7$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ void m56xf94e9d15(Button button, String chordName, String chordSuffix, View v) {
        boolean wasSelected = ((Boolean) button.getTag(R.id.tag_selected)).booleanValue();
        for (int j = 0; j < this.chordButtonsGroup.getChildCount(); j++) {
            View child = this.chordButtonsGroup.getChildAt(j);
            if (child instanceof Button) {
                Button b = (Button) child;
                int savedIndex = ((Integer) b.getTag(R.id.tag_background_index)).intValue();
                b.setBackground(getResources().getDrawable(this.chordBackground[savedIndex]));
                b.setTag(R.id.tag_selected, false);
            }
        }
        if (!wasSelected) {
            button.setBackground(getResources().getDrawable(R.drawable.chord_press));
            button.setTag(R.id.tag_selected, true);
            handleChordSelection(chordName, chordSuffix);
            return;
        }
        handleChordSelection("", "");
    }

    private void createRhythmButtons() {
        this.rhythmButtonsGroup.removeAllViews();
        for (int i = 0; i < this.rhythmList.size(); i++) {
            final Button button = new Button(this);
            final String rhythmName = this.rhythmList.get(i);
            int backgroundIndex = i % this.rhythmBackground.length;
            button.setId(View.generateViewId());
            button.setText("");
            button.setGravity(17);
            button.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dp_50), getResources().getDimensionPixelSize(R.dimen.dp_50));
            params.setMargins(4, 16, 4, 16);
            button.setLayoutParams(params);
            Drawable[] normalLayers = {getResources().getDrawable(R.drawable.rhythm_normal), getResources().getDrawable(this.rhythmBackground[1][backgroundIndex])};
            LayerDrawable normalState = new LayerDrawable(normalLayers);
            Drawable[] pressedLayers = {getResources().getDrawable(R.drawable.rhythm_press), getResources().getDrawable(this.rhythmBackground[0][backgroundIndex])};
            new LayerDrawable(pressedLayers);
            button.setBackground(normalState);
            button.setTag(R.id.tag_background_index, Integer.valueOf(backgroundIndex));
            button.setTag(R.id.tag_selected, false);
            button.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda9
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ChordPanelActivity.this.m57xf8706d4a(button, rhythmName, view);
                }
            });
            this.rhythmButtonsGroup.addView(button);
        }
    }

    /* JADX INFO: renamed from: lambda$createRhythmButtons$8$com-example-guitardemo-ChordPanelActivity, reason: not valid java name */
    /* synthetic */ void m57xf8706d4a(Button button, String rhythmName, View v) {
        boolean wasSelected = ((Boolean) button.getTag(R.id.tag_selected)).booleanValue();
        int curIndex = ((Integer) button.getTag(R.id.tag_background_index)).intValue();
        int i = 2;
        Drawable[] pressedLayers2 = {getResources().getDrawable(R.drawable.rhythm_press), getResources().getDrawable(this.rhythmBackground[0][curIndex])};
        LayerDrawable pressedState2 = new LayerDrawable(pressedLayers2);
        int j = 0;
        while (j < this.rhythmButtonsGroup.getChildCount()) {
            View child = this.rhythmButtonsGroup.getChildAt(j);
            if (child instanceof Button) {
                Button b = (Button) child;
                int savedIndex = ((Integer) b.getTag(R.id.tag_background_index)).intValue();
                Drawable[] normalLayers2 = new Drawable[i];
                normalLayers2[0] = getResources().getDrawable(R.drawable.rhythm_normal);
                normalLayers2[1] = getResources().getDrawable(this.rhythmBackground[1][savedIndex]);
                LayerDrawable normalState2 = new LayerDrawable(normalLayers2);
                b.setBackground(normalState2);
                b.setTag(R.id.tag_selected, false);
            }
            j++;
            i = 2;
        }
        if (wasSelected) {
            handleRhythmSelection("");
            return;
        }
        button.setBackground(pressedState2);
        button.setTag(R.id.tag_selected, true);
        handleRhythmSelection(rhythmName);
    }

    private void handleChordSelection(String root, String type) {
        Log.d("ChordPanelActivity", "fengtao handleChordSelection: root: " + root + ", type: " + type);
        this.currentPlayRoot = root;
        this.currentPlayChordType = type;
        this.sharedChordManager.setCurrentPlayChord(root, type);
        if (root.isEmpty()) {
            setFrets(new int[]{0, 0, 0, 0, 0, 0});
            setFingers(new int[]{0, 0, 0, 0, 0, 0});
            this.stringPlayer.setFrets(new int[]{0, 0, 0, 0, 0, 0});
            this.stringPlayer.setFingers(new int[]{0, 0, 0, 0, 0, 0});
            this.offsetFret = 0;
            clearChordMarkers(findViewById(R.id.strings));
            return;
        }
        ChordItem currentItem = this.sharedChordManager.findChordItem(root, type);
        setFrets(currentItem.getFrets());
        setFingers(currentItem.getFingers());
        updateOffsetFret();
        updateChordUI();
        addFretNumberToContainer();
        this.stringPlayer.setFrets(currentItem.getFrets());
        this.stringPlayer.setFingers(currentItem.getFingers());
    }

    private void updateOffsetFret() {
        int max = 5;
        int min = 13;
        int[] iArr = this.currentFrets;
        int length = iArr.length;
        for (int i = 0; i < length; i++) {
            int fret = iArr[i];
            max = Math.max(max, fret);
            min = fret > 0 ? Math.min(min, fret) : min;
        }
        this.offsetFret = max > 5 ? Math.max(max - 5, min - 1) : 0;
        Log.d("ChordPanelActivity", "fengtao updateOffsetFret: offsetFret: " + this.offsetFret);
    }

    private void handleRhythmSelection(String rhythm) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        SongPlaybackManager manager = SongPlaybackManager.getInstance();
        int mode = manager.takeMode();
        if (mode != SongPlaybackManager.MODE_CHORDS && mode != SongPlaybackManager.MODE_BOTH) {
            return;
        }
        Song song = manager.song();
        if (song == null) {
            return;
        }
        playComposedChords(song);
        if (mode == SongPlaybackManager.MODE_BOTH) {
            playComposedMelody(song);
        }
    }

    /** 在和弦面板同时演奏旋律：发声并在左上角显示当前音符。 */
    private void playComposedMelody(Song song) {
        if (song.melody == null || song.melody.isEmpty()) {
            return;
        }
        int beatMs = song.bpm > 0 ? 60000 / song.bpm : 500;
        Handler handler = new Handler(Looper.getMainLooper());
        long melodyEndMs = 0;
        for (MelodyNote note : song.melody) {
            if (note == null) {
                continue;
            }
            int midi = NoteMapper.parsePitch(note.pitch);
            if (midi < 0) {
                continue;
            }
            int[] sf = NoteMapper.findStringFret(midi);
            if (sf == null) {
                continue;
            }
            final int str = sf[0];
            final int fret = sf[1];
            final String pitch = note.pitch;
            long delay = (long) ((note.bar - 1) * 4 * beatMs + (note.beat - 1) * beatMs);
            if (delay < 0) {
                delay = 0;
            }
            handler.postDelayed(() -> {
                this.stringPlayer.playString(str, fret);
                showPlayingNote(pitch);
            }, delay);
            melodyEndMs = Math.max(melodyEndMs, delay + (long) (Math.max(note.duration, 1) * beatMs));
        }
        if (melodyEndMs > 0) {
            handler.postDelayed(this::hidePlayingNote, melodyEndMs);
        }
    }

    private void showPlayingNote(String pitch) {
        if (this.tvPlayingNote == null) {
            return;
        }
        String display = pitch == null ? "" : pitch.replace("#", "♯").replace("b", "♭");
        this.tvPlayingNote.setText("♪ " + display);
        this.tvPlayingNote.setVisibility(View.VISIBLE);
    }

    private void hidePlayingNote() {
        if (this.tvPlayingNote != null) {
            this.tvPlayingNote.setVisibility(View.GONE);
        }
    }

    private void playComposedChords(Song song) {
        List<String> names = new ArrayList<>();
        if (song.sections != null) {
            for (SongSection section : song.sections) {
                if (section == null || section.chords == null) {
                    continue;
                }
                for (String chord : section.chords) {
                    if (chord != null && !chord.trim().isEmpty()) {
                        names.add(chord.trim());
                    }
                }
            }
        }
        if (names.isEmpty()) {
            return;
        }
        int beatMs = song.bpm > 0 ? 60000 / song.bpm : 500;
        long chordDuration = beatMs * 4L;
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            handler.postDelayed(() -> playChordByName(name), i * chordDuration);
        }
    }

    private void playChordByName(String name) {
        String[] ref = ChordResolver.resolve(name);
        if (ref == null) {
            return;
        }
        ChordImageManager manager = ChordImageManager.getInstance();
        if (manager == null) {
            return;
        }
        JSONObject json = manager.getJsonChord(ref[0], ref[1]);
        if (json == null) {
            return;
        }
        try {
            ChordFingering fingering = new ChordFingering(
                    json.getString("pattern"), json.getString("fingers"));
            displayAndStrum(fingering);
        } catch (JSONException ignored) {
        }
    }

    private void displayAndStrum(ChordFingering fingering) {
        int[] frets = fingering.getFrets();
        int[] fingers = fingering.getFingers();
        setFrets(frets);
        setFingers(fingers);
        updateOffsetFret();
        updateChordUI();
        addFretNumberToContainer();
        this.stringPlayer.setFrets(frets);
        this.stringPlayer.setFingers(fingers);

        Handler handler = new Handler(Looper.getMainLooper());
        int index = 0;
        for (int s = 0; s < 6; s++) {
            if (frets[s] >= 0) {
                final int str = s;
                final int fret = frets[s];
                handler.postDelayed(() -> this.stringPlayer.playString(str, fret), index * 35L);
                index++;
            }
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.sharedChordManager.removeChangeListener(this);
        this.sharedChordManager.removePlayerChordListener(this);
        if (this.stringPlayer != null) {
            this.stringPlayer.release();
        }
        super.onDestroy();
    }
    @Override // com.example.pocketguitar.util.SharedChordManager.ChordListChangeListener
    public void onChordListChanged(List<ChordItem> chordList) {
        runOnUiThread(new Runnable() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                ChordPanelActivity.this.createChordButtons();
            }
        });
    }

    @Override // com.example.pocketguitar.util.SharedChordManager.CurrentPlayChordChangeListener
    public void onPlayChordChanged(String root, String type) {
        this.currentPlayRoot = root;
        this.currentPlayChordType = type;
    }

    public void setFrets(int[] frets) {
        if (frets != null && frets.length == 6) {
            this.currentFrets = frets;
            for (int i = 0; i < frets.length; i++) {
                View stringView = findViewById(this.stringIds[i]);
                if (stringView != null) {
                    if (frets[i] == -1) {
                        stringView.setEnabled(false);
                    } else {
                        stringView.setEnabled(true);
                    }
                }
            }
        }
    }

    public void setFingers(int[] fingers) {
        if (fingers != null && fingers.length == 6) {
            this.currentFingers = fingers;
        }
    }

    private void updateChordUI() {
        View stringsContainer = findViewById(R.id.strings);
        clearChordMarkers(stringsContainer);
        List<BarreInfo> barres = detectBarres();
        Set<String> processedPositions = new HashSet<>();
        for (BarreInfo barre : barres) {
            addBarreMarker(barre);
            for (int stringIndex = barre.getStartString(); stringIndex <= barre.getEndString(); stringIndex++) {
                if (this.currentFrets[stringIndex] == barre.getFret() && this.currentFingers[stringIndex] == barre.getFinger()) {
                    processedPositions.add(stringIndex + ":" + barre.getFret());
                }
            }
        }
        for (int stringIndex2 = 0; stringIndex2 < 6; stringIndex2++) {
            int fret = this.currentFrets[stringIndex2];
            int finger = this.currentFingers[stringIndex2];
            View stringView = findViewById(this.stringIds[stringIndex2]);
            String position = stringIndex2 + ":" + fret;
            if (fret == -1) {
                addMutedStringMarker(stringView, stringIndex2);
            } else if (fret == 0) {
                addOpenStringMarker(stringView, stringIndex2);
            } else if (finger > 0 && !processedPositions.contains(position)) {
                addFingerMarker(stringView, stringIndex2, fret, finger);
            }
        }
    }

    private void clearChordMarkers(View parent) {
        if (parent instanceof ConstraintLayout) {
            ConstraintLayout container = (ConstraintLayout) parent;
            List<View> viewsToRemove = new ArrayList<>();
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                Object tag = child.getTag();
                if (tag != null && tag.equals("chord_marker")) {
                    viewsToRemove.add(child);
                }
            }
            for (View view : viewsToRemove) {
                container.removeView(view);
            }
        }
    }

    private void addFingerMarker(View stringView, int stringIndex, int fret, int finger) {
        Log.d("ChordPanelActivity", "fengtao addFingerMarker: stringIndex: " + stringIndex + " fret: " + fret + ", finger: " + finger);
        TextView marker = new TextView(this);
        switch (finger) {
            case 1:
                marker.setBackgroundResource(R.drawable.finger1);
                break;
            case 2:
                marker.setBackgroundResource(R.drawable.finger2);
                break;
            case 3:
                marker.setBackgroundResource(R.drawable.finger3);
                break;
            case 4:
                marker.setBackgroundResource(R.drawable.finger4);
                break;
        }
        marker.setTag("chord_marker");
        marker.setGravity(17);
        addMarkerToContainer(marker, stringView, fret, stringIndex);
    }

    private void addOpenStringMarker(View stringView, int stringIndex) {
        Log.d("ChordPanelActivity", "fengtao addOpenStringMarker: stringIndex: " + stringIndex + " O");
        TextView marker = new TextView(this);
        marker.setBackgroundResource(R.drawable.o);
        marker.setTag("chord_marker");
        marker.setGravity(17);
        addMarkerToContainer(marker, stringView, 0, stringIndex);
    }

    private void addMutedStringMarker(View stringView, int stringIndex) {
        Log.d("ChordPanelActivity", "fengtao addMutedStringMarker: stringIndex: " + stringIndex);
        TextView marker = new TextView(this);
        marker.setBackgroundResource(R.drawable.x);
        marker.setTag("chord_marker");
        marker.setGravity(17);
        addMarkerToContainer(marker, stringView, 0, stringIndex);
    }

    private void addBarreMarker(BarreInfo barre) {
        Log.d("ChordPanelActivity", "fengtao addBarreMarker: " + barre.getFinger() + ", fret: " + barre.getFret() + ", start: " + barre.getStartString() + ", end: " + barre.getEndString());
        addBarreLine(barre);
        View startStringView = findViewById(this.stringIds[barre.getStartString()]);
        View endStringView = findViewById(this.stringIds[barre.getEndString()]);
        addFingerMarker(startStringView, barre.getStartString(), barre.getFret(), barre.getFinger());
        if (barre.getStartString() != barre.getEndString()) {
            addFingerMarker(endStringView, barre.getEndString(), barre.getFret(), barre.getFinger());
        }
    }

    private void addBarreLine(BarreInfo barre) {
        int backgroundResId;
        int fretGuidelineId;
        ConstraintLayout container = (ConstraintLayout) findViewById(R.id.strings);
        switch (barre.getFinger()) {
            case 1:
                backgroundResId = R.drawable.barre1;
                break;
            case 2:
                backgroundResId = R.drawable.barre2;
                break;
            case 3:
                backgroundResId = R.drawable.barre3;
                break;
            case 4:
                backgroundResId = R.drawable.barre4;
                break;
            default:
                backgroundResId = R.drawable.barre1;
                break;
        }
        View barreLine = new View(this);
        barreLine.setBackgroundResource(backgroundResId);
        barreLine.setTag("chord_marker");
        int barreWidth = (int) (getResources().getDisplayMetrics().density * 30.0f);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(barreWidth, 0);
        params.topToTop = this.stringIds[barre.getStartString()];
        params.bottomToBottom = this.stringIds[barre.getEndString()];
        if (barre.getFret() == 0) {
            fretGuidelineId = R.id.guideline_fret0;
        } else {
            fretGuidelineId = getResources().getIdentifier("guideline_fret" + (barre.getFret() - this.offsetFret), "id", getPackageName());
        }
        params.startToStart = fretGuidelineId;
        params.leftMargin = (-barreWidth) / 2;
        params.bottomMargin = barreWidth / 6;
        barreLine.setLayoutParams(params);
        container.addView(barreLine);
    }

    private void addMarkerToContainer(TextView marker, View stringView, int fret, int stringIndex) {
        int fretGuidelineId;
        Log.d("ChordPanelActivity", "fengtao addMarkerToContainer: stringIndex: " + stringIndex + " fret: " + fret);
        ConstraintLayout container = (ConstraintLayout) findViewById(R.id.strings);
        int markerSize = (int) (getResources().getDisplayMetrics().density * 30.0f);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(markerSize, markerSize);
        params.topToTop = this.stringIds[stringIndex];
        params.topMargin = (int) (((double) (-markerSize)) / 2.4d);
        if (fret == 0) {
            fretGuidelineId = R.id.guideline_fret0;
        } else {
            fretGuidelineId = getResources().getIdentifier("guideline_fret" + (fret - this.offsetFret), "id", getPackageName());
        }
        params.startToStart = fretGuidelineId;
        params.leftMargin = (-markerSize) / 2;
        marker.setLayoutParams(params);
        container.addView(marker);
    }

    private void addFretNumberToContainer() {
        ConstraintLayout container = (ConstraintLayout) findViewById(R.id.strings);
        int markerSize = (int) (getResources().getDisplayMetrics().density * 30.0f);
        for (int i = 1; i < 6; i++) {
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(markerSize, markerSize);
            TextView marker = new TextView(this);
            marker.setText(String.valueOf(this.offsetFret + i));
            marker.setTextColor(-1);
            marker.setTextSize(2, 18.0f);
            marker.setTag("chord_marker");
            marker.setGravity(17);
            params.topToTop = R.id.guideline_fret_number;
            int fretGuidelineId = getResources().getIdentifier("guideline_fret" + i, "id", getPackageName());
            params.startToStart = fretGuidelineId;
            params.leftMargin = (int) (((double) (-markerSize)) * 0.6d);
            marker.setLayoutParams(params);
            container.addView(marker);
        }
    }

    private List<BarreInfo> detectBarres() {
        List<BarreInfo> barres = new ArrayList<>();
        Map<Integer, Map<Integer, List<Integer>>> fingerFretMap = new HashMap<>();
        for (int stringIndex = 0; stringIndex < 6; stringIndex++) {
            int fret = this.currentFrets[stringIndex];
            int finger = this.currentFingers[stringIndex];
            if (fret > 0 && finger > 0) {
                Map<Integer, List<Integer>> fretMap = fingerFretMap.computeIfAbsent(Integer.valueOf(finger), new Function() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda11
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ChordPanelActivity.lambda$detectBarres$9((Integer) obj);
                    }
                });
                fretMap.computeIfAbsent(Integer.valueOf(fret), new Function() { // from class: com.example.pocketguitar.ChordPanelActivity$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ChordPanelActivity.lambda$detectBarres$10((Integer) obj);
                    }
                }).add(Integer.valueOf(stringIndex));
            }
        }
        for (Map.Entry<Integer, Map<Integer, List<Integer>>> fingerEntry : fingerFretMap.entrySet()) {
            int finger2 = fingerEntry.getKey().intValue();
            Map<Integer, List<Integer>> fretMap2 = fingerEntry.getValue();
            for (Map.Entry<Integer, List<Integer>> fretEntry : fretMap2.entrySet()) {
                int fret2 = fretEntry.getKey().intValue();
                List<Integer> stringList = fretEntry.getValue();
                if (stringList.size() > 1) {
                    int minString = ((Integer) Collections.min(stringList)).intValue();
                    int maxString = ((Integer) Collections.max(stringList)).intValue();
                    barres.add(new BarreInfo(finger2, fret2, minString, maxString));
                }
            }
        }
        return barres;
    }

    static /* synthetic */ Map lambda$detectBarres$9(Integer k) {
        return new HashMap();
    }

    static /* synthetic */ List lambda$detectBarres$10(Integer k) {
        return new ArrayList();
    }
}
